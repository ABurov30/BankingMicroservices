package accountservice.entity;

import enums.account.AccountCurrency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "currencies")
public class CurrencyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true)
    private AccountCurrency name;

    @Column(name = "rate_from_usd", nullable = false)
    private BigDecimal rateFromUSD;
}
