package apigateway.config;

import apigateway.exception.InvalidAccessTokenException;
import apigateway.exception.MissingAccessTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Configuration
public class CookieConfig {

    private final JwtDecoder jwtDecoder;

    public CookieConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    public void setCookieTokens(HttpServletResponse response,
                                String accessToken,
                                int accessTokenMinutesTtl,
                                String refreshToken,
                                int refreshTokenDaysTtl) {

        ResponseCookie atCookie = ResponseCookie.from("at", accessToken)
                .maxAge(accessTokenMinutesTtl * 60L)
                .path("/")
                .httpOnly(true)
                .secure(true)
//                .sameSite("Strict")
                .build();

        ResponseCookie rtCookie = ResponseCookie.from("rt", refreshToken)
                .maxAge(refreshTokenDaysTtl * 24L * 60 * 60)
                .path("/")
                .httpOnly(true)
                .secure(true)
//                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
    }

    public void clearCookieTokens(HttpServletResponse response) {
        ResponseCookie atCookie = ResponseCookie.from("at", "")
                .maxAge(0)
                .path("/")
                .httpOnly(true)
                .secure(true)
//                .sameSite("Strict")
                .build();

        ResponseCookie rtCookie = ResponseCookie.from("rt", "")
                .maxAge(0)
                .path("/")
                .httpOnly(true)
                .secure(true)
//                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
    }

    public String getCookieByKey (HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return  null;
        }

        return Arrays.stream(request.getCookies())
                .filter((c) -> cookieName.equals(c.getName()))
                .map((c) -> c.getValue())
                .findFirst()
                .orElse(null);
    }

    public Jwt getAccessTokenJwt(HttpServletRequest request) {
        String accessToken = getCookieByKey(request, "at");

        if (accessToken == null || accessToken.isBlank()) {
            throw new MissingAccessTokenException();
        }

        Jwt jwt = jwtDecoder.decode(accessToken);
        try {
            UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new InvalidAccessTokenException();
        }

        return jwt;
    }

    public String extractRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0);
        }

        return jwt.getClaimAsString("role");
    }
}
