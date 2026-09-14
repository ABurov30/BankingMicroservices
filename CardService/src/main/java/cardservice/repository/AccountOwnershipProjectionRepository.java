package cardservice.repository;

import cardservice.entity.AccountOwnershipProjectionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountOwnershipProjectionRepository
    extends JpaRepository<AccountOwnershipProjectionEntity, UUID> {
  List<AccountOwnershipProjectionEntity> findByAccountIdIn(List<UUID> accountIds);
}
