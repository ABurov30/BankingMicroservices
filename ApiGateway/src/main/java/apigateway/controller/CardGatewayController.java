package apigateway.controller;

import apigateway.client.CardGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.UpdateCardRequestDto;
import apigateway.dto.card.UpdateCardResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/card")
public class CardGatewayController {

    private final CardGrpcClient cardClient;
    private final CookieConfig cookieConfig;

    public CardGatewayController(
            CardGrpcClient cardClient,
            CookieConfig cookieConfig
    ) {
        this.cardClient = cardClient;
        this.cookieConfig = cookieConfig;
    }

    @GetMapping("/health")
    public String getCardHealth() {
        return cardClient.getCardHealth();
    }

    @PostMapping("/create")
    public CreateCardResponseDto createCard(
            @Valid @RequestBody CreateCardRequestDto request,
            HttpServletRequest httpRequest
    ) {
        Jwt jwt = cookieConfig.getAccessTokenJwt(httpRequest);
        return cardClient.createCard(request, UUID.fromString(jwt.getSubject()), cookieConfig.extractRole(jwt));
    }

    @PutMapping("/update")
    public UpdateCardResponseDto updateCard(
            @Valid @RequestBody UpdateCardRequestDto request,
            HttpServletRequest httpRequest
    ) {
        Jwt jwt = cookieConfig.getAccessTokenJwt(httpRequest);
        return cardClient.updateCard(request, UUID.fromString(jwt.getSubject()), cookieConfig.extractRole(jwt));
    }
}
