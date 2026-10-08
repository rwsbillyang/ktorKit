package com.github.rwsbillyang.ktorKit.db

import java.sql.DriverManager
import org.komapper.jdbc.JdbcDatabase
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.metamodel.EntityMetamodel


/**
 * @param DbConfig.host           PostgreSQL 主机host
 * @param DbConfig.port           端口（默认 5432）
 * @param DbConfig.dbName      业务数据库名
 * @param DbConfig.userName        业务连接账号
 * @param DbConfig.pwd    业务连接密码
 * @param adminUser      有建库权限的管理员账号（通常连 postgres 库）
 * @param adminPassword  管理员密码
 * @param entities       实体 metamodel 列表
 * @param forceRecreate  true = 先 DROP 旧库再 CREATE（危险！仅开发/测试用），false = CREATE DATABASE IF NOT EXISTS
 * */
data class PostgresqlConfig(
    override val config: DbConfig,
    override val entities: List<EntityMetamodel<*, *, *>>,
    override val forceRecreate: Boolean = false,

    val adminUser: String,
    val adminPassword: String
) : InitDbConfig

/**
 * PostgreSQL 初始化：建库（可选强制重建）+ 按实体建表
 *
 */
fun initPostgresql( config: PostgresqlConfig): Boolean {
    val host = config.config.host
    val port = config.config.port
    val database = config.config.dbName
    val appUser = config.config.userName?:""
    val appPassword = config.config.pwd?:""

    val adminUser = config.adminUser
    val adminPassword = config.adminPassword
    val entities = config.entities
    val forceRecreate = config.forceRecreate


    // PostgreSQL 管理员连接通常连到默认维护库 "postgres"
    val adminUrl = "jdbc:postgresql://$host:$port/postgres?user=$adminUser"
    val bizUrl = "jdbc:postgresql://$host:$port/$database"

    // 1. 管理员连接：建库 / 删库重建
    // ⚠️ PostgreSQL 的 CREATE/DROP DATABASE 不能在事务中执行，必须 autoCommit=true
    DriverManager.getConnection(adminUrl, adminUser, adminPassword).use { conn ->
        conn.autoCommit = true

        if (forceRecreate) {
            // 查 OID
            val dbOid = conn.prepareStatement(
                "SELECT oid FROM pg_database WHERE datname = ?"
            ).use { ps ->
                ps.setString(1, database)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong("oid") else null
                }
            }

            // ⚠️ 危险操作：如果有人正连着目标库，DROP DATABASE 会失败
            // 先断开所有连接到目标库的会话

            // 踢连接
            if (dbOid != null) {
                conn.prepareStatement("""
                SELECT pg_terminate_backend(pid)
                FROM pg_stat_activity
                WHERE datid = ? AND pid <> pg_backend_pid()
            """).use { ps ->
                    ps.setLong(1, dbOid)
                    ps.execute()
                }
            }

            // 删库
            conn.createStatement().execute("DROP DATABASE IF EXISTS \"$database\"")
            println("[PostgreSQL]  delete old db: $database done")
        }

        // 建库
        conn.createStatement().execute("""
        CREATE DATABASE "$database"
        WITH ENCODING 'UTF8'
        LC_COLLATE 'C'
        LC_CTYPE 'C'
        TEMPLATE template0
    """.trimIndent())

        if (forceRecreate) {
            println("[PostgreSQL] create new db: $database done")
        } else {
            println("[PostgreSQL] db $database already exists")
        }
    }


    // 2. 业务连接：按实体建表
    val db = JdbcDatabase(bizUrl, appUser, appPassword)
    db.withTransaction {
        db.runQuery { QueryDsl.create(*entities.toTypedArray()) }
    }

    println("[PostgreSQL] create tables done, entities count: ${entities.size}")

    return true
}