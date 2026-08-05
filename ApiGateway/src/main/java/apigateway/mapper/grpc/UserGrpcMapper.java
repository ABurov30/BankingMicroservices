package apigateway.mapper.grpc;

import apigateway.dto.user.GetUserInfoByEmailRequestDto;
import apigateway.dto.user.GetUserInfoRequestDto;
import apigateway.dto.user.GetUserInfoResponseDto;
import org.mapstruct.Mapper;
import user.contract.v1.GetUserInfoByEmailRequest;
import user.contract.v1.GetUserInfoGrpcRequest;

@Mapper(componentModel = "spring")
public interface UserGrpcMapper {
    default GetUserInfoGrpcRequest toGetUserInfoGrpcRequest(GetUserInfoRequestDto request) {
        return GetUserInfoGrpcRequest.newBuilder().setAuthUserId(request.authUserId().toString()).build();
    }

    default GetUserInfoByEmailRequest toGetUserInfoByEmailRequest (GetUserInfoByEmailRequestDto request) {
        return GetUserInfoByEmailRequest.newBuilder().setEmail(request.email()).build();
    }
}
