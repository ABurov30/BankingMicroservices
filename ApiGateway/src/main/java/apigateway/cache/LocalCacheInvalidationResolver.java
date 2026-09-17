package apigateway.cache;

import apigateway.query.AccountOverviewCachedQueryService;
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

  public void invalidate(String key) {
    try {
      var parsedKey = CacheKeyGenerator.parseKey(key);
      CachePrefix prefix = (CachePrefix) parsedKey.get("prefix");

      switch (prefix) {
        case ACCOUNT_OVERVIEW -> accountOverviewCache.invalidateL1(key);
        default ->
            log.warn("Ignoring unsupported local cache prefix: prefix={}, key={}", prefix, key);
      }
    } catch (IllegalArgumentException | ClassCastException exception) {
      log.warn("Ignoring invalid local cache invalidation key: key={}", key, exception);
    }
  }
}
