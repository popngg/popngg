package gg.popn.infra.db.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QHistoryEntity is a Querydsl query type for HistoryEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QHistoryEntity extends EntityPathBase<HistoryEntity> {

    private static final long serialVersionUID = 1624012251L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QHistoryEntity historyEntity = new QHistoryEntity("historyEntity");

    public final QChartEntity chart;

    public final DateTimePath<java.util.Date> createdAt = createDateTime("createdAt", java.util.Date.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> medal = createNumber("medal", Integer.class);

    public final NumberPath<Integer> popclass = createNumber("popclass", Integer.class);

    public final NumberPath<Integer> rank = createNumber("rank", Integer.class);

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public final QUserEntity user;

    public QHistoryEntity(String variable) {
        this(HistoryEntity.class, forVariable(variable), INITS);
    }

    public QHistoryEntity(Path<? extends HistoryEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QHistoryEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QHistoryEntity(PathMetadata metadata, PathInits inits) {
        this(HistoryEntity.class, metadata, inits);
    }

    public QHistoryEntity(Class<? extends HistoryEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chart = inits.isInitialized("chart") ? new QChartEntity(forProperty("chart")) : null;
        this.user = inits.isInitialized("user") ? new QUserEntity(forProperty("user")) : null;
    }

}

