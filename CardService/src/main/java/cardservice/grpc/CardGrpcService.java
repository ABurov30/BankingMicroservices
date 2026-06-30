package cardservice.grpc;


import card.contract.v1.CardRpcServiceGrpc;
import card.contract.v1.GetCardRequest;
import card.contract.v1.GetCardResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class CardGrpcService extends CardRpcServiceGrpc.CardRpcServiceImplBase {

    @Override
    public void getCard(GetCardRequest request, StreamObserver<GetCardResponse> responseObserver) {
        GetCardResponse response = GetCardResponse.newBuilder().setMessage("Card service GRPC").build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
