///*
// * Copyright © 2022 rwsbillyang@qq.com
// *
// * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-29 15:33
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.github.rwsbillyang.ktorKit.db
//
//import com.github.rwsbillyang.ktorKit.apiBox.IUmiPaginationParams
//import com.github.rwsbillyang.ktorKit.cache.ICache
//import org.bson.conversions.Bson
//import org.koin.core.component.inject
//import org.koin.core.qualifier.named
//
//open class MongoCRUDService(cache: ICache, dbName: String) : MongoGenericService(cache) {
//    val dbSource: MongoDataSource by inject(qualifier = named(dbName))
//
//    inline fun <reified T : Any> col(colName: String) = dbSource.mongoDb.getCollection<T>(colName)
//
//    /**
//     * find a document，use cache if cacheKey is not null
//     * @param colName CoroutineCollection name
//     * @param id _id of document
//     * @param cacheKey cache key，cache it if not null
//     * @return the inserted/updated record
//     * */
//    inline fun <reified T> findOne(colName: String, id: String, toObejectId: Boolean = true, cacheKey: String? = null): T?
//    = findOne(col(colName),id, toObejectId, cacheKey)
//
//
//    inline fun <reified T> findAll(colName: String, filter: Bson): List<T>  = findAll(col(colName),filter)
//
//    inline fun <reified T> findPage(colName: String, params: IUmiPaginationParams): List<T> = findPage(col(colName), params)
//
//
//    /**
//     * save(insert or update depends on isInsert) a record，use cache if cacheKey is not null
//     * @param colName CoroutineCollection name
//     * @param doc entity data
//     * @param cacheKey cache key，evict if not null
//     * @param updateCache if true, update cache, else evict cache，default false
//     * @return the inserted/updated record
//     * */
//    inline fun <reified T : Any> save(colName: String, doc: T, cacheKey: String? = null, updateCache: Boolean = false): T
//    = save(col(colName), doc, cacheKey, updateCache)
//
//
//    /**
//     * update values of some fields，evict cache if cacheKey or cacheKeys is not null
//     * @param col CoroutineCollection
//     * @param filter query Bson
//     * @param update update Bson
//     * @param cacheKey cache key，evict if not null
//     * @param cacheKeys cache keys，evict if not null
//     * @param w WhereDeclaration
//     * @return affected rows count
//     * */
//    inline fun <reified T: Any> updateValues(colName: String, filter: Bson, update: Bson, cacheKey: String? = null,
//                              cacheKeys: List<String>? = null,) = updateValues(col(colName),filter,update, cacheKey,cacheKeys)
//
//
//    /**
//     * delete record(s)，evict cache if cacheKey or cacheKeys is not null
//     * @param col CoroutineCollection
//     * @param cacheKey cache key，evict if not null
//     * @param cacheKey cache key，evict if not null
//     * @return affected rows count
//     * */
//    inline fun <reified T> deleteOne(
//        colName: String,
//        id: String, toObejectId: Boolean = true,
//        cacheKey: String? = null,
//    ) = deleteOne(col(colName), id, toObejectId, cacheKey)
//
//
//    /**
//     * delete record(s)，evict cache if cacheKey or cacheKeys is not null
//     * @param col CoroutineCollection
//     * @param ids _id list of documents
//     * @param cacheKeyPrefix cache key prefix，evict if not null, cacheKey: "cacheKeyPrefix/id"
//     * @return affected rows count
//     * */
//    inline fun <reified T> deleteMulti(
//        colName: String,
//        ids: List<String>, toObejectId: Boolean = true,
//        cacheKeyPrefix: String? = null,
//    ) = deleteMulti(col(colName), ids, toObejectId, cacheKeyPrefix)
//
//}
