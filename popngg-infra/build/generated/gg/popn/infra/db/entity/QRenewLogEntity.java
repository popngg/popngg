package gg.popn.infra.db.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRenewLogEntity is a Querydsl query type for RenewLogEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRenewLogEntity extends EntityPathBase<RenewLogEntity> {

    private static final long serialVersionUID = 1126571126L;

    public static final QRenewLogEntity renewLogEntity = new QRenewLogEntity("renewLogEntity");

    public final NumberPath<Integer> chartCount = createNumber("chartCount", Integer.class);

    public final DateTimePath<java.util.Date> createdAt = createDateTime("createdAt", java.util.Date.class);

    public final StringPath failureReason = createString("failureReason");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath ip = createString("ip");

    public final NumberPath<Integer> isRegister = createNumber("isRegister", Integer.class);

    public final NumberPath<Integer> isSucceeded = createNumber("isSucceeded", Integer.class);

    public final StringPath poptomoId = createString("poptomoId");

    public QRenewLogEntity(String variable) {
        super(RenewLogEntity.class, forVariable(variable));
    }

    public QRenewLogEntity(Path<? extends RenewLogEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRenewLogEntity(PathMetadata metadata) {
        super(RenewLogEntity.class, metadata);
    }

}

