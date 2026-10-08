package com.github.rwsbillyang.ktorKit.db

import org.komapper.core.dsl.metamodel.EntityMetamodel
import java.io.File

sealed interface InitDbConfig {
    val config: DbConfig
    val entities: List<EntityMetamodel<*, *, *>>
    val forceRecreate: Boolean
}






fun initDb(config: InitDbConfig, markerFile: String) {
    // 如果传了标志文件且已存在，直接跳过
    val file = File(markerFile)
    if (file.exists()) {
        println("[DB Init] markerFile exists，skip initializeDatabase")
        return
    }

    when (config) {
        is SqliteConfig -> initSqlite(config)
        is MysqlConfig -> initMysql(config)
        is PostgresqlConfig -> initPostgresql(config)
        is OracleConfig -> initOracle(config)
    }

    // 创建标志文件
    file.parentFile?.mkdirs()
    file.createNewFile()
    println("[DB Init] initializeDatabase done, and markerFile($markerFile) created!")
}