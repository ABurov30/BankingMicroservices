package transactionservice.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import transactionservice.grpc.TransactionGrpcService;

@Configuration
public class GrpcServerConfig {

  @Bean(destroyMethod = "shutdown")
  public Server grpcServer(
      TransactionGrpcService transactionGrpcService, @Value("${grpc.server.port:9090}") int port)
      throws IOException {
    return ServerBuilder.forPort(port).addService(transactionGrpcService).build().start();
  }
}
