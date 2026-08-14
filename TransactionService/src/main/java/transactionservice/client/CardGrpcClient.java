package transactionservice.client;

import card.contract.v1.*;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import transactionservice.dto.ReservationResponseDto;
import transactionservice.mapper.dto.TransactionDtoMapper;

@Service
public class CardGrpcClient {
  private final CardRpcServiceGrpc.CardRpcServiceBlockingStub stub;
  private final TransactionDtoMapper dtoMapper;

  public CardGrpcClient(
      CardRpcServiceGrpc.CardRpcServiceBlockingStub stub, TransactionDtoMapper dtoMapper) {
    this.stub = stub;
    this.dtoMapper = dtoMapper;
  }

  public ReservationResponseDto reserveLimitsForTransaction(
      ReserveLimitsForTransactionGrpcRequest grpcRequest) {
    return dtoMapper.toReservationResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).reserveLimitsForTransaction(grpcRequest));
  }
}
