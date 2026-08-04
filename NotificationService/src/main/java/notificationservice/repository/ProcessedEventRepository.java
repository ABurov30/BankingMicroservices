package notificationservice.repository;

import notificationservice.entity.ProcessedEventEntity;
import outboxsupport.BaseProcessedEventRepository;

public interface ProcessedEventRepository extends BaseProcessedEventRepository<ProcessedEventEntity> {
}
