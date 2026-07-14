package authservice.config;

import authservice.grpc.AuthGrpcService;
import authservice.grpc.GrpcExceptionInterceptor;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.Server;
import java.io.IOException;

@Configuration
public class GrpcServerConfig {

    @Bean(destroyMethod = "shutdown")
    public Server grpcServer(AuthGrpcService authGrpcService, GrpcExceptionInterceptor grpcExceptionInterceptor,@Value("${grpc.server.port:9090}") int port) throws IOException {
        return ServerBuilder.forPort(port).addService(ServerInterceptors.intercept(authGrpcService,grpcExceptionInterceptor)).build().start();
    }
}
