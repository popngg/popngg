package gg.popn.infra.db.adapter;

import gg.popn.infra.db.jpa.PlaydataJpaRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaydataRepositoryJpaAdapter {
    private final PlaydataJpaRepository playdataJpaRepository;

}
