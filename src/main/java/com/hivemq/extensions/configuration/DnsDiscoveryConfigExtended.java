/*
 * Copyright 2018 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class that encapsulate dnsDiscoveryConfig to enable usage of environment Variables
 *
 * @author Anja Helmbrecht-Schaar
 */
public class DnsDiscoveryConfigExtended {

    private static final Logger log = LoggerFactory.getLogger(DnsDiscoveryConfigExtended.class);

    private static final String DISCOVERY_ADDRESS_ENV = "HIVEMQ_DNS_DISCOVERY_ADDRESS";
    private static final String DISCOVERY_TIMEOUT_ENV = "HIVEMQ_DNS_DISCOVERY_TIMEOUT";

    private final DnsDiscoveryConfig dnsDiscoveryConfig;

    public DnsDiscoveryConfigExtended(final @NotNull ConfigurationReader reader) {
        dnsDiscoveryConfig = reader.get();
    }

    /**
     * method to get discovery address
     * either from environment variable
     * or from properties configuration
     *
     * @return String - the discovery address
     */
    public String discoveryAddress() {
        final @Nullable String discoveryAddress = System.getenv(DISCOVERY_ADDRESS_ENV);

        if (discoveryAddress == null || discoveryAddress.isEmpty()) {
            try {
                return dnsDiscoveryConfig.discoveryAddress();
            } catch (Exception e) {
                log.error("No discovery address was set in the configuration file or environment variable");
                return null;
            }
        }
        return discoveryAddress;
    }

    /**
     * method to get discovery timeout
     * either from environment variable
     * or from properties configuration
     * or default setting
     *
     * @return int - the resolution timeout
     */
    public int resolutionTimeout() {
        final @Nullable String resolveTimeout = System.getenv(DISCOVERY_TIMEOUT_ENV);

        if (resolveTimeout != null && !resolveTimeout.isEmpty()) {
            try {
                return Integer.parseInt(resolveTimeout);
            } catch (NumberFormatException e) {
                log.error("Resolution timeout from env {} couldn't be parsed to int. Fallback to config value.", DISCOVERY_TIMEOUT_ENV);
            }
        }
        return dnsDiscoveryConfig.resolutionTimeout();
    }
}
