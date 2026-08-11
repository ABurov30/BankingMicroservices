package accountservice.service;

import accountservice.dto.CompensationFundsCommand;
import accountservice.dto.ExecuteFundsTransferCommand;
import accountservice.dto.ReserveFundsForTransactionCommand;
import accountservice.dto.ReserveFundsForTransactionResult;
import accountservice.entity.AccountHoldEntity;
import accountservice.exception.*;
import accountservice.mapper.result.AccountResultMapper;
import accountservice.repository.AccountHoldRepository;
import accountservice.repository.AccountRepository;
import enums.account.AccountCurrency;
import enums.account.ReservationStatus;
import jakarta.transaction.Transactional;
import kafkacontracts.account.AccountEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TransferService {
    private final AccountHoldRepository accountHoldRepository;
    private final AccountRepository accountRepository;
    private final CurrencyService currencyService;
    private final AccountOutboxService accountOutboxService;
    private final AccountResultMapper resultMapper;
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final long HOLD_TTL_MINUTES = 5;

    public TransferService (
            AccountHoldRepository accountHoldRepository,
            AccountRepository accountRepository,
            CurrencyService currencyService,
            AccountOutboxService accountOutboxService,
            AccountResultMapper accountResultMapper
    ) {
        this.accountHoldRepository = accountHoldRepository;
        this.accountRepository = accountRepository;
        this.currencyService  = currencyService;
        this.accountOutboxService = accountOutboxService;
        this.resultMapper = accountResultMapper;
    }

    private BigDecimal convertAmountForTransactionToTagetCurrency(BigDecimal amount, AccountCurrency sourceCurrency, AccountCurrency targetCurrency) {
        BigDecimal amountInUSD = currencyService.convertToUSD(amount, sourceCurrency);
        return currencyService.convertFromUSD(amountInUSD, targetCurrency);
    }

    @Transactional
    public void executeFundsTransfer(ExecuteFundsTransferCommand command) {
        var accountHold = accountHoldRepository.findByIdForUpdate(command.accountHold().getId())
                .orElseThrow(() -> new AccountHoldNotFoundException(command.accountHold().getId()));

        if (accountHold.getStatus() != ReservationStatus.RESERVED) {
            throw new AccountHoldInvalidStatusException(accountHold.getStatus());
        }

        var targetAccount = accountRepository.findByIdForUpdate(command.targetAccountId())
                .orElseThrow(() -> new AccountNotFoundException(command.targetAccountId()));
        var sourceAccount = accountRepository.findByIdForUpdate(accountHold.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException(accountHold.getAccountId()));

        var sourceCurrency = sourceAccount.getCurrency().getName();
        var targetCurrency = targetAccount.getCurrency().getName();

        targetAccount.setAvailableBalance(
                targetAccount.getAvailableBalance().add(
                        convertAmountForTransactionToTagetCurrency(
                                accountHold.getAmount(),
                                sourceCurrency,
                                targetCurrency
                        )
                ));

        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(accountHold.getAmount()));
        sourceAccount.setReservedBalance(
                sourceAccount.getReservedBalance().subtract(accountHold.getAmount())
        );

        accountHold.setStatus(ReservationStatus.RELEASED);
        accountHold.setReleasedAt(LocalDateTime.now());

        accountRepository.save(targetAccount);
        accountRepository.save(sourceAccount);
        accountHoldRepository.save(accountHold);

        Map<String, Object> payload = Map.of(
                "accountNumber", targetAccount.getAccountNumber(),
                "amount", accountHold.getAmount(),
                "authUserId", command.authUserId(),
                "transactionId", accountHold.getTransactionId()
        );

        accountOutboxService.saveAccountOutboxEvent(accountHold.getTransactionId(), AccountEventType.TRANSACTION_COMPLETED, payload);
    }

    @Transactional
    public ReserveFundsForTransactionResult reserveFundsForTransactional(ReserveFundsForTransactionCommand command) {
        try {
            var sourceAccount = accountRepository.findByIdForUpdate(command.sourceAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(command.sourceAccountId()));

            var targetAccount = accountRepository.findById(command.sourceAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(command.targetAccountId()));

            if (accountHoldRepository.existsByTransactionId(command.transactionId())) {
                throw new TransactionAlreadyProcessedException(command.transactionId());
            }

            BigDecimal availableBalanceWithoutReservedBalance = sourceAccount.getAvailableBalance().subtract(sourceAccount.getReservedBalance());

            if (availableBalanceWithoutReservedBalance.compareTo(command.amount()) < 0) {
                throw new InsufficientFundsException(sourceAccount.getId());
            }

            var accountHold = new AccountHoldEntity();
            accountHold.setAccountId(sourceAccount.getId());
            accountHold.setTransactionId(command.transactionId());
            accountHold.setCurrency(sourceAccount.getCurrency().getName());
            accountHold.setAmount(
                    command.amount()
            );
            accountHold.setStatus(ReservationStatus.RESERVED);
            accountHold.setExpiresAt(LocalDateTime.now().plusMinutes(HOLD_TTL_MINUTES));
            accountHoldRepository.save(accountHold);

            sourceAccount.setReservedBalance(sourceAccount.getReservedBalance().add(command.amount()));
            accountRepository.save(sourceAccount);

            return new ReserveFundsForTransactionResult(
                    resultMapper.toGetAccountResult(sourceAccount),
                    resultMapper.toGetAccountResult(targetAccount),
                    ReservationStatus.RESERVED,
                    "Funds reserved for transaction " + command.transactionId());
        } catch (Exception e) {
            log.error("Failed to reserve funds: transactionId={}", command.transactionId(), e);
            return new ReserveFundsForTransactionResult(
                    null,
                    null,
                    ReservationStatus.FAILED,
                    e.getMessage()
            );
        }
    }

    @Transactional
    public void compensateFunds(CompensationFundsCommand command) {
        var accountHold = accountHoldRepository.findByIdForUpdate(command.accountHoldId())
                .orElseThrow(() -> new AccountHoldNotFoundException(command.accountHoldId()));

        var sourceAccount = accountRepository.findByIdForUpdate(accountHold.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException(accountHold.getAccountId()));

        sourceAccount.setReservedBalance(sourceAccount.getReservedBalance().subtract(accountHold.getAmount()));
        accountHold.setStatus(ReservationStatus.COMPENSATED);
        accountHold.setReleasedAt(LocalDateTime.now());

        Map<String, Object> payload = Map.of(
                "transactionId", accountHold.getTransactionId()
        );

        accountOutboxService.saveAccountOutboxEvent(accountHold.getTransactionId(), AccountEventType.TRANSACTION_COMPENSATED, payload);
    }
}
