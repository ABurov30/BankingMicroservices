package apigateway.query;

import apigateway.cache.CacheProperties;
import apigateway.cache.RedisCacheService;
import apigateway.dto.command.transaction.GetTransactionByMeCommandDto;
import apigateway.dto.response.transaction.TransactionResponseDto;
import cache.BaseCachedQueryService;
import cache.CacheKeyGenerator;
import cache.CaffeineCacheServiceFactory;
import cache.enums.CachePath;
import cache.enums.CachePrefix;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TransactionHistoryCachedQueryService
    extends BaseCachedQueryService<List<TransactionResponseDto>> {
  private static final TypeReference<List<TransactionResponseDto>> TRANSACTION_HISTORY_TYPE =
      new TypeReference<>() {};

  private final TransactionQueryHandler loader;
  private final CacheProperties cacheProperties;
  private final RedisCacheService redisCacheService;

  public TransactionHistoryCachedQueryService(
      TransactionQueryHandler loader,
      CacheProperties cacheProperties,
      RedisCacheService redisCacheService) {
    super(CaffeineCacheServiceFactory.create(cacheProperties));
    this.loader = loader;
    this.cacheProperties = cacheProperties;
    this.redisCacheService = redisCacheService;
  }

  public List<TransactionResponseDto> getTransactionsByMe(GetTransactionByMeCommandDto command) {
    if (!cacheProperties.isEnabled()) {
      return loader.getTransactionsByMe(command);
    }

    String key =
        CacheKeyGenerator.generateKey(
            CachePrefix.TRANSACTION_HISTORY, CachePath.ME, command.authUserId());
    return getFromL1(
        key,
        ignored ->
            redisCacheService
                .get(key, TRANSACTION_HISTORY_TYPE)
                .orElseGet(
                    () -> {
                      var result = loader.getTransactionsByMe(command);
                      redisCacheService.put(key, result);
                      return result;
                    }));
  }
}
