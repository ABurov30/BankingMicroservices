package userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import enums.auth.Roles;
import enums.user.UserProfileStatus;
import java.util.Optional;
import java.util.UUID;
import kafkacontracts.user.UserEventType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import userservice.dto.BlockedUserCommand;
import userservice.dto.ChangeUserRoleCommand;
import userservice.dto.CreatUserFromSocialAccountCommand;
import userservice.dto.VerifyUserCommand;
import userservice.entity.UserOutboxEventEntity;
import userservice.entity.UserProfileEntity;
import userservice.mapper.result.UserResultMapper;
import userservice.repository.UserOutboxEventRepository;
import userservice.repository.UserProfileRepository;

class UserServiceTest {

  private final UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
  private final UserOutboxEventRepository userOutboxEventRepository =
      mock(UserOutboxEventRepository.class);
  private final UserResultMapper resultMapper = mock(UserResultMapper.class);
  private final UserService userService =
      new UserService(userProfileRepository, userOutboxEventRepository, resultMapper);

  private static final String FIRST_NAME = "Firstname";
  private static final String LAST_NAME = "Lastname";
  private static final String EMAIL = "test@gmail.com";
  private static final UUID AUTH_USER_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  @Test
  void createUserFromSocialAccountPublishesUserProfileCreatedEvent() {
    var command = new CreatUserFromSocialAccountCommand(AUTH_USER_ID, EMAIL, FIRST_NAME, LAST_NAME);

    when(userProfileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.empty());
    when(userProfileRepository.save(any(UserProfileEntity.class)))
        .thenAnswer(
            invocation -> {
              UserProfileEntity entity = invocation.getArgument(0);
              entity.setId(USER_ID);
              return entity;
            });

    userService.createUserFromSocialAccount(command);

    ArgumentCaptor<UserProfileEntity> profileCaptor =
        ArgumentCaptor.forClass(UserProfileEntity.class);
    verify(userProfileRepository).save(profileCaptor.capture());

    UserProfileEntity entity = profileCaptor.getValue();
    assertThat(entity.getAuthUserId()).isEqualTo(AUTH_USER_ID);
    assertThat(entity.getEmail()).isEqualTo(EMAIL);
    assertThat(entity.getFirstName()).isEqualTo(FIRST_NAME);
    assertThat(entity.getLastName()).isEqualTo(LAST_NAME);
    assertThat(entity.getStatus()).isEqualTo(UserProfileStatus.ACTIVE);

    ArgumentCaptor<UserOutboxEventEntity> eventOutboxCaptor =
        ArgumentCaptor.forClass(UserOutboxEventEntity.class);
    verify(userOutboxEventRepository).save(eventOutboxCaptor.capture());

    UserOutboxEventEntity event = eventOutboxCaptor.getValue();
    assertThat(event.getEventType()).isEqualTo(UserEventType.USER_PROFILE_CREATED.name());
    assertThat(event.getTopic()).isEqualTo(UserEventType.USER_PROFILE_CREATED.getTopic());
    assertThat(event.getAggregateId()).isEqualTo(USER_ID);
    assertThat(event.getEventKey()).isEqualTo(USER_ID.toString());
    assertThat(event.getPayload()).containsEntry("userId", USER_ID);
    assertThat(event.getPayload()).containsEntry("authUserId", AUTH_USER_ID);
  }

  @Test
  void createUserFromSocialAccountSkipsExistingProfile() {
    var command = new CreatUserFromSocialAccountCommand(AUTH_USER_ID, EMAIL, FIRST_NAME, LAST_NAME);

    when(userProfileRepository.findByAuthUserId(AUTH_USER_ID))
        .thenReturn(Optional.of(new UserProfileEntity()));

    userService.createUserFromSocialAccount(command);

    verify(userProfileRepository, never()).save(any(UserProfileEntity.class));
    verify(userOutboxEventRepository, never()).save(any(UserOutboxEventEntity.class));
  }

  private UserProfileEntity prepareUserProfileEntity() {
    var profile = new UserProfileEntity();
    profile.setId(USER_ID);
    profile.setAuthUserId(AUTH_USER_ID);
    profile.setStatus(UserProfileStatus.PENDING);
    profile.setEmail(EMAIL);
    profile.setFirstName(FIRST_NAME);
    profile.setLastName(LAST_NAME);
    return profile;
  }

  @Test
  void shouldActivateExistingUser() {
    var profile = prepareUserProfileEntity();

    when(userProfileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(profile));

    var commandToVerify = new VerifyUserCommand(AUTH_USER_ID);
    userService.verifyUser(commandToVerify);

    assertThat(profile.getStatus()).isEqualTo(UserProfileStatus.ACTIVE);
    verify(userProfileRepository).save(profile);
  }

  @Test
  void shouldNotSaveUserWhenProfileIsMissing() {
    when(userProfileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.empty());
    var commandToVerify = new VerifyUserCommand(AUTH_USER_ID);
    userService.verifyUser(commandToVerify);

    verify(userProfileRepository, never()).save(any(UserProfileEntity.class));
  }

  @Test
  void shouldChangeRoleExistingUser() {
    var profile = prepareUserProfileEntity();

    when(userProfileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(profile));

    var commandToChangeRole = new ChangeUserRoleCommand(AUTH_USER_ID, Roles.MANAGER.name());
    userService.changeUserRole(commandToChangeRole);

    assertThat(profile.getRole()).isEqualTo(Roles.MANAGER.name());
    verify(userProfileRepository).save(profile);
  }

  @Test
  void shouldNotSaveUserWhenChangingRoleForMissingProfile() {
    when(userProfileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.empty());
    var commandToChangeRole = new ChangeUserRoleCommand(AUTH_USER_ID, Roles.MANAGER.name());
    userService.changeUserRole(commandToChangeRole);

    verify(userProfileRepository, never()).save(any(UserProfileEntity.class));
  }

  @Test
  void shouldBlockActiveUserAndCreateOutboxEvent() {
    var userProfileEntity = prepareUserProfileEntity();
    userProfileEntity.setStatus(UserProfileStatus.ACTIVE);
    var expectedUserId = userProfileEntity.getId();

    when(userProfileRepository.findByAuthUserId(userProfileEntity.getAuthUserId()))
        .thenReturn(Optional.of(userProfileEntity));

    userService.blockUser(new BlockedUserCommand(userProfileEntity.getAuthUserId()));

    ArgumentCaptor<UserProfileEntity> profileBlockedCaptor =
        ArgumentCaptor.forClass(UserProfileEntity.class);
    verify(userProfileRepository).save(profileBlockedCaptor.capture());

    UserProfileEntity blockedEntity = profileBlockedCaptor.getValue();

    assertThat(blockedEntity.getId()).isEqualTo(expectedUserId);
    assertThat(blockedEntity.getStatus()).isEqualTo(UserProfileStatus.BLOCKED);

    ArgumentCaptor<UserOutboxEventEntity> eventOutboxCaptor =
        ArgumentCaptor.forClass(UserOutboxEventEntity.class);
    verify(userOutboxEventRepository).save(eventOutboxCaptor.capture());

    UserOutboxEventEntity event = eventOutboxCaptor.getValue();

    assertThat(event.getPayload()).containsEntry("userId", expectedUserId);
    assertThat(event.getEventType()).isEqualTo(UserEventType.USER_PROFILE_BLOCKED.name());
    assertThat(event.getAggregateId()).isEqualTo(expectedUserId);
    assertThat(event.getEventKey()).isEqualTo(expectedUserId.toString());
  }

  @Test
  void shouldSkipBlockingAlreadyBlockedUser() {
    var userProfileEntity = prepareUserProfileEntity();
    userProfileEntity.setStatus(UserProfileStatus.BLOCKED);

    when(userProfileRepository.findByAuthUserId(userProfileEntity.getAuthUserId()))
        .thenReturn(Optional.of(userProfileEntity));

    userService.blockUser(new BlockedUserCommand(userProfileEntity.getAuthUserId()));
    verify(userProfileRepository, never()).save(any(UserProfileEntity.class));
    verify(userOutboxEventRepository, never()).save(any(UserOutboxEventEntity.class));

    assertThat(userProfileEntity.getStatus()).isEqualTo(UserProfileStatus.BLOCKED);
  }

  @Test
  void shouldSkipBlockingWhenProfileIsMissing() {
    when(userProfileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.empty());

    userService.blockUser(new BlockedUserCommand(AUTH_USER_ID));
    verify(userProfileRepository, never()).save(any(UserProfileEntity.class));
    verify(userOutboxEventRepository, never()).save(any(UserOutboxEventEntity.class));
  }
}
