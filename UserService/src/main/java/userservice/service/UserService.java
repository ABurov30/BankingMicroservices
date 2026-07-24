package userservice.service;

import enums.user.UserProfileStatus;
import jakarta.transaction.Transactional;
import kafkacontracts.user.UserEventType;
import org.springframework.stereotype.Service;
import userservice.dto.*;
import userservice.entity.UserOutboxEventEntity;
import userservice.entity.UserProfileEntity;
import userservice.exception.UserProfileAlreadyExist;
import userservice.exception.UserProfileNotFoundException;
import userservice.mapper.UserMapper;
import userservice.repository.UserOutboxEventRepository;
import userservice.repository.UserProfileRepository;

import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final UserProfileRepository userProfileRepository;
    private final UserOutboxEventRepository userOutboxEventRepository;
    private final UserMapper userMapper;

    public UserService(
            UserProfileRepository userProfileRepository,
            UserOutboxEventRepository userOutboxEventRepository,
            UserMapper userMapper
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userOutboxEventRepository = userOutboxEventRepository;
        this.userMapper = userMapper;
    }

    @Transactional()
    public GetUserInfoResult getUserInfo(GetUserInfoCommand getUserInfoCommand) {
        UserProfileEntity userProfileEntity = userProfileRepository.findByAuthUserId(getUserInfoCommand.authUserId())
                .orElseThrow(() -> new UserProfileNotFoundException(getUserInfoCommand.authUserId()));

        return userMapper.toGetUserInfoResult(userProfileEntity);
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

        userProfileRepository.save(userProfileEntity);

        UserOutboxEventEntity userOutboxEventEntity = new UserOutboxEventEntity();
        userOutboxEventEntity.setAggregateType("USER_PROFILE");
        userOutboxEventEntity.setAggregateId(userProfileEntity.getId());
        userOutboxEventEntity.setEventType(UserEventType.USER_PROFILE_CREATED.name());
        userOutboxEventEntity.setTopic(UserEventType.USER_PROFILE_CREATED.getTopic());
        userOutboxEventEntity.setEventKey(userProfileEntity.getId().toString());
        userOutboxEventEntity.setSchemaVersion(UserEventType.USER_PROFILE_CREATED.getVersion());

        userOutboxEventEntity.setPayload(Map.of("userId", userProfileEntity.getId()));

        userOutboxEventRepository.save(userOutboxEventEntity);
    }

    public List<GetUserInfoResult> getAllUserInfo () {
       List<UserProfileEntity> userProfileEntities = userProfileRepository.findAll();
       return userProfileEntities.stream().map(userMapper::toGetUserInfoResult).toList();
    }

    public void blockUser(BlockedUserCommand blockedUserCommand) {
        UserProfileEntity userProfileEntity = userProfileRepository.findByAuthUserId(blockedUserCommand.authUserId())
                .orElseThrow(() -> new UserProfileNotFoundException(blockedUserCommand.authUserId()));

        if (userProfileEntity.getStatus() == UserProfileStatus.BLOCKED) {
            throw new IllegalArgumentException("User profile already blocked");
        }

        userProfileEntity.setStatus(UserProfileStatus.BLOCKED);
        userProfileRepository.save(userProfileEntity);

        UserOutboxEventEntity userOutboxEventEntity = new UserOutboxEventEntity();
        userOutboxEventEntity.setAggregateType("USER_PROFILE");
        userOutboxEventEntity.setAggregateId(userProfileEntity.getId());
        userOutboxEventEntity.setEventType(UserEventType.USER_PROFILE_BLOCKED.name());
        userOutboxEventEntity.setTopic(UserEventType.USER_PROFILE_BLOCKED.getTopic());
        userOutboxEventEntity.setEventKey(userProfileEntity.getId().toString());
        userOutboxEventEntity.setSchemaVersion(UserEventType.USER_PROFILE_BLOCKED.getVersion());

        userOutboxEventEntity.setPayload(Map.of("userId", userProfileEntity.getId()));

        userOutboxEventRepository.save(userOutboxEventEntity);
    }

    public void unlockUser(UnlockUserCommand unlockUserCommand) {
        UserProfileEntity userProfileEntity = userProfileRepository.findByAuthUserId(unlockUserCommand.authUserId())
                .orElseThrow(() -> new UserProfileNotFoundException(unlockUserCommand.authUserId()));

        if (userProfileEntity.getStatus() == UserProfileStatus.ACTIVE) {
            throw new IllegalArgumentException("User profile already active");
        }

        userProfileEntity.setStatus(UserProfileStatus.ACTIVE);
        userProfileRepository.save(userProfileEntity);

        UserOutboxEventEntity userOutboxEventEntity = new UserOutboxEventEntity();
        userOutboxEventEntity.setAggregateType("USER_PROFILE");
        userOutboxEventEntity.setAggregateId(userProfileEntity.getId());
        userOutboxEventEntity.setEventType(UserEventType.USER_PROFILE_UNLOCK.name());
        userOutboxEventEntity.setTopic(UserEventType.USER_PROFILE_UNLOCK.getTopic());
        userOutboxEventEntity.setEventKey(userProfileEntity.getId().toString());
        userOutboxEventEntity.setSchemaVersion(UserEventType.USER_PROFILE_UNLOCK.getVersion());

        userOutboxEventEntity.setPayload(Map.of("userId", userProfileEntity.getId()));

        userOutboxEventRepository.save(userOutboxEventEntity);
    }
}
