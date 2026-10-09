//package com.github.rwsbillyang.ktorKit.db
//
//
//import org.komapper.jdbc.JdbcDialect
//import org.komapper.core.BuilderDialect
//import org.komapper.core.DataType
//import org.komapper.core.dsl.builder.EntityUpsertStatementBuilder
//import org.komapper.core.dsl.builder.SchemaStatementBuilder
//import org.komapper.core.dsl.context.EntityUpsertContext
//import org.komapper.core.dsl.metamodel.EntityMetamodel
//
//import java.sql.SQLException
//import kotlin.reflect.KType
//
//
//import org.komapper.core.*
//import org.komapper.core.dsl.builder.*
//import org.komapper.core.dsl.expression.Operand
//import org.komapper.core.dsl.metamodel.*
//import org.komapper.jdbc.*
//
//
//class PureSqliteJdbcDialect : JdbcDialect, BuilderDialect {
//
//    // ============================================================
//    // JdbcDialect
//    // ============================================================
//
//    override val driver: String = "org.sqlite.JDBC"
//
//    override fun enquote(name: String): String = "\"$name\""
//
//    /**
//     * SQLite 不支持 sequence，返回空字符串即可
//     * Komapper 在不需要 sequence 时不会调用此方法
//     */
//    override fun getSequenceSql(sequenceName: String): String {
//        return ""
//    }
//
//    override fun getSchemaStatementBuilder(dialect: BuilderDialect): SchemaStatementBuilder {
//        val base = object : AbstractSchemaStatementBuilder(dialect) {
//            // 只改自增：H2 常用 AUTO_INCREMENT，SQLite 用 AUTOINCREMENT
//            override fun resolveIdentity(property: org.komapper.core.dsl.metamodel.PropertyMetamodel<*, *, *>): String {
//                // 返回空也行；SQLite 的 "INTEGER PRIMARY KEY" 本身自增
//                return "AUTOINCREMENT"
//            }
//        }
//        return object : SchemaStatementBuilder by base {
//            override fun createTable(table: org.komapper.core.dsl.metamodel.Table): String {
//                return base.createTable(table)
//                    .replace("AUTO_INCREMENT", "AUTOINCREMENT")
//                    .replace(" \"VARCHAR\"", " TEXT") // 若生成带引号类型名再处理
//            }
//        }
//    }
//
//    override fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> getEntityUpsertStatementBuilder(
//        dialect: BuilderDialect,
//        context: EntityUpsertContext<ENTITY, ID, META>,
//        entities: List<ENTITY>
//    ): EntityUpsertStatementBuilder<ENTITY> {
//        return object : EntityUpsertStatementBuilder<ENTITY> {
//            override fun build(
//                assignments: List<Pair<PropertyMetamodel<ENTITY, *, *>, Operand>>
//            ): Statement {
//                if (entities.isEmpty()) return Statement.Empty
//
//                val table = context.target
//                val props = context.metamodel.properties()
//                val columns = props.joinToString(", ") { dialect.enquote(it.name) }
//                val placeholders = props.joinToString(", ") { "?" }
//
//                // 简单全量替换；要 ON CONFLICT(col) DO UPDATE SET ... 再读 assignments
//                val sql = "INSERT OR REPLACE INTO ${dialect.enquote(table.name)} ($columns) VALUES ($placeholders)"
//                return Statement.Simple(sql, emptyList())
//            }
//        }
//    }
//    override fun isUniqueConstraintViolationError(exception: SQLException): Boolean {
//        // SQLite 唯一约束违反 errorCode = 19 (SQLITE_CONSTRAINT)
//        return exception.errorCode == 19
//    }
//
//    // ============================================================
//    // BuilderDialect
//    // ============================================================
//
//    /**
//     * 将 Kotlin 值格式化为 SQL 字面量字符串
//     */
//    override fun <T : Any> formatValue(
//        value: T?,
//        type: KType,
//        masking: Boolean
//    ): String {
//        if (value == null) return "null"
//        if (masking) return "?"
//
//        return when (type.classifier) {
//            String::class -> "'" + value.toString().replace("'", "''") + "'"
//            Boolean::class -> if (value as Boolean) "1" else "0"
//            else -> value.toString()
//        }
//    }
//
//    /**
//     * Kotlin 类型 → SQLite 类型名
//     * 这是解决 "dataType not found for kotlin.String" 的核心
//     */
//    override fun <T : Any> getDataTypeName(type: KType): String {
//        return when (type.classifier) {
//            String::class -> "TEXT"
//            Int::class -> "INTEGER"
//            Long::class -> "INTEGER"
//            Short::class -> "INTEGER"
//            Byte::class -> "INTEGER"
//            Double::class -> "REAL"
//            Float::class -> "REAL"
//            Boolean::class -> "INTEGER"
//            java.math.BigDecimal::class -> "NUMERIC"
//            ByteArray::class -> "BLOB"
//            java.time.LocalDate::class -> "TEXT"
//            java.time.LocalTime::class -> "TEXT"
//            java.time.LocalDateTime::class -> "TEXT"
//            java.time.OffsetDateTime::class -> "TEXT"
//            java.util.UUID::class -> "TEXT"
//            else -> {
//                // 枚举类型存为 TEXT
//                val clazz = type.classifier as? Class<*>
//                if (clazz?.isEnum == true) "TEXT" else "TEXT"
//            }
//        }
//    }
//
//    /**
//     * 获取 DataType 实例（用于类型转换）
//     */
//    override fun <T : Any> getDataType(type: KType): DataType {
//        return getDataTypeOrNull(type)
//            ?: throw IllegalStateException("DataType not found for type: $type")
//    }
//
//    /**
//     * 获取 DataType 实例，找不到返回 null
//     */
//    override fun <T : Any> getDataTypeOrNull(type: KType): DataType? {
//        @Suppress("UNCHECKED_CAST")
//        return when (type.classifier) {
//            String::class -> JdbcStringDataType("TEXT")
//            Int::class -> JdbcIntDataType("INTEGER")
//            Long::class -> JdbcLongDataType("INTEGER")
//            Short::class -> JdbcShortDataType("INTEGER")
//            Byte::class -> JdbcByteDataType("INTEGER")
//            Double::class -> JdbcDoubleDataType("REAL")
//            Float::class -> JdbcFloatDataType("REAL")
//            Boolean::class -> JdbcBooleanDataType("INTEGER")
//            java.math.BigDecimal::class -> JdbcBigDecimalDataType("NUMERIC")
//            ByteArray::class -> JdbcByteArrayDataType("BLOB")
//            java.time.LocalDate::class -> JdbcLocalDateDataType("TEXT")
//            java.time.LocalTime::class -> JdbcLocalTimeDataType("TEXT")
//            java.time.LocalDateTime::class -> JdbcLocalDateTimeDataType("TEXT")
//            java.time.OffsetDateTime::class -> JdbcOffsetDateTimeDataType("TEXT")
//            java.util.UUID::class -> JdbcUuidDataType("TEXT")
//            else -> {
//                // 枚举类型
//                val clazz = type.classifier as? Class<*>
//                if (clazz?.isEnum == true) JdbcEnumDataType("TEXT", clazz as Class<Enum<*>>)
//                else null
//            }
//        } as DataType?
//    }
//}