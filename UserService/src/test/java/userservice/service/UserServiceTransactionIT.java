package userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import enums.auth.Roles;
import enums.user.UserProfileStatus;
import java.util.UUID;
import kafkacontracts.user.UserEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import userservice.dto.CreateUserCommand;
import userservice.entity.UserOutboxEventEntity;
import userservice.mapper.result.UserResultMapper;
import userservice.repository.UserOutboxEventRepository;
import userservice.repository.UserProfileRepository;

@Tag("integration")
@ActiveProfiles("test")
@Testcontainers
@DataJpaTest
@Import(UserService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class UserServiceTransactionIT {
  private static final UUID AUTH_USER_ID = UUID.randomUUID();
  private static final String EMAIL = "test@example.com";
  private static final String FIRST_NAME = "Test";
  private static final String LAST_NAME = "User";

  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private UserService userService;

  @Autowired private UserProfileRepository repository;

  @MockitoSpyBean private UserOutboxEventRepository outboxEventRepository;

  @MockitoBean private UserResultMapper resultMapper;

  @BeforeEach
  void cleanDatabase() {
    outboxEventRepository.deleteAll();
    repository.deleteAll();
  }

  @Test
  void shouldRollbackProfileWhenOutboxSaveFails() {
    var command = new CreateUserCommand(AUTH_USER_ID, EMAIL, FIRST_NAME, LAST_NAME);

    doThrow(new IllegalStateException("Outbox unavailable"))
        .when(outboxEventRepository)
        .save(any(UserOutboxEventEntity.class));

    assertThatThrownBy(() -> userService.createUser(command))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Outbox unavailable");

    var result = repository.findByAuthUserId(AUTH_USER_ID);
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void shouldSaveProfileAndOutboxEventTogether() {
    var command = new CreateUserCommand(AUTH_USER_ID, EMAIL, FIRST_NAME, LAST_NAME);
    userService.createUser(command);
    var result = repository.findByAuthUserId(AUTH_USER_ID);
    assertThat(result).isPresent();
    assertThat(result.get().getAuthUserId()).isEqualTo(command.authUserId());
    assertThat(result.get().getEmail()).isEqualTo(command.email());
    assertThat(result.get().getStatus()).isEqualTo(UserProfileStatus.PENDING);
    assertThat(result.get().getRole()).isEqualTo(Roles.USER.name());
    assertThat(result.get().getFirstName()).isEqualTo(command.firstName());
    assertThat(result.get().getLastName()).isEqualTo(command.lastName());

    var events = outboxEventRepository.findAll();
    assertThat(events).hasSize(1);
    var event = events.get(0);
    assertThat(event.getAggregateId()).isEqualTo(result.get().getId());
    assertThat(event.getEventType()).isEqualTo(UserEventType.USER_PROFILE_CREATED.name());
  }

  @Test
  void shouldNotCreateDuplicateProfileOrEventWhenUserAlreadyExists() {
    var command = new CreateUserCommand(AUTH_USER_ID, EMAIL, FIRST_NAME, LAST_NAME);
    userService.createUser(command);
    userService.createUser(command);
    var result = repository.findByAuthUserId(AUTH_USER_ID);
    assertThat(result).isPresent();
    assertThat(repository.count()).isEqualTo(1L);

    var events = outboxEventRepository.findAll();
    assertThat(events).hasSize(1);
    var event = events.get(0);
    assertThat(event.getAggregateId()).isEqualTo(result.get().getId());
    assertThat(event.getEventType()).isEqualTo(UserEventType.USER_PROFILE_CREATED.name());
  }
}
