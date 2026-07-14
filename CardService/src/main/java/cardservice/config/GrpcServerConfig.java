package cardservice.config;

import cardservice.grpc.CardGrpcService;
import cardservice.grpc.GrpcExceptionInterceptor;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.Server;
import java.io.IOException;

@Configuration
public class GrpcServerConfig {

    @Bean(destroyMethod = "shutdown")
    public Server grpcServer(
            CardGrpcService cardGrpcService,
            GrpcExceptionInterceptor grpcExceptionInterceptor,
            @Value("${grpc.server.port:9093}") int port
    ) throws IOException {
        return ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(cardGrpcService, grpcExceptionInterceptor))
                .build()
                .start();
    }
}
