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

import com.codahale.metrics.MetricRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hivemq.extensions.cluster.discovery.dns.DnsDiscoveryMetrics.DNS_DISCOVERY_EXTENSION;
import static com.hivemq.extensions.cluster.discovery.dns.DnsDiscoveryMetrics.HIVEMQ_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

class DnsDiscoveryMetricsTest {

    private @NotNull MetricRegistry metricRegistry;
    private @NotNull DnsDiscoveryMetrics metrics;

    @BeforeEach
    void setUp() {
        metricRegistry = new MetricRegistry();
        metrics = new DnsDiscoveryMetrics(metricRegistry);
    }

    @AfterEach
    void tearDown() {
        metrics.stop();
    }

    @Test
    void test_resolutionRequestCounter() {
        final var counter = metrics.getQuerySuccessCount();
        counter.inc();
        final var name = MetricRegistry.name(HIVEMQ_PREFIX, DNS_DISCOVERY_EXTENSION, "query.success.count");
        final var counterFromRegistry = metricRegistry.counter(name);
        assertThat(counterFromRegistry.getCount()).isEqualTo(counter.getCount());
    }

    @Test
    void test_resolutionRequestCounterFailed() {
        final var counter = metrics.getQueryFailedCount();
        counter.inc();
        final var name = MetricRegistry.name(HIVEMQ_PREFIX, DNS_DISCOVERY_EXTENSION, "query.failed.count");
        final var counterFromRegistry = metricRegistry.counter(name);
        assertThat(counterFromRegistry.getCount()).isEqualTo(counter.getCount());
    }

    @Test
    void test_registerAddressCountGauge() {
        final var addresses = new ArrayList<>(List.of(1));
        final var addressesCount = new AtomicInteger(0);
        addressesCount.set(addresses.size());

        metrics.registerAddressCountGauge(addressesCount::get);

        final var name = MetricRegistry.name(HIVEMQ_PREFIX, DNS_DISCOVERY_EXTENSION, "resolved-addresses");
        final var gauge = metricRegistry.getGauges().get(name);
        assertThat(gauge.getValue()).isEqualTo(addresses.size());

        addresses.add(2);
        addresses.add(3);
        addressesCount.set(addresses.size());
        assertThat(gauge.getValue()).isEqualTo(addresses.size());
    }

    @Test
    void test_stop() {
        final var addressesCount = new AtomicInteger(0);
        metrics.registerAddressCountGauge(addressesCount::get);
        final var counters = metricRegistry.getCounters();
        assertThat(metricRegistry.getGauges()).isNotEmpty();

        metrics.stop();
        assertThat(metricRegistry.getCounters()).isEqualTo(counters);
        assertThat(metricRegistry.getGauges()).isEmpty();
    }
}
