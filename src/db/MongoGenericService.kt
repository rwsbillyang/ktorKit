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
//import com.github.rwsbillyang.ktorKit.cache.CacheService
//import com.github.rwsbillyang.ktorKit.cache.ICache
//import com.github.rwsbillyang.ktorKit.toObjectId
//import kotlinx.coroutines.runBlocking
////import org.bson.conversions.Bson
////import org.litote.kmongo.and
////import org.litote.kmongo.bson
////import org.litote.kmongo.coroutine.CoroutineCollection
//
//open class MongoGenericService(cache: ICache) : CacheService(cache) {
//
//    /**
//     * find a document，use cache if cacheKey is not null
//     * @param col CoroutineCollection
//     * @param id _id of document
//     * @param cacheKey cache key，cache it if not null
//     * @return the inserted/updated record
//     * */
//    inline fun <reified T: Any> findOne(col: CoroutineCollection<T>, id: String, toObejectId: Boolean = true, cacheKey: String? = null) = runBlocking{
//        val _id = if(toObejectId) id.toObjectId() else id
//        col.findOneById(_id)?.also { if (cacheKey != null) cache.put(cacheKey, it) }
//    }
//
//
//    inline fun <reified T: Any> findAll(col: CoroutineCollection<T>, filter: Bson) = runBlocking{
//        col.find(filter).toList()
//    }
//
//    inline fun <reified T: Any> findPage(col: CoroutineCollection<T>, params: IUmiPaginationParams) = runBlocking{
//        val pagination = params.pagination
//        val sort = pagination.sortJson.bson
//        val filter = params.toFilter()
//        if(pagination.lastId == null)
//            col.find(filter).skip((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize).sort(sort).toList()
//        else{
//            col.find(and(pagination.lastIdFilter(),filter)).limit(pagination.pageSize).sort(sort).toList()
//        }
//    }
//
//
//
//    /**
//     * save(insert or update depends on isInsert) a record，use cache if cacheKey is not null
//     * @param col CoroutineCollection
//     * @param doc entity data
//     * @param cacheKey cache key，evict if not null
//     * @param updateCache if true, update cache, else evict cache，default false
//     * @return the inserted/updated record
//     * */
//    inline fun <reified T: Any> save(col: CoroutineCollection<T>, doc: T, cacheKey: String? = null, updateCache: Boolean = false) = runBlocking{
//        col.save(doc)
//        if (cacheKey != null){
//            if (updateCache) cache.put(cacheKey, doc) else cache.evict(cacheKey)
//        }
//        doc
//    }
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
//    inline fun <reified T: Any> updateValues(col: CoroutineCollection<T>, filter: Bson, update: Bson, cacheKey: String? = null,
//                              cacheKeys: List<String>? = null,) = runBlocking{
//        val count = col.updateMany(filter, update).modifiedCount
//        if (cacheKey != null) cache.evict(cacheKey) else if (!cacheKeys.isNullOrEmpty()) cacheKeys.forEach {
//            cache.evict(it)
//        }
//        count
//    }
//
//
//    /**
//     * delete record(s)，evict cache if cacheKey or cacheKeys is not null
//     * @param col CoroutineCollection
//     * @param cacheKey cache key，evict if not null
//     * @param cacheKey cache key，evict if not null
//     * @return affected rows count
//     * */
//    inline fun <reified T: Any> deleteOne(
//        col: CoroutineCollection<T>,
//        id: String, toObejectId: Boolean = true,
//        cacheKey: String? = null,
//    ) = runBlocking {
//        val _id = if(toObejectId) id.toObjectId() else id
//        val count = col.deleteOneById(_id).deletedCount
//        if (cacheKey != null) cache.evict(cacheKey)
//        count
//    }
//
//
//    /**
//     * delete record(s)，evict cache if cacheKey or cacheKeys is not null
//     * @param col CoroutineCollection
//     * @param ids _id list of documents
//     * @param cacheKeyPrefix cache key prefix，evict if not null, cacheKey: "cacheKeyPrefix/id"
//     * @return affected rows count
//     * */
//    inline fun <reified T: Any> deleteMulti(
//        col: CoroutineCollection<T>,
//        ids: List<String>, toObejectId: Boolean = true,
//        cacheKeyPrefix: String? = null,
//    ) = runBlocking {
//        val jsonIds = if(toObejectId)
//            ids.joinToString(",", "[", "]") { "ObjectId(\"${it.toObjectId().toHexString()}" }
//        else
//            ids.joinToString(",", "[", "]")
//
//        val count = col.deleteMany("{ _id: { \$in: $jsonIds } }").deletedCount //{ field: { $in: [<value1>, <value2>, ... <valueN> ] } }
//
//        if (cacheKeyPrefix != null) {
//            ids.map{ "$cacheKeyPrefix/$it"}.forEach { cache.evict(it) }
//        }
//
//        count
//    }
//}
