plugins {
    id("com.hivemq.extension")
    id("com.github.hierynomus.license-report")
    id("org.owasp.dependencycheck")
    id("pmd")
    id("com.github.spotbugs")
    id("com.github.sgtsilvio.gradle.utf8")
    id("org.asciidoctor.jvm.convert")
}


/* ******************** metadata ******************** */

group = "com.hivemq.extensions"
description = "Cluster discovery extension using round-robin DNS A records"

hivemqExtension {
    name = "HiveMQ DNS Cluster Discovery Extension"
    author = "HiveMQ"
    priority = 1000
    startPriority = 10000
    mainClass = "com.hivemq.extensions.dns.DnsDiscoveryExtensionMain"
    sdkVersion = "$version"
}


/* ******************** dependencies ******************** */

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.aeonbits.owner:owner:${property("owner.version")}")
    implementation("ch.qos.logback:logback-classic:${property("logback-classic.version")}")
    implementation("io.netty:netty-resolver-dns:${property("netty-resolver-dns.version")}")
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

tasks.hivemqExtensionResources {
    from("README.adoc") { rename { "README.txt" } }
    from(tasks.asciidoctor)
    exclude("**/.gitkeep")
}


/* ******************** test ******************** */

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit-jupiter.version")}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${property("junit-jupiter.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit-jupiter.version")}")

    testImplementation("org.mockito:mockito-core:${property("mockito.version")}")
    testImplementation("org.mockito:mockito-junit-jupiter:${property("mockito.version")}")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events("STARTED", "FAILED", "SKIPPED")
    }
    ignoreFailures = System.getenv().containsKey("CI_RUN")

    val inclusions = rootDir.resolve("inclusions.txt")
    val exclusions = rootDir.resolve("exclusions.txt")
    if (inclusions.exists()) {
        include(inclusions.readLines())
    } else if (exclusions.exists()) {
        exclude(exclusions.readLines())
    }
}


/* ******************** integration test ******************** */

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

configurations {
    getByName("integrationTestImplementation").extendsFrom(testImplementation.get())
    getByName("integrationTestRuntimeOnly").extendsFrom(testRuntimeOnly.get())
}

dependencies {
    "integrationTestImplementation"("org.apache.directory.server:apacheds-protocol-dns:${property("apache-dns.version")}")
    "integrationTestImplementation"("org.testcontainers:junit-jupiter:${property("testcontainers.version")}")
    "integrationTestImplementation"("com.hivemq:hivemq-testcontainer-junit5:${property("hivemq-testcontainer.version")}")
    "integrationTestImplementation"("net.lingala.zip4j:zip4j:${property("zip4j.version")}")
}

val prepareExtensionTest by tasks.registering(Sync::class) {
    group = "hivemq extension"
    description = "Prepares the extension for integration testing."

    from(tasks.hivemqExtensionZip.map { zipTree(it.archiveFile) })
    into(buildDir.resolve("hivemq-extension-test"))
}

val integrationTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs integration tests."

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter(tasks.test)
    dependsOn(prepareExtensionTest)
}

tasks.check { dependsOn(integrationTest) }


/* ******************** static code analysis ******************** */

pmd {
    toolVersion = "${property("pmd.version")}"
    sourceSets = listOf(project.sourceSets.main.get())
    isIgnoreFailures = true //TODO
    rulePriority = 3
}

spotbugs {
    toolVersion.set("${property("spotbugs.version")}")
    ignoreFailures.set(true) //TODO
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
}


/* ******************** license ******************** */

downloadLicenses {
    //TODO This needs a proper setup
    dependencyConfiguration = "runtimeClasspath"
}


/* ******************** run ******************** */

val unzipHivemq by tasks.registering(Sync::class) {
    from(zipTree(rootDir.resolve("/Users/lbrand/Desktop/hivemq-4.4.0.zip")))
    into({ temporaryDir })
}

tasks.prepareHivemqHome {
    unzipHivemq.map { it.destinationDir.resolve("hivemq-4.4.0") }
}

tasks.runHivemqWithExtension {
    debugOptions {
        enabled.set(true)
    }
}