package accountservice.repository;

import accountservice.entity.AccountInterestAccrualEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountInterestAccrualRepository
    extends JpaRepository<AccountInterestAccrualEntity, UUID> {
  boolean existsByAccountIdAndAccrualDate(UUID accountId, LocalDate accrualDate);

  @Modifying
  @Query(
      value =
          """
      INSERT INTO account_interest_accruals
          (id, account_id, accrual_date, amount_minor_units, rate, created_at)
      VALUES (:id, :accountId, :accrualDate, :amount, :rate, CURRENT_TIMESTAMP)
      ON CONFLICT (account_id, accrual_date) DO NOTHING
          """,
      nativeQuery = true)
  int tryInsert(
      @Param("id") UUID id,
      @Param("accountId") UUID accountId,
      @Param("accrualDate") LocalDate accrualDate,
      @Param("amount") long amount,
      @Param("rate") BigDecimal rate);
}
