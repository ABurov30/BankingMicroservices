package accountservice.service;

import accountservice.dto.*;
import accountservice.entity.AccountEntity;
import accountservice.entity.AccountOutboxEventEntity;
import accountservice.entity.CurrencyEntity;
import accountservice.exception.AccountAlreadyFrozenException;
import accountservice.exception.AccountClosedException;
import accountservice.exception.AccountGenerationFailedException;
import accountservice.exception.AccountNotFoundException;
import accountservice.exception.AccountNotFrozenException;
import accountservice.exception.AccountsNotFoundException;
import accountservice.mapper.result.AccountResultMapper;
import accountservice.repository.AccountOutboxEventRepository;
import accountservice.repository.AccountRepository;
import accountservice.repository.CurrencyRepository;
import enums.account.AccountStatus;
import jakarta.transaction.Transactional;
import kafkacontracts.account.AccountEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountOutboxEventRepository accountOutboxEventRepository;
    private static final int TRY_TO_GENERATE_ACCOUNT_NUMBER = 10;
    private final CurrencyRepository currencyRepository;
    private final AccountResultMapper resultMapper;
    private static final Logger log = LoggerFactory.getLogger(CurrencyScheduler.class);

    public AccountService(
            AccountRepository accountRepository,
            AccountOutboxEventRepository accountOutboxEventRepository,
            AccountResultMapper resultMapper,
            CurrencyRepository currencyRepository
    ) {
        this.accountRepository = accountRepository;
        this.accountOutboxEventRepository = accountOutboxEventRepository;
        this.resultMapper = resultMapper;
        this.currencyRepository = currencyRepository;
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
                log.error("account_number collision, retry " + e.getMessage());
            }
        }
        throw new AccountGenerationFailedException("Failed to create account with unique account number");
    }

    @Transactional
    public CreateAccountResult createAccount(CreateAccountCommand createAccountCommand) {
        AccountEntity accountEntity = tryToCreateAccount(createAccountCommand);

        AccountOutboxEventEntity accountOutboxEventEntity = new AccountOutboxEventEntity();
        accountOutboxEventEntity.setAggregateType("ACCOUNT_TYPE");
        accountOutboxEventEntity.setAggregateId(accountEntity.getId());
        accountOutboxEventEntity.setEventType(AccountEventType.ACCOUNT_CREATED.name());
        accountOutboxEventEntity.setTopic(AccountEventType.ACCOUNT_CREATED.getTopic());
        accountOutboxEventEntity.setEventKey(accountEntity.getId() + ":" + AccountEventType.ACCOUNT_CREATED.name());
        accountOutboxEventEntity.setSchemaVersion(AccountEventType.ACCOUNT_CREATED.getVersion());

        accountOutboxEventEntity.setPayload(Map.of(
                "accountId", accountEntity.getId(),
                "authUserId", accountEntity.getOwnerAuthUserId(),
                "accountNumber", accountEntity.getAccountNumber()
        ));

        accountOutboxEventRepository.save(accountOutboxEventEntity);

        return new CreateAccountResult(
                accountEntity.getId(),
                accountEntity.getOwnerUserId(),
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
        return  accountEntityList.stream()
                .map(resultMapper::toGetAccountResult)
                .toList();
    }

    @Transactional
    public void freezeAccountByUserId(FreezeAccountsByUserIdCommand command) {
        List<AccountEntity> accountEntityList = accountRepository.findAllByOwnerUserId(command.userId())
                .orElseThrow(() ->new AccountsNotFoundException(command.userId()));

        accountEntityList.forEach((account) -> {
            FreezeAccountCommand freezeAccountCommand = new FreezeAccountCommand(account.getId(), null, null);
            this.freezeAccount(freezeAccountCommand);
        });
    }

    @Transactional
    public void freezeAccount(FreezeAccountCommand command) {
        AccountEntity account = accountRepository.findById(command.accountId())
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

        AccountOutboxEventEntity accountOutboxEventEntity = new AccountOutboxEventEntity();
        accountOutboxEventEntity.setAggregateType("ACCOUNT_TYPE");
        accountOutboxEventEntity.setAggregateId(account.getId());
        accountOutboxEventEntity.setEventType(AccountEventType.ACCOUNT_FROZEN.name());
        accountOutboxEventEntity.setTopic(AccountEventType.ACCOUNT_FROZEN.getTopic());
        accountOutboxEventEntity.setEventKey(account.getId() + ":" + AccountEventType.ACCOUNT_FROZEN.name());
        accountOutboxEventEntity.setSchemaVersion(AccountEventType.ACCOUNT_FROZEN.getVersion());

        accountOutboxEventEntity.setPayload(Map.of(
                "accountId", account.getId(),
                "authUserId", account.getOwnerAuthUserId(),
                "accountNumber", account.getAccountNumber()
        ));

        accountOutboxEventRepository.save(accountOutboxEventEntity);
    }

    @Transactional
    public void unfreezeAccount(UnfreezeAccountCommand command) {
        AccountEntity account = accountRepository.findById(command.accountId())
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

        AccountOutboxEventEntity accountOutboxEventEntity = new AccountOutboxEventEntity();
        accountOutboxEventEntity.setAggregateType("ACCOUNT_TYPE");
        accountOutboxEventEntity.setAggregateId(account.getId());
        accountOutboxEventEntity.setEventType(AccountEventType.ACCOUNT_UNFROZEN.name());
        accountOutboxEventEntity.setTopic(AccountEventType.ACCOUNT_UNFROZEN.getTopic());
        accountOutboxEventEntity.setEventKey(account.getId() + ":" + AccountEventType.ACCOUNT_UNFROZEN.name());
        accountOutboxEventEntity.setSchemaVersion(AccountEventType.ACCOUNT_UNFROZEN.getVersion());

        accountOutboxEventEntity.setPayload(Map.of(
                "accountId", account.getId(),
                "authUserId", account.getOwnerAuthUserId(),
                "accountNumber", account.getAccountNumber()
        ));

        accountOutboxEventRepository.save(accountOutboxEventEntity);
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
}
