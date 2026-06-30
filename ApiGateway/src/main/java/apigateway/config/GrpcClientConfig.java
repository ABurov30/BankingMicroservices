package apigateway.config;

import account.contract.v1.AccountRpcServiceGrpc;
import auth.contract.v1.AuthRpcServiceGrpc;
import card.contract.v1.CardRpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import transaction.contract.v1.TransactionRpcServiceGrpc;
import user.contract.v1.UserRpcServiceGrpc;

@Configuration
public class GrpcClientConfig {

  @Bean(destroyMethod = "shutdown")
  ManagedChannel authChannel(
      @Value("${AUTH_GRPC_HOST}") String host, @Value("${AUTH_GRPC_PORT}") int port) {

    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  @Bean
  AuthRpcServiceGrpc.AuthRpcServiceBlockingStub authStub(ManagedChannel authChannel) {
    return AuthRpcServiceGrpc.newBlockingStub(authChannel);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel userChannel(
      @Value("${USER_GRPC_HOST}") String host, @Value("${USER_GRPC_PORT}") int port) {

    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  @Bean
  UserRpcServiceGrpc.UserRpcServiceBlockingStub userStub(ManagedChannel userChannel) {
    return UserRpcServiceGrpc.newBlockingStub(userChannel);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel accountChannel(
      @Value("${ACCOUNT_GRPC_HOST}") String host, @Value("${ACCOUNT_GRPC_PORT}") int port) {

    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  @Bean
  AccountRpcServiceGrpc.AccountRpcServiceBlockingStub accountStub(ManagedChannel accountChannel) {
    return AccountRpcServiceGrpc.newBlockingStub(accountChannel);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel cardChannel(
      @Value("${CARD_GRPC_HOST}") String host, @Value("${CARD_GRPC_PORT}") int port) {

    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  @Bean
  CardRpcServiceGrpc.CardRpcServiceBlockingStub cardStub(ManagedChannel cardChannel) {
    return CardRpcServiceGrpc.newBlockingStub(cardChannel);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel transactionChannel(
      @Value("${TRANSACTION_GRPC_HOST}") String host, @Value("${TRANSACTION_GRPC_PORT}") int port) {

    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  @Bean
  TransactionRpcServiceGrpc.TransactionRpcServiceBlockingStub transactionStub(
      ManagedChannel transactionChannel) {
    return TransactionRpcServiceGrpc.newBlockingStub(transactionChannel);
  }
}
