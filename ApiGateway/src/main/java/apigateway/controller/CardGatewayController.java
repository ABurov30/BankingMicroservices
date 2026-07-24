package apigateway.controller;

import apigateway.client.CardGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.UpdateCardRequestDto;
import apigateway.dto.card.UpdateCardResponseDto;
import apigateway.exception.InvalidAccessTokenException;
import apigateway.exception.MissingAccessTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/card")
public class CardGatewayController {

    private final CardGrpcClient cardClient;
    private final CookieConfig cookieConfig;
    private final JwtDecoder jwtDecoder;

    public CardGatewayController(
            CardGrpcClient cardClient,
            CookieConfig cookieConfig,
            JwtDecoder jwtDecoder
    ) {
        this.cardClient = cardClient;
        this.cookieConfig = cookieConfig;
        this.jwtDecoder = jwtDecoder;
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
        Jwt jwt = getAccessTokenJwt(httpRequest);
        return cardClient.createCard(request, UUID.fromString(jwt.getSubject()), extractRole(jwt));
    }

    @PutMapping("/update")
    public UpdateCardResponseDto updateCard(
            @Valid @RequestBody UpdateCardRequestDto request,
            HttpServletRequest httpRequest
    ) {
        Jwt jwt = getAccessTokenJwt(httpRequest);
        return cardClient.updateCard(request, UUID.fromString(jwt.getSubject()), extractRole(jwt));
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
