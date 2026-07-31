package apigateway.client;

import account.contract.v1.*;
import apigateway.dto.account.CreateAccountRequestDto;
import apigateway.dto.account.CreateAccountResponseDto;
import apigateway.dto.account.GetAccountResponseDto;
import apigateway.mapper.dto.AccountDtoMapper;
import apigateway.mapper.grpc.AccountGrpcMapper;
import com.google.protobuf.Empty;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class AccountGrpcClient {
  private final AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub;
  private final AccountGrpcMapper grpcMapper;
  private final AccountDtoMapper dtoMapper;

  public AccountGrpcClient(
          AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub,
          AccountGrpcMapper grpcMapper, AccountDtoMapper dtoMapper
  ) {
    this.stub = stub;
    this.grpcMapper = grpcMapper; this.dtoMapper = dtoMapper;
  }

  public String getAccountHealth() {
    GetAccountHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getAccountHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }

  public CreateAccountResponseDto createAccount(CreateAccountRequestDto request, UUID authUserId) {
      CreateAccountGrpcRequest grpcRequest = grpcMapper.toCreateAccountGrpcRequest(request, authUserId);
      return dtoMapper.toCreateAccountResponseDto(stub.withDeadlineAfter(2, TimeUnit.SECONDS).createAccount(grpcRequest));
  }

  public List<GetAccountResponseDto> getAccountsByOwnerId(UUID ownerUserId) {
      GetAccountByOwnerUserIdGrpcRequest request = GetAccountByOwnerUserIdGrpcRequest.newBuilder()
              .setOwnerUserId(ownerUserId.toString())
              .build();
      GetAccountsGrpcResponse response = stub.withDeadlineAfter(2, TimeUnit.SECONDS)
              .getAccountsByOwnerUserId(request);

      return dtoMapper.toListGetAccountResponseDto(response);
  }

  public List<GetAccountResponseDto> getAllAccounts() {
      GetAccountsGrpcResponse response = stub.withDeadlineAfter(2, TimeUnit.SECONDS)
              .getAllAccounts(Empty.getDefaultInstance());
      return dtoMapper.toListGetAccountResponseDto(response);
  }

  public void freezeAccount(UUID accountId, UUID authUserId, String role) {
        FreezeAccountGrpcRequest request = FreezeAccountGrpcRequest.newBuilder()
                .setAccountId(accountId.toString())
                .setAuthUserId(authUserId.toString())
                .setRole(role == null ? "" : role)
                .build();
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
              .freezeAccount(request);
  }
}
