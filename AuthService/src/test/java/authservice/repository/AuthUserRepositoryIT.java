package authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import authservice.entity.AuthUserEntity;
import enums.auth.AuthUserStatus;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("integration")
@ActiveProfiles("test")
@Testcontainers
@DataJpaTest
public class AuthUserRepositoryIT {
  private static final String EMAIL = "test@example.com";
  private static final String SECOND_EMAIL = "second_test@example.com";
  private static final String PASSWORD_HASH = "test_password";

  @Autowired private EntityManager entityManager;

  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private AuthUserRepository repository;

  private AuthUserEntity prepareAuthUserEntity(String email) {
    AuthUserEntity authUser = new AuthUserEntity();
    authUser.setPasswordHash(PASSWORD_HASH);
    authUser.setEmail(email);

    return authUser;
  }

  @Test
  void shouldFindUserById() {
    AuthUserEntity authUser = prepareAuthUserEntity(EMAIL);

    repository.saveAndFlush(authUser);
    entityManager.clear();

    var result = repository.findById(authUser.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(authUser.getId());
    assertThat(result.get().getPasswordHash()).isEqualTo(PASSWORD_HASH);
    assertThat(result.get().getEmail()).isEqualTo(EMAIL);
    assertThat(result.get().isEmailVerified()).isEqualTo(false);
    assertThat(result.get().getStatus()).isEqualTo(AuthUserStatus.PENDING);
  }

  @Test
  void shouldReturnEmptyWhenUserIdDoesNotExist() {
    UUID unknownUserId = UUID.randomUUID();
    var result = repository.findById(unknownUserId);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldFindUserByEmail() {
    AuthUserEntity authUser = prepareAuthUserEntity(EMAIL);

    repository.saveAndFlush(authUser);
    entityManager.clear();

    var result = repository.findByEmail(authUser.getEmail());

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(authUser.getId());
    assertThat(result.get().getPasswordHash()).isEqualTo(PASSWORD_HASH);
    assertThat(result.get().getEmail()).isEqualTo(EMAIL);
    assertThat(result.get().isEmailVerified()).isFalse();
    assertThat(result.get().getStatus()).isEqualTo(AuthUserStatus.PENDING);
  }

  @Test
  void shouldReturnEmptyWhenEmailDoesNotExist() {
    String unknownEmail = "unknown@gmail.com";
    var result = repository.findByEmail(unknownEmail);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldRejectDuplicateEmail() {
    var authUserEntity = prepareAuthUserEntity(EMAIL);
    var secondAuthUserEntity = prepareAuthUserEntity(EMAIL);

    repository.saveAndFlush(authUserEntity);
    assertThatThrownBy(() -> repository.saveAndFlush(secondAuthUserEntity))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldFindCorrectUserAmongMultipleUsers() {
    var authUserEntity = prepareAuthUserEntity(EMAIL);
    var secondAuthUserEntity = prepareAuthUserEntity(SECOND_EMAIL);

    repository.save(authUserEntity);
    repository.saveAndFlush(secondAuthUserEntity);
    entityManager.clear();

    var result = repository.findByEmail(SECOND_EMAIL);
    assertThat(result.get().getEmail()).isEqualTo(SECOND_EMAIL);
    assertThat(result.get().getId()).isEqualTo(secondAuthUserEntity.getId());
  }

  @Test
  void shouldPersistEmailChangeForManagedUser() {
    var authUserEntity = prepareAuthUserEntity(EMAIL);

    repository.saveAndFlush(authUserEntity);
    entityManager.clear();

    var result = repository.findByEmail(EMAIL);
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(authUserEntity.getId());
    result.get().setEmail(SECOND_EMAIL);
    repository.flush();
    entityManager.clear();

    var secondResult = repository.findByEmail(SECOND_EMAIL);
    assertThat(secondResult).isPresent();
    assertThat(secondResult.get().getEmail()).isEqualTo(SECOND_EMAIL);
    assertThat(secondResult.get().getId()).isEqualTo(authUserEntity.getId());
  }
}
