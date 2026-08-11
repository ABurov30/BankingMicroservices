package transactionservice.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import transactionservice.entity.TransactionEntity;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tr from TransactionEntity tr WHERE tr.id = :transactionId")
    Optional<TransactionEntity> findByIdToUpdate(@Param("transactionId") UUID transactionId);
}
