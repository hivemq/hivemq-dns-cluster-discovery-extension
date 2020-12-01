package com.hivemq.extensions.dns.metrics;

import com.codahale.metrics.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hivemq.extensions.dns.metrics.DnsDiscoveryMetrics.DNS_DISCOVERY_EXTENSION;
import static com.hivemq.extensions.dns.metrics.DnsDiscoveryMetrics.HIVEMQ_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DnsDiscoveryMetricsTest {

    private @NotNull MetricRegistry metricRegistry;
    private @NotNull DnsDiscoveryMetrics metrics;

    @BeforeEach
    public void before() {
        metricRegistry = new MetricRegistry();
        metrics = new DnsDiscoveryMetrics(metricRegistry);
    }

    @Test
    public void test_resolutionRequestCounter() {
        final Counter counter = metrics.getResolutionRequestCounter();
        counter.inc();
        String name = HIVEMQ_PREFIX + "." +  DNS_DISCOVERY_EXTENSION + "." +  "query.success.count";
        final Counter counterFromRegistry = metricRegistry.counter(name);
        assertEquals(counter.getCount(), counterFromRegistry.getCount());
    }

    @Test
    public void test_resolutionRequestCounterFailed() {
        final Counter counter = metrics.getResolutionRequestFailedCounter();
        counter.inc();
        String name = HIVEMQ_PREFIX + "." +  DNS_DISCOVERY_EXTENSION + "." + "query.failed.count";
        final Counter counterFromRegistry = metricRegistry.counter(name);
        assertEquals(counter.getCount(), counterFromRegistry.getCount());
    }


    @Test
    public void test_subscribed_message_duration_histogram() {
        final List<Integer> addresses = new ArrayList<>(List.of(1));
        final AtomicInteger addressesCount = new AtomicInteger(0);

        addressesCount.set(addresses.size());

        metrics.registerAddressCountGauge(addressesCount::get);

        String name = HIVEMQ_PREFIX + "." +  DNS_DISCOVERY_EXTENSION + "." + "resolved-addresses";
        final Gauge<Integer> gauge = metricRegistry.getGauges().get(name);

        assertEquals(addresses.size(), gauge.getValue());

        addresses.add(2);
        addresses.add(3);
        addressesCount.set(addresses.size());

        assertEquals(addresses.size(), gauge.getValue());
    }


}