package accountservice.repository;

import accountservice.entity.AccountHoldEntity;
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

public interface AccountHoldRepository extends JpaRepository<AccountHoldEntity, UUID> {
  boolean existsByTransactionId(UUID transactionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT ah
      FROM AccountHoldEntity ah
      WHERE ah.status = :reservationStatus AND ah.expiresAt <= :expiresAt
      ORDER BY ah.createdAt
      """)
  List<AccountHoldEntity>
      findForUpdateTop50ByReservationStatusAndExpiresAtLessThanEqualOrderByCreatedAtAsc(
          @Param("reservationStatus") ReservationStatus reservationStatus,
          @Param("expiresAt") LocalDateTime expiresAt,
          Pageable pageable);

  Optional<AccountHoldEntity> findByTransactionId(UUID transactionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT ah from AccountHoldEntity ah WHERE ah.id = :accountHoldId")
  Optional<AccountHoldEntity> findByIdForUpdate(@Param("accountHoldId") UUID accountHoldId);
}
