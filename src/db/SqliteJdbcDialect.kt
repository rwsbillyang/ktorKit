package com.github.rwsbillyang.ktorKit.db

import org.komapper.dialect.h2.jdbc.H2JdbcDialect

import java.sql.SQLException

/**
 * 自定义实现的 SQLite JDBC 方言
 */
class SqliteJdbcDialect : H2JdbcDialect {

    // 不要 override driver，保持父类的 "org.h2.Driver"
    // 这样 Komapper 就能找到对应的数据类型映射，解决 String 找不到的问题

    // SQLite 标识符用双引号（和 H2 默认一样，这行不覆写也行，但写了更明确）
    override fun enquote(name: String): String {
        return "\"$name\""
    }

    // 【可选】覆写唯一约束异常判断，SQLite 的错误码是 19
    override fun isUniqueConstraintViolationError(exception: SQLException): Boolean {
        // SQLite 唯一约束违反返回 errorCode = 19 (SQLITE_CONSTRAINT)
        return exception.errorCode == 19
    }

    // 其他语法 H2 和 SQLite 很相似，基本不需要改
}

//class CustomSqliteDialect : JdbcDialect {
//    // 手动注册 SQLite 兼容的类型映射
//    private val dataOperator = DefaultJdbcDataOperator(
//        "org.sqlite.JDBC",
//        listOf(
//            JdbcStringDataType("TEXT"),
//            JdbcIntDataType("INTEGER"),
//            JdbcLongDataType("INTEGER"),
//            JdbcBooleanDataType("INTEGER"),
//            JdbcDoubleDataType("REAL"),
//            // 按需添加 UUID、ByteArray 等
//        )
//    )
//
//    override val driver: String = "org.sqlite.JDBC"
//    override fun enquote(name: String) = "\"$name\""
//    override fun getDataOperator() = dataOperator
//    // ... 实现其他必要方法
//}