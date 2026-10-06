/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-07-24 21:38
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.ktorKit.db

/**
 * KINGBASE = POSTGRE
 * DAMENG = ORACLE
 * */
enum class DatabaseType {
    NOSQL,
    SQL_MYSQL, SQL_POSTGRL, SQL_KINGBASE, SQL_ORACLE, SQL_DAMENG, SQL_SQLITE
}

/**
 * @param dbName 数据库名称(or sqlite file name) 确保名称唯一，否则依赖注入时可能识别错误
 * @param dbType DbType.NOSQL, SQL_MYSQL, SQL_POSTGRL, SQL_KINGBASE, SQL_ORACLE, SQL_DAMENG, SQL_SQLITE
 * @param host 数据库host 默认127.0.0.1
 * @param port 数据库port 0: 对于NOSQL MongoDB，默认27017， SQL之MySQL为3306
 * @param userName 连接数据的用户名，mysql通常需要赋值
 * @param pwd 连接数据的密码，mysql通常需要赋值
 * */
class DbConfig(
    val dbName: String,
    val dbType: DatabaseType,
    val host: String = "127.0.0.1",
    val port: Int = 0,
    val userName: String? = null,
    val pwd: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false
        return if (other is DbConfig) {
            other.dbName == dbName && other.dbType == dbType && other.host == host && other.port == port
        } else
            false
    }

    override fun hashCode(): Int {
        var result = dbName.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + port
        return result
    }
}
