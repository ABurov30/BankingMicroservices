package accountservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import accountservice.dto.UpdateAccountBalanceCommand;
import accountservice.entity.AccountEntity;
import accountservice.mapper.command.TransferCommandMapper;
import accountservice.mapper.result.AccountResultMapper;
import accountservice.repository.AccountHoldRepository;
import accountservice.repository.AccountRepository;
import accountservice.repository.CurrencyRepository;
import enums.account.AccountStatus;
import enums.account.AccountType;
import enums.auth.Roles;
import enums.common.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountBalanceServiceTest {
  private final AccountRepository accounts = mock(AccountRepository.class);
  private final AccountEntity account = new AccountEntity();
  private final AccountService service =
      new AccountService(
          accounts,
          mock(CurrencyRepository.class),
          mock(AccountResultMapper.class),
          mock(AccountHoldRepository.class),
          mock(TransferService.class),
          mock(TransferCommandMapper.class),
          mock(AccountOutboxService.class));
  private final UUID accountId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  private void givenAccount(long balance) {
    account.setId(accountId);
    account.setOwnerAuthUserId(userId);
    account.setOwnerUserId(userId);
    account.setAvailableBalanceMinorUnits(balance);
    account.setAccountStatus(AccountStatus.ACTIVE);
    when(accounts.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
  }

  @Test
  void topUpAndWithdrawUpdateOwnedAccount() {
    givenAccount(100);
    service.topUpAccount(new UpdateAccountBalanceCommand(accountId, 50L, userId));
    assertThat(account.getAvailableBalanceMinorUnits()).isEqualTo(150);
    service.withdrawAccount(new UpdateAccountBalanceCommand(accountId, 25L, userId));
    assertThat(account.getAvailableBalanceMinorUnits()).isEqualTo(125);
    verify(accounts, times(2)).save(account);
  }

  @Test
  void balanceOperationsRejectWrongOwnerAndInsufficientFunds() {
    givenAccount(100);
    assertThatThrownBy(
            () ->
                service.topUpAccount(
                    new UpdateAccountBalanceCommand(accountId, 1L, UUID.randomUUID())))
        .hasMessageContaining(accountId.toString());
    assertThatThrownBy(
            () -> service.withdrawAccount(new UpdateAccountBalanceCommand(accountId, 101L, userId)))
        .hasMessageContaining(accountId.toString());
    when(accounts.findByIdForUpdate(accountId)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> service.topUpAccount(new UpdateAccountBalanceCommand(accountId, 1L, userId)))
        .hasMessageContaining(accountId.toString());
    verify(accounts, never()).save(any());
  }

  @Test
  void createsFreezesAndUnfreezesAccount() {
    var currency = new accountservice.entity.CurrencyEntity();
    currency.setName(Currency.USD);
    var currencies = mock(CurrencyRepository.class);
    var outbox = mock(AccountOutboxService.class);
    var createService =
        new AccountService(
            accounts,
            currencies,
            mock(AccountResultMapper.class),
            mock(AccountHoldRepository.class),
            mock(TransferService.class),
            mock(TransferCommandMapper.class),
            outbox);
    when(currencies.findByName(Currency.USD)).thenReturn(currency);
    when(accounts.existsByAccountNumber(anyString())).thenReturn(false);
    when(accounts.saveAndFlush(any(AccountEntity.class)))
        .thenAnswer(
            i -> {
              var value = i.getArgument(0, AccountEntity.class);
              value.setId(accountId);
              return value;
            });
    var result =
        createService.createAccount(
            new accountservice.dto.CreateAccountCommand(
                UUID.randomUUID(), userId, AccountType.CHECKING, Currency.USD));
    assertThat(result.accountId()).isEqualTo(accountId);
    givenAccount(100);
    account.setAccountNumber("ACC");
    createService.freezeAccount(
        new accountservice.dto.FreezeAccountCommand(accountId, userId, "USER"));
    assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.FROZEN);
    createService.unfreezeAccount(
        new accountservice.dto.UnfreezeAccountCommand(accountId, userId, "USER"));
    assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    verify(outbox, atLeast(3)).saveAccountOutboxEvent(any(), any(), any());
  }

  @Test
  void readsAccountsForOwnersAndPrivilegedUsers() {
    givenAccount(100);
    when(accounts.findByOwnerUserId(userId)).thenReturn(Optional.of(List.of(account)));
    when(accounts.findByOwnerAuthUserId(userId)).thenReturn(Optional.of(List.of(account)));
    when(accounts.findById(accountId)).thenReturn(Optional.of(account));
    when(accounts.findAll()).thenReturn(List.of(account));
    assertThat(
            service.getAccountsByOwnerUserId(
                new accountservice.dto.GetAccountsByOwnerUserIdCommand(userId, userId, Roles.USER)))
        .hasSize(1);
    assertThat(
            service.getAccountsByAuthUserId(
                new accountservice.dto.GetAccountByAuthUserIdCommand(userId, Roles.USER)))
        .hasSize(1);
    assertThat(service.getAllAccounts(new accountservice.dto.GetAllAccountsCommand(Roles.ADMIN)))
        .hasSize(1);
    assertThat(
            service.getAccountById(
                new accountservice.dto.GetAccountByIdCommand(accountId, userId, Roles.USER)))
        .isNull();
    assertThat(
            service.getAccountByIdsForTransaction(
                new accountservice.dto.GetAccountByIdsForTransactionCommand(List.of())))
        .isEmpty();
  }

  @Test
  void rejectsUnauthorizedAndInvalidAccountStateChanges() {
    givenAccount(100);
    account.setAccountNumber("ACC");
    account.setAccountStatus(AccountStatus.FROZEN);
    assertThatThrownBy(
            () ->
                service.freezeAccount(
                    new accountservice.dto.FreezeAccountCommand(accountId, userId, "USER")))
        .hasMessageContaining(accountId.toString());
    assertThatCode(
            () ->
                service.unfreezeAccount(
                    new accountservice.dto.UnfreezeAccountCommand(accountId, userId, "USER")))
        .doesNotThrowAnyException();
    account.setAccountStatus(AccountStatus.CLOSED);
    assertThatThrownBy(
            () ->
                service.unfreezeAccount(
                    new accountservice.dto.UnfreezeAccountCommand(accountId, userId, "USER")))
        .hasMessageContaining(accountId.toString());
    assertThatThrownBy(
            () -> service.getAllAccounts(new accountservice.dto.GetAllAccountsCommand(Roles.USER)))
        .hasMessageContaining("not allowed");
  }
}
