package apigateway.controller;

import apigateway.client.AccountGrpcClient;
import apigateway.dto.account.CreateAccountRequestDto;
import apigateway.dto.account.CreateAccountResponseDto;
import apigateway.dto.account.GetAccountWithCardsResponseDto;
import apigateway.query.AccountOverviewQueryHandler;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/account")
public class AccountGatewayController {

    private final AccountGrpcClient accountClient;
    private final AccountOverviewQueryHandler accountOverviewQueryHandler;

    public AccountGatewayController(
            AccountGrpcClient accountClient,
            AccountOverviewQueryHandler accountOverviewQueryHandler
    ) {
        this.accountClient = accountClient;
        this.accountOverviewQueryHandler = accountOverviewQueryHandler;
    }

    @GetMapping("/health")
    public String getAccountHealth() {
        return accountClient.getAccountHealth();
    }

    @PostMapping("/create")
    public CreateAccountResponseDto postCreateAccount(@Valid @RequestBody CreateAccountRequestDto request) {
        return accountClient.createAccount(request);
    }

    @GetMapping("/all-accounts")
    public List<GetAccountWithCardsResponseDto> getAllAccountsWithCards () {
        return accountOverviewQueryHandler.getAllAccountsWithCards();
    }

    @GetMapping("/accounts/{ownerUserId}")
    public List<GetAccountWithCardsResponseDto> getAccountsWithCardsByOwnerId (@PathVariable UUID ownerUserId) {
        return accountOverviewQueryHandler.getAccountsWithCardsByOwnerId(ownerUserId);
    }
}
