/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-07-24 21:49
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

import com.zaxxer.hikari.HikariDataSource
import java.util.*
import javax.sql.DataSource

//import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
//import org.apache.shardingsphere.example.config.ExampleConfiguration;
//import org.apache.shardingsphere.example.core.api.DataSourceUtil;
//import org.apache.shardingsphere.infra.config.RuleConfiguration;
//import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
//import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
//import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
//import org.apache.shardingsphere.sharding.api.config.strategy.keygen.KeyGenerateStrategyConfiguration;
//import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;

class SqlSharding {



    //    private static ShardingType shardingType = ShardingType.SHARDING_TABLES;
//    private static ShardingType shardingType = ShardingType.SHARDING_DATABASES_AND_TABLES;
    fun configDataSource() {
        val dataSourceMap: MutableMap<String, DataSource> = HashMap<String, DataSource>()

        // 配置第 1 个数据源
        val dataSource1 = HikariDataSource().apply {
            driverClassName = "com.mysql.jdbc.Driver"
            jdbcUrl = "jdbc:mysql://localhost:3306/ds_1"
            username = "root"
            password = ""
        }

        dataSourceMap["ds_1"] = dataSource1


        // 配置第 2 个数据源
        val dataSource2 = HikariDataSource().apply {
            driverClassName = "com.mysql.jdbc.Driver"
            jdbcUrl = "jdbc:mysql://localhost:3306/ds_2"
            username = "root"
            password = ""
        }
        dataSourceMap["ds_2"] = dataSource2
    }

    //https://github.com/apache/shardingsphere/blob/master/examples/shardingsphere-jdbc-example/single-feature-example/sharding-example/sharding-raw-jdbc-example/src/main/java/org/apache/shardingsphere/example/sharding/raw/jdbc/config/ShardingDatabasesConfigurationRange.java
//    @Throws(SQLException::class)
//    fun getDataSource(): DataSource? {
//        val shardingRuleConfig: ShardingRuleConfiguration = createShardingRuleConfiguration()
//        val configs: MutableCollection<RuleConfiguration> = LinkedList()
//        configs.add(shardingRuleConfig)
//        return ShardingSphereDataSourceFactory.createDataSource(createDataSourceMap(), configs, Properties())
//    }
//
//    private fun createShardingRuleConfiguration(): ShardingRuleConfiguration {
//        val result = ShardingRuleConfiguration()
//        result.getTables().add(getOrderTableRuleConfiguration())
//        result.getTables().add(getOrderItemTableRuleConfiguration())
//        result.getBroadcastTables().add("t_address")
//        result.setDefaultDatabaseShardingStrategy(StandardShardingStrategyConfiguration("user_id", "standard_test_db"))
//        result.getShardingAlgorithms()
//            .put("standard_test_db", ShardingSphereAlgorithmConfiguration("STANDARD_TEST_DB", Properties()))
//        result.getKeyGenerators().put("snowflake", ShardingSphereAlgorithmConfiguration("SNOWFLAKE", Properties()))
//        return result
//    }
//
//    private fun getOrderTableRuleConfiguration(): ShardingTableRuleConfiguration? {
//        val result = ShardingTableRuleConfiguration("t_order")
//        result.setKeyGenerateStrategy(KeyGenerateStrategyConfiguration("order_id", "snowflake"))
//        return result
//    }
//
//    private fun getOrderItemTableRuleConfiguration(): ShardingTableRuleConfiguration? {
//        val result = ShardingTableRuleConfiguration("t_order_item")
//        result.setKeyGenerateStrategy(KeyGenerateStrategyConfiguration("order_item_id", "snowflake"))
//        return result
//    }
}
