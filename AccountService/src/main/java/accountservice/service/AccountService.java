package accountservice.service;

import accountservice.dto.*;
import accountservice.entity.AccountEntity;
import accountservice.entity.AccountOutboxEventEntity;
import accountservice.exception.AccountAlreadyFrozenException;
import accountservice.exception.AccountClosedException;
import accountservice.exception.AccountGenerationFailedException;
import accountservice.exception.AccountNotFoundException;
import accountservice.exception.AccountsNotFoundException;
import accountservice.mapper.AccountMapper;
import accountservice.repository.AccountOutboxEventRepository;
import accountservice.repository.AccountRepository;
import enums.account.AccountStatus;
import jakarta.transaction.Transactional;
import kafkacontracts.account.AccountEventType;
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
    private final AccountMapper accountMapper;

    public AccountService(
            AccountRepository accountRepository,
            AccountOutboxEventRepository accountOutboxEventRepository,
            AccountMapper accountMapper
    ) {
        this.accountRepository = accountRepository;
        this.accountOutboxEventRepository = accountOutboxEventRepository;
        this.accountMapper = accountMapper;
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
        for (int i = 0; i < TRY_TO_GENERATE_ACCOUNT_NUMBER; i++) {
            try {
                AccountEntity accountEntity = new AccountEntity();
                accountEntity.setAccountType(createAccountCommand.type());
                accountEntity.setAccountStatus(AccountStatus.ACTIVE);
                accountEntity.setCurrency(createAccountCommand.currency());
                accountEntity.setOwnerUserId(createAccountCommand.userId());
                accountEntity.setOwnerAuthUserId(createAccountCommand.authUserId());
                accountEntity.setAccountNumber(generateUniqueAccountNumber());
                accountEntity.setAvailableBalance(BigDecimal.ZERO);
                accountEntity.setReservedBalance(BigDecimal.ZERO);
                accountRepository.save(accountEntity);
                return accountEntity;
            } catch (DataIntegrityViolationException e) {
                // account_number collision, retry
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
        accountOutboxEventEntity.setEventKey(accountEntity.getId().toString());
        accountOutboxEventEntity.setSchemaVersion(AccountEventType.ACCOUNT_CREATED.getVersion());

        accountOutboxEventEntity.setPayload(Map.of(
                "accountId", accountEntity.getId(),
                "authUserId", accountEntity.getOwnerAuthUserId()
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
                accountEntity.getCurrency()
        );
    }

    public List<GetAccountResult> getAccountsByOwnerUserId(GetAccountsByOwnerUserIdCommand command) {

        List<AccountEntity> accountEntityList = accountRepository.findByOwnerUserId(command.ownerUserId())
                .orElseThrow(() -> new AccountsNotFoundException(command.ownerUserId()));

        return accountEntityList.stream()
                .map(accountMapper::toGetAccountResult)
                .toList();
    }

    public List<GetAccountResult> getAllAccounts() {
        List<AccountEntity> accountEntityList = accountRepository.findAll();
        return  accountEntityList.stream()
                .map(accountMapper::toGetAccountResult)
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

        AccountOutboxEventEntity accountOutboxEventEntity = new AccountOutboxEventEntity();
        accountOutboxEventEntity.setAggregateType("ACCOUNT_TYPE");
        accountOutboxEventEntity.setAggregateId(account.getId());
        accountOutboxEventEntity.setEventType(AccountEventType.ACCOUNT_FROZEN.name());
        accountOutboxEventEntity.setTopic(AccountEventType.ACCOUNT_FROZEN.getTopic());
        accountOutboxEventEntity.setEventKey(account.getId().toString());
        accountOutboxEventEntity.setSchemaVersion(AccountEventType.ACCOUNT_FROZEN.getVersion());

        accountOutboxEventEntity.setPayload(Map.of("accountId", account.getId()));

        accountOutboxEventRepository.save(accountOutboxEventEntity);
    }

    private boolean canAccessAccount(AccountEntity account, FreezeAccountCommand command) {
        if (command.authUserId() == null) {
            return true;
        }

        if (isPrivileged(command.role())) {
            return true;
        }

        return account.getOwnerAuthUserId().equals(command.authUserId());
    }

    private boolean isPrivileged(String role) {
        return "ADMIN".equals(role) || "MANAGER".equals(role);
    }
}
