package apigateway.query;

import apigateway.cache.CacheProperties;
import apigateway.cache.RedisCacheService;
import apigateway.dto.request.user.GetRecipientRequestDto;
import apigateway.dto.response.user.GetRecipientInfoResponseDto;
import cache.BaseCachedQueryService;
import cache.CacheKeyGenerator;
import cache.CaffeineCacheServiceFactory;
import cache.enums.CachePath;
import cache.enums.CachePrefix;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RecipientInfoCachedQueryService
    extends BaseCachedQueryService<GetRecipientInfoResponseDto> {
  private static final TypeReference<GetRecipientInfoResponseDto> RECIPIENT_INFO_TYPE =
      new TypeReference<>() {};

  private final UserInfoQueryHandler loader;
  private final CacheProperties cacheProperties;
  private final RedisCacheService redisCacheService;

  public RecipientInfoCachedQueryService(
      UserInfoQueryHandler loader,
      CacheProperties cacheProperties,
      RedisCacheService redisCacheService) {
    super(CaffeineCacheServiceFactory.create(cacheProperties));
    this.loader = loader;
    this.cacheProperties = cacheProperties;
    this.redisCacheService = redisCacheService;
  }

  public GetRecipientInfoResponseDto getRecipientInfo(GetRecipientRequestDto request) {
    if (!cacheProperties.isEnabled()) {
      return loader.getRecipientInfo(request);
    }

    String email = request.email().trim().toLowerCase(Locale.ROOT);
    String key = CacheKeyGenerator.generateKey(CachePrefix.RECIPIENT_INFO, CachePath.EMAIL, email);
    return getFromL1(
        key,
        ignored ->
            redisCacheService
                .get(key, RECIPIENT_INFO_TYPE)
                .orElseGet(
                    () -> {
                      var result = loader.getRecipientInfo(new GetRecipientRequestDto(email));
                      redisCacheService.put(key, result);
                      return result;
                    }));
  }
}
