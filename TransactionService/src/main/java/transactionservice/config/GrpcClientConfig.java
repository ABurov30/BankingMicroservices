package transactionservice.config;

import account.contract.v1.AccountRpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

  @Bean(destroyMethod = "shutdown")
  ManagedChannel accountChannel(
      @Value("${ACCOUNT_GRPC_HOST}") String host, @Value("${ACCOUNT_GRPC_PORT}") int port) {

    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  @Bean
  AccountRpcServiceGrpc.AccountRpcServiceBlockingStub accountStub(ManagedChannel accountChannel) {
    return AccountRpcServiceGrpc.newBlockingStub(accountChannel);
  }
}
