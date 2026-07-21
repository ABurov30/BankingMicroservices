package apigateway.client;

import account.contract.v1.*;
import apigateway.dto.account.CreateAccountRequestDto;
import apigateway.dto.account.CreateAccountResponseDto;
import apigateway.mapper.AccountMapper;
import com.google.protobuf.Empty;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class AccountGrpcClient {
  private final AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub;
  private final AccountMapper accountMapper;

  public AccountGrpcClient(
          AccountRpcServiceGrpc.AccountRpcServiceBlockingStub stub,
          AccountMapper accountMapper
  ) {
    this.stub = stub;
    this.accountMapper = accountMapper;
  }

  public String getAccountHealth() {
    GetAccountHealthGrpcResponse response =
        stub.withDeadlineAfter(2, TimeUnit.SECONDS)
            .getAccountHealth(Empty.getDefaultInstance());
    return response.getMessage();
  }

  public CreateAccountResponseDto createAccount(CreateAccountRequestDto request) {
      CreateAccountGrpcRequest grpcRequest = accountMapper.toCreateAccountGrpcRequest(request);
      return accountMapper.toCreateAccountResponseDto(stub.withDeadlineAfter(2, TimeUnit.SECONDS).createAccount(grpcRequest));
  }
}
