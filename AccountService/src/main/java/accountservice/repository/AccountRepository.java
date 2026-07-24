package accountservice.repository;

import accountservice.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    boolean existsByAccountNumber(String accountNumber);

    Optional<List<AccountEntity>> findByOwnerUserId(UUID ownerUserId);
    Optional<List<AccountEntity>> findAllByOwnerUserId(UUID ownerUserId);
}
