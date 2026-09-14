package apigateway.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import apigateway.client.*;
import apigateway.dto.command.account.GetAllAccountsWithCardsCommandDto;
import apigateway.dto.command.card.GetCardsByAccountIdsCommandDto;
import apigateway.dto.response.account.GetAccountResponseDto;
import apigateway.dto.response.card.GetCardByAccountIdResponseDto;
import apigateway.mapper.request.AccountRequestMapper;
import apigateway.mapper.request.UserRequestMapper;
import enums.auth.Roles;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountQueryHandlerTest {
  private final AccountGrpcClient accounts = mock(AccountGrpcClient.class);
  private final CardGrpcClient cards = mock(CardGrpcClient.class);
  private final AccountQueryHandler handler =
      new AccountQueryHandler(
          accounts,
          cards,
          mock(AccountRequestMapper.class),
          mock(UserGrpcClient.class),
          mock(UserRequestMapper.class));

  @Test
  void joinsCardsByAccountIdAndPreservesAccountsWithoutCards() {
    var first = account();
    var second = account();
    var third = account();
    var firstCard = card(first.accountId());
    var thirdCard = card(third.accountId());
    var caller = UUID.randomUUID();
    var batch =
        new GetCardsByAccountIdsCommandDto(
            List.of(first.accountId(), second.accountId(), third.accountId()),
            caller,
            Roles.MANAGER);
    when(accounts.getAllAccounts(any())).thenReturn(List.of(first, second, third));
    when(cards.getCardsByAccountIds(batch)).thenReturn(List.of(thirdCard, firstCard));
    var result =
        handler.getAllAccountsWithCards(
            new GetAllAccountsWithCardsCommandDto(caller, Roles.MANAGER));
    assertThat(result).extracting(value -> value.account()).containsExactly(first, second, third);
    assertThat(result.get(0).cards()).containsExactly(firstCard);
    assertThat(result.get(1).cards()).isEmpty();
    assertThat(result.get(2).cards()).containsExactly(thirdCard);
    verify(cards).getCardsByAccountIds(batch);
    verifyNoMoreInteractions(cards);
  }

  @Test
  void skipsCardsForEmptyAccountList() {
    when(accounts.getAllAccounts(any())).thenReturn(List.of());
    assertThat(
            handler.getAllAccountsWithCards(
                new GetAllAccountsWithCardsCommandDto(UUID.randomUUID(), Roles.MANAGER)))
        .isEmpty();
    verifyNoInteractions(cards);
  }

  private GetAccountResponseDto account() {
    return new GetAccountResponseDto(UUID.randomUUID(), null, null, null, null, null, null, null);
  }

  private GetCardByAccountIdResponseDto card(UUID accountId) {
    return new GetCardByAccountIdResponseDto(
        UUID.randomUUID(), accountId, null, null, null, null, null, null, null, null);
  }
}
