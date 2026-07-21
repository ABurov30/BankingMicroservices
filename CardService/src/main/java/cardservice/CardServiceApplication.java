package cardservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class CardServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CardServiceApplication.class, args);
  }
}
