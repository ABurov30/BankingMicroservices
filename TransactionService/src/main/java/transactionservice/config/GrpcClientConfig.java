package transactionservice.config;

import account.contract.v1.AccountRpcServiceGrpc;
import card.contract.v1.CardRpcServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

  private final GrpcResilience resilience;

  public GrpcClientConfig(GrpcResilience resilience) {
    this.resilience = resilience;
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel accountChannel(
      @Value("${ACCOUNT_GRPC_HOST}") String host, @Value("${ACCOUNT_GRPC_PORT}") int port) {

    return resilience.channel("account", host, port, AccountRpcServiceGrpc.getServiceDescriptor());
  }

  @Bean
  AccountRpcServiceGrpc.AccountRpcServiceBlockingStub accountStub(ManagedChannel accountChannel) {
    return AccountRpcServiceGrpc.newBlockingStub(accountChannel);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel cardChannel(
      @Value("${CARD_GRPC_HOST}") String host, @Value("${CARD_GRPC_PORT}") int port) {

    return resilience.channel("card", host, port, CardRpcServiceGrpc.getServiceDescriptor());
  }

  @Bean
  CardRpcServiceGrpc.CardRpcServiceBlockingStub cardStub(ManagedChannel cardChannel) {
    return CardRpcServiceGrpc.newBlockingStub(cardChannel);
  }
}
