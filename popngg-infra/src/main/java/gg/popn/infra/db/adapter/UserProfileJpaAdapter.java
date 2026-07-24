package gg.popn.infra.db.adapter;

import gg.popn.application.user.dto.command.UpdateUserProfileCommand;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.exception.UserProfileNotFoundException;
import gg.popn.application.user.port.out.UserProfilePort;
import gg.popn.infra.db.entity.UserEntity;
import gg.popn.infra.db.jpa.UserJpaRepository;
import gg.popn.infra.db.jpa.UserProfileJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileJpaAdapter implements UserProfilePort {
    private final UserJpaRepository userRepository;
    private final UserProfileJpaRepository profileRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<UserProfileResult> findByPoptomoId(String poptomoId) {
        return userRepository.findByPoptomoId(poptomoId).map(this::toResult);
    }

    @Override
    public UserProfileResult update(UpdateUserProfileCommand command) {
        var user = userRepository.findByPoptomoId(command.poptomoId())
                .orElseThrow(UserProfileNotFoundException::new);
        user.getProfile().update(
                command.userName(),
                command.characterName(),
                command.comment(),
                command.profileImageUrl(),
                command.hidden(),
                LocalDateTime.now());
        profileRepository.save(user.getProfile());
        return toResult(user);
    }

    @Override
    public RankingPage findRankings(UserRankingQuery query) {
        String sortProperty = switch (query.sort()) {
            case DISPLAY_POPCLASS -> "displayPopclass";
            case POTENTIAL_POPCLASS -> "potentialPopclass";
            case LEGACY_POPCLASS -> "legacyPopclass";
        };
        List<UserEntity> users = entityManager.createQuery(
                        "select u from UserEntity u join fetch u.profile p "
                                + "where p.hidden = false "
                                + "order by p." + sortProperty + " desc, u.poptomoId asc",
                        UserEntity.class)
                .setFirstResult(query.page() * query.size())
                .setMaxResults(query.size())
                .getResultList();
        long total = entityManager.createQuery(
                        "select count(u) from UserEntity u join u.profile p where p.hidden = false",
                        Long.class)
                .getSingleResult();
        return new RankingPage(users.stream().map(this::toResult).toList(), total);
    }

    private UserProfileResult toResult(UserEntity user) {
        var profile = user.getProfile();
        return new UserProfileResult(
                user.getPoptomoId(),
                profile.getUserName(),
                profile.getCharacterName(),
                profile.getComment(),
                profile.getProfileImageUrl(),
                profile.isHidden(),
                profile.getDisplayPopclass(),
                profile.getPotentialPopclass(),
                profile.getLegacyPopclass(),
                profile.getNormalCredit(),
                profile.getExtraCredit(),
                profile.getTimePlay10Credit(),
                profile.getTimePlay16Credit());
    }
}
