package apigateway.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class CookieConfig {

    public void setCookieTokens (HttpServletResponse response,
                                 String accessToken,
                                 int accessTokenMinutesTtl,
                                 String refreshToken,
                                 int refreshTokenDaysTtl) {
        Cookie atCookie = new Cookie("at", accessToken);
        atCookie.setMaxAge(accessTokenMinutesTtl * 60);
        atCookie.setPath("/");
        atCookie.setHttpOnly(true);
        atCookie.setSecure(true);

        Cookie rtCookie = new Cookie("rt", refreshToken);
        rtCookie.setMaxAge(refreshTokenDaysTtl * 24 * 60 * 60);
        rtCookie.setPath("/");
        rtCookie.setHttpOnly(true);
        rtCookie.setSecure(true);

        response.addCookie(rtCookie);
        response.addCookie(atCookie);
    }

    public void clearCookieTokens (HttpServletResponse response){
        Cookie atCookie = new Cookie("at", null);
        atCookie.setMaxAge(0);
        atCookie.setPath("/");
        atCookie.setHttpOnly(true);
        atCookie.setSecure(true);

        Cookie rtCookie = new Cookie("rt", null);
        rtCookie.setMaxAge(0);
        rtCookie.setPath("/");
        rtCookie.setHttpOnly(true);
        rtCookie.setSecure(true);

        response.addCookie(rtCookie);
        response.addCookie(atCookie);
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
}
