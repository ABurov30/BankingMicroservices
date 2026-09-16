package apigateway.query;

import apigateway.cache.CacheProperties;
import apigateway.cache.RedisCacheService;
import apigateway.dto.command.account.GetAllAccountsWithCardsByAuthUserIdCommandDto;
import apigateway.dto.response.account.GetAccountWithCardsResponseDto;
import cache.CacheKeyGenerator;
import cache.enums.CachePath;
import cache.enums.CachePrefix;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountOverviewCachedQueryService {
  private final AccountQueryHandler loader;
  private final CacheProperties cacheProperties;
  private final RedisCacheService redisCacheService;
  private final Cache<String, List<GetAccountWithCardsResponseDto>> cache;
  private static final TypeReference<List<GetAccountWithCardsResponseDto>> ACCOUNT_OVERVIEW_TYPE =
      new TypeReference<>() {};

  public AccountOverviewCachedQueryService(
      AccountQueryHandler loader,
      CacheProperties cacheProperties,
      RedisCacheService redisCacheService) {
    this.loader = loader;
    this.cacheProperties = cacheProperties;
    this.redisCacheService = redisCacheService;
    this.cache =
        Caffeine.newBuilder()
            .maximumSize(cacheProperties.getL1MaxSize())
            .expireAfterWrite(cacheProperties.getL1Ttl())
            .build();
  }

  public List<GetAccountWithCardsResponseDto> getMyAccounts(
      GetAllAccountsWithCardsByAuthUserIdCommandDto dto) {
    if (!cacheProperties.isEnabled()) {
      return loader.getAccountsWithCardsByAuthUserId(dto);
    }

    var key =
        CacheKeyGenerator.generateKey(
            CachePrefix.ACCOUNT_OVERVIEW, CachePath.ME, dto.authUserId(), dto.role());

    return cache.get(
        key,
        ignored ->
            redisCacheService
                .get(key, ACCOUNT_OVERVIEW_TYPE)
                .orElseGet(
                    () -> {
                      var result = loader.getAccountsWithCardsByAuthUserId(dto);
                      redisCacheService.put(key, result, cacheProperties.getL2Ttl());
                      return result;
                    }));
  }
}
