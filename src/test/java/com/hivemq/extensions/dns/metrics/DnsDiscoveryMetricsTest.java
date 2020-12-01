package com.hivemq.extensions.dns.metrics;

import com.codahale.metrics.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

        final Counter counterFromRegistry = metricRegistry.counter("com.hivemq.dns-cluster-discovery-extension.resolution-request-success.count");


        assertEquals(counter.getCount(), counterFromRegistry.getCount());
    }

    @Test
    public void test_resolutionRequestCounterFailed() {
        final Counter counter = metrics.getResolutionRequestFailedCounter();
        counter.inc();

        final Counter counterFromRegistry = metricRegistry.counter("com.hivemq.dns-cluster-discovery-extension.resolution-request-failed.count");


        assertEquals(counter.getCount(), counterFromRegistry.getCount());
    }


    @Test
    public void test_subscribed_message_duration_histogram() {
        final List<Integer> addresses = new ArrayList<>(List.of(1));
        final AtomicInteger addressesCount = new AtomicInteger(0);

        addressesCount.set(addresses.size());

        metrics.registerAddressCountGauge(addressesCount::get);

        final Gauge<Integer> gauge = metricRegistry.getGauges().get("com.hivemq.dns-cluster-discovery-extension.resolved-addresses.gauge");

        assertEquals(addresses.size(), gauge.getValue());

        addresses.add(2);
        addresses.add(3);
        addressesCount.set(addresses.size());

        assertEquals(addresses.size(), gauge.getValue());
    }


}