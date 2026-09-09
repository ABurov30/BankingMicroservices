package cardservice.repository;

import cardservice.entity.ProcessedEventEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import processedevent.BaseProcessedEventRepository;

public interface ProcessedEventRepository
    extends BaseProcessedEventRepository<ProcessedEventEntity> {

  @Modifying
  @Query(
      value =
          """
        INSERT INTO processed_events (id, event_key, processed_at)
        VALUES (:id, :eventKey, :processedAt)
        ON CONFLICT (event_key) DO NOTHING
          """,
      nativeQuery = true)
  int tryClaim(
      @Param("id") UUID id,
      @Param("eventKey") String eventKey,
      @Param("processedAt") Instant processedAt);
}
