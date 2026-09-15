package cardservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cardservice.dto.ReserveLimitsForTransactionCommand;
import cardservice.entity.AccountOwnershipProjectionEntity;
import cardservice.entity.CardEntity;
import cardservice.repository.AccountOwnershipProjectionRepository;
import cardservice.repository.CardLimitHoldRepository;
import cardservice.repository.CardRepository;
import enums.account.ReservationStatus;
import enums.common.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardLimitReservationServiceTest {
  private final CardRepository cards = mock(CardRepository.class);
  private final AccountOwnershipProjectionRepository ownership =
      mock(AccountOwnershipProjectionRepository.class);
  private final CardLimitHoldRepository holds = mock(CardLimitHoldRepository.class);
  private final CardLimitReservationService service =
      new CardLimitReservationService(cards, ownership, holds);
  private final UUID cardId = UUID.randomUUID();
  private final UUID accountId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID transactionId = UUID.randomUUID();

  private CardEntity card() {
    var card = new CardEntity();
    card.setId(cardId);
    card.setAccountId(accountId);
    card.setCurrency(Currency.USD);
    card.setDailyLimitMinorUnits(1000L);
    card.setMonthlyLimitMinorUnits(2000L);
    return card;
  }

  private ReserveLimitsForTransactionCommand command(long amount) {
    return new ReserveLimitsForTransactionCommand(
        cardId, amount, transactionId, userId, Currency.USD);
  }

  @Test
  void reservesAmountAndCreatesHoldWhenCardAndOwnerAreValid() {
    var card = card();
    var projection = new AccountOwnershipProjectionEntity();
    projection.setOwnerAuthUserId(userId);
    when(cards.findByIdForUpdate(cardId)).thenReturn(Optional.of(card));
    when(ownership.findById(accountId)).thenReturn(Optional.of(projection));
    when(holds.existsByTransactionId(transactionId)).thenReturn(false);

    var result = service.reserve(command(250));

    assertThat(result.status()).isEqualTo(ReservationStatus.RESERVED);
    assertThat(card.getSpendDailyLimitMinorUnits()).isEqualTo(250);
    assertThat(card.getSpendMonthlyLimitMinorUnits()).isEqualTo(250);
    verify(holds).save(any());
    verify(cards).save(card);
  }

  @Test
  void rejectsDuplicateNegativeAndLimitViolations() {
    when(holds.existsByTransactionId(transactionId)).thenReturn(true);
    assertThatThrownBy(() -> service.reserve(command(10)))
        .hasMessageContaining(transactionId.toString());
    when(holds.existsByTransactionId(transactionId)).thenReturn(false);
    assertThatThrownBy(() -> service.reserve(command(-1)))
        .hasMessageContaining(transactionId.toString());
    var card = card();
    when(cards.findByIdForUpdate(cardId)).thenReturn(Optional.of(card));
    var projection = new AccountOwnershipProjectionEntity();
    projection.setOwnerAuthUserId(userId);
    when(ownership.findById(accountId)).thenReturn(Optional.of(projection));
    assertThatThrownBy(() -> service.reserve(command(1001)))
        .hasMessageContaining(transactionId.toString());
  }
}
