package gg.popn.infra.db.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity @Data
@RequiredArgsConstructor @AllArgsConstructor
@Builder @Getter
@Table(name = "history")
public class HistoryEntity {
    @Id
    @Column(name = "history_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chart_id")
    private ChartEntity chart;

    @Column(name = "\"rank\"")
    private Integer rank;

    @Column(name = "medal")
    private Integer medal;

    @Column(name = "score")
    private Integer score;

    @Column(name = "popclass")
    private Integer popclass;

    @Column(name = "created_at")
    private Date createdAt;

}