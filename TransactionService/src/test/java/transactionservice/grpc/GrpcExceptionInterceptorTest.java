package transactionservice.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import transaction.contract.v1.TransactionRpcServiceGrpc;

class GrpcExceptionInterceptorTest {
  @ParameterizedTest
  @EnumSource(
      value = Status.Code.class,
      names = {"UNAVAILABLE", "DEADLINE_EXCEEDED", "INTERNAL", "NOT_FOUND", "PERMISSION_DENIED"})
  @SuppressWarnings("unchecked")
  void preservesDownstreamStatus(Status.Code code) {
    ServerCall<Object, Object> call = mock(ServerCall.class);
    ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);
    when(call.getMethodDescriptor())
        .thenReturn(
            (io.grpc.MethodDescriptor) TransactionRpcServiceGrpc.getCreateTransactionMethod());
    when(next.startCall(any(), any()))
        .thenReturn(
            new ServerCall.Listener<>() {
              @Override
              public void onHalfClose() {
                throw Status.fromCode(code)
                    .withDescription("Dependency unavailable")
                    .asRuntimeException();
              }
            });
    new GrpcExceptionInterceptor().interceptCall(call, new Metadata(), next).onHalfClose();
    var status = ArgumentCaptor.forClass(Status.class);
    verify(call).close(status.capture(), any());
    assertThat(status.getValue().getCode()).isEqualTo(code);
  }
}
