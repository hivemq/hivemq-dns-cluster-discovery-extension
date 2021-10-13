import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("com.hivemq.extension")
    id("com.github.hierynomus.license")
    id("com.github.sgtsilvio.gradle.utf8")
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
}

dependencies {
    implementation("org.aeonbits.owner:owner:${property("owner.version")}")
    implementation("io.netty:netty-resolver-dns:${property("netty.version")}")
    implementation("commons-validator:commons-validator:${property("commons-validator.version")}")
}

/* ******************** resources ******************** */

val prepareAsciidoc by tasks.registering(Sync::class) {
    from("README.adoc").into({ temporaryDir })
}

tasks.asciidoctor {
    dependsOn(prepareAsciidoc)
    sourceDir(prepareAsciidoc.map { it.destinationDir })
}

hivemqExtension.resources {
    from("LICENSE")
    from("README.adoc") { rename { "README.txt" } }
    from("dns-discovery-diagram.png")
    from(tasks.asciidoctor)
}

/* ******************** test ******************** */

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit-jupiter.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito:mockito-core:${property("mockito.version")}")
    testImplementation("org.awaitility:awaitility:${property("awaitility.version")}")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events = setOf(TestLogEvent.STARTED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }

    val outputCache = mutableListOf<String>()
    addTestOutputListener { _, outputEvent ->
        outputCache.add(outputEvent.message)
        while (outputCache.size > 1000) {
            outputCache.removeAt(0)
        }
    }
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {}
        override fun beforeTest(testDescriptor: TestDescriptor) = outputCache.clear()
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            if (result.resultType == TestResult.ResultType.FAILURE && outputCache.size > 0) {
                println()
                println(" Output of ${testDescriptor.className}.${testDescriptor.name}:")
                outputCache.forEach { print(" > $it") }
            }
        }
    })
}

/* ******************** integration test ******************** */

dependencies {
    integrationTestImplementation("org.testcontainers:testcontainers:${property("testcontainers.version")}")
    integrationTestImplementation("com.hivemq:hivemq-testcontainer-junit5:${property("hivemq-testcontainer.version")}")
    integrationTestImplementation("org.apache.directory.server:apacheds-protocol-dns:${property("apache-dns.version")}")
}

/* ******************** checks ******************** */

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}