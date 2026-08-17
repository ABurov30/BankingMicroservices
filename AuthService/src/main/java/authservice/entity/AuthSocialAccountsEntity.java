package authservice.entity;

import enums.auth.SocialLoginProvider;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "auth_social_accounts")
public class AuthSocialAccountsEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private AuthUserEntity authUser;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false)
  private SocialLoginProvider provider;

  @Column(name = "provider_user_id", nullable = false)
  private String providerUserId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
