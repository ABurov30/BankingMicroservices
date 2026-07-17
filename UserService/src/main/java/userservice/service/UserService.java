package userservice.service;

import org.springframework.stereotype.Service;
import userservice.dto.GetUserInfoCommand;
import userservice.dto.GetUserInfoResult;
import userservice.entity.UserProfileEntity;
import userservice.exception.UserProfileNotFoundException;
import userservice.repository.UserProfileRepository;

@Service
public class UserService {
    private final UserProfileRepository userProfileRepository;

    public UserService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public GetUserInfoResult getUserInfo(GetUserInfoCommand getUserInfoCommand) {
        UserProfileEntity userProfileEntity = userProfileRepository.findByAuthUserId(getUserInfoCommand.authUserId())
                .orElseThrow(() -> new UserProfileNotFoundException(getUserInfoCommand.authUserId()));

        return new GetUserInfoResult(
                userProfileEntity.getId(),
                userProfileEntity.getEmail(),
                userProfileEntity.getFirstName(),
                userProfileEntity.getLastName(),
                userProfileEntity.getStatus()
                );
    }
}
