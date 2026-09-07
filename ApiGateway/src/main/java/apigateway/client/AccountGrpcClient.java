package apigateway.client;

import account.contract.v1.*;
import apigateway.dto.command.account.GetAccountsWithCardsByOwnerIdCommandDto;
import apigateway.dto.request.account.CreateAccountRequestDto;
import apigateway.dto.request.account.GetAccountByIdRequestDto;
import apigateway.dto.request.account.GetAllAccountsRequestDto;
import apigateway.dto.request.account.UpdateAccountBalanceRequestDto;
import apigateway.dto.response.account.AccountResponseWithoutSensitiveInfo;
import apigateway.dto.response.account.CreateAccountResponseDto;
import apigateway.dto.response.account.GetAccountResponseDto;
import apigateway.mapper.grpc.AccountGrpcMapper;
import apigateway.mapper.result.AccountResultMapper;
import com.google.protobuf.Empty;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class AccountGrpcClient {
  private final AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub;
  private final AccountGrpcMapper grpcMapper;
  private final AccountResultMapper dtoMapper;

  public AccountGrpcClient(
      AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub,
      AccountGrpcMapper grpcMapper,
      AccountResultMapper dtoMapper) {
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

  public List<GetAccountResponseDto> getAccountsByOwnerId(
      GetAccountsWithCardsByOwnerIdCommandDto command) {
    GetAccountByOwnerUserIdGrpcRequest request =
        grpcMapper.toGetAccountByOwnerUserIdGrpcRequest(command);
    GetAccountsGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS).getAccountsByOwnerUserId(request);

    return dtoMapper.toListGetAccountResponseDto(response);
  }

  public List<AccountResponseWithoutSensitiveInfo> getRecipientAccounts(UUID ownerUserId) {
    GetRecipientAccountsByOwnerUserIdGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getRecipientAccountsByOwnerUserId(
                grpcMapper.toGetRecipientAccountsByOwnerUserIdGrpcRequest(ownerUserId));
    return dtoMapper.toRecipientAccounts(response);
  }

  public List<GetAccountResponseDto> getAllAccounts(GetAllAccountsRequestDto request) {
    GetAccountsGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getAllAccounts(grpcMapper.toGetAllAccountsGrpRequest(request));
    return dtoMapper.toListGetAccountResponseDto(response);
  }

  public void freezeAccount(UUID accountId, UUID authUserId, String role) {
    FreezeAccountGrpcRequest request =
        grpcMapper.toFreezeAccountGrpcRequest(accountId, authUserId, role);
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).freezeAccount(request);
  }

  public void unfreezeAccount(UUID accountId, UUID authUserId, String role) {
    UnfreezeAccountGrpcRequest request =
        grpcMapper.toUnfreezeAccountGrpcRequest(accountId, authUserId, role);
    stub.withDeadlineAfter(2, TimeUnit.SECONDS).unfreezeAccount(request);
  }

  public GetAccountResponseDto getAccountById(GetAccountByIdRequestDto request) {
    return dtoMapper.toGetAccountByIdResponseDto(
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getAccountById(grpcMapper.toGetAccountByIdGrpcRequest(request)));
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
