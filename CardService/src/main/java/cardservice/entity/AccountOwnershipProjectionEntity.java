package cardservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "account_ownership_projection")
public class AccountOwnershipProjectionEntity {
  @Id
  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "owner_auth_user_id", nullable = false)
  private UUID ownerAuthUserId;

  @Column(name = "account_number")
  private String accountNumber;
}
