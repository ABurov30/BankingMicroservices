package apigateway.query;

import apigateway.cache.CacheProperties;
import apigateway.cache.RedisCacheService;
import apigateway.dto.response.user.GetUserInfoWithAuthInfoResponseDto;
import cache.BaseCachedQueryService;
import cache.CacheKeyGenerator;
import cache.CaffeineCacheServiceFactory;
import cache.enums.CachePath;
import cache.enums.CachePrefix;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserInfoCachedQueryService
    extends BaseCachedQueryService<GetUserInfoWithAuthInfoResponseDto> {
  private static final TypeReference<GetUserInfoWithAuthInfoResponseDto> USER_INFO_TYPE =
      new TypeReference<>() {};

  private final UserInfoQueryHandler loader;
  private final CacheProperties cacheProperties;
  private final RedisCacheService redisCacheService;

  public UserInfoCachedQueryService(
      UserInfoQueryHandler loader,
      CacheProperties cacheProperties,
      RedisCacheService redisCacheService) {
    super(CaffeineCacheServiceFactory.create(cacheProperties));
    this.loader = loader;
    this.cacheProperties = cacheProperties;
    this.redisCacheService = redisCacheService;
  }

  public GetUserInfoWithAuthInfoResponseDto getUserInfo(UUID authUserId) {
    if (!cacheProperties.isEnabled()) {
      return loader.getUserInfoWithAuthInfo(authUserId);
    }

    String key =
        CacheKeyGenerator.generateKey(CachePrefix.USER_INFO, CachePath.AUTH_USER, authUserId);
    return getFromL1(
        key,
        ignored ->
            redisCacheService
                .get(key, USER_INFO_TYPE)
                .orElseGet(
                    () -> {
                      var result = loader.getUserInfoWithAuthInfo(authUserId);
                      redisCacheService.put(key, result);
                      return result;
                    }));
  }
}
