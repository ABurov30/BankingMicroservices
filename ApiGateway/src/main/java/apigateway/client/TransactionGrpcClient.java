package apigateway.client;

import account.contract.v1.AccountResponse;
import apigateway.dto.transaction.CreateTransactionRequestDto;
import apigateway.dto.transaction.CreateTransactionResponseDto;
import apigateway.dto.transaction.TransactionResponseDto;
import apigateway.mapper.dto.TransactionDtoMapper;
import apigateway.mapper.grpc.TransactionGrpcMapper;
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
  private final TransactionDtoMapper dtoMapper;

  public TransactionGrpcClient(
      TransactionRpcServiceGrpc.TransactionRpcServiceBlockingStub stub,
      TransactionGrpcMapper grpcMapper,
      TransactionDtoMapper dtoMapper) {
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
      CreateTransactionRequestDto request, UUID sourceAuthUserId, UUID targetAuthUserId) {
    CreateTransactionGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .createTransaction(
                grpcMapper.toCreateTransactionGrpcRequest(
                    request, sourceAuthUserId, targetAuthUserId));
    return dtoMapper.toCreateTransactionResponseDto(response);
  }

  public List<TransactionResponseDto> getTransactionsByAccounts(List<AccountResponse> accounts) {
    GetTransactionsByAccountsGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getTransactionsByAccounts(grpcMapper.toGetTransactionsByAccountsGrpcRequest(accounts));
    return grpcMapper.toTransactionResponseDtos(response);
  }
}
