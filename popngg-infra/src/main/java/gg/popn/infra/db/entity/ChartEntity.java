package gg.popn.infra.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "chart")
public class ChartEntity {
    @Id
    @Column(name = "chart_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "song_hash", nullable = false)
    private String songHash;

    @Column(name = "genre_name", nullable = false)
    private String genreName;

    @Column(name = "song_name", nullable = false)
    private String songName;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "difficulty", nullable = false)
    private Integer difficulty;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "jacket")
    private String jacket;

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted;

    @Column(name = "is_upper", nullable = false)
    private Integer isUpper;

    @Column(name = "created_at")
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}
