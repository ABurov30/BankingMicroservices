package cardservice.repository;

import cardservice.entity.AccountOwnershipProjectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountOwnershipProjectionRepository extends JpaRepository<AccountOwnershipProjectionEntity, UUID> {
}
