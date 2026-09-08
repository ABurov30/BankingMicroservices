package apigateway.controller;

import apigateway.client.AccountGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.request.account.CreateAccountRequestDto;
import apigateway.dto.request.account.UpdateAccountBalanceRequestDto;
import apigateway.dto.response.account.CreateAccountResponseDto;
import apigateway.dto.response.account.GetAccountResponseDto;
import apigateway.dto.response.account.GetAccountWithCardsResponseDto;
import apigateway.dto.result.auth.AuthUserIdAndRoleResult;
import apigateway.mapper.command.AccountCommandMapper;
import apigateway.query.AccountQueryHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountGatewayController {

  private final AccountGrpcClient accountClient;
  private final AccountQueryHandler accountOverviewQueryHandler;
  private final CookieConfig cookieConfig;
  private final AccountCommandMapper accountCommandMapper;

  public AccountGatewayController(
      AccountGrpcClient accountClient,
      AccountQueryHandler accountOverviewQueryHandler,
      CookieConfig cookieConfig,
      AccountCommandMapper accountCommandMapper) {
    this.accountClient = accountClient;
    this.accountOverviewQueryHandler = accountOverviewQueryHandler;
    this.cookieConfig = cookieConfig;
    this.accountCommandMapper = accountCommandMapper;
  }

  @GetMapping("/health")
  public String getAccountHealth() {
    return accountClient.getAccountHealth();
  }

  @PostMapping("/create")
  public CreateAccountResponseDto createAccount(
      @Valid @RequestBody CreateAccountRequestDto request, HttpServletRequest httpRequest) {
    return accountOverviewQueryHandler.createAccount(
        accountCommandMapper.toCreateAccountCommandDto(
            request, cookieConfig.getAuthUserId(httpRequest)));
  }

  @GetMapping("/accounts/me")
  public List<GetAccountWithCardsResponseDto> getAccountsWithCardsByAuthUserId(
      HttpServletRequest httpRequest) {
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(httpRequest);
    return accountOverviewQueryHandler.getAccountsWithCardsByAuthUserId(
        accountCommandMapper.toGetAllAccountsWithCardsByAuthUserIdCommandDto(
            authUser.authUserId(), authUser.role()));
  }

  @PostMapping("/topUp")
  public GetAccountResponseDto topUpAccount(
      @Valid @RequestBody UpdateAccountBalanceRequestDto request, HttpServletRequest httpRequest) {
    return accountClient.topUpAccount(request, cookieConfig.getAuthUserId(httpRequest));
  }

  @PostMapping("/withdraw")
  public GetAccountResponseDto withdrawAccount(
      @Valid @RequestBody UpdateAccountBalanceRequestDto request, HttpServletRequest httpRequest) {
    return accountClient.withdrawAccount(request, cookieConfig.getAuthUserId(httpRequest));
  }

  @PreAuthorize("hasRole('USER')")
  @PutMapping("/freeze/{accountId}")
  public void freezeAccount(@PathVariable UUID accountId, HttpServletRequest request) {
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(request);
    accountClient.freezeAccount(accountId, authUser.authUserId(), authUser.role().name());
  }

  @PreAuthorize("hasRole('USER')")
  @PutMapping("/unfreeze/{accountId}")
  public void unfreezeAccount(@PathVariable UUID accountId, HttpServletRequest request) {
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(request);
    accountClient.unfreezeAccount(accountId, authUser.authUserId(), authUser.role().name());
  }

  @PutMapping("/manager/freeze/{accountId}")
  public void freezeAccountByManager(@PathVariable UUID accountId, HttpServletRequest request) {
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(request);
    accountClient.freezeAccount(accountId, authUser.authUserId(), authUser.role().name());
  }

  @PutMapping("/manager/unfreeze/{accountId}")
  public void unfreezeAccountByManager(@PathVariable UUID accountId, HttpServletRequest request) {
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(request);
    accountClient.unfreezeAccount(accountId, authUser.authUserId(), authUser.role().name());
  }

  @GetMapping("/manager/all-accounts")
  public List<GetAccountWithCardsResponseDto> getAllAccountsWithCards(HttpServletRequest request) {
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(request);
    return accountOverviewQueryHandler.getAllAccountsWithCards(
        accountCommandMapper.toGetAllAccountsWithCardsCommandDto(
            authUser.authUserId(), authUser.role()));
  }

  @GetMapping("/manager/accounts/{ownerUserId}")
  public List<GetAccountWithCardsResponseDto> getAccountsWithCardsByOwnerIdByManager(
      @PathVariable UUID ownerUserId, HttpServletRequest httpRequest) {
    AuthUserIdAndRoleResult authUser = cookieConfig.getAuthUserIdAndRole(httpRequest);
    return accountOverviewQueryHandler.getAccountsWithCardsByOwnerId(
        accountCommandMapper.toGetAccountsWithCardsByOwnerIdCommandDto(
            ownerUserId, authUser.authUserId(), authUser.role()));
  }
}
