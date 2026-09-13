package apigateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import apigateway.client.AuthGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.request.auth.SignupRequestDto;
import apigateway.exception.GlobalExceptionHandler;
import io.grpc.Status;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class AuthGatewayControllerTest {
  private static final String SIGNUP_URL = "/auth/signup";

  private static final String EMAIL = "test@example.com";
  private static final String INVALID_EMAIL = "invalid-email";

  private static final String PASSWORD = "password123";
  private static final String INVALID_PASSWORD = "1234567";

  private static final String FIRST_NAME = "Test";
  private static final String INVALID_FIRST_NAME = " ";

  private static final String LAST_NAME = "User";
  private static final String INVALID_LAST_NAME = " ";

  private final AuthGrpcClient authGrpcClient = mock(AuthGrpcClient.class);
  private final CookieConfig cookieConfig = mock(CookieConfig.class);

  private final MockMvc mockMvc =
      MockMvcBuilders.standaloneSetup(new AuthGatewayController(authGrpcClient, cookieConfig))
          .setControllerAdvice(new GlobalExceptionHandler())
          .build();

  private static String signupJson(
      String email, String password, String firstName, String lastName) {
    return """
        {
          "email": "%s",
          "password": "%s",
          "firstName": "%s",
          "lastName": "%s"
        }
        """
        .formatted(email, password, firstName, lastName);
  }

  @Test
  void shouldSignupWhenRequestIsValid() throws Exception {
    mockMvc
        .perform(
            post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(EMAIL, PASSWORD, FIRST_NAME, LAST_NAME)))
        .andExpect(status().isOk());

    ArgumentCaptor<SignupRequestDto> signupRequestDtoCaptor =
        ArgumentCaptor.forClass(SignupRequestDto.class);

    verify(authGrpcClient).signup(signupRequestDtoCaptor.capture());

    SignupRequestDto dto = signupRequestDtoCaptor.getValue();

    assertThat(dto.email()).isEqualTo(EMAIL);
    assertThat(dto.password()).isEqualTo(PASSWORD);
    assertThat(dto.firstName()).isEqualTo(FIRST_NAME);
    assertThat(dto.lastName()).isEqualTo(LAST_NAME);
  }

  @Test
  void shouldRejectWhenEmailAlreadyInUse() throws Exception {
    doThrow(Status.ALREADY_EXISTS.withDescription("Email already exists").asRuntimeException())
        .when(authGrpcClient)
        .signup(any(SignupRequestDto.class));

    mockMvc
        .perform(
            post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(EMAIL, PASSWORD, FIRST_NAME, LAST_NAME)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.message[0]").value("Email already exists"));
  }

  @ParameterizedTest(name = "Should return bad request on {0}")
  @MethodSource("jsonToRejectProvider")
  void shouldRejectSignupWhenRequestIsInvalid(String signupJson) throws Exception {
    mockMvc
        .perform(post(SIGNUP_URL).contentType(MediaType.APPLICATION_JSON).content(signupJson))
        .andExpect(status().isBadRequest());
    verify(authGrpcClient, never()).signup(any(SignupRequestDto.class));
  }

  static Stream<Arguments> jsonToRejectProvider() {
    return Stream.of(
        Arguments.of(signupJson(INVALID_EMAIL, PASSWORD, FIRST_NAME, LAST_NAME)),
        Arguments.of(signupJson(EMAIL, INVALID_PASSWORD, FIRST_NAME, LAST_NAME)),
        Arguments.of(signupJson(EMAIL, PASSWORD, INVALID_FIRST_NAME, LAST_NAME)),
        Arguments.of(signupJson(EMAIL, PASSWORD, FIRST_NAME, INVALID_LAST_NAME)));
  }
}
