package apigateway.controller;

import apigateway.client.AccountGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.account.CreateAccountRequestDto;
import apigateway.dto.account.CreateAccountResponseDto;
import apigateway.dto.account.GetAccountWithCardsResponseDto;
import apigateway.exception.InvalidAccessTokenException;
import apigateway.exception.MissingAccessTokenException;
import apigateway.query.AccountOverviewQueryHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/account")
public class AccountGatewayController {

    private final AccountGrpcClient accountClient;
    private final AccountOverviewQueryHandler accountOverviewQueryHandler;
    private final CookieConfig cookieConfig;
    private final JwtDecoder jwtDecoder;

    public AccountGatewayController(
            AccountGrpcClient accountClient,
            AccountOverviewQueryHandler accountOverviewQueryHandler,
            CookieConfig cookieConfig,
            JwtDecoder jwtDecoder
    ) {
        this.accountClient = accountClient;
        this.accountOverviewQueryHandler = accountOverviewQueryHandler;
        this.cookieConfig = cookieConfig;
        this.jwtDecoder = jwtDecoder;
    }

    @GetMapping("/health")
    public String getAccountHealth() {
        return accountClient.getAccountHealth();
    }

    @PostMapping("/create")
    public CreateAccountResponseDto postCreateAccount(@Valid @RequestBody CreateAccountRequestDto request) {
        return accountClient.createAccount(request);
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
        Jwt jwt = getAccessTokenJwt(request);
        accountClient.freezeAccount(accountId, UUID.fromString(jwt.getSubject()), extractRole(jwt));
    }

    @GetMapping("/manager/all-accounts")
    public List<GetAccountWithCardsResponseDto> getAllAccountsWithCards () {
        return accountOverviewQueryHandler.getAllAccountsWithCards();
    }

    private String extractRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0);
        }

        return jwt.getClaimAsString("role");
    }

    private Jwt getAccessTokenJwt(HttpServletRequest request) {
        String accessToken = cookieConfig.getCookieByKey(request, "at");

        if (accessToken == null || accessToken.isBlank()) {
            throw new MissingAccessTokenException();
        }

        Jwt jwt = jwtDecoder.decode(accessToken);
        try {
            UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new InvalidAccessTokenException();
        }

        return jwt;
    }
}
