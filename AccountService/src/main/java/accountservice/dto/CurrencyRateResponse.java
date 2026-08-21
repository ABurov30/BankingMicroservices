package accountservice.dto;

import enums.common.Currency;
import java.math.BigDecimal;

public record CurrencyRateResponse(Currency quote, BigDecimal rate) {}
