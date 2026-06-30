package accountservice.config;

import accountservice.grpc.AccountGrpcService;
import io.grpc.ServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.Server;
import java.io.IOException;

@Configuration
public class GrpcServerConfig {

    @Bean(destroyMethod = "shutdown")
    public Server grpcServer(AccountGrpcService accountGrpcService, @Value("${grpc.server.port:9092}") int port) throws IOException {
        return ServerBuilder.forPort(port).addService(accountGrpcService).build().start();
    }
}
