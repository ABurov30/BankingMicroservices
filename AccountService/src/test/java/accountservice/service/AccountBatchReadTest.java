package accountservice.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import accountservice.dto.GetAccountByIdsForTransactionCommand;
import accountservice.entity.AccountEntity;
import accountservice.exception.AccountNotFoundException;
import accountservice.mapper.result.AccountResultMapper;
import accountservice.repository.AccountRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountBatchReadTest {
  @Mock private AccountRepository repository;
  @Mock private AccountResultMapper mapper;
  @InjectMocks private AccountService service;

  @Test
  void emptyInputSkipsDatabase() {
    assertThat(
            service.getAccountByIdsForTransaction(
                new GetAccountByIdsForTransactionCommand(List.of())))
        .isEmpty();
    verifyNoInteractions(repository, mapper);
  }

  @Test
  void oneInQueryDeduplicatesIdsAndRestoresRequestOrder() {
    var first = account();
    var second = account();
    var ids = List.of(first.getId(), second.getId());
    when(repository.findByIdIn(ids)).thenReturn(List.of(second, first));
    service.getAccountByIdsForTransaction(
        new GetAccountByIdsForTransactionCommand(
            List.of(first.getId(), second.getId(), first.getId())));
    var order = inOrder(mapper);
    order.verify(mapper).toGetAccountResult(first);
    order.verify(mapper).toGetAccountResult(second);
    verify(repository).findByIdIn(ids);
    verifyNoMoreInteractions(repository, mapper);
  }

  @Test
  void missingAccountFailsEntireRequest() {
    var ids = List.of(UUID.randomUUID());
    when(repository.findByIdIn(ids)).thenReturn(List.of());
    assertThatThrownBy(
            () ->
                service.getAccountByIdsForTransaction(
                    new GetAccountByIdsForTransactionCommand(ids)))
        .isInstanceOf(AccountNotFoundException.class);
  }

  private AccountEntity account() {
    var account = new AccountEntity();
    account.setId(UUID.randomUUID());
    return account;
  }
}
