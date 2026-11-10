/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-07-28 14:09
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
 * Twitter_Snowflake<br></br>
 *

 *
 * SnowFlake的结构如下(每部分用-分开):<br></br>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <br></br>
 * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0<br></br>
 * 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
 * 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69<br></br>
 * 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId<br></br>
 * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号<br></br>
 * 加起来刚好64位，为一个Long型。<br></br>
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
 */
object SnowflakeId {
    /** 工作机器ID(0~31)  */
    private var _workerId: Long = 0L

    /** 数据中心ID(0~31)  */
    private var _datacenterId: Long = 0L

    /**
     * @param workerId 工作ID (0~31)
     * @param dataCenterId 数据中心ID (0~31)
     * */
    fun initialize(workerId: Long, dataCenterId: Long = 0L){
        require(!(workerId > maxWorkerId || workerId < 0)) {
            "worker Id can't be greater than $maxWorkerId or less than 0"
        }
        require(!(dataCenterId > maxDatacenterId || dataCenterId < 0)) {
            "datacenter Id can't be greater than $maxDatacenterId or less than 0"
        }
        _workerId = workerId
        _datacenterId = dataCenterId
    }



    /** 开始时间截 (2015-01-01)  */
    //private val epoch = 1420041600000L

    //2026-11-11 00:00:00（北京时间）
    private val epoch = 1794326400000L

    /** 机器id所占的位数  */
    private val workerIdBits = 5L

    /** 数据标识id所占的位数  */
    private val datacenterIdBits = 5L

    /** 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)  */
    private val maxWorkerId = -1L xor (-1L shl workerIdBits.toInt())

    /** 支持的最大数据标识id，结果是31  */
    private val maxDatacenterId = -1L xor (-1L shl datacenterIdBits.toInt())

    /** 序列在id中占的位数  */
    private val sequenceBits = 12L

    /** 机器ID向左移12位  */
    private val workerIdShift = sequenceBits

    /** 数据标识id向左移17位(12+5)  */
    private val datacenterIdShift = sequenceBits + workerIdBits

    /** 时间截向左移22位(5+5+12)  */
    private val timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits

    /** 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)  */
    private val sequenceMask = -1L xor (-1L shl sequenceBits.toInt())



    /** 毫秒内序列(0~4095)  */
    private var sequence = 0L

    /** 上次生成ID的时间截  */
    private var lastTimestamp = -1L
    // ==============================Methods==========================================
    /**
     * 获得一个ID (该方法是线程安全的)
     * @return SnowflakeId
     */
    @Synchronized
    fun getId(): Long {
        var timestamp = System.currentTimeMillis()

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw RuntimeException(
                "Clock moved backwards.  Refusing to generate id for ${lastTimestamp - timestamp} milliseconds"
            )
        }

        //如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = sequence + 1 and sequenceMask
            //毫秒内序列溢出
            if (sequence == 0L) {
                //阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp)
            }
        } else {
            sequence = 0L
        }

        //上次生成ID的时间截
        lastTimestamp = timestamp

        //移位并通过或运算拼到一起组成64位的ID
        return (timestamp - epoch shl timestampLeftShift.toInt() //
                or (_datacenterId shl datacenterIdShift.toInt()) //
                or (_workerId shl workerIdShift.toInt()) //
                or sequence)
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp = System.currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }
}

//fun main(args: Array<String>) {
//    for (i in 0..9) {
//        val id = SnowflakeId.nextId()
//        println(java.lang.Long.toBinaryString(id))
//        println(id)
//    }
//}