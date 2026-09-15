package cardservice.grpc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import card.contract.v1.*;
import cardservice.mapper.command.CardCommandMapper;
import cardservice.mapper.grpc.CardGrpcMapper;
import cardservice.service.CardService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardGrpcServiceTest {
  private final CardCommandMapper commands = mock(CardCommandMapper.class);
  private final CardGrpcMapper responses = mock(CardGrpcMapper.class);
  private final CardService service = mock(CardService.class);
  private final CardGrpcService grpc = new CardGrpcService(commands, responses, service);

  @Test
  void handlesAllCardGrpcOperations() {
    grpc.getCardHealth(Empty.getDefaultInstance(), mock(StreamObserver.class));
    grpc.createCard(CreateCardGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.updateCard(UpdateCardGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    when(service.getCardsByAccountId(any())).thenReturn(List.of());
    when(service.getCardsByAccountIds(any())).thenReturn(List.of());
    grpc.getCardsByAccountId(
        GetCardByAccountIdGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.getCardsByAccountIds(
        GetCardByAccountIdsGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    grpc.reserveLimitsForTransaction(
        ReserveLimitsForTransactionGrpcRequest.getDefaultInstance(), mock(StreamObserver.class));
    verify(service).createCard(any());
    verify(service).updateCard(any());
  }
}
