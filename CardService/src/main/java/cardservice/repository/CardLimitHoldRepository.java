package cardservice.repository;

import cardservice.entity.CardLimitHoldEntity;
import enums.account.ReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardLimitHoldRepository extends JpaRepository<CardLimitHoldEntity, UUID> {
  boolean existsByTransactionId(UUID transactionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT clh
      FROM CardLimitHoldEntity clh
      WHERE clh.status = :reservationStatus AND clh.expiresAt <= :expiresAt
      ORDER BY clh.createdAt
      """)
  List<CardLimitHoldEntity>
      findForUpdateTop50ByReservationStatusAndExpiresAtLessThanEqualOrderByCreatedAtAsc(
          @Param("reservationStatus") ReservationStatus reservationStatus,
          @Param("expiresAt") LocalDateTime expiresAt,
          Pageable pageable);

  Optional<CardLimitHoldEntity> findByTransactionId(UUID transactionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT clh from CardLimitHoldEntity clh WHERE clh.transactionId = :transactionId")
  Optional<CardLimitHoldEntity> findByTransactionIdForUpdate(
      @Param("transactionId") UUID transactionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT clh from CardLimitHoldEntity clh WHERE clh.id = :cardLimitHoldId")
  Optional<CardLimitHoldEntity> findByIdForUpdate(@Param("cardLimitHoldId") UUID cardLimitHoldId);
}
