import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin)
    //alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
}

group = "com.github.rwsbillyang"
version = libs.versions.ktorKitVersion.get()
//application {   mainClass = "io.ktor.server.cio.EngineMain" }

dependencies {
    //选用 compileOnly， application可重新选
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.serialization.kotlinx.json)
    compileOnly(ktorLibs.server.resources)
    compileOnly(ktorLibs.server.auth)
    compileOnly(ktorLibs.server.auth.jwt)
    compileOnly(ktorLibs.server.autoHeadResponse)
    compileOnly(ktorLibs.server.cachingHeaders)
    compileOnly(ktorLibs.server.callLogging)
    compileOnly(ktorLibs.server.cio)
    compileOnly(libs.ktor.server.conditional.headers)
    compileOnly(ktorLibs.server.cors)
    compileOnly(ktorLibs.server.defaultHeaders)
    compileOnly(ktorLibs.server.forwardedHeader)
    compileOnly(ktorLibs.server.partialContent)
    compileOnly(ktorLibs.server.statusPages)
    compileOnly(libs.ktor.server.websockets)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)

    //compileOnly(ktorLibs.server.requestValidation)
    //compileOnly(ktorLibs.server.compression)
    //compileOnly(ktorLibs.server.di)
    //compileOnly(ktorLibs.server.sse)


    // 测试套件依赖
    //testImplementation(libs.kotlin.test.junit)
   // testImplementation(ktorLibs.server.tests)
    //testImplementation(ktorLibs.server.test.host)

    // Ktor 客户端
    compileOnly(libs.ktor.client.core)
    compileOnly(libs.ktor.client.cio)
    compileOnly(libs.ktor.client.content.negotiation)
    compileOnly(libs.ktor.client.logging)
    compileOnly(libs.ktor.client.encoding)

    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.logging)

    // 日志
    implementation(libs.logback.classic)

    // 依赖注入 Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger)
    //testImplementation(libs.koin.test)


    // 缓存缓存
    implementation(libs.caffeine)
    testImplementation(libs.caffeine)
    //implementation(libs.ucasoft.ktorSimpleCache)
    //implementation(libs.ucasoft.ktorSimpleMemoryCache)


    // NoSQL 相关
    // compileOnly("org.litote.kmongo:kmongo-id:4.11.0")
    //  compileOnly("com.github.jershell:kbson:0.7.0")
    //  compileOnly("org.litote.kmongo:kmongo-coroutine-serialization:4.11.0")
    compileOnly(libs.mongodb.driver)
    compileOnly(libs.mongodb.bson)

    // SQL / 数据库相关
    //compileOnly(libs.mysql.connector.java)
    compileOnly(libs.hikariCP)


    //compileOnly(libs.komapper.template)
    //compileOnly(platform(libs.komapper.platform))
    platform(libs.komapper.platform).let {
        implementation(it)
        ksp(it)
    }
    ksp("org.komapper:komapper-processor")


    implementation(libs.komapper.starter.jdbc)
    compileOnly("org.komapper:komapper-jdbc")

    // 内置方言（按需引入）
    compileOnly(libs.komapper.dialect.mysql.jdbc)
    compileOnly(libs.mysql.connector.j)// JDBC 驱动

    //compileOnly(libs.komapper.dialect.h2.jdbc)
    compileOnly(libs.sqlite.jdbc)

    compileOnly(libs.komapper.dialect.postgresql.jdbc)
    compileOnly(libs.postgresql.jdbc)

    compileOnly(libs.komapper.dialect.oracle.jdbc)
    //compileOnly("org.komapper:komapper-dialect-oracle-jdbc:7.0.0")
    compileOnly("com.oracle.database.jdbc:ojdbc11:23.5.0.24.07")  // JDK 11+

    //testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    //testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")


    //implementation(libs.exposed.core)
    //implementation(libs.exposed.r2dbc)
    //implementation(libs.h2database.h2)
    //implementation(libs.h2database.r2dbc)


    // 公共工具包
    //compileOnly(libs.apache.commons.lang3)
    //implementation(libs.apache.commons.codec)


    // 邮件
    compileOnly("com.sun.mail:javax.mail:1.6.2")

}

kotlin {
    // 保持 KSP 生成的源文件路径映射
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

// Java 编译及兼容性配置（已修复版本冲突）
java {
    toolchain {
        // Toolchain 决定了编译时使用的 JDK 核心版本
        languageVersion.set(JavaLanguageVersion.of(21)) 
    }
    // 源码兼容性与目标字节码兼容性，必须小于或等于 Toolchain 的版本
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// 传统的非标准目录布局转换
sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        kotlin.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test"))
        kotlin.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("testresources"))
    }
}

// Maven 发布配置
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}