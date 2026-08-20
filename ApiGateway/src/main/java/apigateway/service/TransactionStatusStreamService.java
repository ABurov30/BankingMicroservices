package apigateway.service;

import apigateway.mapper.grpc.TransactionGrpcMapper;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import transaction.contract.v1.TransactionRpcServiceGrpc;
import transaction.contract.v1.TransactionStatusResponse;
import transaction.contract.v1.WatchTransactionStatusRequest;

@Service
public class TransactionStatusStreamService {
  private static final Logger logger =
      LoggerFactory.getLogger(TransactionStatusStreamService.class);

  private final TransactionRpcServiceGrpc.TransactionRpcServiceStub asyncStub;
  private final SimpMessagingTemplate messagingTemplate;
  private final TransactionGrpcMapper grpcMapper;
  private final Map<String, ClientCallStreamObserver<WatchTransactionStatusRequest>> streams =
      new ConcurrentHashMap<>();

  public TransactionStatusStreamService(
      TransactionRpcServiceGrpc.TransactionRpcServiceStub asyncStub,
      SimpMessagingTemplate messagingTemplate,
      TransactionGrpcMapper grpcMapper) {
    this.asyncStub = asyncStub;
    this.messagingTemplate = messagingTemplate;
    this.grpcMapper = grpcMapper;
  }

  public void watch(UUID transactionId, UUID authUserId, UUID subscriptionKey, String key) {
    unwatch(key);

    asyncStub.watchTransactionStatus(
        grpcMapper.toWatchTransactionStatusRequest(transactionId, authUserId, subscriptionKey),
        new ClientResponseObserver<WatchTransactionStatusRequest, TransactionStatusResponse>() {
          @Override
          public void beforeStart(
              ClientCallStreamObserver<WatchTransactionStatusRequest> requestStream) {
            streams.put(key, requestStream);
          }

          @Override
          public void onNext(TransactionStatusResponse response) {
            logger.debug(
                "Forwarding transaction status: transactionId={}, authUserId={}, status={}",
                transactionId,
                authUserId,
                response.getStatus());
            messagingTemplate.convertAndSendToUser(
                authUserId.toString(),
                "/queue/transactions/" + transactionId,
                grpcMapper.toTransactionStatusResponseDto(response));
          }

          @Override
          public void onError(Throwable throwable) {
            streams.remove(key);
            logger.warn(
                "Transaction status stream failed: transactionId={}, authUserId={}, streamKey={}",
                transactionId,
                authUserId,
                subscriptionKey,
                throwable);
          }

          @Override
          public void onCompleted() {
            streams.remove(key);
            logger.info(
                "Transaction status stream completed: tx={}, user={}, stream={}",
                transactionId,
                authUserId,
                subscriptionKey);
          }
        });
  }

  public void unwatch(String key) {
    ClientCallStreamObserver<WatchTransactionStatusRequest> stream = streams.remove(key);
    if (stream != null) {
      stream.cancel("STOMP subscription closed", null);
    }
  }
}
