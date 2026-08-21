package accountservice.repository;

import accountservice.entity.CurrencyEntity;
import enums.common.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<CurrencyEntity, UUID> {
  CurrencyEntity findByName(Currency name);

  List<CurrencyEntity> findAllByNameIn(List<Currency> names);
}
