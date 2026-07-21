package apigateway.controller;

import apigateway.client.AccountGrpcClient;
import apigateway.dto.account.CreateAccountRequestDto;
import apigateway.dto.account.CreateAccountResponseDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountGatewayController {

    private final AccountGrpcClient accountClient;

    public AccountGatewayController(AccountGrpcClient accountClient) {
        this.accountClient = accountClient;
    }

    @GetMapping("/health")
    public String getAccountHealth() {
        return accountClient.getAccountHealth();
    }

    @PostMapping("/create")
    public CreateAccountResponseDto postCreateAccount(@Valid @RequestBody CreateAccountRequestDto request) {
        return accountClient.createAccount(request);
    }
}
