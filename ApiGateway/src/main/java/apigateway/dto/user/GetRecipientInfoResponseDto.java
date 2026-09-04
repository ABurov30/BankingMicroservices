package apigateway.dto.user;

import apigateway.dto.account.AccountResponseWithoutSensitiveInfo;
import java.util.List;

public record GetRecipientInfoResponseDto(
    UserInfoWithoutIds userInfo, List<AccountResponseWithoutSensitiveInfo> accounts) {}
