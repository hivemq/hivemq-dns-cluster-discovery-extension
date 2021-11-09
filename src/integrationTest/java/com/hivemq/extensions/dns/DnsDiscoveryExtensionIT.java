/*
 * Copyright 2018-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.dns;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.testcontainer.junit5.HiveMQTestContainerExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;
import org.testcontainers.shaded.okhttp3.Response;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Lukas Brand
 */
class DnsDiscoveryExtensionIT {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DnsDiscoveryExtensionIT.class);

    private static final @NotNull String SUCCESS_METRIC = "com_hivemq_dns_cluster_discovery_extension_query_success_count";
    private static final @NotNull String FAILURE_METRIC = "com_hivemq_dns_cluster_discovery_extension_query_failed_count";
    private static final @NotNull String IP_COUNT_METRIC = "com_hivemq_dns_cluster_discovery_extension_resolved_addresses";

    private @NotNull TestDnsServer testDnsServer;
    private @NotNull HiveMQTestContainerExtension node1;

    @BeforeEach
    void setUp(final @NotNull @TempDir Path extensionTempPath) throws IOException {
        testDnsServer = new TestDnsServer(Set.of("tasks.hivemq"), 4);
        testDnsServer.start();

        final Path dnsConfigFile = extensionTempPath.resolve("dnsdiscovery.properties");
        final String replacedConfig = Files.readString(Path.of(getClass().getResource("/dnsdiscovery.properties").getPath()))
                .replace("dnsServerPlaceholder", "host.docker.internal:" + testDnsServer.localAddress().getPort())
                .replace("discoveryPlaceholder", "tasks.hivemq");
        Files.writeString(dnsConfigFile, replacedConfig, StandardOpenOption.CREATE);

        node1 = new HiveMQTestContainerExtension(DockerImageName.parse("hivemq/hivemq4").withTag("latest"))
                .withHiveMQConfig(MountableFile.forClasspathResource("config.xml"))
                .withExtension(MountableFile.forClasspathResource("hivemq-dns-cluster-discovery"))
                .withFileInExtensionHomeFolder(MountableFile.forHostPath(dnsConfigFile), "hivemq-dns-cluster-discovery")
                .withExtension(MountableFile.forClasspathResource("hivemq-prometheus-extension"))
                .withExposedPorts(9399)
                .withCreateContainerCmdModifier(createContainerCmd -> Objects.requireNonNull(createContainerCmd.getHostConfig()).withExtraHosts("host.docker.internal:host-gateway"));
    }

    @AfterEach
    void tearDown() {
        node1.stop();
        testDnsServer.stop();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void test_metric_success() throws IOException {
        node1.start();

        final Map<String, Float> metrics = getMetrics();
        assertEquals(1, metrics.get(SUCCESS_METRIC));
        assertEquals(0, metrics.get(FAILURE_METRIC));
        assertEquals(4, metrics.get(IP_COUNT_METRIC));
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void test_metric_failure() throws IOException {
        testDnsServer.stop();
        node1.start();

        final Map<String, Float> metrics = getMetrics();
        assertEquals(0, metrics.get(SUCCESS_METRIC));
        assertEquals(1, metrics.get(FAILURE_METRIC));
        assertEquals(0, metrics.get(IP_COUNT_METRIC));
    }

    private @NotNull Map<String, Float> getMetrics() throws IOException {

        final OkHttpClient client = new OkHttpClient();
        final Request request1 = new Request.Builder()
                .url("http://" + node1.getHost() + ":" + node1.getMappedPort(9399) + "/metrics")
                .build();

        final Response response1 = client.newCall(request1).execute();
        assertNotNull(response1.body());
        final String string = response1.body().string();

        return parseMetrics(string, Set.of(SUCCESS_METRIC, FAILURE_METRIC, IP_COUNT_METRIC));
    }

    private @NotNull Map<String, Float> parseMetrics(
            final @NotNull String metricsDump, final @NotNull Set<String> metrics) {

        return metricsDump.lines()
                .filter(s -> !s.startsWith("#"))
                .map(s -> s.split(" "))
                .filter(splits -> metrics.contains(splits[0]))
                .peek(strings -> log.info(Arrays.toString(strings)))
                .map(splits -> Map.entry(splits[0], Float.parseFloat(splits[1])))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Float::max));
    }
}