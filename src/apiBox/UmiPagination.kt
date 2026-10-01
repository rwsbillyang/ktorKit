package com.github.rwsbillyang.ktorKit.apiBox

import com.github.rwsbillyang.ktorKit.db.SqlPagination
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bson.conversions.Bson
import java.net.URLDecoder




/**
 * 1 for acs
 * -1 for desc
 * */
object Sort {
    const val ASC = 1
    const val DESC = -1
}


/**
 * the type of sort key, receives from the client(front end),
 * includes: TypeNumber, TypeString, TypeObjectId
 * */
enum class SortKeyType{
 TypeNumber, TypeString, TypeObjectId
}

/**
 * front end show list, encode search parameters and pagination info: &umi=encodeURIComponent(pagination_and_sort_parameters:UmiPagination)
 * sever side get umi value,  using pagination to decode it and get UmiPagination info
 * */
interface IUmiPaginationParams{
    val umi: String?
    val pagination: UmiPagination
        get() = umi?.let { Json.decodeFromString(URLDecoder.decode(it,"UTF-8")) }?:UmiPagination()

    /**
     * convert search params to Bson(not include UmiPagination.lastIdFilter) for mongodb/Kmongo
     * */
//    fun toFilter(): Bson {
//        TODO("Not Implement")
//    }

    /**
     * convert search params to SqlPagination for mysql/komapper
     * */
    fun toSqlPagination(): SqlPagination{
        TODO("Not Implement")
    }
}

/**
 * pagination info, sort info, and filter key info
 * @param pageSize default 20
 * @param current starts from 1, not 0
 * @param sKey sort key.  mongodb example: "sorter":{"updatedAt":"ascend"} , the sort key is "updatedAt"
 * @param sort 1 for asc，-1 for desc, same as MongoDB
 * @param sKeyType the type of sKey
 * @param lastId the last value of sort key in current page when load more
 * @param fKey filter key
 * @param filters  items which contains values of filters, "filter":{"someKey":["value1",123,"value3"]}
 *
 * using var instead of val，aims to modify them for permission
 * */
@Serializable
class UmiPagination(
     var pageSize: Int = 10,
     var current: Int = 1,
     var sKey: String = "_id", //sortKey
     var sort: Int = Sort.DESC, //1用于升序，而-1用于降序
     val sKeyType: SortKeyType = SortKeyType.TypeObjectId,
     val lastId: String? = null,
     var fKey: String? = null, //filter key
     var filters: List<String>? = null
){
    /**
     * for mongodb/kmongo sort
     * */
    val sortJson = "{'${sKey}':${sort}}"

    /**
     * setup mongodb bson for pagination
     * new version: ignore the parameter. legacy: pass lastId in listSearchParams,
     * new version, lastId is in UmiPagination, legacy version it's in listSearchParams
     * @return mongodb bson
     * */
//    fun lastIdFilter(): Bson? {
//        if(lastId == null) return null
//        val s = if(sort == Sort.DESC) "\$lt" else "\$gt"
//        return when(sKeyType){
//            SortKeyType.TypeNumber -> "{ '${sKey}': { $s: $lastId } }"
//            SortKeyType.TypeString -> "{ '${sKey}': { $s: `$lastId` } }"
//            SortKeyType.TypeObjectId -> "{ '${sKey}': { $s: ObjectId(\"${lastId.toObjectId().toHexString()}\") } }"
//        }.bson
//    }
}




