package apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

  @LocalServerPort private int port;

  @Test
  void contextLoads() {}

  @Test
  void asyncApiDocsArePubliclyAvailable() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpResponse<String> uiResponse = get(client, "/asyncapi-ui.html");
    HttpResponse<String> specResponse = get(client, "/asyncapi.yaml");
    HttpResponse<String> specResponseWithInvalidCookie =
        get(client, "/asyncapi.yaml", "at=invalid-token");
    HttpResponse<String> aliasResponse = get(client, "/asyncapi");
    HttpResponse<String> rootResponse = get(client, "/");

    assertThat(uiResponse.statusCode()).isEqualTo(200);
    assertThat(uiResponse.body())
        .contains("Bank API Gateway AsyncAPI", "/asyncapi.yaml", "/browser/standalone/index.js");
    assertThat(specResponse.statusCode()).isEqualTo(200);
    assertThat(specResponse.body()).contains("asyncapi: 3.0.0");
    assertThat(specResponseWithInvalidCookie.statusCode()).isEqualTo(200);
    assertThat(specResponseWithInvalidCookie.body()).contains("asyncapi: 3.0.0");
    assertThat(aliasResponse.statusCode()).isBetween(300, 399);
    assertThat(aliasResponse.headers().firstValue("location"))
        .hasValueSatisfying(location -> assertThat(location).endsWith("/asyncapi-ui.html"));
    assertThat(rootResponse.statusCode()).isBetween(300, 399);
    assertThat(rootResponse.headers().firstValue("location"))
        .hasValueSatisfying(location -> assertThat(location).endsWith("/swagger-ui.html"));
  }

  private HttpResponse<String> get(HttpClient client, String path)
      throws IOException, InterruptedException {
    return get(client, path, null);
  }

  private HttpResponse<String> get(HttpClient client, String path, String cookie)
      throws IOException, InterruptedException {
    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
    if (cookie != null) {
      requestBuilder.header("Cookie", cookie);
    }
    HttpRequest request = requestBuilder.build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
