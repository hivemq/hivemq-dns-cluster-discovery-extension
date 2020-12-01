package com.hivemq.extensions.dns.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;


/**
 * @author Lukas Brand
 */
public class DnsDiscoveryMetrics {

    public final static @NotNull String DNS_DISCOVERY_EXTENSION = "dns-cluster-discovery-extension";
    public final static @NotNull String HIVEMQ_PREFIX = "com.hivemq";

    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull Counter connectCounter;
    private final @NotNull Counter failedConnectCounter;

    public DnsDiscoveryMetrics(final @NotNull MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.connectCounter = metricRegistry.counter(MetricRegistry.name(HIVEMQ_PREFIX, DNS_DISCOVERY_EXTENSION, "query.success.count"));
        this.failedConnectCounter = metricRegistry.counter(MetricRegistry.name(HIVEMQ_PREFIX, DNS_DISCOVERY_EXTENSION, "query.failed.count"));
    }

    public @NotNull Counter getResolutionRequestCounter() {
        return connectCounter;
    }

    public @NotNull Counter getResolutionRequestFailedCounter() {
        return failedConnectCounter;
    }


    public void registerAddressCountGauge(final @NotNull Gauge<Integer> supplier) {
        metricRegistry.gauge(
                MetricRegistry.name(HIVEMQ_PREFIX, DNS_DISCOVERY_EXTENSION, "resolved-addresses"),
                () -> supplier);
    }


}
