package com.github.rwsbillyang.ktorKit.db

import java.io.File
import org.komapper.jdbc.JdbcDatabase
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.metamodel.EntityMetamodel


/**
 * @param DbConfig.dbName      数据库文件路径，如 ./data/app.db
 * @param entities       实体 metamodel 列表 val entities = listOf(Meta.user, Meta.order)
 * @param forceRecreate  true = 先 DROP 旧库再 CREATE（危险！仅开发/测试用），false = CREATE DATABASE IF NOT EXISTS
 * */
data class SqliteConfig(
    override val config: DbConfig,
    override val entities: List<EntityMetamodel<*, *, *>>
) : InitDbConfig

/**
 * SQLite 初始化：建文件 + 按实体建表
 *
 */
fun initSqlite(config: SqliteConfig, forceRecreate: Boolean) : Boolean{
    val dbPath = config.config.dbName
    val entities = config.entities
    //val forceRecreate = config.forceRecreate

    val file = File(dbPath)

    // 强行重建：删除旧文件
    if (forceRecreate) {
        if (file.exists()) {
            // 确保没有残留连接，尝试删除
            val deleted = file.delete()
            if (!deleted) {
                throw IllegalStateException("fail to delete old db file : $dbPath，maybe because used by other process")
            }
            println("[SQLite] successful to delete old db file: $dbPath")
        }
        // 确保父目录存在
        file.parentFile?.mkdirs()
        println("[SQLite] create new db file: $dbPath")
    } else {
        // 非强行模式：文件已存在就正常走，JDBC 连上即可
        file.parentFile?.mkdirs()
    }

    // JDBC URL，连接即建文件
    val url = "jdbc:sqlite:$dbPath"
    val db = JdbcDatabase(url, dialect =  SqliteJdbcDialect() )

    // 执行建表（QueryDsl.create 生成 CREATE TABLE IF NOT EXISTS）
    db.withTransaction {
        db.runQuery { QueryDsl.create(*entities.toTypedArray()) }
    }

    println("[SQLite] create tables done, entities count: ${entities.size}")
    return true
}



