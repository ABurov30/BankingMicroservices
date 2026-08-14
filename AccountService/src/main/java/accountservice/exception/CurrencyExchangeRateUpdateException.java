package accountservice.exception;

public class CurrencyExchangeRateUpdateException extends RuntimeException {
  public CurrencyExchangeRateUpdateException() {
    super("Response empty unavailable to update currency exchange rate");
  }
}
