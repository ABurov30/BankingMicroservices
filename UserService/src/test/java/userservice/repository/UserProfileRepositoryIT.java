package userservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import enums.auth.Roles;
import enums.user.UserProfileStatus;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import userservice.entity.UserProfileEntity;

@Tag("integration")
@ActiveProfiles("test")
@Testcontainers
@DataJpaTest
public class UserProfileRepositoryIT {
  private static final UUID AUTH_USER_ID = UUID.randomUUID();
  private static final String EMAIL = "test@example.com";
  private static final String FIRST_NAME = "Test";
  private static final String LAST_NAME = "User";

  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private UserProfileRepository repository;

  @Test
  void shouldFindProfileByAuthUserId() {
    UserProfileEntity profile = new UserProfileEntity();
    profile.setAuthUserId(AUTH_USER_ID);
    profile.setEmail(EMAIL);
    profile.setFirstName(FIRST_NAME);
    profile.setLastName(LAST_NAME);

    repository.saveAndFlush(profile);

    var result = repository.findByAuthUserId(AUTH_USER_ID);

    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo(EMAIL);
    assertThat(result.get().getLastName()).isEqualTo(LAST_NAME);
    assertThat(result.get().getFirstName()).isEqualTo(FIRST_NAME);
    assertThat(result.get().getRole()).isEqualTo(Roles.USER.name());
    assertThat(result.get().getStatus()).isEqualTo(UserProfileStatus.PENDING);
  }
}
