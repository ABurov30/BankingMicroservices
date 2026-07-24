package cardservice.repository;

import cardservice.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<CardEntity, UUID> {
    boolean existsByPan(String pan);

    Optional<List<CardEntity>> findByAccountId(UUID accountId);
    Optional<List<CardEntity>> findAllByAccountId(UUID accountId);
}
