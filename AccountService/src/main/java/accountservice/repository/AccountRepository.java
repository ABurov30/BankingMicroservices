package accountservice.repository;

import accountservice.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    boolean existsByAccountNumber(String accountNumber);

    Optional<List<AccountEntity>> findByOwnerUserId(UUID ownerUserId);
    Optional<List<AccountEntity>> findAllByOwnerUserId(UUID ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a from AccountEntity a WHERE a.id = :accountId")
    Optional<AccountEntity> findByIdForUpdate(@Param("accountId") UUID accountId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a from AccountEntity a WHERE a.ownerUserId = :ownerUserId")
    Optional<List<AccountEntity>> findAllByOwnerUserIdForUpdate(@Param("ownerUserId") UUID ownerUserId);
}
