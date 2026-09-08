package transactionservice.client;

import card.contract.v1.*;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import transactionservice.dto.ReservationResponseDto;
import transactionservice.mapper.dto.TransactionDtoMapper;

@Service
@RequiredArgsConstructor
public class CardGrpcClient {
  private final CardRpcServiceGrpc.CardRpcServiceBlockingStub stub;
  private final TransactionDtoMapper dtoMapper;

  public ReservationResponseDto reserveLimitsForTransaction(
      ReserveLimitsForTransactionGrpcRequest grpcRequest) {
    return dtoMapper.toReservationResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).reserveLimitsForTransaction(grpcRequest));
  }
}
