package authservice.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {
  private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
  private final JwtProperties jwtProperties;

  public JwtConfig(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  @Bean
  public JwtEncoder jwtEncoder() {
    Resource publicKeyResource = jwtProperties.publicKeyLocation();
    Resource privateKeyResource = jwtProperties.privateKeyLocation();

    try (InputStream publicKeyStream = publicKeyResource.getInputStream();
        InputStream privateKeyStream = privateKeyResource.getInputStream();
        Reader publicKeyReader = new InputStreamReader(publicKeyStream, StandardCharsets.UTF_8);
        Reader privateKeyReader = new InputStreamReader(privateKeyStream, StandardCharsets.UTF_8);
        PEMParser publicKeyPemParser = new PEMParser(publicKeyReader);
        PEMParser privateKeyPemParser = new PEMParser(privateKeyReader); ) {

      Object publicPemObject = publicKeyPemParser.readObject();
      Object privatePemObject = privateKeyPemParser.readObject();

      JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

      RSAPublicKey publicKey =
          (RSAPublicKey) converter.getPublicKey((SubjectPublicKeyInfo) publicPemObject);
      RSAPrivateKey privateKey =
          (RSAPrivateKey) converter.getPrivateKey((PrivateKeyInfo) privatePemObject);

      return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
    } catch (Exception exception) {
      log.error("Could not initialize JWT encoder from configured RSA key resources", exception);
      throw new IllegalStateException(
          "Could not initialize JWT encoder from configured RSA key resources", exception);
    }
  }
}
