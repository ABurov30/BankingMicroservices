package apigateway.mapper.dto;

import apigateway.dto.auth.SocialLoginRequestDto;
import enums.auth.SocialLoginProvider;
import org.mapstruct.Mapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Mapper(componentModel = "spring")
public interface SocialLoginDtoMapper {
  default SocialLoginRequestDto toSocialLoginRequestDto(OidcUser user) {
    return new SocialLoginRequestDto(
        SocialLoginProvider.GOOGLE,
        user.getSubject(),
        user.getEmail(),
        user.getEmailVerified(),
        user.getGivenName(),
        user.getFamilyName());
  }
}
