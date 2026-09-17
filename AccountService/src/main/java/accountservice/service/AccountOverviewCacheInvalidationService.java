package accountservice.service;

import accountservice.entity.AccountEntity;
import cache.CacheKeyGenerator;
import cache.enums.CachePath;
import cache.enums.CachePrefix;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kafkacontracts.cache.CacheEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountOverviewCacheInvalidationService {
  private final AccountOutboxService accountOutboxService;

  public void invalidate(AccountEntity account) {
    invalidate(account.getId(), account.getOwnerAuthUserId());
  }

  public void invalidate(UUID aggregateId, UUID ownerAuthUserId) {
    String key =
        CacheKeyGenerator.generateKey(CachePrefix.ACCOUNT_OVERVIEW, CachePath.ME, ownerAuthUserId);
    accountOutboxService.saveCacheInvalidateEvent(
        aggregateId, CacheEventType.CACHE_INVALIDATION, Map.of("keys", List.of(key)));
  }

  public void invalidateTransactionHistory(AccountEntity account) {
    String key =
        CacheKeyGenerator.generateKey(
            CachePrefix.TRANSACTION_HISTORY, CachePath.ME, account.getOwnerAuthUserId());
    accountOutboxService.saveCacheInvalidateEvent(
        account.getId(), CacheEventType.CACHE_INVALIDATION, Map.of("keys", List.of(key)));
  }
}
