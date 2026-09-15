package cardservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cardservice.dto.*;
import cardservice.entity.CardEntity;
import cardservice.mapper.result.CardResultMapper;
import cardservice.repository.*;
import enums.auth.Roles;
import enums.card.CardStatus;
import enums.common.Currency;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardServiceTest {
  private final CardRepository cards = mock(CardRepository.class);
  private final AccountOwnershipProjectionRepository ownership =
      mock(AccountOwnershipProjectionRepository.class);
  private final CardOutboxEventRepository outbox = mock(CardOutboxEventRepository.class);
  private final CardLimitHoldRepository holds = mock(CardLimitHoldRepository.class);
  private final CardResultMapper mapper = mock(CardResultMapper.class);
  private final CardService service =
      new CardService(
          cards, ownership, outbox, holds, mock(CardLimitReservationService.class), mapper);
  private final UUID accountId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID cardId = UUID.randomUUID();

  private CardEntity card(CardStatus status) {
    var card = new CardEntity();
    card.setId(cardId);
    card.setAccountId(accountId);
    card.setPan("4000000000000000");
    card.setCardStatus(status);
    card.setCurrency(Currency.USD);
    card.setDailyLimitMinorUnits(1000L);
    card.setMonthlyLimitMinorUnits(2000L);
    return card;
  }

  @Test
  void createsFreezesUnfreezesAndUpdatesCard() {
    when(cards.existsByPan(anyString())).thenReturn(false);
    var created = card(CardStatus.ACTIVE);
    when(cards.save(any(CardEntity.class)))
        .thenAnswer(
            i -> {
              var value = i.getArgument(0, CardEntity.class);
              value.setId(cardId);
              return value;
            });
    var result =
        service.createCard(
            new CreatedCardCommand(accountId, Currency.USD, userId, "ACC", null), Currency.USD);
    verify(cards).save(any(CardEntity.class));
    verify(outbox).save(any());

    var frozen = card(CardStatus.ACTIVE);
    when(cards.findAllByAccountId(accountId)).thenReturn(Optional.of(List.of(frozen)));
    service.freezeCards(new FreezeCardsCommand(accountId, userId, "ACC"));
    assertThat(frozen.getCardStatus()).isEqualTo(CardStatus.FROZEN);
    service.unfreezeCards(new UnfreezeCardsCommand(accountId, userId, "ACC"));
    assertThat(frozen.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
    when(cards.findById(cardId)).thenReturn(Optional.of(frozen));
    var projection = new cardservice.entity.AccountOwnershipProjectionEntity();
    projection.setCurrency(Currency.USD);
    when(ownership.findById(accountId)).thenReturn(Optional.of(projection));
    service.updateCard(
        new UpdateCardCommand(cardId, CardStatus.BLOCKED, 500L, 1000L, null, "ADMIN"));
    assertThat(frozen.getCardStatus()).isEqualTo(CardStatus.BLOCKED);
    assertThat(frozen.getDailyLimitMinorUnits()).isEqualTo(500);
  }

  @Test
  void batchReadReturnsEmptyForNoAccounts() {
    assertThat(
            service.getCardsByAccountIds(
                new GetCardsByAccountIdsCommand(List.of(), userId, Roles.USER)))
        .isEmpty();
    verifyNoInteractions(cards);
  }

  @Test
  void readsCardsForOwnerAndHandlesEmptyFreezeLists() {
    var projection = new cardservice.entity.AccountOwnershipProjectionEntity();
    projection.setOwnerAuthUserId(userId);
    projection.setCurrency(Currency.USD);
    when(ownership.findById(accountId)).thenReturn(Optional.of(projection));
    when(cards.findByAccountId(accountId)).thenReturn(List.of());
    assertThat(
            service.getCardsByAccountId(
                new GetCardsByAccountIdCommand(accountId, userId, Roles.USER)))
        .isEmpty();
    when(cards.findAllByAccountId(accountId)).thenReturn(Optional.empty());
    service.freezeCards(new FreezeCardsCommand(accountId, userId, "ACC"));
    service.unfreezeCards(new UnfreezeCardsCommand(accountId, userId, "ACC"));
    verify(cards, never()).saveAll(any());
  }

  @Test
  void updateRejectsExpiredBlockedAndInvalidLimits() {
    var expired = card(CardStatus.EXPIRED);
    when(cards.findById(cardId)).thenReturn(Optional.of(expired));
    assertThatThrownBy(
            () ->
                service.updateCard(
                    new UpdateCardCommand(cardId, CardStatus.ACTIVE, null, null, null, "ADMIN")))
        .hasMessageContaining(cardId.toString());
    var blocked = card(CardStatus.BLOCKED);
    when(cards.findById(cardId)).thenReturn(Optional.of(blocked));
    assertThatThrownBy(
            () ->
                service.updateCard(
                    new UpdateCardCommand(cardId, CardStatus.ACTIVE, null, null, null, "ADMIN")))
        .hasMessageContaining(cardId.toString());
    var active = card(CardStatus.ACTIVE);
    when(cards.findById(cardId)).thenReturn(Optional.of(active));
    assertThatThrownBy(
            () ->
                service.updateCard(
                    new UpdateCardCommand(cardId, null, 3000L, 2000L, null, "ADMIN")))
        .hasMessageContaining("Daily limit");
  }

  @Test
  void failedLimitReservationIsConvertedToFailedResult() {
    var reservation = mock(CardLimitReservationService.class);
    var failing = new CardService(cards, ownership, outbox, holds, reservation, mapper);
    when(reservation.reserve(any())).thenThrow(new IllegalStateException("failed"));
    var result =
        failing.reserveLimitsForTransaction(
            new ReserveLimitsForTransactionCommand(
                UUID.randomUUID(), 1L, UUID.randomUUID(), userId, Currency.USD));
    assertThat(result.status()).isEqualTo(enums.account.ReservationStatus.FAILED);
    assertThat(result.message()).isEqualTo("failed");
  }

  @Test
  void compensatesAndReleasesReservedLimitHolds() {
    var entity = card(CardStatus.ACTIVE);
    entity.setSpendDailyLimitMinorUnits(200L);
    entity.setSpendMonthlyLimitMinorUnits(300L);
    var hold = new cardservice.entity.CardLimitHoldEntity();
    hold.setCardId(cardId);
    hold.setTransactionId(UUID.randomUUID());
    hold.setMinorUnits(100L);
    hold.setStatus(enums.account.ReservationStatus.RESERVED);
    hold.setCreatedAt(LocalDateTime.now());
    when(holds.findByTransactionIdForUpdate(hold.getTransactionId())).thenReturn(Optional.of(hold));
    when(cards.findByIdForUpdate(cardId)).thenReturn(Optional.of(entity));
    service.compensateLimitsForTransaction(
        new CompensateLimitsForTransactionCommand(hold.getTransactionId()));
    assertThat(hold.getStatus()).isEqualTo(enums.account.ReservationStatus.COMPENSATED);
    assertThat(entity.getSpendDailyLimitMinorUnits()).isEqualTo(100);
    hold.setStatus(enums.account.ReservationStatus.RESERVED);
    service.markLimitReservationAsReleased(
        new MarkLimitReservationAsReleasedCommand(hold.getTransactionId()));
    assertThat(hold.getStatus()).isEqualTo(enums.account.ReservationStatus.RELEASED);
  }
}
