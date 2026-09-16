package transactionservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import enums.account.ReservationStatus;
import enums.common.Currency;
import enums.transaction.TransactionStatus;
import java.util.UUID;
import kafkacontracts.transaction.TransactionEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import transactionservice.client.AccountGrpcClient;
import transactionservice.client.CardGrpcClient;
import transactionservice.dto.CreateTransactionCommand;
import transactionservice.dto.ReservationResponseDto;
import transactionservice.dto.ReserveFudsForTransactionResponseDto;
import transactionservice.entity.TransactionEntity;
import transactionservice.exception.FundsReservationFailedException;
import transactionservice.grpc.TransactionStatusStreamRegistry;
import transactionservice.mapper.grpc.TransactionGrpcMapper;
import transactionservice.repository.TransactionOutboxEventRepository;
import transactionservice.repository.TransactionRepository;

@Tag("integration")
@ActiveProfiles("test")
@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import({ReservationService.class, ReservationFailureService.class, TransactionOutboxService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservationServiceIT {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private ReservationService reservationService;
  @Autowired private TransactionRepository transactions;
  @Autowired private TransactionOutboxEventRepository outbox;
  @MockitoBean private AccountGrpcClient account;
  @MockitoBean private CardGrpcClient card;
  @MockitoBean private TransactionGrpcMapper grpcMapper;
  @MockitoBean private TransactionStatusStreamRegistry streams;

  @BeforeEach
  void cleanDatabase() {
    outbox.deleteAll();
    transactions.deleteAll();
  }

  @Test
  void persistsFailureAndCardCompensationAfterAccountReservationIsRejected() {
    var transaction = transactions.saveAndFlush(transaction());
    var command = command(transaction);
    when(card.reserveLimitsForTransaction(any()))
        .thenReturn(new ReservationResponseDto(ReservationStatus.RESERVED, "Card limit reserved"));
    when(account.reserveFundsForTransaction(any()))
        .thenReturn(
            new ReserveFudsForTransactionResponseDto(
                null,
                null,
                new ReservationResponseDto(
                    ReservationStatus.FAILED, "Insufficient account funds")));

    assertThatThrownBy(() -> reservationService.reserve(command, transaction))
        .isInstanceOf(FundsReservationFailedException.class)
        .hasMessageContaining("Insufficient account funds");

    var saved = transactions.findById(transaction.getId()).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(TransactionStatus.FAILED);
    assertThat(saved.getErrorMessage()).contains("Insufficient account funds");
    assertThat(outbox.findAll())
        .extracting(event -> event.getEventType())
        .containsExactlyInAnyOrder(
            TransactionEventType.TRANSACTION_CARD_LIMIT_HOLD_COMPENSATION.name(),
            TransactionEventType.TRANSACTION_FAILED.name());
  }

  private CreateTransactionCommand command(TransactionEntity transaction) {
    return new CreateTransactionCommand(
        transaction.getSourceAccountId(),
        transaction.getTargetAccountId(),
        transaction.getMinorUnits(),
        transaction.getCurrency(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID());
  }

  private TransactionEntity transaction() {
    var transaction = new TransactionEntity();
    transaction.setSourceAccountId(UUID.randomUUID());
    transaction.setTargetAccountId(UUID.randomUUID());
    transaction.setIdempotencyKey(UUID.randomUUID());
    transaction.setMinorUnits(100L);
    transaction.setCurrency(Currency.USD);
    transaction.setStatus(TransactionStatus.CREATED);
    return transaction;
  }
}
