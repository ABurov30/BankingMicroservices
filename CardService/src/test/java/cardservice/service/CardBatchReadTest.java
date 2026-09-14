package cardservice.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cardservice.dto.GetCardsByAccountIdsCommand;
import cardservice.entity.AccountOwnershipProjectionEntity;
import cardservice.entity.CardEntity;
import cardservice.exception.CardsNotFoundException;
import cardservice.mapper.result.CardResultMapper;
import cardservice.repository.*;
import enums.auth.Roles;
import enums.common.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardBatchReadTest {
  private final CardRepository cards = mock(CardRepository.class);
  private final AccountOwnershipProjectionRepository projections =
      mock(AccountOwnershipProjectionRepository.class);
  private final CardResultMapper mapper = mock(CardResultMapper.class);
  private final CardService service =
      new CardService(
          cards,
          projections,
          mock(CardOutboxEventRepository.class),
          mock(CardLimitHoldRepository.class),
          mapper);

  @Test
  void emptyBatchSkipsDatabase() {
    assertThat(
            service.getCardsByAccountIds(
                new GetCardsByAccountIdsCommand(List.of(), UUID.randomUUID(), Roles.USER)))
        .isEmpty();
    verifyNoInteractions(cards, projections);
  }

  @Test
  void loadsDistinctAccountsAndCurrenciesInBatches() {
    UUID owner = UUID.randomUUID();
    var first = projection(owner, Currency.USD);
    var second = projection(owner, Currency.EUR);
    var ids = List.of(first.getAccountId(), second.getAccountId());
    var card = new CardEntity();
    card.setAccountId(second.getAccountId());
    when(projections.findByAccountIdIn(ids)).thenReturn(List.of(second, first));
    when(cards.findByAccountIdIn(ids)).thenReturn(List.of(card));
    service.getCardsByAccountIds(
        new GetCardsByAccountIdsCommand(
            List.of(first.getAccountId(), second.getAccountId(), first.getAccountId()),
            owner,
            Roles.USER));
    verify(projections).findByAccountIdIn(ids);
    verify(cards).findByAccountIdIn(ids);
    verify(mapper).toGetCardResult(card, Currency.EUR);
    verifyNoMoreInteractions(cards, projections);
  }

  @Test
  void rejectsWholeBatchWhenOneAccountIsForeignOrMissing() {
    UUID owner = UUID.randomUUID();
    var own = projection(owner, Currency.USD);
    var foreign = projection(UUID.randomUUID(), Currency.EUR);
    var ids = List.of(own.getAccountId(), foreign.getAccountId());
    when(projections.findByAccountIdIn(ids)).thenReturn(List.of(own, foreign));
    var command = new GetCardsByAccountIdsCommand(ids, owner, Roles.USER);
    assertThatThrownBy(() -> service.getCardsByAccountIds(command))
        .isInstanceOf(CardsNotFoundException.class);
    when(projections.findByAccountIdIn(ids)).thenReturn(List.of(own));
    assertThatThrownBy(() -> service.getCardsByAccountIds(command))
        .isInstanceOf(CardsNotFoundException.class);
    verifyNoInteractions(cards);
  }

  @Test
  void privilegedRolesCanReadForeignAccounts() {
    var foreign = projection(UUID.randomUUID(), Currency.USD);
    var ids = List.of(foreign.getAccountId());
    when(projections.findByAccountIdIn(ids)).thenReturn(List.of(foreign));
    when(cards.findByAccountIdIn(ids)).thenReturn(List.of());
    for (var role : List.of(Roles.ADMIN, Roles.MANAGER)) {
      assertThat(
              service.getCardsByAccountIds(
                  new GetCardsByAccountIdsCommand(ids, UUID.randomUUID(), role)))
          .isEmpty();
    }
    verify(cards, times(2)).findByAccountIdIn(ids);
  }

  private AccountOwnershipProjectionEntity projection(UUID owner, Currency currency) {
    var projection = new AccountOwnershipProjectionEntity();
    projection.setAccountId(UUID.randomUUID());
    projection.setOwnerAuthUserId(owner);
    projection.setCurrency(currency);
    return projection;
  }
}
