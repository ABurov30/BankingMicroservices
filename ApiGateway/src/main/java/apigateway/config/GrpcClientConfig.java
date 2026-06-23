package apigateway.config;

import auth.contract.v1.AuthRpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Bean(destroyMethod = "shutdown")
    ManagedChannel authChannel(
            @Value("${AUTH_GRPC_HOST}") String host,
            @Value("${AUTH_GRPC_PORT}") int port) {

        return ManagedChannelBuilder.forAddress(host,port).usePlaintext().build();
    }

    @Bean
    AuthRpcServiceGrpc.AuthRpcServiceBlockingStub authStub(ManagedChannel authChannel) {
        return  AuthRpcServiceGrpc.newBlockingStub(authChannel);
    }
}
