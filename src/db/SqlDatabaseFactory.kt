package com.github.rwsbillyang.ktorKit.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.komapper.jdbc.JdbcDatabase
import org.komapper.jdbc.JdbcDialect
import org.komapper.dialect.mysql.jdbc.MySqlJdbcDialect
import org.komapper.dialect.postgresql.jdbc.PostgreSqlJdbcDialect
import org.komapper.dialect.oracle.jdbc.OracleJdbcDialect



object SqlDatabaseFactory {
    lateinit var db: JdbcDatabase

    fun init(dbConfig: DbConfig) {
        val dbType = dbConfig.dbType
        val dbName = dbConfig.dbName
        val host = dbConfig.host
        val port = dbConfig.port
        val userName = dbConfig.userName
        val pwd = dbConfig.pwd

        val dialect: JdbcDialect

        val config = HikariConfig().apply {

            optimizedHikariConfig()

            this.username = userName?:"root"
            //this.password = pwd
            if(pwd != null) password = pwd
            this.maximumPoolSize = 10

            when (dbType) {
                DatabaseType.SQL_SQLITE -> {
                    driverClassName = "org.sqlite.JDBC"
                    maximumPoolSize = 1
                    dialect = SqliteJdbcDialect()
                    // 可以是绝对路径，也可以是相对于运行路径的相对路径（如 ./data.db）
                    jdbcUrl = "jdbc:sqlite:${dbName}"
                }
                DatabaseType.SQL_MYSQL -> {
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                    val dbPort = if (port == 0) 3306 else port
                    dialect = MySqlJdbcDialect()
                    jdbcUrl = "jdbc:mysql://$host:$dbPort/$dbName?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai"
                }
                DatabaseType.SQL_POSTGRL, DatabaseType.SQL_KINGBASE -> {
                    driverClassName = "org.postgresql.Driver"
                    val dbPort = if (port == 0) 54321 else port
                    dialect = PostgreSqlJdbcDialect()
                    // 人大金仓默认端口通常为 54321，内核兼容 PG
                    jdbcUrl = "jdbc:postgresql://$host:$dbPort/$dbName"
                }
                DatabaseType.SQL_ORACLE, DatabaseType.SQL_DAMENG -> {
                    driverClassName = "dm.jdbc.driver.DmDriver"
                    val dbPort = if (port == 0) 5236 else port

                    dialect = OracleJdbcDialect()
                    // 达梦默认端口通常为 5236
                    // 强烈建议加上 oracleLike=true 参数，让达梦在语法兼容性上更贴合标准方言
                    jdbcUrl = "jdbc:dm://$host:$dbPort/$dbName?oracleLike=true"
                }
                DatabaseType.NOSQL ->{
                    TODO("Should not come here: should not use SqlDatabaseFactory to create NOSQL db")
                }
            }
        }

        val dataSource = HikariDataSource(config)

        // 创建 Komapper 数据库实例
        db = JdbcDatabase(dataSource = dataSource, dialect = dialect)

    }
}