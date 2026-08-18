package authservice.repository;

import authservice.entity.AuthSocialAccountsEntity;
import enums.auth.SocialLoginProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSocialAccountsRepository
    extends JpaRepository<AuthSocialAccountsEntity, UUID> {
  Optional<AuthSocialAccountsEntity> findByProviderAndProviderUserId(
      SocialLoginProvider provider, String providerSubject);

  List<AuthSocialAccountsEntity> findAllByAuthUserId(UUID authUserId);
}
