package accountservice.config;

import accountservice.grpc.AccountGrpcService;
import accountservice.grpc.GrpcExceptionInterceptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfig {

  @Bean(destroyMethod = "shutdown")
  public Server grpcServer(
      AccountGrpcService accountGrpcService,
      GrpcExceptionInterceptor grpcExceptionInterceptor,
      @Value("${grpc.server.port:9092}") int port)
      throws IOException {
    return ServerBuilder.forPort(port)
        .addService(ServerInterceptors.intercept(accountGrpcService, grpcExceptionInterceptor))
        .build()
        .start();
  }
}
