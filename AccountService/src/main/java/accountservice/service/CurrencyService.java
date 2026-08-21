package accountservice.service;

import accountservice.entity.CurrencyEntity;
import accountservice.repository.CurrencyRepository;
import enums.common.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class CurrencyService {
  private final CurrencyRepository currencyRepository;
  private static final int RATE_SCALE = 8;
  private static final int MONEY_SCALE = 2;

  public CurrencyService(CurrencyRepository currencyRepository) {
    this.currencyRepository = currencyRepository;
  }

  public BigDecimal convertToUSD(BigDecimal amount, Currency currency) {
    CurrencyEntity currencyEntity = currencyRepository.findByName(currency);
    return amount
        .divide(currencyEntity.getRateFromUSD(), RATE_SCALE, RoundingMode.HALF_UP)
        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal convertFromUSD(BigDecimal amount, Currency currency) {
    CurrencyEntity currencyEntity = currencyRepository.findByName(currency);
    return amount
        .multiply(currencyEntity.getRateFromUSD())
        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }
}
