package cardservice.grpc;

import account.contract.v1.CreateAccountGrpcRequest;
import card.contract.v1.CardRpcServiceGrpc;
import card.contract.v1.CreateCardGrpcRequest;
import card.contract.v1.CreateCardGrpcResponse;
import card.contract.v1.GetCardHealthGrpcResponse;
import cardservice.dto.CreatedCardCommand;
import cardservice.mapper.CardMapper;
import cardservice.service.CardService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CardGrpcService extends CardRpcServiceGrpc.CardRpcServiceImplBase {
    private final CardMapper cardMapper;
    private final CardService cardService;

    public CardGrpcService(
            CardMapper cardMapper,
            CardService cardService
    ) {
        this.cardMapper = cardMapper;
        this.cardService = cardService;
    }

    @Override
    public void getCardHealth(Empty request, StreamObserver<GetCardHealthGrpcResponse> responseObserver) {
        GetCardHealthGrpcResponse response = GetCardHealthGrpcResponse.newBuilder().setMessage("Card service GRPC health " + LocalDateTime.now()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createCard(CreateCardGrpcRequest request, StreamObserver<CreateCardGrpcResponse> responseObserver) {
        CreatedCardCommand createdCardCommand = cardMapper.toCreateCardCommand(request);
        CreateCardGrpcResponse response = cardMapper.toCreateCardGrpcResponse(cardService.createCard(createdCardCommand));

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
