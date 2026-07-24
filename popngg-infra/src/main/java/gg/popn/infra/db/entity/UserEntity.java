package gg.popn.infra.db.entity;


import jakarta.persistence.*;

import lombok.*;

import java.util.Date;

@Entity @Data
@RequiredArgsConstructor @AllArgsConstructor
@Builder @Getter
@Table(name = "\"user\"")
public class UserEntity {
    @Id @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "poptomo_id")
    private String poptomoId;

    @Column(name = "popclass")
    private Integer popclass;

    @Column(name = "\"character\"")
    private String character;

    @Column(name = "comment")
    private String comment;

    @Column(name = "is_hidden")
    private Integer isHidden;

    @Column(name = "password")
    private String password;

    @Column(name = "normal_credit")
    private Integer normalCredit;

    @Column(name = "battle_credit")
    private Integer battleCredit;

    @Column(name = "local_credit")
    private Integer localCredit;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @Column(name = "role")
    private String role;

//    @Builder.Default
//    @OneToMany(mappedBy = "user")
//    private List<Playdata> playdatas = new ArrayList<>();
}