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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;
import org.testcontainers.shaded.okhttp3.Response;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hivemq.extensions.dns.metrics.DnsDiscoveryMetrics.DNS_DISCOVERY_EXTENSION;
import static com.hivemq.extensions.dns.metrics.DnsDiscoveryMetrics.HIVEMQ_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Lukas Brand
 */
public class DnsDiscoveryExtensionIT {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DnsDiscoveryExtensionIT.class);

    private final @NotNull Network network = Network.newNetwork();

    private @NotNull TestDnsServer testDnsServer;
    private @NotNull HiveMQTestContainerExtension node1;

    @BeforeEach
    void setUp(final @NotNull @TempDir Path extensionTempPath) throws IOException {
        testDnsServer = new TestDnsServer(Set.of("tasks.hivemq"), 4);
        testDnsServer.start();

        FileUtils.copyDirectory(new File("build/hivemq-extension-test"), extensionTempPath.toFile());
        final Path extensionDir = extensionTempPath.resolve("hivemq-dns-cluster-discovery");

        final String dnsConfigPath = "src/integrationTest/resources/dnsdiscovery.properties";

        final String replacedConfig = Files.readString(Path.of(dnsConfigPath))
                .replace("dnsServerPlaceholder", "host.docker.internal" + ":" + testDnsServer.localAddress().getPort())
                .replace("discoveryPlaceholder", "tasks.hivemq");
        Files.writeString(extensionDir.resolve("dnsdiscovery.properties"), replacedConfig, StandardOpenOption.CREATE);

        node1 = new HiveMQTestContainerExtension(DockerImageName.parse("hivemq/hivemq4").withTag("latest"))
                .withHiveMQConfig(MountableFile.forClasspathResource("config.xml"))
                .withExtension(MountableFile.forHostPath(extensionDir))
                .withExtension(MountableFile.forClasspathResource("hivemq-prometheus-extension"))
                .withNetwork(network)
                //.withEnv("HIVEMQ_CLUSTER_TRANSPORT_TYPE", "TCP")
                .withNetworkAliases("node1")
                .withExposedPorts(9399)
                .waitingFor(
                        Wait.forLogMessage(".*Started HiveMQ in.*\\n", 1)
                );
    }


    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void test_metric_success() throws IOException, InterruptedException {
        node1.start();

        assertEquals(1, getMetricValue(MetricType.SUCCESS_METRIC));
        assertEquals(0, getMetricValue(MetricType.FAILURE_METRIC));
        assertEquals(4, getMetricValue(MetricType.IP_COUNT_METRIC));


    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void test_metric_failure() throws IOException {
        testDnsServer.stop();
        node1.start();

        assertEquals(0, getMetricValue(MetricType.SUCCESS_METRIC));
        assertEquals(1, getMetricValue(MetricType.FAILURE_METRIC));
        assertEquals(0, getMetricValue(MetricType.IP_COUNT_METRIC));
    }


    private enum MetricType {
        SUCCESS_METRIC,
        FAILURE_METRIC,
        IP_COUNT_METRIC
    }

    private @NotNull Float getMetricValue(final @NotNull MetricType type) throws IOException {

        final OkHttpClient client = new OkHttpClient();
        final Request request1 = new Request.Builder()
                .url("http://" + node1.getHost() + ":" + node1.getMappedPort(9399) + "/metrics")
                .build();

        final Response response1 = client.newCall(request1).execute();
        assertNotNull(response1.body());
        final String string = response1.body().string();

        final Map<String, Float> metricsWithValues = parseMetrics(string,
                Set.of("com_hivemq_dns_cluster_discovery_extension_query_success_count",
                        "com_hivemq_dns_cluster_discovery_extension_query_failed_count",
                        "com_hivemq_dns_cluster_discovery_extension_resolved_addresses"));

        final String successMetricName = HIVEMQ_PREFIX + "." +  DNS_DISCOVERY_EXTENSION + "." +  "query.success.count";
        final String failedMetricName = HIVEMQ_PREFIX + "." +  DNS_DISCOVERY_EXTENSION + "." + "query.failed.count";



        if (type == MetricType.SUCCESS_METRIC) {
            return metricsWithValues.get("com_hivemq_dns_cluster_discovery_extension_query_success_count");
        } else if (type == MetricType.FAILURE_METRIC) {
            return metricsWithValues.get("com_hivemq_dns_cluster_discovery_extension_query_failed_count");
        } else if (type == MetricType.IP_COUNT_METRIC) {
            return metricsWithValues.get("com_hivemq_dns_cluster_discovery_extension_resolved_addresses");
        } else {
            return Float.NaN;
        }

    }

    private @NotNull Map<String, Float> parseMetrics(
            final @NotNull String metricsDump, final @NotNull Set<String> metrics) {

        return metricsDump.lines()
                .filter(s -> !s.startsWith("#"))
                .map(s -> s.split(" "))
                .filter(splits -> metrics.contains(splits[0]))
                .peek(strings -> log.info(Arrays.toString(strings)))
                .map(splits -> Map.entry(splits[0], Float.parseFloat(splits[1])))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Float::max));
    }
}
