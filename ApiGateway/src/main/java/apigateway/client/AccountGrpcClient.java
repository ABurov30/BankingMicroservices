package apigateway.client;

import account.contract.v1.*;
import apigateway.dto.account.*;
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
      AccountGrpcMapper grpcMapper,
      AccountDtoMapper dtoMapper) {
    this.stub = stub;
    this.grpcMapper = grpcMapper;
    this.dtoMapper = dtoMapper;
  }

  public String getAccountHealth() {
    GetAccountHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAccountHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }

  public CreateAccountResponseDto createAccount(CreateAccountRequestDto request, UUID authUserId) {
    CreateAccountGrpcRequest grpcRequest =
        grpcMapper.toCreateAccountGrpcRequest(request, authUserId);
    return dtoMapper.toCreateAccountResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).createAccount(grpcRequest));
  }

  public List<GetAccountResponseDto> getAccountsByOwnerId(UUID ownerUserId) {
    GetAccountByOwnerUserIdGrpcRequest request =
        GetAccountByOwnerUserIdGrpcRequest.newBuilder()
            .setOwnerUserId(ownerUserId.toString())
            .build();
    GetAccountsGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAccountsByOwnerUserId(request);

    return dtoMapper.toListGetAccountResponseDto(response);
  }

  public List<GetAccountResponseDto> getAllAccounts() {
    GetAccountsGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAllAccounts(Empty.getDefaultInstance());
    return dtoMapper.toListGetAccountResponseDto(response);
  }

  public void freezeAccount(UUID accountId, UUID authUserId, String role) {
    FreezeAccountGrpcRequest request =
        FreezeAccountGrpcRequest.newBuilder()
            .setAccountId(accountId.toString())
            .setAuthUserId(authUserId.toString())
            .setRole(role == null ? "" : role)
            .build();
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).freezeAccount(request);
  }

  public void unfreezeAccount(UUID accountId, UUID authUserId, String role) {
    UnfreezeAccountGrpcRequest request =
        UnfreezeAccountGrpcRequest.newBuilder()
            .setAccountId(accountId.toString())
            .setAuthUserId(authUserId.toString())
            .setRole(role == null ? "" : role)
            .build();
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).unfreezeAccount(request);
  }

  public GetAccountResponseDto getAccountById(GetAccountByIdRequestDto request) {
    return dtoMapper.toGetAccountByIdResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getAccountById(grpcMapper.toGetAccountByIdGrpcRequest(request)));
  }

  public UUID getAccountOwnerAuthUserId(UUID accountId) {
    GetAccountByIdGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getAccountById(
                grpcMapper.toGetAccountByIdGrpcRequest(new GetAccountByIdRequestDto(accountId)));
    return UUID.fromString(response.getAccount().getAuthUserId());
  }

  public GetAccountResponseDto topUpAccount(
      UpdateAccountBalanceRequestDto request, UUID authUserId) {
    return dtoMapper.toGetAccountResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .topUpAccount(grpcMapper.toUpdateAccountBalanceGrpcRequest(request, authUserId)));
  }

  public GetAccountResponseDto withdrawAccount(
      UpdateAccountBalanceRequestDto request, UUID authUserId) {
    return dtoMapper.toGetAccountResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .withdrawAccount(grpcMapper.toUpdateAccountBalanceGrpcRequest(request, authUserId)));
  }
}
