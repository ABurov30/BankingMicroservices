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
import enums.account.ReservationStatus;
import enums.common.Currency;
import enums.transaction.TransactionDirection;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import kafkacontracts.account.AccountEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransferService {
  private final AccountHoldRepository accountHoldRepository;
  private final AccountRepository accountRepository;
  private final CurrencyService currencyService;
  private final AccountOutboxService accountOutboxService;
  private final AccountResultMapper resultMapper;
  private static final Logger log = LoggerFactory.getLogger(TransferService.class);
  private static final long HOLD_TTL_MINUTES = 5;

  public TransferService(
      AccountHoldRepository accountHoldRepository,
      AccountRepository accountRepository,
      CurrencyService currencyService,
      AccountOutboxService accountOutboxService,
      AccountResultMapper accountResultMapper) {
    this.accountHoldRepository = accountHoldRepository;
    this.accountRepository = accountRepository;
    this.currencyService = currencyService;
    this.accountOutboxService = accountOutboxService;
    this.resultMapper = accountResultMapper;
  }

  private BigDecimal convertAmountForTransactionToTagetCurrency(
      BigDecimal amount, Currency sourceCurrency, Currency targetCurrency) {
    BigDecimal amountInUSD = currencyService.convertToUSD(amount, sourceCurrency);
    BigDecimal amountInTargetCurrency =
        currencyService
            .convertFromUSD(amountInUSD, targetCurrency)
            .setScale(targetCurrency.getMinorUnit(), RoundingMode.HALF_EVEN);
    return amountInTargetCurrency;
  }

  @Transactional
  public void executeFundsTransfer(ExecuteFundsTransferCommand command) {
    var optionalAccountHold =
        accountHoldRepository.findByIdForUpdate(command.accountHold().getId());

    if (optionalAccountHold.isEmpty()) {
      log.warn(
          "Skipping funds transfer: accountHoldId={} not found", command.accountHold().getId());
      return;
    }

    var accountHold = optionalAccountHold.get();

    if (accountHold.getStatus() != ReservationStatus.RESERVED) {
      log.info(
          "Skipping funds transfer: transactionId={}, status={}",
          accountHold.getTransactionId(),
          accountHold.getStatus());
      return;
    }

    var targetAccount =
        accountRepository
            .findByIdForUpdate(command.targetAccountId())
            .orElseThrow(() -> new AccountNotFoundException(command.targetAccountId()));
    var sourceAccount =
        accountRepository
            .findByIdForUpdate(accountHold.getAccountId())
            .orElseThrow(() -> new AccountNotFoundException(accountHold.getAccountId()));

    var sourceCurrency = sourceAccount.getCurrency().getName();
    var targetCurrency = targetAccount.getCurrency().getName();

    var creditedAmountBigDecimal =
        convertAmountForTransactionToTagetCurrency(
            BigDecimal.valueOf(accountHold.getMinorUnits(), sourceCurrency.getMinorUnit()),
            sourceCurrency,
            targetCurrency);

    var creditedAmount =
        creditedAmountBigDecimal.movePointRight(targetCurrency.getMinorUnit()).longValueExact();

    targetAccount.setAvailableBalanceMinorUnits(
        targetAccount.getAvailableBalanceMinorUnits() + creditedAmount);

    var reservedAmount = accountHold.getMinorUnits();
    sourceAccount.setAvailableBalanceMinorUnits(
        sourceAccount.getAvailableBalanceMinorUnits() - reservedAmount);
    sourceAccount.setReservedBalanceMinorUnits(
        sourceAccount.getReservedBalanceMinorUnits() - reservedAmount);

    accountHold.setStatus(ReservationStatus.RELEASED);
    accountHold.setReleasedAt(LocalDateTime.now());

    accountRepository.save(targetAccount);
    accountRepository.save(sourceAccount);
    accountHoldRepository.save(accountHold);

    Map<String, Object> recipientPayload =
        Map.of(
            "accountNumber", targetAccount.getAccountNumber(),
            "amountMinorUnits", creditedAmount,
            "currency", targetCurrency.name(),
            "authUserId", targetAccount.getOwnerAuthUserId(),
            "transactionId", accountHold.getTransactionId(),
            "transactionDirection", TransactionDirection.RECIPIENT);
    Map<String, Object> senderPayload =
        Map.of(
            "accountNumber",
            sourceAccount.getAccountNumber(),
            "amountMinorUnits",
            reservedAmount,
            "currency",
            sourceCurrency.name(),
            "authUserId",
            sourceAccount.getOwnerAuthUserId(),
            "transactionId",
            accountHold.getTransactionId(),
            "transactionDirection",
            TransactionDirection.SENDER);

    var eventKeyPrefix =
        accountHold.getTransactionId() + ":" + AccountEventType.TRANSACTION_COMPLETED.name();
    accountOutboxService.saveAccountOutboxEvent(
        accountHold.getTransactionId(),
        AccountEventType.TRANSACTION_COMPLETED,
        eventKeyPrefix + ":" + TransactionDirection.RECIPIENT,
        recipientPayload);
    accountOutboxService.saveAccountOutboxEvent(
        accountHold.getTransactionId(),
        AccountEventType.TRANSACTION_COMPLETED,
        eventKeyPrefix + ":" + TransactionDirection.SENDER,
        senderPayload);
  }

  @Transactional
  public ReserveFundsForTransactionResult reserveFundsForTransactional(
      ReserveFundsForTransactionCommand command) {
    try {
      var sourceAccount =
          accountRepository
              .findByIdForUpdate(command.sourceAccountId())
              .orElseThrow(() -> new AccountNotFoundException(command.sourceAccountId()));

      var targetAccount =
          accountRepository
              .findById(command.targetAccountId())
              .orElseThrow(() -> new AccountNotFoundException(command.targetAccountId()));

      if (!sourceAccount.getOwnerAuthUserId().equals(command.sourceAuthUserId())) {
        throw new AccountOwnershipException();
      }

      if (accountHoldRepository.existsByTransactionId(command.transactionId())) {
        throw new TransactionAlreadyProcessedException(command.transactionId());
      }

      var sourceCurrency = sourceAccount.getCurrency().getName();
      var amount = command.minorUnits();

      Long availableBalanceWithoutReservedBalance =
          sourceAccount.getAvailableBalanceMinorUnits()
              - sourceAccount.getReservedBalanceMinorUnits();

      if (availableBalanceWithoutReservedBalance.compareTo(amount) < 0) {
        throw new InsufficientFundsException(sourceAccount.getId());
      }

      var accountHold = new AccountHoldEntity();
      accountHold.setAccountId(sourceAccount.getId());
      accountHold.setTransactionId(command.transactionId());
      accountHold.setCurrency(sourceCurrency);
      accountHold.setMinorUnits(command.minorUnits());
      accountHold.setStatus(ReservationStatus.RESERVED);
      accountHold.setExpiresAt(LocalDateTime.now().plusMinutes(HOLD_TTL_MINUTES));
      accountHoldRepository.save(accountHold);

      sourceAccount.setReservedBalanceMinorUnits(
          sourceAccount.getReservedBalanceMinorUnits() + amount);
      accountRepository.save(sourceAccount);

      return new ReserveFundsForTransactionResult(
          resultMapper.toGetAccountResult(sourceAccount),
          resultMapper.toGetAccountResult(targetAccount),
          ReservationStatus.RESERVED,
          "Funds reserved for transaction " + command.transactionId());
    } catch (Exception e) {
      log.error("Failed to reserve funds: transactionId={}", command.transactionId(), e);
      return new ReserveFundsForTransactionResult(
          null, null, ReservationStatus.FAILED, e.getMessage());
    }
  }

  @Transactional
  public void compensateFunds(CompensationFundsCommand command) {
    var optionalAccountHold = accountHoldRepository.findByIdForUpdate(command.accountHoldId());

    if (optionalAccountHold.isEmpty()) {
      log.warn("Skipping funds compensation: accountHoldId={} not found", command.accountHoldId());
      return;
    }

    var accountHold = optionalAccountHold.get();

    if (accountHold.getStatus() != ReservationStatus.RESERVED) {
      log.info(
          "Skipping funds compensation: transactionId={}, status={}",
          accountHold.getTransactionId(),
          accountHold.getStatus());
      return;
    }

    var sourceAccount =
        accountRepository
            .findByIdForUpdate(accountHold.getAccountId())
            .orElseThrow(() -> new AccountNotFoundException(accountHold.getAccountId()));

    sourceAccount.setReservedBalanceMinorUnits(
        sourceAccount.getReservedBalanceMinorUnits() - (accountHold.getMinorUnits()));
    accountHold.setStatus(ReservationStatus.COMPENSATED);
    accountHold.setReleasedAt(LocalDateTime.now());

    Map<String, Object> payload = Map.of("transactionId", accountHold.getTransactionId());

    accountOutboxService.saveAccountOutboxEvent(
        accountHold.getTransactionId(), AccountEventType.TRANSACTION_COMPENSATED, payload);
  }
}
