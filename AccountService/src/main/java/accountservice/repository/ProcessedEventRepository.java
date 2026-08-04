package accountservice.repository;

import accountservice.entity.ProcessedEventEntity;
import outboxsupport.BaseProcessedEventRepository;

public interface ProcessedEventRepository extends BaseProcessedEventRepository<ProcessedEventEntity> {
}
