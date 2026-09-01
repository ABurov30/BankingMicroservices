package notificationservice.aspect;

import java.lang.annotation.Annotation;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import notificationservice.annotation.EventKey;
import notificationservice.entity.ProcessedEventEntity;
import notificationservice.repository.ProcessedEventRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import processedevent.IdempotencyHandler;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentKafkaEventAspect implements IdempotencyHandler {

  private static final Logger log = LoggerFactory.getLogger(IdempotentKafkaEventAspect.class);

  private final ProcessedEventRepository processedEventRepository;

  @Around("@annotation(notificationservice.annotation.IdempotentKafkaEvent)")
  public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    String eventKey = extractEventKey(joinPoint);

    if (isAlreadyProcessed(eventKey, processedEventRepository)) {
      return null;
    }

    ProcessedEventEntity processedEvent = new ProcessedEventEntity();
    processedEvent.setEventKey(eventKey);
    processedEvent.setProcessedAt(Instant.now());

    try {
      markAsProcessed(processedEvent, processedEventRepository);
    } catch (DataIntegrityViolationException e) {
      log.warn("Skipping duplicate notification Kafka event: eventKey={}", eventKey, e);
      return null;
    }

    return joinPoint.proceed();
  }

  private String extractEventKey(ProceedingJoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Annotation[][] annotations = signature.getMethod().getParameterAnnotations();

    for (int i = 0; i < annotations.length; i++) {
      for (Annotation annotation : annotations[i]) {
        if (annotation instanceof EventKey) {
          return (String) args[i];
        }
      }
    }

    throw new IllegalArgumentException("Event key parameter not found");
  }
}
