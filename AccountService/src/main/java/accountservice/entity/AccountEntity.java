package accountservice.entity;

import enums.account.AccountStatus;
import enums.account.AccountType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Table(name = "accounts")
public class AccountEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerUserId;

  @Column(name = "owner_auth_user_id", nullable = false)
  private UUID ownerAuthUserId;

  @Column(name = "account_number", nullable = false)
  private String accountNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private AccountType accountType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private AccountStatus accountStatus;

  @Column(name = "available_balance_minor_units", nullable = false)
  private Long availableBalanceMinorUnits = Long.valueOf(0);

  @Column(name = "reserved_balance_minor_units", nullable = false)
  private Long reservedBalanceMinorUnits = Long.valueOf(0);

  @Column(name = "version", nullable = false)
  private int version;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "currency_id", nullable = false)
  private CurrencyEntity currency;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
