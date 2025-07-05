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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brand
 */
class DnsDiscoveryConfigExtendedTest {

    private final @NotNull DnsDiscoveryConfigFile configFile = mock();
    private final @NotNull DnsDiscoveryConfigEnvironment configEnvironment = mock();

    @Test
    void test_dnsServerAddress_env() {
        when(configEnvironment.getEnvDnsServerAddress()).thenReturn("Test.Env:8888");

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.dnsServerAddress();

        assertThat(configExtended.getDnsServerAddress()).hasValueSatisfying(inetSocketAddress -> {
            assertThat(inetSocketAddress.getHostName()).isEqualTo("Test.Env");
            assertThat(inetSocketAddress.getPort()).isEqualTo(8888);
        });
    }

    @Test
    void test_dnsServerAddress_file() {
        when(configEnvironment.getEnvDnsServerAddress()).thenReturn(null);
        when(configFile.getFileDnsServerAddress()).thenReturn("Test.File:8888");

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.dnsServerAddress();
        assertThat(configExtended.getDnsServerAddress()).hasValueSatisfying(inetSocketAddress -> {
            assertThat(inetSocketAddress.getHostName()).isEqualTo("Test.File");
            assertThat(inetSocketAddress.getPort()).isEqualTo(8888);
        });
    }

    @Test
    void test_dnsServerAddress_no_env_no_file() {
        when(configEnvironment.getEnvDnsServerAddress()).thenReturn(null);
        when(configFile.getFileDnsServerAddress()).thenReturn(null);

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.dnsServerAddress();
        assertThat(configExtended.getDnsServerAddress()).isEmpty();
    }

    @Test
    void test_dnsServerAddress_env_bad_port() {
        when(configEnvironment.getEnvDnsServerAddress()).thenReturn("Test:-30");

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        assertThatThrownBy(configExtended::dnsServerAddress).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void test_dnsServerAddress_file_bad_port() {
        when(configEnvironment.getEnvDnsServerAddress()).thenReturn(null);
        when(configFile.getFileDnsServerAddress()).thenReturn("Test:-30");

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        assertThatThrownBy(configExtended::dnsServerAddress).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void test_discoveryAddress_env() {
        when(configEnvironment.getEnvDiscoveryAddress()).thenReturn("Test.Env");

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.discoveryAddress();
        assertThat(configExtended.getDiscoveryAddress()).hasValue("Test.Env");
    }

    @Test
    void test_discoveryAddress_file() {
        when(configEnvironment.getEnvDiscoveryAddress()).thenReturn(null);
        when(configFile.getFileDiscoveryAddress()).thenReturn("Test.File");

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.discoveryAddress();
        assertThat(configExtended.getDiscoveryAddress()).hasValue("Test.File");
    }

    @Test
    void test_discoveryAddress_no_env_no_file() {
        when(configEnvironment.getEnvDiscoveryAddress()).thenReturn(null);
        when(configFile.getFileDiscoveryAddress()).thenReturn(null);

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.discoveryAddress();
        assertThat(configExtended.getDnsServerAddress()).isEmpty();
    }


    @Test
    void test_resolutionTimeout_env() {
        when(configEnvironment.getEnvResolutionTimeout()).thenReturn("1234");

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.resolutionTimeout();
        assertThat(configExtended.getResolutionTimeout()).isEqualTo(1234);
    }

    @Test
    void test_resolutionTimeout_file() {
        when(configEnvironment.getEnvResolutionTimeout()).thenReturn(null);
        when(configFile.getFileResolutionTimeout()).thenReturn(4321);

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.resolutionTimeout();
        assertThat(configExtended.getResolutionTimeout()).isEqualTo(4321);
    }

    @Test
    void test_resolutionTimeout_no_env_no_file() {
        when(configEnvironment.getEnvResolutionTimeout()).thenReturn(null);
        when(configFile.getFileResolutionTimeout()).thenReturn(-1);

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.resolutionTimeout();
        assertThat(configExtended.getResolutionTimeout()).isEqualTo(30);
    }


    @Test
    void test_reloadInterval_env() {
        when(configEnvironment.getEnvReloadInterval()).thenReturn("1234");

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.reloadInterval();
        assertThat(configExtended.getReloadInterval()).isEqualTo(1234);
    }

    @Test
    void test_reloadInterval_file() {
        when(configEnvironment.getEnvReloadInterval()).thenReturn(null);
        when(configFile.getFileReloadInterval()).thenReturn(4321);

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.reloadInterval();
        assertThat(configExtended.getReloadInterval()).isEqualTo(4321);
    }

    @Test
    void test_reloadInterval_no_env_no_file() {
        when(configEnvironment.getEnvReloadInterval()).thenReturn(null);
        when(configFile.getFileReloadInterval()).thenReturn(-1);

        final var configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.reloadInterval();
        assertThat(configExtended.getReloadInterval()).isEqualTo(30);
    }
}
