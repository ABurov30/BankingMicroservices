package apigateway.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleCompletionExceptionMapsWrappedGrpcStatus() {
    var grpcException = Status.NOT_FOUND.withDescription("User not found").asRuntimeException();
    var exception = new CompletionException(grpcException);
    var request = new MockHttpServletRequest("GET", "/api/user/42");

    ResponseEntity<ApiErrorResponse> response =
        handler.handleCompletionException(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody())
        .extracting(ApiErrorResponse::status, ApiErrorResponse::message)
        .containsExactly(404, java.util.List.of("User not found"));
  }
}
