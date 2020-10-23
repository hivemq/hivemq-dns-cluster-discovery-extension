plugins {
    id("com.hivemq.extension")
    id("com.github.hierynomus.license-report")
    id("org.owasp.dependencycheck")
    id("pmd")
    id("com.github.spotbugs")
    id("com.github.sgtsilvio.gradle.utf8")
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


/* ******************** test ******************** */

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit-jupiter.version")}")
    //testImplementation("org.junit.jupiter:junit-jupiter-params:${property("junit-jupiter.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit-jupiter.version")}")

    //testImplementation("io.github.glytching:junit-extensions:${property("junit-extensions.version")}")
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
    hivemqFolder.set("/Users/lbrand/Desktop/hivemq-4.4.0")//unzipHivemq.map { it.destinationDir.resolve("hivemq-4.4.0") } as Any)
}

tasks.runHivemqWithExtension {
    debugOptions {
        enabled.set(true)
    }
}