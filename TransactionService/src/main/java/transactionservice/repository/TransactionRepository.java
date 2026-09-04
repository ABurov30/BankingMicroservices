package transactionservice.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import transactionservice.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT tr from TransactionEntity tr WHERE tr.id = :transactionId")
  Optional<TransactionEntity> findByIdToUpdate(@Param("transactionId") UUID transactionId);

  @Query(
      """
      SELECT tr
      FROM TransactionEntity tr
      WHERE tr.sourceAccountId IN :accountIds OR tr.targetAccountId IN :accountIds
      ORDER BY tr.createdAt DESC
      """)
  List<TransactionEntity> findByAccountIds(@Param("accountIds") Collection<UUID> accountIds);

  Optional<TransactionEntity> findByIdempotencyKey(UUID idempotencyKey);
}
