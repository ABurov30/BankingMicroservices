package accountservice.service;

import accountservice.dto.*;
import accountservice.entity.*;
import accountservice.exception.AccountAlreadyFrozenException;
import accountservice.exception.AccountClosedException;
import accountservice.exception.AccountGenerationFailedException;
import accountservice.exception.AccountHoldInvalidStatusException;
import accountservice.exception.AccountHoldNotFoundException;
import accountservice.exception.AccountNotFoundException;
import accountservice.exception.AccountNotFrozenException;
import accountservice.exception.AccountOwnershipException;
import accountservice.exception.AccountsNotFoundException;
import accountservice.exception.FundsTransferFailedException;
import accountservice.exception.InsufficientFundsException;
import accountservice.exception.TransactionAlreadyProcessedException;
import accountservice.mapper.command.AccountCommandMapper;
import accountservice.mapper.command.TransferCommandMapper;
import accountservice.mapper.result.AccountResultMapper;
import accountservice.repository.AccountHoldRepository;
import accountservice.repository.AccountOutboxEventRepository;
import accountservice.repository.AccountRepository;
import accountservice.repository.CurrencyRepository;
import enums.account.AccountCurrency;
import enums.account.AccountStatus;
import enums.account.ReservationStatus;
import jakarta.transaction.Transactional;
import kafkacontracts.account.AccountEventType;
import kafkacontracts.transaction.TransactionEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private static final int TRY_TO_GENERATE_ACCOUNT_NUMBER = 10;
    private final CurrencyRepository currencyRepository;
    private final AccountResultMapper resultMapper;
    private final AccountHoldRepository accountHoldRepository;
    private final TransferService transferService;
    private final TransferCommandMapper transferCommandMapper;
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final AccountOutboxService accountOutboxService;

    public AccountService(
            AccountRepository accountRepository,
            AccountResultMapper resultMapper,
            CurrencyRepository currencyRepository,
            AccountHoldRepository accountHoldRepository,
            TransferCommandMapper transferCommandMapper,
            AccountCommandMapper accountCommandMapper,
            AccountOutboxService accountOutboxService,
            TransferService transferService
    ) {
        this.accountRepository = accountRepository;
        this.resultMapper = resultMapper;
        this.currencyRepository = currencyRepository;
        this.accountHoldRepository = accountHoldRepository;
        this.transferCommandMapper = transferCommandMapper;
        this.accountOutboxService = accountOutboxService;
        this.transferService =transferService;
    }

    private String generateUniqueAccountNumber() {
        for (int i = 0; i < TRY_TO_GENERATE_ACCOUNT_NUMBER; i++) {
            String accNumber = generateAccountNumber();

            if (!accountRepository.existsByAccountNumber(accNumber)) {
                return accNumber;
            }
        }

        throw new AccountGenerationFailedException("Failed to generate unique account number");
    }

    private String generateAccountNumber() {
        return String.valueOf(1000000000000000L + ThreadLocalRandom.current().nextLong(9000000000000000L));
    }


    private AccountEntity tryToCreateAccount(CreateAccountCommand createAccountCommand) {
        CurrencyEntity currency = currencyRepository.findByName(createAccountCommand.currency());
        for (int i = 0; i < TRY_TO_GENERATE_ACCOUNT_NUMBER; i++) {
            try {
                AccountEntity accountEntity = new AccountEntity();
                accountEntity.setAccountType(createAccountCommand.type());
                accountEntity.setAccountStatus(AccountStatus.ACTIVE);
                accountEntity.setCurrency(currency);
                accountEntity.setOwnerUserId(createAccountCommand.userId());
                accountEntity.setOwnerAuthUserId(createAccountCommand.authUserId());
                accountEntity.setAccountNumber(generateUniqueAccountNumber());
                accountEntity.setAvailableBalance(BigDecimal.ZERO);
                accountEntity.setReservedBalance(BigDecimal.ZERO);
                accountRepository.save(accountEntity);
                return accountEntity;
            } catch (DataIntegrityViolationException e) {
                log.warn("Account-number collision; retrying account creation: attempt={}", i + 1, e);
            }
        }
        throw new AccountGenerationFailedException("Failed to create account with unique account number");
    }

    @Transactional
    public CreateAccountResult createAccount(CreateAccountCommand createAccountCommand) {
        AccountEntity accountEntity = tryToCreateAccount(createAccountCommand);

        Map<String, Object> payload = Map.of(
                "accountId", accountEntity.getId(),
                "authUserId", accountEntity.getOwnerAuthUserId(),
                "accountNumber", accountEntity.getAccountNumber()
        );

        accountOutboxService.saveAccountOutboxEvent(accountEntity.getId(), AccountEventType.ACCOUNT_CREATED, payload);

        return new CreateAccountResult(
                accountEntity.getId(),
                accountEntity.getOwnerUserId(),
                accountEntity.getOwnerAuthUserId(),
                accountEntity.getAccountNumber(),
                accountEntity.getAccountType(),
                accountEntity.getAccountStatus(),
                accountEntity.getAvailableBalance(),
                accountEntity.getReservedBalance(),
                accountEntity.getCurrency().getName()
        );
    }

    public List<GetAccountResult> getAccountsByOwnerUserId(GetAccountsByOwnerUserIdCommand command) {

        List<AccountEntity> accountEntityList = accountRepository.findByOwnerUserId(command.ownerUserId())
                .orElseThrow(() -> new AccountsNotFoundException(command.ownerUserId()));

        return accountEntityList.stream()
                .map(resultMapper::toGetAccountResult)
                .toList();
    }

    public List<GetAccountResult> getAllAccounts() {
        List<AccountEntity> accountEntityList = accountRepository.findAll();
        return accountEntityList.stream()
                .map(resultMapper::toGetAccountResult)
                .toList();
    }

    @Transactional
    public void freezeAccountByUserId(FreezeAccountsByUserIdCommand command) {
        List<AccountEntity> accountEntityList = accountRepository.findAllByOwnerUserIdForUpdate(command.userId())
                .orElseThrow(() -> new AccountsNotFoundException(command.userId()));

        accountEntityList.forEach((account) -> {
            FreezeAccountCommand freezeAccountCommand = new FreezeAccountCommand(account.getId(), null, null);
            this.freezeAccount(freezeAccountCommand);
        });
    }

    @Transactional
    public void freezeAccount(FreezeAccountCommand command) {
        AccountEntity account = accountRepository.findByIdForUpdate(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException(command.accountId()));

        if (!canAccessAccount(account, command)) {
            throw new AccountNotFoundException(command.accountId());
        }

        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException(account.getId());
        }

        if (account.getAccountStatus() == AccountStatus.FROZEN) {
            throw new AccountAlreadyFrozenException(account.getId());
        }

        account.setAccountStatus(AccountStatus.FROZEN);
        accountRepository.save(account);

        Map<String, Object> payload = Map.of(
                "accountId", account.getId(),
                "authUserId", account.getOwnerAuthUserId(),
                "accountNumber", account.getAccountNumber()
        );

        accountOutboxService.saveAccountOutboxEvent(account.getId(), AccountEventType.ACCOUNT_FROZEN, payload);
    }

    @Transactional
    public void unfreezeAccount(UnfreezeAccountCommand command) {
        AccountEntity account = accountRepository.findByIdForUpdate(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException(command.accountId()));

        if (!canAccessAccount(account, command.authUserId(), command.role())) {
            throw new AccountNotFoundException(command.accountId());
        }

        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException(account.getId());
        }

        if (account.getAccountStatus() != AccountStatus.FROZEN) {
            throw new AccountNotFrozenException(account.getId());
        }

        account.setAccountStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);


        Map<String, Object> payload = Map.of(
                "accountId", account.getId(),
                "authUserId", account.getOwnerAuthUserId(),
                "accountNumber", account.getAccountNumber()
        );

        accountOutboxService.saveAccountOutboxEvent(account.getId(), AccountEventType.ACCOUNT_UNFROZEN, payload);
    }


    private boolean canAccessAccount(AccountEntity account, FreezeAccountCommand command) {
        return canAccessAccount(account, command.authUserId(), command.role());
    }

    private boolean canAccessAccount(AccountEntity account, java.util.UUID authUserId, String role) {
        if (authUserId == null) {
            return true;
        }

        if (isPrivileged(role)) {
            return true;
        }

        return account.getOwnerAuthUserId().equals(authUserId);
    }

    private boolean isPrivileged(String role) {
        return "ADMIN".equals(role) || "MANAGER".equals(role);
    }

    public GetAccountResult getAccountById(GetAccountByIdCommand command) {
        AccountEntity account = accountRepository.findById(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException(command.accountId()));

        return resultMapper.toGetAccountResult(account);
    }

    @Transactional
    public GetAccountResult topUpAccount(UpdateAccountBalanceCommand command) {
        AccountEntity account = accountRepository.findByIdForUpdate(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException(command.accountId()));

        if (!account.getOwnerAuthUserId().equals(command.authUserId())) {
            throw new AccountOwnershipException();
        }

        account.setAvailableBalance(account.getAvailableBalance().add(command.amount()));

        accountRepository.save(account);
        return resultMapper.toGetAccountResult(account);
    }

    @Transactional
    public GetAccountResult withdrawAccount(UpdateAccountBalanceCommand command) {
        AccountEntity account = accountRepository.findByIdForUpdate(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException(command.accountId()));

        if (!account.getOwnerAuthUserId().equals(command.authUserId())) {
            throw new AccountOwnershipException();
        }

        if (account.getAvailableBalance().compareTo(command.amount()) < 0) {
            throw new InsufficientFundsException(account.getId());
        }

        account.setAvailableBalance(account.getAvailableBalance().subtract(command.amount()));

        accountRepository.save(account);
        return resultMapper.toGetAccountResult(account);
    }

    public void transactionFundsRequest(TransactionFundsRequestCommand command) {
        var accountHold = accountHoldRepository.findByTransactionId(command.transactionId())
                .orElseThrow(() -> new AccountHoldNotFoundException(command.transactionId()));

        try {
            transferService.executeFundsTransfer(transferCommandMapper.toExecuteFundsTransferCommand(command, accountHold));
        } catch (Exception ex) {
            log.error("Transfer failed; starting compensation: transactionId={}",
                    command.transactionId(), ex);

            try {
                transferService.compensateFunds(
                        transferCommandMapper.toCompensationFundsCommand(accountHold)
                );
            } catch (Exception compensationEx) {
                log.error("Compensation also failed: transactionId={}",
                        command.transactionId(), compensationEx);
                ex.addSuppressed(compensationEx);
            }
        }
    }


}
