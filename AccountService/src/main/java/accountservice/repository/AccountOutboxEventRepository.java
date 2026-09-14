package accountservice.repository;

import accountservice.entity.AccountOutboxEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import outboxsupport.OutboxEventStatus;

public interface AccountOutboxEventRepository
    extends JpaRepository<AccountOutboxEventEntity, UUID> {
  long countByOutboxEventStatus(OutboxEventStatus status);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM account_outbox_events WHERE id = :id"
              + " AND status = 'PROCESSING' AND locked_by = :token"
              + " AND locked_at > CURRENT_TIMESTAMP - (:leaseMs * INTERVAL '1 millisecond'))",
      nativeQuery = true)
  boolean ownsAttempt(
      @Param("id") UUID id, @Param("token") String token, @Param("leaseMs") long leaseMs);
}
