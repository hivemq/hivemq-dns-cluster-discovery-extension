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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Lukas Brand
 */
class DnsDiscoveryMetrics {

    static final @NotNull String DNS_DISCOVERY_EXTENSION = "dns-cluster-discovery-extension";
    static final @NotNull String HIVEMQ_PREFIX = "com.hivemq";

    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull Counter querySuccessCount;
    private final @NotNull Counter queryFailedCount;

    DnsDiscoveryMetrics(final @NotNull MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        querySuccessCount = metricRegistry.counter(MetricRegistry.name(HIVEMQ_PREFIX,
                DNS_DISCOVERY_EXTENSION,
                "query.success.count"));
        queryFailedCount = metricRegistry.counter(MetricRegistry.name(HIVEMQ_PREFIX,
                DNS_DISCOVERY_EXTENSION,
                "query.failed.count"));
    }

    @NotNull Counter getQuerySuccessCount() {
        return querySuccessCount;
    }

    @NotNull Counter getQueryFailedCount() {
        return queryFailedCount;
    }

    void registerAddressCountGauge(final @NotNull Gauge<Integer> supplier) {
        metricRegistry.gauge(MetricRegistry.name(HIVEMQ_PREFIX, DNS_DISCOVERY_EXTENSION, "resolved-addresses"),
                () -> supplier);
    }
}
