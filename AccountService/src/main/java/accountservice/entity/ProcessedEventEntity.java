package accountservice.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity extends processedevent.ProcessedEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
}
