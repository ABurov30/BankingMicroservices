package cardservice.service;

import cardservice.dto.CreateCardResult;
import cardservice.dto.CreatedCardCommand;
import cardservice.entity.CardEntity;
import cardservice.mapper.CardMapper;
import enums.card.CardStatus;
import cardservice.repository.CardRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private static final int CARD_EXPIRATION_YEARS = 5;
    private static final String CARD_BIN = "400000";
    private static final int PAN_LENGTH = 16;
    private static final int ATTEMPTS_TO_CREATE_CARD =10;
    private static final int ATTEMPTS_TO_GENERATE_PAN =10;

    public CardService (
            CardRepository cardRepository,
            CardMapper cardMapper
    ) {
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
    }

    private String generateUniquePan () {

        for (int i = 0; i < ATTEMPTS_TO_GENERATE_PAN; i++) {
            String pan = generatePan();
            if (!cardRepository.existsByPan(pan)) {
                return pan;
            }
        }

        throw new IllegalStateException("Failed to generate unique pan");
    }

    private String generatePan() {
        StringBuilder panWithoutCheckDigit = new StringBuilder(CARD_BIN);

        while (panWithoutCheckDigit.length() < PAN_LENGTH - 1) {
            panWithoutCheckDigit.append(ThreadLocalRandom.current().nextInt(10));
        }

        int checkDigit = calculateLuanCheckDigit(panWithoutCheckDigit.toString());

        return panWithoutCheckDigit.append(checkDigit).toString();
    }

    private int calculateLuanCheckDigit(String number) {
        int sum = 0;
        boolean doubleDigit = true;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (doubleDigit) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return (10 - (sum % 10)) % 10;
    }

    public CreateCardResult createCard (CreatedCardCommand createdCardCommand) {
        for (int i = 0; i< ATTEMPTS_TO_CREATE_CARD; i++) {
            try {
                CardEntity cardEntity = new CardEntity();
                cardEntity.setAccountId(createdCardCommand.accountId());
                cardEntity.setPan(generateUniquePan());
                cardEntity.setCardStatus(CardStatus.ACTIVE);
                cardEntity.setDailyLimit(BigDecimal.ZERO);
                cardEntity.setMonthlyLimit(BigDecimal.ZERO);
                cardEntity.setExpiresAt(LocalDateTime.now().plusYears(CARD_EXPIRATION_YEARS));
                CardEntity savedCard = cardRepository.save(cardEntity);

                return cardMapper.toCreateCardResult(savedCard);
            } catch (DataIntegrityViolationException e) {
                // PAN collision, retry
            }
        }

        throw new IllegalStateException("Failed to create card after retries");
    }
}
