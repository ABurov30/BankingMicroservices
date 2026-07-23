package apigateway.controller;

import apigateway.client.CardGrpcClient;
import apigateway.dto.card.CreateCardRequestDto;
import apigateway.dto.card.CreateCardResponseDto;
import apigateway.dto.card.UpdateCardRequestDto;
import apigateway.dto.card.UpdateCardResponseDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/card")
public class CardGatewayController {

    private final CardGrpcClient cardClient;

    public CardGatewayController(CardGrpcClient cardClient) {
        this.cardClient = cardClient;
    }

    @GetMapping("/health")
    public String getCardHealth() {
        return cardClient.getCardHealth();
    }

    @PostMapping("/create")
    public CreateCardResponseDto createCard(@Valid @RequestBody CreateCardRequestDto request) {
        return cardClient.createCard(request);
    }

    @PutMapping("/uptade")
    public UpdateCardResponseDto updateCard(@Valid @RequestBody UpdateCardRequestDto request) {
        return cardClient.updateCard(request);
    }
}
