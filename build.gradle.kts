plugins {
    id("com.hivemq.extension")
    id("com.github.hierynomus.license")
    id("io.github.sgtsilvio.gradle.defaults")
    id("org.asciidoctor.jvm.convert")
}

group = "com.hivemq.extensions"
description = "Cluster discovery extension using round-robin DNS A records"

hivemqExtension {
    name.set("DNS Cluster Discovery Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(10000)
    sdkVersion.set("${property("hivemq-extension-sdk.version")}")

    resources {
        from("LICENSE")
        from("README.adoc") { rename { "README.txt" } }
        from("dns-discovery-diagram.png")
        from(tasks.asciidoctor)
    }
}

dependencies {
    implementation("org.aeonbits.owner:owner:${property("owner.version")}")
    implementation("io.netty:netty-resolver-dns:${property("netty.version")}")
    implementation("commons-validator:commons-validator:${property("commons-validator.version")}")
}

tasks.asciidoctor {
    sourceDirProperty.set(layout.projectDirectory)
    sources("README.adoc")
    secondarySources { exclude("**") }
}

/* ******************** test ******************** */

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junit-jupiter.version")}")
    testImplementation("org.mockito:mockito-core:${property("mockito.version")}")
    testImplementation("org.mockito:mockito-junit-jupiter:${property("mockito.version")}")
    testRuntimeOnly("ch.qos.logback:logback-classic:${property("logback.version")}")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

/* ******************** integration test ******************** */

dependencies {
    integrationTestCompileOnly("org.jetbrains:annotations:${property("jetbrains-annotations.version")}")
    integrationTestImplementation(platform("org.testcontainers:testcontainers-bom:${property("testcontainers.version")}"))
    integrationTestImplementation("org.testcontainers:testcontainers")
    integrationTestImplementation("org.testcontainers:hivemq")
    integrationTestImplementation("org.apache.directory.server:apacheds-protocol-dns:${property("apache-dns.version")}")
    integrationTestImplementation("com.squareup.okhttp3:okhttp:${property("ok-http.version")}")
    integrationTestRuntimeOnly("ch.qos.logback:logback-classic:${property("logback.version")}")
}

/* ******************** checks ******************** */

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    exclude("com/hivemq/extensions/cluster/discovery/dns/TestDnsServer.java")
    exclude("hivemq-prometheus-extension/**/*")
}
