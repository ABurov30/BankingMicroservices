package userservice.service;

import enums.user.UserProfileStatus;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import kafkacontracts.user.UserEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import userservice.dto.*;
import userservice.entity.UserOutboxEventEntity;
import userservice.entity.UserProfileEntity;
import userservice.exception.UserProfileNotFoundException;
import userservice.mapper.result.UserResultMapper;
import userservice.repository.UserOutboxEventRepository;
import userservice.repository.UserProfileRepository;

@Service
public class UserService {
  private final UserProfileRepository userProfileRepository;
  private final UserOutboxEventRepository userOutboxEventRepository;
  private final UserResultMapper resultMapper;
  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  public UserService(
      UserProfileRepository userProfileRepository,
      UserOutboxEventRepository userOutboxEventRepository,
      UserResultMapper resultMapper) {
    this.userProfileRepository = userProfileRepository;
    this.userOutboxEventRepository = userOutboxEventRepository;
    this.resultMapper = resultMapper;
  }

  @Transactional
  public GetUserInfoResult getUserInfo(GetUserInfoCommand getUserInfoCommand) {
    UserProfileEntity userProfileEntity =
        userProfileRepository
            .findByAuthUserId(getUserInfoCommand.authUserId())
            .orElseThrow(() -> new UserProfileNotFoundException(getUserInfoCommand.authUserId()));

    return resultMapper.toGetUserInfoResult(userProfileEntity);
  }

  @Transactional
  public GetUserInfoResult getUserInfoByEmail(GetUserInfoByEmailCommand command) {
    UserProfileEntity userProfileEntity =
        userProfileRepository
            .findByEmail(command.email())
            .orElseThrow(() -> new UserProfileNotFoundException(command.email()));

    return resultMapper.toGetUserInfoResult(userProfileEntity);
  }

  @Transactional
  public void createUser(CreateUserCommand createUserCommand) {
    if (userProfileRepository.findByAuthUserId(createUserCommand.authUserId()).isPresent()) {
      return;
    }

    UserProfileEntity userProfileEntity = new UserProfileEntity();
    userProfileEntity.setAuthUserId(createUserCommand.authUserId());
    userProfileEntity.setEmail(createUserCommand.email());
    userProfileEntity.setFirstName(createUserCommand.firstName());
    userProfileEntity.setLastName(createUserCommand.lastName());
    userProfileEntity.setRole("USER");

    userProfileRepository.save(userProfileEntity);

    UserOutboxEventEntity userOutboxEventEntity = new UserOutboxEventEntity();
    userOutboxEventEntity.setAggregateType("USER_PROFILE");
    userOutboxEventEntity.setAggregateId(userProfileEntity.getId());
    userOutboxEventEntity.setEventType(UserEventType.USER_PROFILE_CREATED.name());
    userOutboxEventEntity.setTopic(UserEventType.USER_PROFILE_CREATED.getTopic());
    userOutboxEventEntity.setEventKey(
        userProfileEntity.getId() + ":" + UserEventType.USER_PROFILE_CREATED.name());
    userOutboxEventEntity.setSchemaVersion(UserEventType.USER_PROFILE_CREATED.getVersion());

    userOutboxEventEntity.setPayload(
        Map.of(
            "userId", userProfileEntity.getId(),
            "authUserId", userProfileEntity.getAuthUserId()));

    userOutboxEventRepository.save(userOutboxEventEntity);
  }

  public List<GetUserInfoResult> getAllUserInfo() {
    List<UserProfileEntity> userProfileEntities = userProfileRepository.findAll();
    return userProfileEntities.stream().map(resultMapper::toGetUserInfoResult).toList();
  }

  public void blockUser(BlockedUserCommand blockedUserCommand) {
    var userProfile = userProfileRepository.findByAuthUserId(blockedUserCommand.authUserId());

    if (userProfile.isEmpty()) {
      log.warn(
          "Skipping user profile block: authUserId={} not found", blockedUserCommand.authUserId());
      return;
    }

    UserProfileEntity userProfileEntity = userProfile.get();

    if (userProfileEntity.getStatus() == UserProfileStatus.BLOCKED) {
      log.info(
          "Skipping user profile block: authUserId={}, status={}",
          userProfileEntity.getAuthUserId(),
          userProfileEntity.getStatus());
      return;
    }

    userProfileEntity.setStatus(UserProfileStatus.BLOCKED);
    userProfileRepository.save(userProfileEntity);

    UserOutboxEventEntity userOutboxEventEntity = new UserOutboxEventEntity();
    userOutboxEventEntity.setAggregateType("USER_PROFILE");
    userOutboxEventEntity.setAggregateId(userProfileEntity.getId());
    userOutboxEventEntity.setEventType(UserEventType.USER_PROFILE_BLOCKED.name());
    userOutboxEventEntity.setTopic(UserEventType.USER_PROFILE_BLOCKED.getTopic());
    userOutboxEventEntity.setEventKey(
        userProfileEntity.getId() + ":" + UserEventType.USER_PROFILE_BLOCKED.name());
    userOutboxEventEntity.setSchemaVersion(UserEventType.USER_PROFILE_BLOCKED.getVersion());

    userOutboxEventEntity.setPayload(Map.of("userId", userProfileEntity.getId()));

    userOutboxEventRepository.save(userOutboxEventEntity);
  }

  public void unlockUser(UnlockUserCommand unlockUserCommand) {
    var userProfile = userProfileRepository.findByAuthUserId(unlockUserCommand.authUserId());

    if (userProfile.isEmpty()) {
      log.warn(
          "Skipping user profile unlock: authUserId={} not found", unlockUserCommand.authUserId());
      return;
    }

    UserProfileEntity userProfileEntity = userProfile.get();

    if (userProfileEntity.getStatus() == UserProfileStatus.ACTIVE) {
      log.info(
          "Skipping user profile unlock: authUserId={}, status={}",
          userProfileEntity.getAuthUserId(),
          userProfileEntity.getStatus());
      return;
    }

    userProfileEntity.setStatus(UserProfileStatus.ACTIVE);
    userProfileRepository.save(userProfileEntity);

    UserOutboxEventEntity userOutboxEventEntity = new UserOutboxEventEntity();
    userOutboxEventEntity.setAggregateType("USER_PROFILE");
    userOutboxEventEntity.setAggregateId(userProfileEntity.getId());
    userOutboxEventEntity.setEventType(UserEventType.USER_PROFILE_UNLOCK.name());
    userOutboxEventEntity.setTopic(UserEventType.USER_PROFILE_UNLOCK.getTopic());
    userOutboxEventEntity.setEventKey(
        userProfileEntity.getId() + ":" + UserEventType.USER_PROFILE_UNLOCK.name());
    userOutboxEventEntity.setSchemaVersion(UserEventType.USER_PROFILE_UNLOCK.getVersion());

    userOutboxEventEntity.setPayload(Map.of("userId", userProfileEntity.getId()));

    userOutboxEventRepository.save(userOutboxEventEntity);
  }

  public void verifyUser(VerifyUserCommand verifyUserCommand) {
    var userProfile = userProfileRepository.findByAuthUserId(verifyUserCommand.authUserId());

    if (userProfile.isEmpty()) {
      log.warn(
          "Skipping user profile verification: authUserId={} not found",
          verifyUserCommand.authUserId());
      return;
    }

    UserProfileEntity userProfileEntity = userProfile.get();

    userProfileEntity.setStatus(UserProfileStatus.ACTIVE);
    userProfileRepository.save(userProfileEntity);
  }

  @Transactional
  public void changeUserRole(ChangeUserRoleCommand changeUserRoleCommand) {
    var userProfile = userProfileRepository.findByAuthUserId(changeUserRoleCommand.authUserId());

    if (userProfile.isEmpty()) {
      log.warn(
          "Skipping user profile role change: authUserId={} not found",
          changeUserRoleCommand.authUserId());
      return;
    }

    UserProfileEntity userProfileEntity = userProfile.get();

    userProfileEntity.setRole(changeUserRoleCommand.role());
    userProfileRepository.save(userProfileEntity);
  }
}
