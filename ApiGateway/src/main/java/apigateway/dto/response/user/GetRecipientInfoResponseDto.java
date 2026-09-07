package apigateway.dto.response.user;

import apigateway.dto.response.account.AccountResponseWithoutSensitiveInfo;
import java.util.List;

public record GetRecipientInfoResponseDto(
    UserInfoWithoutIds userInfo, List<AccountResponseWithoutSensitiveInfo> accounts) {}
