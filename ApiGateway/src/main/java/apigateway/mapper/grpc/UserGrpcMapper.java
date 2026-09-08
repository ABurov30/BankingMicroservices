package apigateway.mapper.grpc;

import apigateway.dto.request.user.GetRecipientRequestDto;
import apigateway.dto.request.user.GetUserInfoRequestDto;
import apigateway.dto.response.account.AccountResponseWithoutSensitiveInfo;
import apigateway.dto.response.user.GetRecipientInfoResponseDto;
import apigateway.dto.response.user.UserInfoWithoutIds;
import apigateway.dto.result.user.GetRecipientResultDto;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import user.contract.v1.GetRecipientByEmailRequest;
import user.contract.v1.GetUserInfoGrpcRequest;

@Mapper(componentModel = "spring")
public interface UserGrpcMapper {
  default GetUserInfoGrpcRequest toGetUserInfoGrpcRequest(GetUserInfoRequestDto request) {
    return GetUserInfoGrpcRequest.newBuilder()
        .setAuthUserId(request.authUserId().toString())
        .build();
  }

  default GetUserInfoGrpcRequest toGetUserInfoGrpcRequest(UUID authUserId) {
    return GetUserInfoGrpcRequest.newBuilder().setAuthUserId(authUserId.toString()).build();
  }

  default GetRecipientByEmailRequest toGetRecipientByEmailRequest(GetRecipientRequestDto request) {
    return GetRecipientByEmailRequest.newBuilder().setEmail(request.email()).build();
  }

  default GetRecipientInfoResponseDto toGetRecipientInfoResponseDto(
      GetRecipientResultDto recipient, List<AccountResponseWithoutSensitiveInfo> accounts) {
    return new GetRecipientInfoResponseDto(toUserInfoWithoutIds(recipient), accounts);
  }

  default UserInfoWithoutIds toUserInfoWithoutIds(GetRecipientResultDto recipient) {
    return new UserInfoWithoutIds(recipient.email(), recipient.firstName(), recipient.lastName());
  }
}
