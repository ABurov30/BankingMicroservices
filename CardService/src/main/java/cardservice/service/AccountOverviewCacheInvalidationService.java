package cardservice.service;

import cache.CacheKeyGenerator;
import cache.enums.CachePath;
import cache.enums.CachePrefix;
import cardservice.repository.AccountOwnershipProjectionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kafkacontracts.cache.CacheEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountOverviewCacheInvalidationService {
  private final AccountOwnershipProjectionRepository accountOwnershipProjectionRepository;
  private final CardOutboxService cardOutboxService;

  public void invalidate(UUID aggregateId, UUID accountId) {
    accountOwnershipProjectionRepository
        .findById(accountId)
        .ifPresent(projection -> invalidateOwner(aggregateId, projection.getOwnerAuthUserId()));
  }

  public void invalidateAll() {
    accountOwnershipProjectionRepository
        .findAll()
        .forEach(
            projection ->
                invalidateOwner(projection.getAccountId(), projection.getOwnerAuthUserId()));
  }

  private void invalidateOwner(UUID aggregateId, UUID ownerAuthUserId) {
    String key =
        CacheKeyGenerator.generateKey(CachePrefix.ACCOUNT_OVERVIEW, CachePath.ME, ownerAuthUserId);
    cardOutboxService.saveCacheInvalidationEvent(
        aggregateId, CacheEventType.CACHE_INVALIDATION, Map.of("keys", List.of(key)));
  }
}
