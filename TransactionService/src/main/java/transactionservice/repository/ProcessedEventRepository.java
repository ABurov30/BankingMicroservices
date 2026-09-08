package transactionservice.repository;

import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import processedevent.BaseProcessedEventRepository;
import transactionservice.entity.ProcessedEventEntity;

public interface ProcessedEventRepository
    extends BaseProcessedEventRepository<ProcessedEventEntity> {

  @Modifying
  @Query(
      value =
          """
        INSERT INTO processed_events (event_key, processed_at)
        VALUES (:eventKey, :processedAt)
        ON CONFLICT (event_key) DO NOTHING
          """,
      nativeQuery = true)
  int tryClaim(@Param("eventKey") String eventKey, @Param("processedAt") Instant processedAt);
}
