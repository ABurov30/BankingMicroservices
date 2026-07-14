package authservice.grpc;

import authservice.exception.EmailAlreadyExistsException;
import authservice.exception.InvalidEmailOrPasswordException;
import authservice.exception.RefreshTokenNotFoundException;
import authservice.exception.RoleNotFoundException;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.springframework.stereotype.Component;

@Component
public class GrpcExceptionInterceptor implements ServerInterceptor {

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
                        exception instanceof RoleNotFoundException
        ) {
            return Status.NOT_FOUND;
        }

        if (exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT;
        }

        if (exception instanceof IllegalStateException) {
            return Status.FAILED_PRECONDITION;
        }

        if (exception instanceof EmailAlreadyExistsException) {
            return Status.ALREADY_EXISTS;
        }


        return Status.INTERNAL;
    }
}