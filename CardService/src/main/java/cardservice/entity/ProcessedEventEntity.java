package cardservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
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
