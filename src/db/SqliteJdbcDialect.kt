package com.github.rwsbillyang.ktorKit.db

import org.komapper.core.BuilderDialect
import org.komapper.core.dsl.builder.EntityUpsertStatementBuilder
import org.komapper.core.dsl.builder.SchemaStatementBuilder
import org.komapper.core.dsl.context.EntityUpsertContext
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.dialect.h2.jdbc.H2JdbcDialect
import org.komapper.jdbc.JdbcDialect
import java.sql.SQLException

/**
 * 自定义实现的 SQLite JDBC 方言
 */
class SqliteJdbcDialect(
    override val driver: String = "org.sqlite.JDBC"
) : H2JdbcDialect {

    // SQLite 的标识符转义策略：使用双引号 " 或反引号 `
    override fun enquote(name: String): String {
        return "\"$name\""
    }

    // 这里可以根据需要，覆写其他 SQLite 独有的类型映射或语法细节
}

class CustomSqliteDialect : JdbcDialect {
    override val driver: String = "org.sqlite.JDBC"

    override fun enquote(name: String): String = "\"$name\""

    // 覆盖/补充数据类型映射，例如 UUID, Long, Int 等
    // ...实现 JdbcDialect 要求的其它抽象属性与方法
    override fun getSequenceSql(sequenceName: String): String {
        TODO("Not yet implemented")
    }

    override fun getSchemaStatementBuilder(dialect: BuilderDialect): SchemaStatementBuilder {
        TODO("Not yet implemented")
    }

    override fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> getEntityUpsertStatementBuilder(
        dialect: BuilderDialect,
        context: EntityUpsertContext<ENTITY, ID, META>,
        entities: List<ENTITY>
    ): EntityUpsertStatementBuilder<ENTITY> {
        TODO("Not yet implemented")
    }

    // 告诉 Komapper 本数据库支持 LIMIT ... OFFSET ... 分页
    //fun supportsLimitOffset(): Boolean = true
    override fun isUniqueConstraintViolationError(exception: SQLException): Boolean {
        TODO("Not yet implemented")
    }


}