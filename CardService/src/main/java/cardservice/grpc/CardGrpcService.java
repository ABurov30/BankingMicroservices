package cardservice.grpc;

import account.contract.v1.CreateAccountGrpcRequest;
import card.contract.v1.*;
import cardservice.dto.CreatedCardCommand;
import cardservice.dto.GetCardResult;
import cardservice.dto.GetCardsByAccountIdCommand;
import cardservice.dto.UpdateCardCommand;
import cardservice.entity.CardEntity;
import cardservice.mapper.CardMapper;
import cardservice.service.CardService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public void updateCard(UpdateCardGrpcRequest request, StreamObserver<UpdateCardGrpcResponse> responseObserver) {
        UpdateCardCommand updateCardCommand = cardMapper.toUpdateCardCommand(request);
        UpdateCardGrpcResponse response = cardMapper.toUpdateCardGrpcResponse(cardService.updateCard(updateCardCommand));

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getCardsByAccountId (GetCardByAccountIdGrpcRequest request, StreamObserver<GetCardsGrpcResponse> responseObserver) {
        GetCardsByAccountIdCommand command = cardMapper.toGetCardsByAccountIdCommand(request);
        List<GetCardResult> results = cardService.getCardsByAccountId(command);
        List<CardResponse> response = results.stream().map(cardMapper::toCardResponse).toList();

        responseObserver.onNext(cardMapper.toGetCardsGrpcResponse(response));
        responseObserver.onCompleted();
    }
}
