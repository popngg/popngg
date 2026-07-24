package gg.popn.infra.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_profiles")
public class UserProfileEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "character_name", nullable = false)
    private String characterName;

    @Column(name = "comment", nullable = false)
    private String comment;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    @Column(name = "display_popclass", nullable = false)
    private int displayPopclass;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
