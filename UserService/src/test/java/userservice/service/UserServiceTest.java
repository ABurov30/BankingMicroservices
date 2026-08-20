package userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import kafkacontracts.user.UserEventType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import userservice.dto.CreatUserFromSocialAccountCommand;
import userservice.entity.UserOutboxEventEntity;
import userservice.entity.UserProfileEntity;
import userservice.mapper.result.UserResultMapper;
import userservice.repository.UserOutboxEventRepository;
import userservice.repository.UserProfileRepository;

class UserServiceTest {

  private final UserProfileRepository userProfileRepository =
      org.mockito.Mockito.mock(UserProfileRepository.class);
  private final UserOutboxEventRepository userOutboxEventRepository =
      org.mockito.Mockito.mock(UserOutboxEventRepository.class);
  private final UserResultMapper resultMapper = org.mockito.Mockito.mock(UserResultMapper.class);
  private final UserService userService =
      new UserService(userProfileRepository, userOutboxEventRepository, resultMapper);

  @Test
  void createUserFromSocialAccountPublishesUserProfileCreatedEvent() {
    UUID authUserId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    var command =
        new CreatUserFromSocialAccountCommand(authUserId, "social@example.com", "Social", "User");

    when(userProfileRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
    when(userProfileRepository.save(any(UserProfileEntity.class)))
        .thenAnswer(
            invocation -> {
              UserProfileEntity entity = invocation.getArgument(0);
              entity.setId(userId);
              return entity;
            });

    userService.createUserFromSocialAccount(command);

    ArgumentCaptor<UserOutboxEventEntity> eventCaptor =
        ArgumentCaptor.forClass(UserOutboxEventEntity.class);
    verify(userOutboxEventRepository).save(eventCaptor.capture());

    UserOutboxEventEntity event = eventCaptor.getValue();
    assertThat(event.getEventType()).isEqualTo(UserEventType.USER_PROFILE_CREATED.name());
    assertThat(event.getTopic()).isEqualTo(UserEventType.USER_PROFILE_CREATED.getTopic());
    assertThat(event.getAggregateId()).isEqualTo(userId);
    assertThat(event.getPayload()).containsEntry("userId", userId);
    assertThat(event.getPayload()).containsEntry("authUserId", authUserId);
  }

  @Test
  void createUserFromSocialAccountSkipsExistingProfile() {
    UUID authUserId = UUID.randomUUID();
    var command =
        new CreatUserFromSocialAccountCommand(authUserId, "social@example.com", "Social", "User");

    when(userProfileRepository.findByAuthUserId(authUserId))
        .thenReturn(Optional.of(new UserProfileEntity()));

    userService.createUserFromSocialAccount(command);

    verify(userProfileRepository, never()).save(any(UserProfileEntity.class));
    verify(userOutboxEventRepository, never()).save(any(UserOutboxEventEntity.class));
  }
}
