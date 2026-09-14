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

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(
      value = Status.Code.class,
      names = {"UNAVAILABLE", "DEADLINE_EXCEEDED", "INTERNAL", "RESOURCE_EXHAUSTED"})
  void technicalFailureReturns503ForBothReadAndWrite(Status.Code code) {
    for (String verb : java.util.List.of("GET", "POST")) {
      var response =
          handler.handleGrpcException(
              Status.fromCode(code).asRuntimeException(),
              new MockHttpServletRequest(verb, "/account/test"));
      assertThat(response.getStatusCode().value()).isEqualTo(503);
      assertThat(response.getBody().status()).isEqualTo(503);
    }
  }
}
