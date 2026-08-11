package transactionservice.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import transaction.contract.v1.*;
import transactionservice.mapper.command.TransactionCommandMapper;
import transactionservice.service.TransactionService;

import java.time.LocalDateTime;

@Service
public class TransactionGrpcService
        extends TransactionRpcServiceGrpc.TransactionRpcServiceImplBase {

    private final TransactionService transactionService;
    private final TransactionCommandMapper commandMapper;

    public TransactionGrpcService(
            TransactionService transactionService,
            TransactionCommandMapper transactionCommandMapper
    ) {
        this.transactionService = transactionService;
        this.commandMapper = transactionCommandMapper;
    }


    @Override
    public void getTransactionHealth(
            Empty request, StreamObserver<GetTransactionHealthGrpcResponse> responseObserver) {
        GetTransactionHealthGrpcResponse response =
                GetTransactionHealthGrpcResponse.newBuilder().setMessage("Transaction service GRPC health " + LocalDateTime.now()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createTransaction(CreateTransactionGrpcRequest request, StreamObserver<Empty> responseObserver) {
        transactionService.createTransaction(commandMapper.toCreateTransactionCommand(request));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

}
