package apigateway.query;

import apigateway.cache.CacheProperties;
import apigateway.cache.RedisCacheService;
import apigateway.dto.command.account.GetAllAccountsWithCardsByAuthUserIdCommandDto;
import apigateway.dto.response.account.GetAccountWithCardsResponseDto;
import cache.BaseCachedQueryService;
import cache.CacheKeyGenerator;
import cache.CaffeineCacheServiceFactory;
import cache.enums.CachePath;
import cache.enums.CachePrefix;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountOverviewCachedQueryService
    extends BaseCachedQueryService<List<GetAccountWithCardsResponseDto>> {
  private final AccountQueryHandler loader;
  private final CacheProperties cacheProperties;
  private final RedisCacheService redisCacheService;
  private static final TypeReference<List<GetAccountWithCardsResponseDto>> ACCOUNT_OVERVIEW_TYPE =
      new TypeReference<>() {};

  private static final Logger log =
      LoggerFactory.getLogger(AccountOverviewCachedQueryService.class);

  public AccountOverviewCachedQueryService(
      AccountQueryHandler loader,
      CacheProperties cacheProperties,
      RedisCacheService redisCacheService) {
    super(CaffeineCacheServiceFactory.create(cacheProperties));
    this.loader = loader;
    this.cacheProperties = cacheProperties;
    this.redisCacheService = redisCacheService;
  }

  public List<GetAccountWithCardsResponseDto> getMyAccounts(
      GetAllAccountsWithCardsByAuthUserIdCommandDto dto) {
    if (!cacheProperties.isEnabled()) {
      return loader.getAccountsWithCardsByAuthUserId(dto);
    }

    var key =
        CacheKeyGenerator.generateKey(CachePrefix.ACCOUNT_OVERVIEW, CachePath.ME, dto.authUserId());
    return getFromL1(
        key,
        ignored ->
            redisCacheService
                .get(key, ACCOUNT_OVERVIEW_TYPE)
                .orElseGet(
                    () -> {
                      var result = loader.getAccountsWithCardsByAuthUserId(dto);
                      redisCacheService.put(key, result);
                      return result;
                    }));
  }
}
