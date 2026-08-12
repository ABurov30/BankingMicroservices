package notificationservice.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import java.io.IOException;
import notificationservice.grpc.GrpcExceptionInterceptor;
import notificationservice.grpc.NotificationGrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfig {

  @Bean(destroyMethod = "shutdown")
  public Server grpcServer(
      NotificationGrpcService notificationGrpcService,
      GrpcExceptionInterceptor grpcExceptionInterceptor,
      @Value("${grpc.server.port:9095}") int port)
      throws IOException {
    return ServerBuilder.forPort(port)
        .addService(ServerInterceptors.intercept(notificationGrpcService, grpcExceptionInterceptor))
        .build()
        .start();
  }
}
