pluginManagement {
    repositories {
        mavenLocal()

        maven { url = uri("https://mirrors.cloud.tencent.com/gradle/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter/") }
        maven { url = uri("https://jitpack.io") }    

        mavenCentral()
        gradlePluginPortal()
        google()    
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()

        //maven { url = uri("https://mirrors.cloud.tencent.com/gradle/") }
        //maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        //maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public/") }
        //maven { url = uri("https://maven.aliyun.com/repository/google/") }
        //maven { url = uri("https://maven.aliyun.com/repository/jcenter/") }
        maven { url = uri("https://jitpack.io") }

        mavenCentral()
        gradlePluginPortal()
        google()
    }

    // 配置远程 Version Catalog
    versionCatalogs {
        create("libs").from(files("../../libs.versions.toml"))
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.6.0")
    }
}

rootProject.name = "ktorKit"