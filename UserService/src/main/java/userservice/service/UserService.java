package userservice.service;

import jakarta.transaction.Transactional;
import kafkacontracts.user.UserEventType;
import kafkacontracts.user.UserProfileCreatedEventPayload;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import userservice.dto.CreateUserCommand;
import userservice.dto.GetUserInfoCommand;
import userservice.dto.GetUserInfoResult;
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
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;

    public UserService(
            UserProfileRepository userProfileRepository,
            UserOutboxEventRepository userOutboxEventRepository,
            ObjectMapper objectMapper,
            UserMapper userMapper
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userOutboxEventRepository = userOutboxEventRepository;
        this.objectMapper = objectMapper;
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

        UserProfileCreatedEventPayload userProfileCreatedEventPayload = new UserProfileCreatedEventPayload(userProfileEntity.getId());

        Map<String, Object> payload = objectMapper.convertValue(
                userProfileCreatedEventPayload,
                new TypeReference<Map<String, Object>>() {}
        );

        userOutboxEventEntity.setPayload(payload);

        userOutboxEventRepository.save(userOutboxEventEntity);

    }

    public List<GetUserInfoResult> getAllUserInfo () {
       List<UserProfileEntity> userProfileEntities = userProfileRepository.findAll();
       return userProfileEntities.stream().map(userMapper::toGetUserInfoResult).toList();
    }
}
