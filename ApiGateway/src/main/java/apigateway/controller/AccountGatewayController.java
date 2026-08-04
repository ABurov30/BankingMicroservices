package apigateway.controller;

import apigateway.client.AccountGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.account.*;
import apigateway.query.AccountQueryHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/account")
public class AccountGatewayController {

    private final AccountGrpcClient accountClient;
    private final AccountQueryHandler accountOverviewQueryHandler;
    private final CookieConfig cookieConfig;

    public AccountGatewayController(
            AccountGrpcClient accountClient,
            AccountQueryHandler accountOverviewQueryHandler,
            CookieConfig cookieConfig
    ) {
        this.accountClient = accountClient;
        this.accountOverviewQueryHandler = accountOverviewQueryHandler;
        this.cookieConfig = cookieConfig;
    }

    @GetMapping("/health")
    public String getAccountHealth() {
        return accountClient.getAccountHealth();
    }

    @PostMapping("/create")
    public CreateAccountResponseDto postCreateAccount(
            @Valid @RequestBody CreateAccountRequestDto request,
            HttpServletRequest httpRequest
    ) {
        Jwt jwt = cookieConfig.getAccessTokenJwt(httpRequest);
        return accountClient.createAccount(request, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/accounts/{ownerUserId}")
    public List<GetAccountWithCardsResponseDto> getAccountsWithCardsByOwnerId (@PathVariable UUID ownerUserId) {
        return accountOverviewQueryHandler.getAccountsWithCardsByOwnerId(ownerUserId);
    }

    @PutMapping("/freeze/{accountId}")
    public void freezeAccount (
            @PathVariable UUID accountId,
            HttpServletRequest request
    ) {
        Jwt jwt = cookieConfig.getAccessTokenJwt(request);
        accountClient.freezeAccount(accountId, UUID.fromString(jwt.getSubject()), cookieConfig.extractRole(jwt));
    }

    @PutMapping("/unfreeze/{accountId}")
    public void unfreezeAccount (
            @PathVariable UUID accountId,
            HttpServletRequest request
    ) {
        Jwt jwt = cookieConfig.getAccessTokenJwt(request);
        accountClient.unfreezeAccount(accountId, UUID.fromString(jwt.getSubject()), cookieConfig.extractRole(jwt));
    }

    @GetMapping("/manager/all-accounts")
    public List<GetAccountWithCardsResponseDto> getAllAccountsWithCards () {
        return accountOverviewQueryHandler.getAllAccountsWithCards();
    }

    @PostMapping("/topUp")
    public GetAccountResponseDto topUpAccount (@Valid @RequestBody UpdateAccountBalanceRequestDto request) {
        return accountClient.topUpAccount(request);
    }

    @PostMapping("/withdraw")
    public GetAccountResponseDto withdrawAccount (@Valid @RequestBody UpdateAccountBalanceRequestDto request) {
        return accountClient.withdrawAccount(request);
    }
}
