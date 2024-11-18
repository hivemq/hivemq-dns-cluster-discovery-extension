plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.oci)
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

oci {
    registries {
        dockerHub {
            optionalCredentials()
        }
    }
    imageMapping {
        mapModule("com.hivemq", "hivemq-enterprise") {
            toImage("hivemq/hivemq4")
        }
    }
    imageDefinitions {
        register("main") {
            allPlatforms {
                dependencies {
                    runtime("com.hivemq:hivemq-enterprise:latest") { isChanging = true }
                }
                layers {
                    layer("hivemqExtension") {
                        contents {
                            permissions("opt/hivemq/", 0b111_111_000)
                            permissions("opt/hivemq/extensions/", 0b111_111_000)
                            into("opt/hivemq/extensions") {
                                filePermissions = 0b110_100_000 // TODO remove, use default
                                directoryPermissions = 0b111_101_000 // TODO remove, use default
                                permissions("*/", 0b111_111_000)
                                permissions("*/dnsdiscovery.properties", 0b110_110_000)
                                permissions("*/hivemq-extension.xml", 0b110_110_000)
                                from(zipTree(tasks.hivemqExtensionZip.flatMap { it.archiveFile }))
                            }
                        }
                    }
                }
            }
        }
        register("integrationTest") {
            allPlatforms {
                dependencies {
                    runtime(project)
                    runtime("com.hivemq.extensions:hivemq-prometheus-extension")
                }
            }
        }
    }
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
                implementation(libs.gradleOci.junitJupiter)
                implementation(libs.apacheDS.dns)
                implementation(libs.okhttp)
                runtimeOnly(libs.logback.classic)
            }
            oci.of(this) {
                imageDependencies {
                    runtime(project) {
                        capabilities {
                            requireCapability("$group:$name-integration-test") // TODO requireFeature("integrationTest"), update gradle to 8.11
                        }
                    }.tag("latest")
                }
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
