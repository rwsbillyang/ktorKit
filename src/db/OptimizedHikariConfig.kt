/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-07-28 16:38
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

import com.zaxxer.hikari.HikariConfig



/**
 * 官方推荐的优化配置
 * 参见：https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
 * */
fun optimizedHikariConfig()
= HikariConfig().apply {
    //driverClassName = "com.mysql.cj.jdbc.Driver"
    //jdbcUrl = "jdbc:mysql://$host:$port/$dbName?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai"
    //username = userName
    //if(pwd != null) password = pwd

    addDataSourceProperty("cachePrepStmts", "true")
    addDataSourceProperty("prepStmtCacheSize", "250")
    addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    addDataSourceProperty("useServerPrepStmts", "true")
    addDataSourceProperty("useLocalSessionState", "true")
    addDataSourceProperty("rewriteBatchedStatements", "true")
    addDataSourceProperty("cacheResultSetMetadata", "true")
    addDataSourceProperty("cacheServerConfiguration", "true")
    addDataSourceProperty("elideSetAutoCommits", "true")
    addDataSourceProperty("maintainTimeStats", "false")
}


