package userservice.config;

import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.Server;
import userservice.grpc.UserGrpcService;
import userservice.grpc.GrpcExceptionInterceptor;

import java.io.IOException;

@Configuration
public class GrpcServerConfig {

    @Bean(destroyMethod = "shutdown")
    public Server grpcServer(
            UserGrpcService userGrpcService,
            GrpcExceptionInterceptor grpcExceptionInterceptor,
            @Value("${grpc.server.port:9091}") int port
    ) throws IOException {
        return ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(userGrpcService, grpcExceptionInterceptor))
                .build()
                .start();
    }
}
