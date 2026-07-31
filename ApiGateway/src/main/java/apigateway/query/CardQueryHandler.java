package apigateway.query;

import apigateway.client.AccountGrpcClient;
import apigateway.client.CardGrpcClient;
import apigateway.dto.account.GetAccountByIdRequestDto;
import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.UpdateCardRequestDto;
import apigateway.dto.card.UpdateCardResponseDto;
import enums.account.AccountStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CardQueryHandler {
    private final AccountGrpcClient accountGrpcClient;
    private final CardGrpcClient cardGrpcClient;

    public CardQueryHandler(
            AccountGrpcClient accountGrpcClient,
            CardGrpcClient cardGrpcClient
    ) {
        this.accountGrpcClient = accountGrpcClient;
        this.cardGrpcClient = cardGrpcClient;
    }

    public CreateCardResponseDto createCard(CreateCardRequestDto request, UUID authUserId, String role) {
        checkAccountStatus(request.accountId());
        return cardGrpcClient.createCard(request, authUserId, role);
    }

    public UpdateCardResponseDto updateCard(UpdateCardRequestDto request, UUID authUserId, String role) {
        checkAccountStatus(request.accountId());
        return cardGrpcClient.updateCard(request, authUserId, role);
    }

    private void checkAccountStatus(UUID accountId) {
        var account = accountGrpcClient.getAccountById(new GetAccountByIdRequestDto(accountId));
        if (account.status() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account should be in active status");
        }
    }
}
