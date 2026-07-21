package accountservice.service;

import accountservice.dto.CreateAccountCommand;
import accountservice.dto.CreateAccountResult;
import accountservice.entity.AccountEntity;
import accountservice.entity.AccountOutboxEventEntity;
import accountservice.repository.AccountOutboxEventRepository;
import accountservice.repository.AccountRepository;
import enums.account.AccountStatus;
import jakarta.transaction.Transactional;
import kafkacontracts.account.AccountCreatedEventPayload;
import kafkacontracts.account.AccountEventType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountOutboxEventRepository accountOutboxEventRepository;
    private static final int TRY_TO_GENERATE_ACCOUNT_NUMBER = 10;
    private final ObjectMapper objectMapper;

    public AccountService(
            AccountRepository accountRepository,
            AccountOutboxEventRepository accountOutboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.accountRepository = accountRepository;
        this.accountOutboxEventRepository = accountOutboxEventRepository;
        this.objectMapper = objectMapper;
    }

    private String generateUniqueAccountNumber() {
        for (int i = 0; i < TRY_TO_GENERATE_ACCOUNT_NUMBER; i++) {
            String accNumber = generateAccountNumber();

            if (!accountRepository.existsByAccountNumber(accNumber)) {
                return accNumber;
            }
        }

        throw new IllegalStateException("Failed to generate unique account number");
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
                accountEntity.setAccountNumber(generateUniqueAccountNumber());
                accountEntity.setAvailableBalance(BigDecimal.ZERO);
                accountEntity.setReservedBalance(BigDecimal.ZERO);
                accountRepository.save(accountEntity);
                return accountEntity;
            } catch (DataIntegrityViolationException e) {
                // account_number collision, retry
            }
        }
        throw new IllegalStateException("Failed to create account with unique account number");
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

        AccountCreatedEventPayload accountCreatedEventPayload = new AccountCreatedEventPayload(
                accountEntity.getId()
        );

        Map<String, Object> payload = objectMapper.convertValue(
                accountCreatedEventPayload,
                new TypeReference<Map<String, Object>>() {
                }
        );

        accountOutboxEventEntity.setPayload(payload);

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
}
