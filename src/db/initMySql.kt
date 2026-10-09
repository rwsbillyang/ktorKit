package com.github.rwsbillyang.ktorKit.db


import java.sql.DriverManager
import org.komapper.jdbc.JdbcDatabase
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.metamodel.EntityMetamodel

/**
 * @param DbConfig.host           MySQL 主机host
 * @param DbConfig.port           端口（默认 3306）
 * @param DbConfig.dbName      业务数据库名
 * @param DbConfig.userName        业务连接账号
 * @param DbConfig.pwd    业务连接密码
 * @param adminUser      有建库权限的管理员账号（通常连 postgres 库）
 * @param adminPassword  管理员密码
 * @param entities       实体 metamodel 列表
 * @param forceRecreate  true = 先 DROP 旧库再 CREATE（危险！仅开发/测试用），false = CREATE DATABASE IF NOT EXISTS
 * */
data class MysqlConfig(
    override val config: DbConfig,
    override val entities: List<EntityMetamodel<*, *, *>>,

    val adminUser: String,
    val adminPassword: String
) : InitDbConfig

/**
 * MySQL 初始化：建库（可选强制重建）+ 按实体建表
 *
 */
fun initMysql(config: MysqlConfig, forceRecreate: Boolean): Boolean {
    val host = config.config.dbName
    val port = config.config.port
    val database = config.config.dbName
    val appUser = config.config.userName
    val appPassword = config.config.pwd

    val adminUser = config.adminUser
    val adminPassword = config.adminPassword
    val entities = config.entities


    val adminUrl = "jdbc:mysql://$host:$port/?useSSL=false&serverTimezone=UTC&characterEncoding=utf8"
    val bizUrl = "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC&characterEncoding=utf8"

    // 1. 管理员连接：建库 / 删库重建
    DriverManager.getConnection(adminUrl, adminUser, adminPassword).use { conn ->
        if (forceRecreate) {
            // ⚠️ 危险操作：删除整个数据库，所有数据丢失
            conn.createStatement().execute("DROP DATABASE IF EXISTS `$database`")
            println("[MySQL] delete old db $database done!")
        }

        // 建库（无论是否 force，都要确保库存在）
        conn.createStatement().execute("""
            CREATE DATABASE IF NOT EXISTS `$database`
            CHARACTER SET utf8mb4
            COLLATE utf8mb4_0900_ai_ci
        """.trimIndent())

        if (forceRecreate) {
            println("[MySQL] create new db $database done!")
        } else {
            println("[MySQL] db $database already exists（skip）")
        }
    }

    // 2. 业务连接：按实体建表
    val db = JdbcDatabase(bizUrl, appUser?:"", appPassword?:"")
    db.withTransaction {
        db.runQuery { QueryDsl.create(*entities.toTypedArray()) }
    }

    println("[MySQL] create tables done, entities count: ${entities.size}")
    return true
}