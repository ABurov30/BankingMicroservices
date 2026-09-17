package accountservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import accountservice.dto.*;
import accountservice.entity.*;
import accountservice.mapper.result.AccountResultMapper;
import accountservice.repository.AccountHoldRepository;
import accountservice.repository.AccountRepository;
import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.account.ReservationStatus;
import enums.common.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferServiceTest {
  private final AccountHoldRepository holds = mock(AccountHoldRepository.class);
  private final AccountRepository accounts = mock(AccountRepository.class);
  private final CurrencyService currency = mock(CurrencyService.class);
  private final AccountOutboxService outbox = mock(AccountOutboxService.class);
  private final AccountResultMapper mapper = mock(AccountResultMapper.class);
  private final AccountOverviewCacheInvalidationService cacheInvalidationService =
      mock(AccountOverviewCacheInvalidationService.class);
  private final TransferService service =
      new TransferService(holds, accounts, currency, outbox, cacheInvalidationService, mapper);
  private final UUID sourceId = UUID.randomUUID();
  private final UUID targetId = UUID.randomUUID();
  private final UUID transactionId = UUID.randomUUID();

  private AccountEntity account(UUID id, long balance, Currency value) {
    var entity = new AccountEntity();
    entity.setId(id);
    entity.setAccountNumber(id.toString());
    entity.setOwnerAuthUserId(UUID.randomUUID());
    entity.setAccountStatus(AccountStatus.ACTIVE);
    entity.setAccountType(AccountType.CHECKING);
    entity.setAvailableBalanceMinorUnits(balance);
    entity.setReservedBalanceMinorUnits(0L);
    var c = new CurrencyEntity();
    c.setName(value);
    entity.setCurrency(c);
    return entity;
  }

  @Test
  void reservesFundsAndReturnsReservedResult() {
    var source = account(sourceId, 1000, Currency.USD);
    var target = account(targetId, 0, Currency.USD);
    when(accounts.findByIdInForUpdate(List.of(sourceId, targetId)))
        .thenReturn(List.of(source, target));
    when(holds.existsByTransactionId(transactionId)).thenReturn(false);
    var command =
        new ReserveFundsForTransactionCommand(
            sourceId, targetId, 400L, transactionId, source.getOwnerAuthUserId(), Currency.USD);
    var result = service.reserveFundsForTransactional(command);
    assertThat(result.status()).isEqualTo(ReservationStatus.RESERVED);
    assertThat(source.getReservedBalanceMinorUnits()).isEqualTo(400);
    verify(holds).save(any(AccountHoldEntity.class));
    verify(accounts).save(source);
    verify(cacheInvalidationService).invalidate(source);
  }

  @Test
  void failedReservationIsReturnedAsFailedResponse() {
    when(accounts.findByIdInForUpdate(any())).thenReturn(List.of());
    var result =
        service.reserveFundsForTransactional(
            new ReserveFundsForTransactionCommand(
                sourceId, targetId, 1L, transactionId, UUID.randomUUID(), Currency.USD));
    assertThat(result.status()).isEqualTo(ReservationStatus.FAILED);
    verifyNoInteractions(holds);
  }

  @Test
  void compensatesReservedHoldAndExecutesTransfer() {
    var source = account(sourceId, 1000, Currency.USD);
    var target = account(targetId, 0, Currency.USD);
    var hold = new AccountHoldEntity();
    hold.setId(UUID.randomUUID());
    hold.setAccountId(sourceId);
    hold.setTransactionId(transactionId);
    hold.setMinorUnits(100L);
    hold.setStatus(ReservationStatus.RESERVED);
    when(holds.findByIdForUpdate(hold.getId())).thenReturn(Optional.of(hold));
    when(accounts.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
    service.compensateFunds(new CompensationFundsCommand(hold.getId()));
    assertThat(hold.getStatus()).isEqualTo(ReservationStatus.COMPENSATED);
    verify(outbox).saveAccountOutboxEvent(eq(transactionId), any(), any());

    hold.setStatus(ReservationStatus.RESERVED);
    source.setReservedBalanceMinorUnits(100L);
    when(holds.findByIdForUpdate(hold.getId())).thenReturn(Optional.of(hold));
    when(accounts.findByIdInForUpdate(List.of(sourceId, targetId)))
        .thenReturn(List.of(source, target));
    when(currency.convertToUSD(any(), eq(Currency.USD))).thenAnswer(i -> i.getArgument(0));
    when(currency.convertFromUSD(any(), eq(Currency.USD))).thenAnswer(i -> i.getArgument(0));
    service.executeFundsTransfer(
        new ExecuteFundsTransferCommand(transactionId, targetId, null, hold));
    assertThat(hold.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    assertThat(source.getAvailableBalanceMinorUnits()).isEqualTo(900);
    assertThat(target.getAvailableBalanceMinorUnits()).isEqualTo(100);
    verify(accounts, atLeast(2)).save(any(AccountEntity.class));
  }
}
