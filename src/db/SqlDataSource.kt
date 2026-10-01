/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-27 15:07
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

import org.koin.core.component.KoinComponent
import org.komapper.jdbc.JdbcDatabase


class SqlDataSource(dbType: DatabaseType, dbName: String, userName: String? = null, pwd: String? = null, host: String ="localhost", port: Int = 0): KoinComponent {
    init {
        SqlDatabaseFactory.init(dbType, dbName, userName, pwd, host, port)
    }
    val db: JdbcDatabase = SqlDatabaseFactory.db
}