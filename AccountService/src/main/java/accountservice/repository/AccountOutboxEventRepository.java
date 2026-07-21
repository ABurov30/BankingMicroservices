package accountservice.repository;

import accountservice.entity.AccountOutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import outboxsupport.OutboxEventStatus;

import java.util.List;
import java.util.UUID;

public interface AccountOutboxEventRepository extends JpaRepository<AccountOutboxEventEntity, UUID> {
    List<AccountOutboxEventEntity> findTop50ByOutboxEventStatusOrderByCreatedAtAsc(OutboxEventStatus outboxEventStatus);
}
