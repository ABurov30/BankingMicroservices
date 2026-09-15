package authservice.service;

import authservice.config.BootstrapAdminProperties;
import authservice.entity.AuthUserEntity;
import authservice.entity.UserRoleEntity;
import authservice.repository.AuthOutboxEventRepository;
import authservice.repository.AuthUserRepository;
import authservice.repository.RoleRepository;
import authservice.repository.UserRoleRepository;
import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import kafkacontracts.auth.AuthEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.bootstrap-admin.enabled", havingValue = "true")
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class BootstrapAdminSeeder implements ApplicationRunner {
  private final BootstrapAdminProperties properties;
  private final AuthUserRepository users;
  private final AuthOutboxEventRepository outboxEvents;
  private final RoleRepository roles;
  private final UserRoleRepository userRoles;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    // Serialize bootstrap across service replicas using the existing ADMIN role row.
    var adminRole =
        roles
            .findByNameForUpdate(Roles.ADMIN)
            .orElseThrow(() -> new IllegalStateException("ADMIN role is missing"));
    if (userRoles.existsByRoleName(Roles.ADMIN)) {
      return;
    }
    if (users.existsByEmail(properties.email())) {
      throw new IllegalStateException("Bootstrap admin email is already registered");
    }
    if (properties.password().getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new IllegalStateException("Bootstrap admin password must not exceed 72 UTF-8 bytes");
    }

    var user = new AuthUserEntity();
    user.setEmail(properties.email());
    user.setPasswordHash(passwordEncoder.encode(properties.password()));
    user.setStatus(AuthUserStatus.ACTIVE);
    user.setEmailVerified(true);
    users.saveAndFlush(user);

    var userRole = new UserRoleEntity();
    userRole.setAuthUser(user);
    userRole.setRole(adminRole);
    userRoles.save(userRole);

    var outboxEvent = AuthOutboxEventFactory.create(user.getId(), AuthEventType.AUTH_USER_CREATED);
    outboxEvent.setPayload(
        Map.of(
            "authUserId", user.getId(),
            "email", user.getEmail(),
            "firstName", "Admin",
            "lastName", "Admin",
            "verificationCode", "",
            "status", user.getStatus().name(),
            "role", adminRole.getName().name()));
    outboxEvents.save(outboxEvent);
  }
}
