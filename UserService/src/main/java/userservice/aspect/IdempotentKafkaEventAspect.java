package userservice.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import outboxsupport.IdempotencyHandler;
import userservice.annotation.EventKey;
import userservice.annotation.IdempotentKafkaEvent;
import userservice.entity.ProcessedEventEntity;
import userservice.repository.ProcessedEventRepository;

import java.lang.annotation.Annotation;
import java.time.Instant;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentKafkaEventAspect implements IdempotencyHandler {

    private final ProcessedEventRepository processedEventRepository;

    @Around("@annotation(userservice.annotation.IdempotentKafkaEvent)")
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
