package accountservice.repository;

import accountservice.entity.AccountOutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountOutboxRepository extends JpaRepository<AccountOutboxEventEntity, UUID> {
}
