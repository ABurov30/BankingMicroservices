package accountservice.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import processedevent.IdempotencyHandler;
import accountservice.service.IdempotencyTransactionService;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentKafkaEventAspect implements IdempotencyHandler {

  private final IdempotencyTransactionService idempotencyTransactionService;

  @Around("@annotation(processedevent.annotation.IdempotentKafkaEvent)")
  public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    return idempotencyTransactionService.process(joinPoint);
  }
}
