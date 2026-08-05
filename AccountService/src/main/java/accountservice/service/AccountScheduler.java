package accountservice.service;

import accountservice.exception.AccountsNotFoundException;
import accountservice.repository.AccountRepository;
import enums.account.AccountType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AccountScheduler {
    private final AccountRepository accountRepository;
    private static final BigDecimal DEBIT_RATE = new BigDecimal("7");

    public AccountScheduler (
            AccountRepository accountRepository
    ) {
        this.accountRepository = accountRepository;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Paris")
    @Transactional
    public void updateAccountsBalances() {
       var accounts = accountRepository.findAllByTypeForUpdate(AccountType.SAVINGS).orElseThrow(
               () -> new AccountsNotFoundException(AccountType.SAVINGS)
       );
        BigDecimal dailyMultiplier = BigDecimal.ONE.add(
                DEBIT_RATE.divide(
                        BigDecimal.valueOf(36_500),
                        12,
                        RoundingMode.HALF_UP
                )
        );
       accounts.stream().forEach(account -> {
           BigDecimal newBalance = account.getAvailableBalance()
                   .multiply(dailyMultiplier)
                   .setScale(2, RoundingMode.HALF_UP);

           account.setAvailableBalance(newBalance);
       });

       accountRepository.saveAll(accounts);
    }
}
