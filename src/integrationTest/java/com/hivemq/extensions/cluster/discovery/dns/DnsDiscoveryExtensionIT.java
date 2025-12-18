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
package com.hivemq.extensions.cluster.discovery.dns;

import io.github.sgtsilvio.gradle.oci.junit.jupiter.OciImages;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Lukas Brand
 */
class DnsDiscoveryExtensionIT {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DnsDiscoveryExtensionIT.class);

    private static final @NotNull String DNS_DISCOVERY_ADDRESS = "tasks.hivemq";

    private static final @NotNull String SUCCESS_METRIC =
            "com_hivemq_dns_cluster_discovery_extension_query_success_count";
    private static final @NotNull String FAILURE_METRIC =
            "com_hivemq_dns_cluster_discovery_extension_query_failed_count";
    private static final @NotNull String IP_COUNT_METRIC =
            "com_hivemq_dns_cluster_discovery_extension_resolved_addresses";

    private @NotNull TestDnsServer testDnsServer;
    private @NotNull HiveMQContainer node;

    @BeforeEach
    void setUp() throws Exception {
        testDnsServer = new TestDnsServer(Set.of(DNS_DISCOVERY_ADDRESS), 4);
        testDnsServer.start();

        final var config = """
                dnsServerAddress=host.docker.internal:%d
                discoveryAddress=%s
                resolutionTimeout=30
                reloadInterval=60
                """.formatted(testDnsServer.localAddress().getPort(), DNS_DISCOVERY_ADDRESS);

        node = new HiveMQContainer(OciImages.getImageName("hivemq/extensions/hivemq-dns-cluster-discovery")
                .asCompatibleSubstituteFor("hivemq/hivemq4"))
                .withHiveMQConfig(MountableFile.forClasspathResource("config.xml"))
                .withCopyToContainer(Transferable.of(config),
                        "/opt/hivemq/extensions/hivemq-dns-cluster-discovery/dnsdiscovery.properties")
                .withExposedPorts(9399)
                .withExtraHost("host.docker.internal", "host-gateway")
                .withEnv("HIVEMQ_DISABLE_STATISTICS", "true");
    }

    @AfterEach
    void tearDown() {
        node.stop();
        testDnsServer.stop();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void test_metric_success() throws Exception {
        node.start();

        final var metrics = getMetrics();
        assertThat(metrics.get(SUCCESS_METRIC)).isEqualTo(1);
        assertThat(metrics.get(FAILURE_METRIC)).isEqualTo(0);
        assertThat(metrics.get(IP_COUNT_METRIC)).isEqualTo(4);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void test_metric_failure() throws Exception {
        testDnsServer.stop();
        node.start();

        final var metrics = getMetrics();
        assertThat(metrics.get(SUCCESS_METRIC)).isEqualTo(0);
        assertThat(metrics.get(FAILURE_METRIC)).isEqualTo(1);
        assertThat(metrics.get(IP_COUNT_METRIC)).isEqualTo(0);
    }

    private @NotNull Map<String, Float> getMetrics() throws Exception {
        try (final var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
            //noinspection HttpUrlsUsage
            final var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + node.getHost() + ":" + node.getMappedPort(9399) + "/metrics"))
                    .build();
            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return parseMetrics(response.body(), Set.of(SUCCESS_METRIC, FAILURE_METRIC, IP_COUNT_METRIC));
        }
    }

    private @NotNull Map<String, Float> parseMetrics(
            final @NotNull String metricsDump,
            final @NotNull Set<String> metrics) {
        return metricsDump.lines()
                .filter(s -> !s.startsWith("#"))
                .map(s -> s.split(" ", 2))
                .filter(splits -> splits.length == 2 && metrics.contains(splits[0]))
                .peek(strings -> log.info(Arrays.toString(strings)))
                .map(splits -> Map.entry(splits[0], Float.parseFloat(splits[1])))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Float::max));
    }
}
