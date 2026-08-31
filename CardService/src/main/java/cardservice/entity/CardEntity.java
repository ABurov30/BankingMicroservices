package cardservice.entity;

import enums.card.CardStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "cards")
public class CardEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "pan", nullable = false, unique = true)
  private String pan;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CardStatus cardStatus = CardStatus.ACTIVE;

  @Column(name = "daily_limit_minor_units")
  private Long dailyLimitMinorUnits = Long.valueOf(0);

  @Column(name = "monthly_limit_minor_units")
  private Long monthlyLimitMinorUnits = Long.valueOf(0);

  @Column(name = "spend_daily_limit_minor_units", nullable = false)
  private Long spendDailyLimitMinorUnits = Long.valueOf(0);

  @Column(name = "spend_monthly_limit_minor_units", nullable = false)
  private Long spendMonthlyLimitMinorUnits = Long.valueOf(0);

  @Column(name = "expires_at", nullable = false, updatable = false)
  private LocalDateTime expiresAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
