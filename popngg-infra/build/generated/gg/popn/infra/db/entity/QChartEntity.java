package gg.popn.infra.db.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChartEntity is a Querydsl query type for ChartEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChartEntity extends EntityPathBase<ChartEntity> {

    private static final long serialVersionUID = 1814465189L;

    public static final QChartEntity chartEntity = new QChartEntity("chartEntity");

    public final DateTimePath<java.util.Date> createdAt = createDateTime("createdAt", java.util.Date.class);

    public final NumberPath<Integer> difficulty = createNumber("difficulty", Integer.class);

    public final StringPath genreName = createString("genreName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> isDeleted = createNumber("isDeleted", Integer.class);

    public final NumberPath<Integer> isUpper = createNumber("isUpper", Integer.class);

    public final StringPath jacket = createString("jacket");

    public final NumberPath<Integer> level = createNumber("level", Integer.class);

    public final StringPath songHash = createString("songHash");

    public final StringPath songName = createString("songName");

    public final NumberPath<Integer> version = createNumber("version", Integer.class);

    public QChartEntity(String variable) {
        super(ChartEntity.class, forVariable(variable));
    }

    public QChartEntity(Path<? extends ChartEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChartEntity(PathMetadata metadata) {
        super(ChartEntity.class, metadata);
    }

}

