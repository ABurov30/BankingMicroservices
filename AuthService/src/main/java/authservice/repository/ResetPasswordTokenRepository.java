package authservice.repository;

import authservice.entity.ResetPasswordTokenEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResetPasswordTokenRepository
    extends JpaRepository<ResetPasswordTokenEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
        SELECT rpt
        FROM ResetPasswordTokenEntity rpt
        WHERE rpt.tokenHash = :tokenHash
          AND rpt.usedAt IS NULL
          AND rpt.expiresAt > :now
        """)
  Optional<ResetPasswordTokenEntity> findByTokenHashForUpdate(
      @Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
        SELECT rpt
        FROM ResetPasswordTokenEntity rpt
        WHERE rpt.authUser.id = :authUserId
          AND rpt.usedAt IS NULL
          AND rpt.expiresAt > :now
        """)
  List<ResetPasswordTokenEntity> findAllByAuthUserIdForUpdate(
      @Param("authUserId") UUID authUserId, @Param("now") LocalDateTime now);
}
