package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.CardGrpcClient;
import apigateway.dto.account.GetAccountResponseDto;
import apigateway.dto.account.GetAccountWithCardsResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccountOverviewQueryHandler {
    private final AccountGrpcClient accountGrpcClient;
    private final CardGrpcClient cardGrpcClient;

    public AccountOverviewQueryHandler (
            AccountGrpcClient accountGrpcClient,
            CardGrpcClient cardGrpcClient
    ) {
        this.accountGrpcClient = accountGrpcClient;
        this.cardGrpcClient = cardGrpcClient;
    }

    public List<GetAccountWithCardsResponseDto> getAllAccountsWithCards() {
        List<GetAccountResponseDto> accounts = accountGrpcClient.getAllAccounts();

        return accounts.stream()
                .map(account -> new GetAccountWithCardsResponseDto(
                        account,
                        cardGrpcClient.getCardsByAccountId(account.accountId())
                ))
                .toList();
    }

    public List<GetAccountWithCardsResponseDto> getAccountsWithCardsByOwnerId (UUID ownerUserId) {
        List<GetAccountResponseDto> accounts = accountGrpcClient.getAccountsByOwnerId(ownerUserId);

        return accounts.stream()
                .map(account -> new GetAccountWithCardsResponseDto(
                        account,
                        cardGrpcClient.getCardsByAccountId(account.accountId())
                ))
                .toList();
    }
}
