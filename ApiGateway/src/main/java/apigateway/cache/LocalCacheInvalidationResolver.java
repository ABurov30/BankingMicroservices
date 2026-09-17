package apigateway.cache;

import apigateway.query.AccountOverviewCachedQueryService;
import apigateway.query.RecipientInfoCachedQueryService;
import apigateway.query.TransactionHistoryCachedQueryService;
import apigateway.query.UserInfoCachedQueryService;
import cache.CacheKeyGenerator;
import cache.enums.CachePrefix;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalCacheInvalidationResolver {
  private static final Logger log = LoggerFactory.getLogger(LocalCacheInvalidationResolver.class);

  private final AccountOverviewCachedQueryService accountOverviewCache;
  private final TransactionHistoryCachedQueryService transactionHistoryCache;
  private final UserInfoCachedQueryService userInfoCache;
  private final RecipientInfoCachedQueryService recipientInfoCache;

  public void invalidate(String key) {
    try {
      var parsedKey = CacheKeyGenerator.parseKey(key);
      CachePrefix prefix = (CachePrefix) parsedKey.get("prefix");

      switch (prefix) {
        case ACCOUNT_OVERVIEW -> accountOverviewCache.invalidateL1(key);
        case TRANSACTION_HISTORY -> transactionHistoryCache.invalidateL1(key);
        case USER_INFO -> userInfoCache.invalidateL1(key);
        case RECIPIENT_INFO -> recipientInfoCache.invalidateL1(key);
        default ->
            log.warn("Ignoring unsupported local cache prefix: prefix={}, key={}", prefix, key);
      }
    } catch (IllegalArgumentException | ClassCastException exception) {
      log.warn("Ignoring invalid local cache invalidation key: key={}", key, exception);
    }
  }
}
