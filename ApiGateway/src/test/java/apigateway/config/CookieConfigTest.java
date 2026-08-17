package apigateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieConfigTest {

  @Test
  void setCookieTokensUsesConfiguredCookieScope() {
    CookieConfig cookieConfig = new CookieConfig(token -> null, cookieProperties());
    MockHttpServletResponse response = new MockHttpServletResponse();

    cookieConfig.setCookieTokens(response, "access-token", 15, "refresh-token", 7);

    List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertEquals(2, cookies.size());
    assertCookieContains(
        cookies,
        "at",
        "at=access-token",
        "Domain=buro-bank.ru",
        "Max-Age=900",
        "Path=/",
        "Secure",
        "HttpOnly",
        "SameSite=None");
    assertCookieContains(
        cookies,
        "rt",
        "rt=refresh-token",
        "Domain=buro-bank.ru",
        "Max-Age=604800",
        "Path=/",
        "Secure",
        "HttpOnly",
        "SameSite=None");
  }

  @Test
  void clearCookieTokensUsesConfiguredCookieScope() {
    CookieConfig cookieConfig = new CookieConfig(token -> null, cookieProperties());
    MockHttpServletResponse response = new MockHttpServletResponse();

    cookieConfig.clearCookieTokens(response);

    List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertEquals(2, cookies.size());
    assertCookieContains(cookies, "at", "at=", "Domain=buro-bank.ru", "Max-Age=0");
    assertCookieContains(cookies, "rt", "rt=", "Domain=buro-bank.ru", "Max-Age=0");
  }

  private AuthCookieProperties cookieProperties() {
    AuthCookieProperties properties = new AuthCookieProperties();
    properties.setDomain("buro-bank.ru");
    properties.setSameSite("None");
    properties.setSecure(true);
    return properties;
  }

  private void assertCookieContains(List<String> cookies, String name, String... parts) {
    assertTrue(
        cookies.stream()
            .filter(cookie -> cookie.startsWith(name + "="))
            .anyMatch(cookie -> Arrays.stream(parts).allMatch(cookie::contains)),
        () -> "Missing cookie " + name + " with parts " + Arrays.toString(parts) + ": " + cookies);
  }
}
