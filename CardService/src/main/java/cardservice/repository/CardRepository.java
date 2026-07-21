package cardservice.repository;

import cardservice.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CardRepository extends JpaRepository<CardEntity, UUID> {
    boolean existsByPan(String pan);
}
