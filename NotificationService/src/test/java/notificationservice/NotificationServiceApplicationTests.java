package notificationservice;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("integration")
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(
    properties = {
      "spring.kafka.listener.auto-startup=false",
      "KAFKA_BOOTSTRAP_SERVERS=localhost:9092",
      "SCHEMA_REGISTRY_URL=http://localhost:8081"
    })
class NotificationServiceApplicationTests {

  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16-alpine");

  @Container @ServiceConnection
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

  @Test
  void contextLoads() {}
}
