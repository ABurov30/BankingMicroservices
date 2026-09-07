package apigateway.client;

import account.contract.v1.AccountResponse;
import apigateway.dto.request.transaction.CreateTransactionRequestDto;
import apigateway.dto.response.transaction.CreateTransactionResponseDto;
import apigateway.dto.response.transaction.TransactionResponseDto;
import apigateway.mapper.grpc.TransactionGrpcMapper;
import apigateway.mapper.result.TransactionResultMapper;
import com.google.protobuf.Empty;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import transaction.contract.v1.*;

@Service
public class TransactionGrpcClient {
  private final TransactionRpcServiceGrpc.TransactionRpcServiceBlockingStub stub;
  private final TransactionGrpcMapper grpcMapper;
  private final TransactionResultMapper dtoMapper;

  public TransactionGrpcClient(
      TransactionRpcServiceGrpc.TransactionRpcServiceBlockingStub stub,
      TransactionGrpcMapper grpcMapper,
      TransactionResultMapper dtoMapper) {
    this.stub = stub;
    this.grpcMapper = grpcMapper;
    this.dtoMapper = dtoMapper;
  }

  public String getTransactionHealth() {
    GetTransactionHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getTransactionHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }

  public CreateTransactionResponseDto createTransaction(
      CreateTransactionRequestDto request, UUID sourceAuthUserId) {
    CreateTransactionGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .createTransaction(
                grpcMapper.toCreateTransactionGrpcRequest(request, sourceAuthUserId));
    return dtoMapper.toCreateTransactionResponseDto(response);
  }

  public List<TransactionResponseDto> getTransactionsByAccounts(List<AccountResponse> accounts) {
    GetTransactionsByAccountsGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getTransactionsByAccounts(grpcMapper.toGetTransactionsByAccountsGrpcRequest(accounts));
    return grpcMapper.toTransactionResponseDtos(response);
  }
}
