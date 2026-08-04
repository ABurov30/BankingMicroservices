package cardservice.repository;

import cardservice.entity.ProcessedEventEntity;
import outboxsupport.BaseProcessedEventRepository;

public interface ProcessedEventRepository extends BaseProcessedEventRepository<ProcessedEventEntity> {
}
