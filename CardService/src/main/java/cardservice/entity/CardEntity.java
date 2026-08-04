package cardservice.entity;

import enums.card.CardStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(name = "daily_limit")
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit")
    private BigDecimal monthlyLimit;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
