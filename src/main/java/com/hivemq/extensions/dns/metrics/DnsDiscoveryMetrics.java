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
package com.hivemq.extensions.dns.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Lukas Brand
 */
public class DnsDiscoveryMetrics {

    static final @NotNull String DNS_DISCOVERY_EXTENSION = "dns-cluster-discovery-extension";
    static final @NotNull String HIVEMQ_PREFIX = "com.hivemq";

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
