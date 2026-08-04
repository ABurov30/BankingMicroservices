package accountservice.service;

import accountservice.dto.CurrencyRateResponse;
import accountservice.entity.CurrencyEntity;
import accountservice.repository.CurrencyRepository;
import enums.account.AccountCurrency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CurrencyScheduler {
    private final CurrencyRepository currencyRepository;
    private final RestClient restClient = RestClient.create();
    private static final Logger log = LoggerFactory.getLogger(CurrencyScheduler.class);

    public CurrencyScheduler(
            CurrencyRepository currencyRepository
    ) {
        this.currencyRepository = currencyRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void updateCurrencyExchangeRateOnStartup() {
        try {
            updateCurrencyExchangeRate();
        } catch (RestClientException e) {
            log.warn("Currency exchange rate update failed on startup", e);
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Paris")
    @Transactional
    public void updateCurrencyExchangeRate() {
        CurrencyRateResponse[] response = restClient.get()
                .uri("https://api.frankfurter.dev/v2/rates?base=USD&quotes=EUR,CNY,GBP")
                .retrieve()
                .body(CurrencyRateResponse[].class);

        if (response == null) {
            throw new RuntimeException("Response empty unavailable to update currency exchange rate");
        }

        List<CurrencyRateResponse> rates = new ArrayList<>(Arrays.asList(response));
        rates.add(new CurrencyRateResponse(AccountCurrency.USD, BigDecimal.ONE));

        updateCurrency(rates);
    }

    private void updateCurrency(List<CurrencyRateResponse> rates) {
        List<AccountCurrency> currenciesNames = rates.stream().map((r) -> r.quote()).toList();
        Map<AccountCurrency, BigDecimal> ratesMap = rates.stream().collect(Collectors.toMap(
                CurrencyRateResponse::quote,
                CurrencyRateResponse::rate));

        List<CurrencyEntity> currencyEntities = currencyRepository.findAllByNameIn(currenciesNames);
        currencyEntities.stream().forEach((currencyEntity) -> {
            if (ratesMap.containsKey(currencyEntity.getName())) {
                currencyEntity.setRateFromUSD(ratesMap.get(currencyEntity.getName()));
            }
        });

        currencyRepository.saveAll(currencyEntities);
    }
}
