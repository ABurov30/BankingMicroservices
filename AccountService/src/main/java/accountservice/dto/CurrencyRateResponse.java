package accountservice.dto;

import enums.account.AccountCurrency;
import java.math.BigDecimal;

public record CurrencyRateResponse(AccountCurrency quote, BigDecimal rate) {}
