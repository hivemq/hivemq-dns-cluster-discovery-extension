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
package com.hivemq.extensions.cluster.discovery.dns.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * Helper class for environment variables. This is necessary to properly unit test configuration values.
 *
 * @author Lukas Brand
 */
class DnsDiscoveryConfigEnvironment {

    static final @NotNull String DNS_SERVER_ADDRESS = "HIVEMQ_DNS_SERVER_ADDRESS";
    static final @NotNull String DISCOVERY_ADDRESS_ENV = "HIVEMQ_DNS_DISCOVERY_ADDRESS";
    static final @NotNull String DISCOVERY_TIMEOUT_ENV = "HIVEMQ_DNS_DISCOVERY_TIMEOUT";
    static final @NotNull String DISCOVERY_RELOAD_INTERVAL_ENV = "HIVEMQ_DNS_RELOAD_INTERVAL";

    @Nullable String getEnvDnsServerAddress() {
        return System.getenv(DNS_SERVER_ADDRESS);
    }

    @Nullable String getEnvDiscoveryAddress() {
        return System.getenv(DISCOVERY_ADDRESS_ENV);
    }

    @Nullable String getEnvResolutionTimeout() {
        return System.getenv(DISCOVERY_TIMEOUT_ENV);
    }

    @Nullable String getEnvReloadInterval() {
        return System.getenv(DISCOVERY_RELOAD_INTERVAL_ENV);
    }
}
