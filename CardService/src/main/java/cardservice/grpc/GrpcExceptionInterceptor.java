package cardservice.grpc;

import cardservice.exception.CardBlockedException;
import cardservice.exception.CardExpiredException;
import cardservice.exception.CardGenerationFailedException;
import cardservice.exception.CardNotFoundException;
import cardservice.exception.CardsNotFoundException;
import cardservice.exception.InvalidCardLimitException;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GrpcExceptionInterceptor implements ServerInterceptor {
  private static final Logger log = LoggerFactory.getLogger(GrpcExceptionInterceptor.class);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
      @Override
      public void onHalfClose() {
        try {
          super.onHalfClose();
        } catch (Exception exception) {
          Status status = mapException(exception);
          log.error(
              "Unhandled gRPC exception: method={}, status={}",
              call.getMethodDescriptor().getFullMethodName(),
              status.getCode(),
              exception);
          call.close(status.withDescription(exception.getMessage()), new Metadata());
        }
      }
    };
  }

  private Status mapException(Exception exception) {
    if (exception instanceof CardNotFoundException || exception instanceof CardsNotFoundException) {
      return Status.NOT_FOUND;
    }

    if (exception instanceof CardBlockedException
        || exception instanceof CardExpiredException
        || exception instanceof CardGenerationFailedException
        || exception instanceof InvalidCardLimitException) {
      return Status.FAILED_PRECONDITION;
    }

    if (exception instanceof IllegalArgumentException) {
      return Status.INVALID_ARGUMENT;
    }

    if (exception instanceof IllegalStateException) {
      return Status.FAILED_PRECONDITION;
    }

    return Status.INTERNAL;
  }
}
