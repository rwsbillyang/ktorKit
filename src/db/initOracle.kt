package com.github.rwsbillyang.ktorKit.db

import java.sql.DriverManager
import org.komapper.jdbc.JdbcDatabase
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.metamodel.EntityMetamodel


/**
 * @param host           Oracle 主机
 * @param port           端口（默认 1521）
 * @param serviceName    服务名（如 ORCLPDB1、XEPDB1 等）
 * @param sysUser        SYS 或 SYSTEM 管理员账号
 * @param sysPassword    管理员密码
 * @param appUser        业务用户（Schema）名
 * @param appPassword    业务用户密码
 * @param entities       实体 metamodel 列表
 * @param forceRecreate  true = DROP USER CASCADE + 重建；false = 用户存在则跳过
 *
 * */
/**
 * @param DbConfig.host           Oracle 主机host
 * @param DbConfig.port           端口（默认 5432）
 * @param DbConfig.dbName      业务数据库名
 * @param DbConfig.userName        业务连接账号
 * @param DbConfig.pwd    业务连接密码
 * @param serviceName
 * @param sysPassword  密码
 * @param sysUser
 * @param entities       实体 metamodel 列表
 * @param forceRecreate  true = 先 DROP 旧库再 CREATE（危险！仅开发/测试用），false = CREATE DATABASE IF NOT EXISTS
 * */
data class OracleConfig(
    override val config: DbConfig,
    override val entities: List<EntityMetamodel<*, *, *>>,
    override val forceRecreate: Boolean = false,

    val serviceName: String,
    val sysUser: String = "SYS",
    val sysPassword: String
) : InitDbConfig
/**
 * Oracle 初始化：创建用户/Schema（可选强制重建）+ 按实体建表
 *
 * ⚠️ Oracle 的"数据库"是实例级别的，应用层只管用户/Schema。
 *    forceRecreate = true 会 DROP USER CASCADE（删除用户及所有对象），数据全部丢失！
 *
 */
fun initOracle(config: OracleConfig) {
    val host = config.config.host
    val port = config.config.port
    val database = config.config.dbName
    val appUser = config.config.userName?:""
    val appPassword = config.config.pwd?:""

    val serviceName = config.serviceName
    val sysPassword = config.sysPassword
    val sysUser = config.sysUser

    val entities = config.entities
    val forceRecreate = config.forceRecreate

    val adminUrl = "jdbc:oracle:thin:@//$host:$port/$serviceName"
    val bizUrl = "jdbc:oracle:thin:@//$host:$port/$serviceName"

    // 1. 管理员连接：创建/删除用户
    DriverManager.getConnection(adminUrl, sysUser, sysPassword).use { conn ->
        conn.autoCommit = true

        if (forceRecreate) {
            // ⚠️ DROP USER CASCADE 会删除该用户下所有表、视图、序列等
            conn.createStatement().execute("""
                BEGIN
                    EXECUTE IMMEDIATE 'DROP USER $appUser CASCADE';
                EXCEPTION
                    WHEN OTHERS THEN
                        IF SQLCODE != -1918 THEN -- ORA-01918: user does not exist
                            RAISE;
                        END IF;
                END;
            """.trimIndent())
            println("[Oracle] 已删除旧用户(含所有对象): $appUser")
        }

        // 创建用户（已存在则跳过）
        conn.createStatement().execute("""
            BEGIN
                EXECUTE IMMEDIATE 'CREATE USER $appUser IDENTIFIED BY $appPassword DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP';
            EXCEPTION
                WHEN OTHERS THEN
                    IF SQLCODE != -1920 THEN -- ORA-01920: user name conflicts
                        RAISE;
                    END IF;
            END;
        """.trimIndent())

        // 授权
        conn.createStatement().execute("GRANT CONNECT, RESOURCE, CREATE TABLE, CREATE SEQUENCE TO $appUser")

        if (forceRecreate) {
            println("[Oracle] 已强制重建用户: $appUser")
        } else {
            println("[Oracle] 用户 $appUser 已就绪")
        }
    }

    // 2. 业务连接（用应用用户）：按实体建表
    val db = JdbcDatabase(bizUrl, appUser, appPassword)
    db.withTransaction {
        db.runQuery { QueryDsl.create(*entities.toTypedArray()) }
    }

    println("[Oracle] 建表完成，实体数量: ${entities.size}")
}