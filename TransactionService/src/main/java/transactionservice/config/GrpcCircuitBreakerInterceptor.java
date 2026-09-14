package transactionservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Accounts for logical RPC outcomes, including all native retry attempts. */
public class GrpcCircuitBreakerInterceptor implements ClientInterceptor {
  private final CircuitBreaker breaker;

  public GrpcCircuitBreakerInterceptor(CircuitBreaker breaker) {
    this.breaker = breaker;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions options, Channel next) {
    return new ClientCall<>() {
      private volatile ClientCall<ReqT, RespT> delegate;
      private final AtomicReference<Status> cancellation = new AtomicReference<>();
      private final AtomicBoolean completed = new AtomicBoolean();
      private long started;

      @Override
      public void start(Listener<RespT> listener, Metadata headers) {
        if (cancellation.get() != null) {
          listener.onClose(cancellation.get(), new Metadata());
          return;
        }
        if (!breaker.tryAcquirePermission()) {
          listener.onClose(
              Status.UNAVAILABLE.withDescription("Downstream circuit open: " + breaker.getName()),
              new Metadata());
          return;
        }
        started = System.nanoTime();
        try {
          delegate = next.newCall(method, options);
          delegate.start(
              new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(listener) {
                @Override
                public void onMessage(RespT message) {
                  // A long-lived stream proves availability on its first response, not at
                  // disconnect.
                  if (method.getType() == MethodDescriptor.MethodType.SERVER_STREAMING) {
                    complete(Status.OK);
                  }
                  super.onMessage(message);
                }

                @Override
                public void onClose(Status status, Metadata trailers) {
                  complete(status);
                  super.onClose(status, trailers);
                }
              },
              headers);
          Status cancelled = cancellation.get();
          if (cancelled != null) {
            delegate.cancel(cancelled.getDescription(), cancelled.getCause());
          }
        } catch (RuntimeException exception) {
          complete(Status.fromThrowable(exception));
          throw exception;
        }
      }

      private void complete(Status status) {
        if (completed.compareAndSet(false, true)) {
          long elapsed = System.nanoTime() - started;
          if (status.isOk()) {
            breaker.onSuccess(elapsed, TimeUnit.NANOSECONDS);
          } else {
            breaker.onError(elapsed, TimeUnit.NANOSECONDS, status.asRuntimeException());
          }
        }
      }

      @Override
      public void request(int count) {
        if (delegate != null) {
          delegate.request(count);
        }
      }

      @Override
      public void cancel(String message, Throwable cause) {
        cancellation.compareAndSet(
            null, Status.CANCELLED.withDescription(message).withCause(cause));
        if (delegate != null) {
          delegate.cancel(message, cause);
        }
      }

      @Override
      public void halfClose() {
        if (delegate != null) {
          delegate.halfClose();
        }
      }

      @Override
      public void sendMessage(ReqT message) {
        if (delegate != null) {
          delegate.sendMessage(message);
        }
      }

      @Override
      public boolean isReady() {
        return delegate != null && delegate.isReady();
      }

      @Override
      public void setMessageCompression(boolean enabled) {
        if (delegate != null) {
          delegate.setMessageCompression(enabled);
        }
      }
    };
  }
}
