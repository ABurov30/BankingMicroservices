package apigateway.security;

import apigateway.client.AuthGrpcClient;
import apigateway.dto.request.user.GetRoleByAuthUserIdRequestDto;
import apigateway.dto.response.user.GetAuthUserByIdResponseDto;
import enums.auth.AuthUserStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessStateRedisService {
  private static final String KEY_PREFIX = "auth:access-state:";

  private final StringRedisTemplate redisTemplate;
  private final AuthGrpcClient authClient;

  public boolean isActive(UUID authUserId) {
    String cachedState = redisTemplate.opsForValue().get(key(authUserId));
    if (cachedState != null) {
      return AuthUserStatus.ACTIVE.name().equals(cachedState);
    }

    GetAuthUserByIdResponseDto authUser =
        authClient.getAuthUserById(new GetRoleByAuthUserIdRequestDto(authUserId));
    update(authUser.authUserId(), authUser.status());
    return authUser.status() == AuthUserStatus.ACTIVE;
  }

  public void update(UUID authUserId, AuthUserStatus status) {
    redisTemplate.opsForValue().set(key(authUserId), status.name());
  }

  private String key(UUID authUserId) {
    return KEY_PREFIX + authUserId;
  }
}
