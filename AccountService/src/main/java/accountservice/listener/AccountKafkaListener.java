package accountservice.listener;

import accountservice.mapper.command.AccountCommandMapper;
import accountservice.service.AccountService;
import kafkacontracts.account.TransactionFundsRequestedEventPayload;
import kafkacontracts.user.UserProfileBlockedEventPayload;
import kafkacontracts.user.UserProfileCreatedEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import processedevent.annotation.EventKey;
import processedevent.annotation.IdempotentKafkaEvent;

@Component
@RequiredArgsConstructor
public class AccountKafkaListener {

  private final AccountService accountService;
  private final AccountCommandMapper commandMapper;

  @IdempotentKafkaEvent
  @KafkaListener(topics = "#{T(kafkacontracts.user.UserEventType).USER_PROFILE_CREATED.getTopic()}")
  public void handleUserProfileCreated(
      UserProfileCreatedEventPayload payload,
      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
    accountService.createAccount(commandMapper.toCreateAccountCommand(payload));
  }

  @IdempotentKafkaEvent
  @KafkaListener(topics = "#{T(kafkacontracts.user.UserEventType).USER_PROFILE_BLOCKED.getTopic()}")
  public void handleUserProfileBlocked(
      UserProfileBlockedEventPayload payload,
      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
    accountService.freezeAccountByUserId(commandMapper.toFreezeAccountsByUserIdCommand(payload));
  }

  @IdempotentKafkaEvent
  @KafkaListener(
      topics =
          "#{T(kafkacontracts.transaction.TransactionEventType)"
              + ".TRANSACTION_FUNDS_REQUESTED.getTopic()}")
  public void handleTransactionFundsRequested(
      TransactionFundsRequestedEventPayload payload,
      @EventKey @Header(KafkaHeaders.RECEIVED_KEY) String eventKey) {
    accountService.transactionFundsRequest(commandMapper.toTransactionFundsRequestCommand(payload));
  }
}
