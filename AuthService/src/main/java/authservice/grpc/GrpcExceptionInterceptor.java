package authservice.grpc;

import authservice.exception.AuthUserAlreadyVerifiedException;
import authservice.exception.AuthUserNotFoundException;
import authservice.exception.AuthUserNotActiveException;
import authservice.exception.EmailAlreadyExistsException;
import authservice.exception.InvalidEmailOrPasswordException;
import authservice.exception.InvalidVerificationCodeException;
import authservice.exception.RefreshTokenAlreadyExpiredException;
import authservice.exception.RefreshTokenAlreadyRevokedException;
import authservice.exception.RefreshTokenNotFoundException;
import authservice.exception.RoleNotFoundException;
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
        if (exception instanceof EmailAlreadyExistsException) {
            return Status.ALREADY_EXISTS;
        }

        if (exception instanceof InvalidEmailOrPasswordException) {
            return Status.UNAUTHENTICATED;
        }

        if (
                exception instanceof RefreshTokenNotFoundException ||
                        exception instanceof RoleNotFoundException ||
                        exception instanceof AuthUserNotFoundException
        ) {
            return Status.NOT_FOUND;
        }

        if (
                exception instanceof RefreshTokenAlreadyExpiredException ||
                        exception instanceof RefreshTokenAlreadyRevokedException ||
                        exception instanceof AuthUserAlreadyVerifiedException ||
                        exception instanceof AuthUserNotActiveException
        ) {
            return Status.FAILED_PRECONDITION;
        }

        if (exception instanceof InvalidVerificationCodeException) {
            return Status.INVALID_ARGUMENT;
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
