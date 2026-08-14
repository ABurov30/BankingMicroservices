package cardservice.grpc;

import card.contract.v1.*;
import cardservice.dto.*;
import cardservice.mapper.command.CardCommandMapper;
import cardservice.mapper.grpc.CardGrpcMapper;
import cardservice.service.CardService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CardGrpcService extends CardRpcServiceGrpc.CardRpcServiceImplBase {
  private final CardCommandMapper commandMapper;
  private final CardGrpcMapper grpcMapper;
  private final CardService cardService;

  public CardGrpcService(
      CardCommandMapper commandMapper, CardGrpcMapper grpcMapper, CardService cardService) {
    this.commandMapper = commandMapper;
    this.grpcMapper = grpcMapper;
    this.cardService = cardService;
  }

  @Override
  public void getCardHealth(
      Empty request, StreamObserver<GetCardHealthGrpcResponse> responseObserver) {
    GetCardHealthGrpcResponse response =
        GetCardHealthGrpcResponse.newBuilder()
            .setMessage("Card service GRPC health " + LocalDateTime.now())
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void createCard(
      CreateCardGrpcRequest request, StreamObserver<CreateCardGrpcResponse> responseObserver) {
    CreatedCardCommand createdCardCommand = commandMapper.toCreateCardCommand(request);
    CreateCardGrpcResponse response =
        grpcMapper.toCreateCardGrpcResponse(cardService.createCard(createdCardCommand));

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateCard(
      UpdateCardGrpcRequest request, StreamObserver<UpdateCardGrpcResponse> responseObserver) {
    UpdateCardCommand updateCardCommand = commandMapper.toUpdateCardCommand(request);
    UpdateCardGrpcResponse response =
        grpcMapper.toUpdateCardGrpcResponse(cardService.updateCard(updateCardCommand));

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getCardsByAccountId(
      GetCardByAccountIdGrpcRequest request,
      StreamObserver<GetCardsGrpcResponse> responseObserver) {
    GetCardsByAccountIdCommand command = commandMapper.toGetCardsByAccountIdCommand(request);
    List<GetCardResult> results = cardService.getCardsByAccountId(command);
    List<CardResponse> response = results.stream().map(grpcMapper::toCardResponse).toList();

    responseObserver.onNext(grpcMapper.toGetCardsGrpcResponse(response));
    responseObserver.onCompleted();
  }

  @Override
  public void reserveLimitsForTransaction(
      ReserveLimitsForTransactionGrpcRequest request,
      StreamObserver<ReserveLimitsForTransactionGrpcResponse> responseObserver) {
    var command = commandMapper.toReserveLimitsForTransactionCommand(request);
    var result = cardService.reserveLimitsForTransaction(command);
    var response = grpcMapper.toReserveLimitsGrpcResponse(result);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
