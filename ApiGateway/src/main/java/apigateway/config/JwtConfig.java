package apigateway.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {
  @Bean
  public JwtDecoder jwtDecoder(@Value("${JWT_PUBLIC_KEY_PATH}") String publicKeyPath)
      throws Exception {
    String pem =
        Files.readString(Path.of(publicKeyPath))
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

    byte[] decoded = Base64.getDecoder().decode(pem);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
    RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
    return NimbusJwtDecoder.withPublicKey(publicKey).build();
  }
}
