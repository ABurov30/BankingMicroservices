package userservice.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import kafkacontracts.auth.*;
import org.junit.jupiter.api.Test;
import userservice.mapper.command.UserCommandMapper;
import userservice.service.UserService;

class UserKafkaListenerTest {
  private final UserService service = mock(UserService.class);
  private final UserCommandMapper mapper = mock(UserCommandMapper.class);
  private final UserKafkaListener listener = new UserKafkaListener(service, mapper);

  @Test
  void forwardsAllAuthEventsToProfileService() {
    listener.handleAuthUserCreated(mock(AuthUserCreatedEventPayload.class), "id");
    var blocked = mock(AuthUserStatusChangedEventPayload.class);
    when(blocked.getStatus()).thenReturn("BLOCKED");
    listener.handleAuthUserStatusChanged(blocked, "id");
    var unlocked = mock(AuthUserStatusChangedEventPayload.class);
    when(unlocked.getStatus()).thenReturn("ACTIVE");
    listener.handleAuthUserStatusChanged(unlocked, "id");
    listener.handleAuthUserVerified(mock(AuthUserVerifiedEventPayload.class), "id");
    listener.handleAuthUserRoleChanged(mock(AuthUserRoleChangedEventPayload.class), "id");
    listener.handleAuthSocialAccountAuthUserCreated(
        mock(AuthSocialAccountAuthUserCreatedEventPayload.class), "id");
    verify(service).createUser(any());
    verify(service).blockUser(any());
    verify(service).unlockUser(any());
    verify(service).verifyUser(any());
    verify(service).changeUserRole(any());
    verify(service).createUserFromSocialAccount(any());
  }
}
