package gg.popn.infra.db.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPlaydataEntity is a Querydsl query type for PlaydataEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPlaydataEntity extends EntityPathBase<PlaydataEntity> {

    private static final long serialVersionUID = 352483997L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPlaydataEntity playdataEntity = new QPlaydataEntity("playdataEntity");

    public final QChartEntity chart;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> medal = createNumber("medal", Integer.class);

    public final NumberPath<Integer> popclass = createNumber("popclass", Integer.class);

    public final NumberPath<Integer> rank = createNumber("rank", Integer.class);

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public final QUserEntity user;

    public QPlaydataEntity(String variable) {
        this(PlaydataEntity.class, forVariable(variable), INITS);
    }

    public QPlaydataEntity(Path<? extends PlaydataEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPlaydataEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPlaydataEntity(PathMetadata metadata, PathInits inits) {
        this(PlaydataEntity.class, metadata, inits);
    }

    public QPlaydataEntity(Class<? extends PlaydataEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chart = inits.isInitialized("chart") ? new QChartEntity(forProperty("chart")) : null;
        this.user = inits.isInitialized("user") ? new QUserEntity(forProperty("user")) : null;
    }

}

