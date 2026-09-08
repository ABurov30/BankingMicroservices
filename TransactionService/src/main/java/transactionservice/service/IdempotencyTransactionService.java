package transactionservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import processedevent.IdempotencyHandler;
import transactionservice.repository.ProcessedEventRepository;

@Service
public class IdempotencyTransactionService implements IdempotencyHandler {

  private final ProcessedEventRepository repository;
  private final Counter duplicateEventsCounter;
  private final Logger logger = LoggerFactory.getLogger(IdempotencyTransactionService.class);

  public IdempotencyTransactionService(
      ProcessedEventRepository repository, MeterRegistry meterRegistry) {
    this.repository = repository;
    this.duplicateEventsCounter =
        Counter.builder("kafka.idempotency.duplicates")
            .description("Number of duplicate Kafka events skipped by idempotency handling")
            .register(meterRegistry);
  }

  @Transactional(rollbackFor = Throwable.class)
  public Object process(ProceedingJoinPoint joinPoint) throws Throwable {
    String eventKey = extractEventKey(joinPoint);

    if (repository.tryClaim(eventKey, Instant.now()) == 0) {
      duplicateEventsCounter.increment();
      logger.info("Skipping duplicate Kafka event: eventKey={}", eventKey);
      return null;
    }

    return joinPoint.proceed();
  }
}
