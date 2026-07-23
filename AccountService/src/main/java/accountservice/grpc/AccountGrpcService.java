package accountservice.grpc;

import account.contract.v1.*;
import accountservice.dto.CreateAccountCommand;
import accountservice.dto.GetAccountResult;
import accountservice.dto.GetAccountsByOwnerUserIdCommand;
import accountservice.mapper.AccountMapper;
import accountservice.service.AccountService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountGrpcService extends AccountRpcServiceGrpc.AccountRpcServiceImplBase {

    private final AccountMapper accountMapper;
    private final AccountService accountService;

    public AccountGrpcService(
            AccountMapper accountMapper,
            AccountService accountService
    ) {
        this.accountMapper = accountMapper;
        this.accountService = accountService;
    }

    @Override
    public void getAccountHealth(Empty request, StreamObserver<GetAccountHealthGrpcResponse> responseObserver) {
        GetAccountHealthGrpcResponse response = GetAccountHealthGrpcResponse.newBuilder().setMessage("Account service GRPC health " + LocalDateTime.now()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createAccount(CreateAccountGrpcRequest request, StreamObserver<CreateAccountGrpcResponse> responseObserver) {
        CreateAccountCommand updateAccountCommand = accountMapper.toCreateAccountCommand(request);
        CreateAccountGrpcResponse response = accountMapper.toCreateAccountGrpcResponse(accountService.createAccount(updateAccountCommand));

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAccountsByOwnerUserId(GetAccountByOwnerUserIdGrpcRequest request, StreamObserver<GetAccountsGrpcResponse> responseObserver) {
        GetAccountsByOwnerUserIdCommand command = accountMapper.toGetAccountsByOwnerUserIdCommand(request);
        List<GetAccountResult> result = accountService.getAccountsByOwnerUserId(command);
        List<AccountResponse> accountResponseList = result.stream()
                .map(accountMapper::toAccountResponse)
                .toList();

        responseObserver.onNext(accountMapper.toGetAccountsGrpcResponse(accountResponseList));
        responseObserver.onCompleted();
    }

    @Override
    public void getAllAccounts(Empty request, StreamObserver<GetAccountsGrpcResponse> responseObserver) {
        List<GetAccountResult> result = accountService.getAllAccounts();
        List<AccountResponse> accountResponseList = result.stream()
                .map(accountMapper::toAccountResponse)
                .toList();

        responseObserver.onNext(accountMapper.toGetAccountsGrpcResponse(accountResponseList));
        responseObserver.onCompleted();
    }
}
