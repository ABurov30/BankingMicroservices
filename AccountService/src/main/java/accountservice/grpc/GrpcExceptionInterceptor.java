package accountservice.grpc;

import accountservice.exception.AccountAlreadyFrozenException;
import accountservice.exception.AccountClosedException;
import accountservice.exception.AccountGenerationFailedException;
import accountservice.exception.AccountNotFoundException;
import accountservice.exception.AccountsNotFoundException;
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
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
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
                            exception
                    );
                    call.close(status.withDescription(exception.getMessage()), new Metadata());
                }
            }
        };
    }

    private Status mapException(Exception exception) {
        if (
                exception instanceof AccountNotFoundException ||
                        exception instanceof AccountsNotFoundException
        ) {
            return Status.NOT_FOUND;
        }

        if (
                exception instanceof AccountAlreadyFrozenException ||
                        exception instanceof AccountClosedException ||
                        exception instanceof AccountGenerationFailedException
        ) {
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
