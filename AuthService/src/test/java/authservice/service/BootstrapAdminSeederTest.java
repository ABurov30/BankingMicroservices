package authservice.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import authservice.config.BootstrapAdminProperties;
import authservice.entity.AuthUserEntity;
import authservice.entity.RoleEntity;
import authservice.entity.UserRoleEntity;
import authservice.repository.AuthUserRepository;
import authservice.repository.RoleRepository;
import authservice.repository.UserRoleRepository;
import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminSeederTest {
  @Mock private AuthUserRepository users;
  @Mock private RoleRepository roles;
  @Mock private UserRoleRepository userRoles;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final BootstrapAdminProperties properties =
      new BootstrapAdminProperties("admin@example.com", "test-password-12345");
  private BootstrapAdminSeeder seeder;
  private RoleEntity adminRole;

  @BeforeEach
  void setUp() {
    adminRole = new RoleEntity();
    adminRole.setName(Roles.ADMIN);
    when(roles.findByNameForUpdate(Roles.ADMIN)).thenReturn(Optional.of(adminRole));
    seeder = new BootstrapAdminSeeder(properties, users, roles, userRoles, encoder);
  }

  @Test
  void createsVerifiedActiveAdminWithHashedPassword() {
    seeder.run(null);
    var userCaptor = ArgumentCaptor.forClass(AuthUserEntity.class);
    verify(users).saveAndFlush(userCaptor.capture());
    var user = userCaptor.getValue();
    assertThat(user.getEmail()).isEqualTo(properties.email());
    assertThat(user.getStatus()).isEqualTo(AuthUserStatus.ACTIVE);
    assertThat(user.isEmailVerified()).isTrue();
    assertThat(encoder.matches(properties.password(), user.getPasswordHash())).isTrue();
    assertThat(user.getPasswordHash()).isNotEqualTo(properties.password());
    var roleCaptor = ArgumentCaptor.forClass(UserRoleEntity.class);
    verify(userRoles).save(roleCaptor.capture());
    assertThat(roleCaptor.getValue().getAuthUser()).isSameAs(user);
    assertThat(roleCaptor.getValue().getRole()).isSameAs(adminRole);
  }

  @Test
  void existingAdminSkipsAllCredentialChanges() {
    when(userRoles.existsByRoleName(Roles.ADMIN)).thenReturn(true);
    seeder.run(null);
    verifyNoInteractions(users);
    verify(userRoles, never()).save(any());
  }

  @Test
  void refusesToPromoteExistingEmail() {
    when(users.existsByEmail(properties.email())).thenReturn(true);
    assertThatThrownBy(() -> seeder.run(null)).isInstanceOf(IllegalStateException.class);
    verify(users, never()).saveAndFlush(any());
    verify(userRoles, never()).save(any());
  }

  @Test
  void rejectsPasswordExceedingBcryptByteLimit() {
    seeder =
        new BootstrapAdminSeeder(
            new BootstrapAdminProperties("admin@example.com", "я".repeat(40)),
            users,
            roles,
            userRoles,
            encoder);
    assertThatThrownBy(() -> seeder.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("72 UTF-8 bytes");
    verify(users, never()).saveAndFlush(any());
  }
}
