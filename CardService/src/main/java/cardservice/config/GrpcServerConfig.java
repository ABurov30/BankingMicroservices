package cardservice.config;

import cardservice.grpc.CardGrpcService;
import io.grpc.ServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.Server;
import java.io.IOException;

@Configuration
public class GrpcServerConfig {

    @Bean(destroyMethod = "shutdown")
    public Server grpcServer(CardGrpcService cardGrpcService, @Value("${grpc.server.port:9093}") int port) throws IOException {
        return ServerBuilder.forPort(port).addService(cardGrpcService).build().start();
    }
}
