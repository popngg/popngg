package gg.popn.infra.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@RequiredArgsConstructor @AllArgsConstructor
@Builder @Getter
@Table(name = "login_logs")
public class LoginLogEntity {
    @Id @Column(name = "login_log_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "poptomo_id")
    private String poptomoId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "ip")
    private String ip;
}
