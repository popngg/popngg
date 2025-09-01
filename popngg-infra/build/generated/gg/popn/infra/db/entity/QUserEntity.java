package gg.popn.infra.db.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserEntity is a Querydsl query type for UserEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserEntity extends EntityPathBase<UserEntity> {

    private static final long serialVersionUID = 314146250L;

    public static final QUserEntity userEntity = new QUserEntity("userEntity");

    public final NumberPath<Integer> battleCredit = createNumber("battleCredit", Integer.class);

    public final StringPath character = createString("character");

    public final StringPath comment = createString("comment");

    public final DateTimePath<java.util.Date> createdAt = createDateTime("createdAt", java.util.Date.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> isHidden = createNumber("isHidden", Integer.class);

    public final NumberPath<Integer> localCredit = createNumber("localCredit", Integer.class);

    public final NumberPath<Integer> normalCredit = createNumber("normalCredit", Integer.class);

    public final StringPath password = createString("password");

    public final NumberPath<Integer> popclass = createNumber("popclass", Integer.class);

    public final StringPath poptomoId = createString("poptomoId");

    public final StringPath role = createString("role");

    public final DateTimePath<java.util.Date> updatedAt = createDateTime("updatedAt", java.util.Date.class);

    public final StringPath userName = createString("userName");

    public QUserEntity(String variable) {
        super(UserEntity.class, forVariable(variable));
    }

    public QUserEntity(Path<? extends UserEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserEntity(PathMetadata metadata) {
        super(UserEntity.class, metadata);
    }

}

