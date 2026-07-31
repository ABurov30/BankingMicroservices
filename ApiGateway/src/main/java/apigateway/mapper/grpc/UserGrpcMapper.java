package apigateway.mapper.grpc;

import apigateway.dto.user.GetUserInfoRequestDto;
import org.mapstruct.Mapper;
import user.contract.v1.GetUserInfoGrpcRequest;

@Mapper(componentModel = "spring")
public interface UserGrpcMapper {
    default GetUserInfoGrpcRequest toGetUserInfoGrpcRequest(GetUserInfoRequestDto request) { return GetUserInfoGrpcRequest.newBuilder().setAuthUserId(request.authUserId().toString()).build(); }
}
