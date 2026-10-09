package com.github.rwsbillyang.ktorKit.db

import org.komapper.core.dsl.metamodel.EntityMetamodel
import java.io.File

sealed interface InitDbConfig {
    val config: DbConfig
    val entities: List<EntityMetamodel<*, *, *>>
}


/**
 * 若是新建数据库返回true，已存在则返回false
 * */
fun initDb(config: InitDbConfig, forceRecreate: Boolean, markerFile: String): Boolean {
    val file = File(markerFile)
    if(forceRecreate){
        println("[DB Init] forceRecreate, try to delete markerFile")
        if (file.exists()) file.delete()
    }else{
        // 如果传了标志文件且已存在，直接跳过
        if (file.exists()) {
            println("[DB Init] markerFile exists，skip initializeDatabase")
            return false
        }
    }

    when (config) {
        is SqliteConfig -> initSqlite(config, forceRecreate)
        is MysqlConfig -> initMysql(config, forceRecreate)
        is PostgresqlConfig -> initPostgresql(config, forceRecreate)
        is OracleConfig -> initOracle(config, forceRecreate)
    }

    // 创建标志文件
    file.parentFile?.mkdirs()
    file.createNewFile()
    println("[DB Init] initializeDatabase done, and markerFile($markerFile) created!")

    return true
}