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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Optional;

import static com.hivemq.extensions.cluster.discovery.dns.ExtensionConstants.EXTENSION_NAME;

/**
 * Configuration class that encapsulates the dnsDiscoveryConfig to enable usage of environment Variables.
 *
 * @author Anja Helmbrecht-Schaar
 * @author Lukas Brand
 */
public class DnsDiscoveryConfigExtended {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DnsDiscoveryConfigExtended.class);

    private final @NotNull DnsDiscoveryConfigFile configFile;
    private final @NotNull DnsDiscoveryConfigEnvironment configEnvironment;

    private @Nullable InetSocketAddress dnsServerAddress = null;
    private @Nullable String discoveryAddress = null;
    private int resolutionTimeout = 30;
    private int reloadInterval = 30;

    DnsDiscoveryConfigExtended(
            final @NotNull DnsDiscoveryConfigFile configFile,
            final @NotNull DnsDiscoveryConfigEnvironment configEnvironment) {
        this.configFile = configFile;
        this.configEnvironment = configEnvironment;
    }

    public static @NotNull DnsDiscoveryConfigExtended createInstance(final @NotNull DnsDiscoveryConfigFile configFile) {
        final var configEnvironment = new DnsDiscoveryConfigEnvironment();
        final var extendedConfig = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        extendedConfig.dnsServerAddress();
        extendedConfig.discoveryAddress();
        extendedConfig.resolutionTimeout();
        extendedConfig.reloadInterval();
        return extendedConfig;
    }

    void dnsServerAddress() {
        final var envDnsServerAddress = configEnvironment.getEnvDnsServerAddress();
        if (envDnsServerAddress != null && !envDnsServerAddress.isBlank()) {
            try {
                dnsServerAddress = processDnsServerAddress(envDnsServerAddress);
            } catch (final Exception e) {
                log.error("{}: Could not read the DNS server address from the environment variable.", EXTENSION_NAME);
                throw new ConfigurationException(e);
            }
        } else {
            try {
                final var propDnsServerAddress = configFile.getFileDnsServerAddress();
                if (propDnsServerAddress != null && !propDnsServerAddress.isBlank()) {
                    dnsServerAddress = processDnsServerAddress(propDnsServerAddress);
                } else {
                    log.debug("{}: No DNS server address was set in the configuration file or environment variable.",
                            EXTENSION_NAME);
                }
            } catch (final Exception e) {
                log.error("{}: Could not read the DNS server address from the properties file.", EXTENSION_NAME);
                throw new ConfigurationException(e);
            }
        }
    }

    @NotNull InetSocketAddress processDnsServerAddress(final @NotNull String dnsServerAddress) {
        if (dnsServerAddress.contains(":")) {
            final var address = dnsServerAddress.split(":")[0];
            int port;
            try {
                port = Integer.parseInt(dnsServerAddress.split(":")[1]);
            } catch (final NumberFormatException e) {
                log.error("{}: The DNS server address port could not be read. Taking default port 53.", EXTENSION_NAME);
                port = 53;
            }
            return new InetSocketAddress(address, port);
        } else {
            return new InetSocketAddress(dnsServerAddress, 53);
        }
    }

    void discoveryAddress() {
        final var envDiscoveryAddress = configEnvironment.getEnvDiscoveryAddress();
        if (envDiscoveryAddress != null && !envDiscoveryAddress.isEmpty()) {
            discoveryAddress = envDiscoveryAddress;
        } else {
            try {
                final var propDiscoveryAddress = configFile.getFileDiscoveryAddress();
                if (propDiscoveryAddress != null && !propDiscoveryAddress.isBlank()) {
                    discoveryAddress = propDiscoveryAddress;
                } else {
                    log.warn("{}: No discovery address was set in the configuration file or environment variable.",
                            EXTENSION_NAME);
                }
            } catch (final Exception e) {
                log.error("{}: Could not read the discovery address from the properties file.", EXTENSION_NAME);
                throw new ConfigurationException(e);
            }
        }
    }

    void resolutionTimeout() {
        final var envResolutionTimeout = configEnvironment.getEnvResolutionTimeout();
        if (envResolutionTimeout != null && !envResolutionTimeout.isEmpty()) {
            try {
                resolutionTimeout = Integer.parseInt(envResolutionTimeout);
                return;
            } catch (final NumberFormatException e) {
                log.error(
                        "{}: Resolution timeout from env {} could not be parsed to int. Fallback to configuration value 'resolutionTimeout'.",
                        EXTENSION_NAME,
                        DnsDiscoveryConfigEnvironment.DISCOVERY_TIMEOUT_ENV);
            }
        }
        try {
            final int propResolutionTimeout = configFile.getFileResolutionTimeout();
            if (propResolutionTimeout != -1) {
                resolutionTimeout = propResolutionTimeout;
            } else {
                log.debug(
                        "{}: No resolution timeout was set in the configuration file or environment variable. Defaulting to {}.",
                        EXTENSION_NAME,
                        resolutionTimeout);
            }
        } catch (final Exception e) {
            log.error("{}: Could not read the resolution timeout from the properties file.", EXTENSION_NAME);
            throw new ConfigurationException(e);
        }
    }

    void reloadInterval() {
        final var envReloadInterval = configEnvironment.getEnvReloadInterval();
        if (envReloadInterval != null && !envReloadInterval.isBlank()) {
            try {
                reloadInterval = Integer.parseInt(envReloadInterval);
                return;
            } catch (final NumberFormatException e) {
                log.error(
                        "{}: Reload interval from env {} could not be parsed to int. Fallback to configuration value 'reloadInterval'.",
                        EXTENSION_NAME,
                        DnsDiscoveryConfigEnvironment.DISCOVERY_RELOAD_INTERVAL_ENV);
            }
        }
        try {
            final var propReloadInterval = configFile.getFileReloadInterval();
            if (propReloadInterval != -1) {
                reloadInterval = propReloadInterval;
            } else {
                log.debug(
                        "{}: No reload interval was set in the configuration file or environment variable. Defaulting to {}.",
                        EXTENSION_NAME,
                        reloadInterval);
            }
        } catch (final Exception e) {
            log.error("{}: Could not read the reload interval from the properties file.", EXTENSION_NAME);
            throw new ConfigurationException(e);
        }
    }

    /**
     * Getter for the DNS server address. Its value is either from an environment
     * variable or a property configuration.
     *
     * @return String - the DNS server address
     */
    public @NotNull Optional<InetSocketAddress> getDnsServerAddress() {
        return Optional.ofNullable(dnsServerAddress);
    }

    /**
     * Getter for the discovery address. Its value is either from an environment
     * variable or a properties configuration.
     *
     * @return String - the discovery address
     */
    public @NotNull Optional<String> getDiscoveryAddress() {
        return Optional.ofNullable(discoveryAddress);
    }

    /**
     * Getter for the discovery resolution timeout. Its value is either from an environment
     * variable, a properties configuration or its default setting.
     *
     * @return int - the resolution timeout
     */
    public int getResolutionTimeout() {
        return resolutionTimeout;
    }

    /**
     * Getter for the discovery reload interval. Its value is either from an environment
     * variable, a properties configuration or its default setting.
     *
     * @return int - the reload interval
     */
    public int getReloadInterval() {
        return reloadInterval;
    }
}
