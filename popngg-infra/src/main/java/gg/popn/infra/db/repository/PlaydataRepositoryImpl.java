package gg.popn.infra.db.repository;

import gg.popn.infra.db.jpa.PlaydataJpaRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaydataRepositoryImpl {
    private final PlaydataJpaRepository playdataJpaRepository;

}
