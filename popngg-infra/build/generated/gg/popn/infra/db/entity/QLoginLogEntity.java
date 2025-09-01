package gg.popn.infra.db.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLoginLogEntity is a Querydsl query type for LoginLogEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLoginLogEntity extends EntityPathBase<LoginLogEntity> {

    private static final long serialVersionUID = -801902822L;

    public static final QLoginLogEntity loginLogEntity = new QLoginLogEntity("loginLogEntity");

    public final DateTimePath<java.util.Date> createdAt = createDateTime("createdAt", java.util.Date.class);

    public final StringPath failureReason = createString("failureReason");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath ip = createString("ip");

    public final NumberPath<Integer> isSucceeded = createNumber("isSucceeded", Integer.class);

    public final StringPath password = createString("password");

    public final StringPath poptomoId = createString("poptomoId");

    public QLoginLogEntity(String variable) {
        super(LoginLogEntity.class, forVariable(variable));
    }

    public QLoginLogEntity(Path<? extends LoginLogEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLoginLogEntity(PathMetadata metadata) {
        super(LoginLogEntity.class, metadata);
    }

}

