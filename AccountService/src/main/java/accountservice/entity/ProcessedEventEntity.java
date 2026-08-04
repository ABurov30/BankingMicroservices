package accountservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity extends outboxsupport.ProcessedEvent {
    @Id
    @GeneratedValue
    private UUID id;
}
