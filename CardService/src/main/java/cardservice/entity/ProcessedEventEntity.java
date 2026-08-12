package cardservice.entity;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity extends outboxsupport.ProcessedEvent {
  @Id @GeneratedValue private UUID id;
}
