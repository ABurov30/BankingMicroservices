package transactionservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity extends processedevent.ProcessedEvent {
  @Id @GeneratedValue private UUID id;
}
