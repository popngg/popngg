package gg.popn.infra.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity @Data
@RequiredArgsConstructor @AllArgsConstructor
@Builder @Getter
@Table(name = "renew_log")
public class RenewLogEntity {
    @Id @Column(name = "renew_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "poptomo_id")
    private String poptomoId;

    @Column(name = "is_succeeded")
    private Integer isSucceeded;

    @Column(name = "is_register")
    private Integer isRegister;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "chart_count")
    private Integer chartCount;

    @Column(name = "ip") //TODO: ip 수집 로직 + 약관 업데이트
    private String ip;
}