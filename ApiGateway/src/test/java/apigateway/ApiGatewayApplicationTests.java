package apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.kafka.listener.auto-startup=false",
      "KAFKA_BOOTSTRAP_SERVERS=localhost:9092",
      "SCHEMA_REGISTRY_URL=http://localhost:8081"
    })
@Testcontainers
class ApiGatewayApplicationTests {

  @LocalServerPort private int port;

  @Test
  void contextLoads() {}

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

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
