package com.hivemq.extensions.dns.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * Helper class for environment variables. This is necessary to properly unit test configuration values.
 *
 * @author Lukas Brand
 */
public class DnsDiscoveryConfigEnvironment {

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
