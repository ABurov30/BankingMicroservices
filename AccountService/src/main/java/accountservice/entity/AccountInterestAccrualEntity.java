package accountservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "account_interest_accruals")
public class AccountInterestAccrualEntity {
  @Id private UUID id;

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "accrual_date", nullable = false)
  private LocalDate accrualDate;

  @Column(name = "amount_minor_units", nullable = false)
  private long amountMinorUnits;

  @Column(nullable = false, precision = 19, scale = 12)
  private BigDecimal rate;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
