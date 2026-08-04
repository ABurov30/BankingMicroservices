package accountservice.repository;

import accountservice.entity.CurrencyEntity;
import enums.account.AccountCurrency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CurrencyRepository extends JpaRepository<CurrencyEntity, UUID> {
    CurrencyEntity findByName (AccountCurrency name);
    List<CurrencyEntity> findAllByNameIn (List<AccountCurrency> names);
}
