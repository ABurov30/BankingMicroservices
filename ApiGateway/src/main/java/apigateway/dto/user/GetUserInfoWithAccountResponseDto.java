package apigateway.dto.user;

import apigateway.dto.account.GetAccountResponseDto;
import java.util.List;

public record GetUserInfoWithAccountResponseDto(
    GetUserInfoResponseDto userInfo, List<GetAccountResponseDto> accounts) {}
