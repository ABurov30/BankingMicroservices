package transactionservice.repository;

import outboxsupport.BaseProcessedEventRepository;
import transactionservice.entity.ProcessedEventEntity;

public interface ProcessedEventRepository
    extends BaseProcessedEventRepository<ProcessedEventEntity> {}
