package cardservice.repository;

import cardservice.entity.CardEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<CardEntity, UUID> {
  boolean existsByPan(String pan);

  List<CardEntity> findByAccountId(UUID accountId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c from CardEntity c WHERE c.id = :cardId")
  Optional<CardEntity> findByIdForUpdate(@Param("cardId") UUID cardId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE CardEntity c SET c.spendDailyLimitMinorUnits = :limit"
          + " WHERE c.spendDailyLimitMinorUnits <> :limit")
  int resetSpendDailyLimitMinorUnits(@Param("limit") Long limit);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE CardEntity c SET c.spendMonthlyLimitMinorUnits = :limit"
          + " WHERE c.spendMonthlyLimitMinorUnits <> :limit")
  int resetSpendMonthlyLimitMinorUnits(@Param("limit") Long limit);

  Optional<List<CardEntity>> findAllByAccountId(UUID accountId);
}
