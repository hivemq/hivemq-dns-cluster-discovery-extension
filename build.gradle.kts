plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.license)
}

group = "com.hivemq.extensions"
description = "Cluster discovery extension using round-robin DNS A records"

hivemqExtension {
    name = "DNS Cluster Discovery Extension"
    author = "HiveMQ"
    priority = 1000
    startPriority = 10000
    sdkVersion = libs.versions.hivemq.extensionSdk

    resources {
        from("LICENSE")
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    implementation(libs.owner)
    implementation(libs.netty.resolver.dns)
    implementation(libs.commonsValidator)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(libs.versions.junit.jupiter)
        }
        "test"(JvmTestSuite::class) {
            dependencies {
                compileOnly(libs.jetbrains.annotations)
                implementation(libs.mockito)
                implementation(libs.mockito.junitJupiter)
                runtimeOnly(libs.logback.classic)
            }
        }
        "integrationTest"(JvmTestSuite::class) {
            dependencies {
                compileOnly(libs.jetbrains.annotations)
                implementation(libs.testcontainers)
                implementation(libs.testcontainers.hivemq)
                implementation(libs.apacheDS.dns)
                implementation(libs.okhttp)
                runtimeOnly(libs.logback.classic)
            }
        }
    }
}

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    exclude("com/hivemq/extensions/cluster/discovery/dns/TestDnsServer.java")
    exclude("hivemq-prometheus-extension/**/*")
}
