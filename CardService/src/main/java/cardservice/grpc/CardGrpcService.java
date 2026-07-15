package cardservice.grpc;


import card.contract.v1.CardRpcServiceGrpc;

import card.contract.v1.GetCardHealthGrpcRequest;
import card.contract.v1.GetCardHealthGrpcResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CardGrpcService extends CardRpcServiceGrpc.CardRpcServiceImplBase {

    @Override
    public void getCardHealth(GetCardHealthGrpcRequest request, StreamObserver<GetCardHealthGrpcResponse> responseObserver) {
        GetCardHealthGrpcResponse response = GetCardHealthGrpcResponse.newBuilder().setMessage("Card service GRPC health " + LocalDateTime.now()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
