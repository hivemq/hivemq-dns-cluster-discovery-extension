plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.license)
    alias(libs.plugins.asciidoctor)
}

group = "com.hivemq.extensions"
description = "Cluster discovery extension using round-robin DNS A records"

hivemqExtension {
    name.set("DNS Cluster Discovery Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(10000)
    sdkVersion.set(libs.versions.hivemq.extensionSdk)

    resources {
        from("LICENSE")
        from("README.adoc") { rename { "README.txt" } }
        from("dns-discovery-diagram.png")
        from(tasks.asciidoctor)
    }
}

dependencies {
    implementation(libs.owner)
    implementation(libs.netty.resolver.dns)
    implementation(libs.commonsValidator)
}

tasks.asciidoctor {
    sourceDirProperty.set(layout.projectDirectory)
    sources("README.adoc")
    secondarySources { exclude("**") }
}

/* ******************** test ******************** */

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.junitJupiter)
    testRuntimeOnly(libs.logback.classic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

/* ******************** integration test ******************** */

dependencies {
    integrationTestCompileOnly(libs.jetbrains.annotations)
    integrationTestImplementation(platform(libs.testcontainers.bom))
    integrationTestImplementation(libs.testcontainers)
    integrationTestImplementation(libs.testcontainers.hivemq)
    integrationTestImplementation(libs.apacheDS.dns)
    integrationTestImplementation(libs.okhttp)
    integrationTestRuntimeOnly(libs.logback.classic)
}

/* ******************** checks ******************** */

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    exclude("com/hivemq/extensions/cluster/discovery/dns/TestDnsServer.java")
    exclude("hivemq-prometheus-extension/**/*")
}
