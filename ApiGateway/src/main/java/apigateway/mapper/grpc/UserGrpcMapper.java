package apigateway.mapper.grpc;

import apigateway.dto.account.AccountResponseWithoutSensitiveInfo;
import apigateway.dto.account.GetAccountWithCardsResponseDto;
import apigateway.dto.user.GetRecipientInfoResponseDto;
import apigateway.dto.user.GetRecipientRequestDto;
import apigateway.dto.user.GetUserInfoRequestDto;
import apigateway.dto.user.GetUserInfoResponseDto;
import apigateway.dto.user.UserInfoWithoutIds;
import java.util.List;
import org.mapstruct.Mapper;
import user.contract.v1.GetUserInfoByEmailRequest;
import user.contract.v1.GetUserInfoGrpcRequest;

@Mapper(componentModel = "spring")
public interface UserGrpcMapper {
  default GetUserInfoGrpcRequest toGetUserInfoGrpcRequest(GetUserInfoRequestDto request) {
    return GetUserInfoGrpcRequest.newBuilder()
        .setAuthUserId(request.authUserId().toString())
        .build();
  }

  default GetUserInfoByEmailRequest toGetUserInfoByEmailRequest(
      GetRecipientRequestDto request) {
    return GetUserInfoByEmailRequest.newBuilder().setEmail(request.email()).build();
  }

  default GetRecipientInfoResponseDto toGetRecipientInfoResponseDto(
      GetUserInfoResponseDto userInfo, List<GetAccountWithCardsResponseDto> accounts) {
    return new GetRecipientInfoResponseDto(
        toUserInfoWithoutIds(userInfo),
        accounts.stream().map(this::toAccountResponseWithoutSensitiveInfo).toList());
  }

  default UserInfoWithoutIds toUserInfoWithoutIds(GetUserInfoResponseDto userInfo) {
    return new UserInfoWithoutIds(userInfo.email(), userInfo.firstName(), userInfo.lastName());
  }

  default AccountResponseWithoutSensitiveInfo toAccountResponseWithoutSensitiveInfo(
      GetAccountWithCardsResponseDto accountWithCards) {
    var account = accountWithCards.account();
    return new AccountResponseWithoutSensitiveInfo(
        account.accountId(), account.accountNumber(), account.type(), account.currency());
  }
}
