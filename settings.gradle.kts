rootProject.name = "hivemq-dns-cluster-discovery"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        id("com.hivemq.extension") version "${extra["plugin.hivemq-extension.version"]}"
        id("com.github.hierynomus.license-report") version "${extra["plugin.license.version"]}"
        id("org.owasp.dependencycheck") version "${extra["plugin.dependencycheck.version"]}"
        id("com.github.spotbugs") version "${extra["plugin.spotbugs.version"]}"
        id("com.github.sgtsilvio.gradle.utf8") version "${extra["plugin.utf8.version"]}"
        id("org.asciidoctor.jvm.convert") version "${extra["plugin.asciidoctor.version"]}"
    }
}

val isCiRun = System.getenv().containsKey("CI_RUN")
buildCache {
    local {
        isEnabled = !isCiRun
    }
    remote(HttpBuildCache::class.java) {
        isEnabled = isCiRun
        isPush = isCiRun
        url = uri("http://jenkins-hmq.office.dc2:8085/cache/")
    }
}