package apigateway.exception;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleGrpcException(
            StatusRuntimeException exception,
            HttpServletRequest request
    ) {
        log.error("gRPC request failed: httpMethod={}, requestUri={}, status={}, description={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getStatus().getCode(),
                exception.getStatus().getDescription(),
                exception
        );

        HttpStatus httpStatus = mapGrpcStatus(exception.getStatus().getCode());

        String message = exception.getStatus().getDescription();
        if (message == null || message.isBlank()) {
            message = "Service request failed";
        }


        List<String> messages = new ArrayList<>();
        messages.add(message);
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                messages);

        return ResponseEntity.status(httpStatus).body(response);
    }

    @ExceptionHandler(MissingAccessTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(MissingAccessTokenException exception) {
        log.error("Access token exception cause={}, description={}",
                exception.getCause(),
                exception.getMessage(),
                exception
        );

        List messages = new ArrayList();
        messages.add(exception.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                messages
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }


    @ExceptionHandler(MissingRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(MissingRefreshTokenException exception) {
        log.error("Refresh token exception cause={}, description={}",
                exception.getCause(),
                exception.getMessage(),
                exception
        );

        List messages = new ArrayList();
        messages.add(exception.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                messages
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        log.error("Validation exception cause={}, description={}",
                exception.getCause(),
                exception.getMessage(),
                exception
        );

        List<String> messages = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map((e) -> e.getField() + ": " + e.getDefaultMessage())
                .toList();

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                messages
        );
        return ResponseEntity.badRequest().body(response);
    }

    private HttpStatus mapGrpcStatus(Status.Code code) {
        return switch (code) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case FAILED_PRECONDITION -> HttpStatus.PRECONDITION_FAILED;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case CANCELLED -> HttpStatus.BAD_GATEWAY;
            case INTERNAL, UNKNOWN, DATA_LOSS -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
