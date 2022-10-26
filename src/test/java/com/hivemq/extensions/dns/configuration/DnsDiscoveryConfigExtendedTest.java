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
package com.hivemq.extensions.dns.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.dns.exception.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brand
 */
@ExtendWith(MockitoExtension.class)
class DnsDiscoveryConfigExtendedTest {

    @Mock
    @NotNull DnsDiscoveryConfigFile configFile;

    @Mock
    @NotNull DnsDiscoveryConfigEnvironment configEnvironment;

    @Test
    void test_dnsServerAddress_env() {
        when(configEnvironment.getEnvDnsServerAddress()).thenReturn("Test.Env:8888");

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.dnsServerAddress();

        assertTrue(configExtended.getDnsServerAddress().isPresent());
        assertEquals("Test.Env", configExtended.getDnsServerAddress().get().getHostName());
        assertEquals(8888, configExtended.getDnsServerAddress().get().getPort());
    }

    @Test
    void test_dnsServerAddress_file() {
        when(configEnvironment.getEnvDnsServerAddress()).thenReturn(null);
        when(configFile.getFileDnsServerAddress()).thenReturn("Test.File:8888");

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.dnsServerAddress();

        assertTrue(configExtended.getDnsServerAddress().isPresent());
        assertEquals("Test.File", configExtended.getDnsServerAddress().get().getHostName());
        assertEquals(8888, configExtended.getDnsServerAddress().get().getPort());
    }

    @Test
    void test_dnsServerAddress_no_env_no_file() {
        when(configEnvironment.getEnvDnsServerAddress()).thenReturn(null);
        when(configFile.getFileDnsServerAddress()).thenReturn(null);

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.dnsServerAddress();

        assertTrue(configExtended.getDnsServerAddress().isEmpty());
    }

    @Test
    void test_dnsServerAddress_env_bad_port() {
        when(configEnvironment.getEnvDnsServerAddress()).thenReturn("Test:-30");

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);

        assertThrows(ConfigurationException.class, configExtended::dnsServerAddress);
    }

    @Test
    void test_dnsServerAddress_file_bad_port() {
        when(configEnvironment.getEnvDnsServerAddress()).thenReturn(null);
        when(configFile.getFileDnsServerAddress()).thenReturn("Test:-30");

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);

        assertThrows(ConfigurationException.class, configExtended::dnsServerAddress);
    }


    @Test
    void test_discoveryAddress_env() {
        when(configEnvironment.getEnvDiscoveryAddress()).thenReturn("Test.Env");

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.discoveryAddress();

        assertTrue(configExtended.getDiscoveryAddress().isPresent());
        assertEquals("Test.Env", configExtended.getDiscoveryAddress().get());
    }

    @Test
    void test_discoveryAddress_file() {
        when(configEnvironment.getEnvDiscoveryAddress()).thenReturn(null);
        when(configFile.getFileDiscoveryAddress()).thenReturn("Test.File");

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.discoveryAddress();

        assertTrue(configExtended.getDiscoveryAddress().isPresent());
        assertEquals("Test.File", configExtended.getDiscoveryAddress().get());
    }

    @Test
    void test_discoveryAddress_no_env_no_file() {
        when(configEnvironment.getEnvDiscoveryAddress()).thenReturn(null);
        when(configFile.getFileDiscoveryAddress()).thenReturn(null);

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.discoveryAddress();

        assertTrue(configExtended.getDnsServerAddress().isEmpty());
    }


    @Test
    void test_resolutionTimeout_env() {
        when(configEnvironment.getEnvResolutionTimeout()).thenReturn("1234");

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.resolutionTimeout();

        assertEquals(1234, configExtended.getResolutionTimeout());
    }

    @Test
    void test_resolutionTimeout_file() {
        when(configEnvironment.getEnvResolutionTimeout()).thenReturn(null);
        when(configFile.getFileResolutionTimeout()).thenReturn(4321);

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.resolutionTimeout();

        assertEquals(4321, configExtended.getResolutionTimeout());
    }

    @Test
    void test_resolutionTimeout_no_env_no_file() {
        when(configEnvironment.getEnvResolutionTimeout()).thenReturn(null);
        when(configFile.getFileResolutionTimeout()).thenReturn(-1);

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.resolutionTimeout();

        assertEquals(30, configExtended.getResolutionTimeout());
    }


    @Test
    void test_reloadInterval_env() {
        when(configEnvironment.getEnvReloadInterval()).thenReturn("1234");

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.reloadInterval();

        assertEquals(1234, configExtended.getReloadInterval());
    }

    @Test
    void test_reloadInterval_file() {
        when(configEnvironment.getEnvReloadInterval()).thenReturn(null);
        when(configFile.getFileReloadInterval()).thenReturn(4321);

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.reloadInterval();

        assertEquals(4321, configExtended.getReloadInterval());
    }

    @Test
    void test_reloadInterval_no_env_no_file() {
        when(configEnvironment.getEnvReloadInterval()).thenReturn(null);
        when(configFile.getFileReloadInterval()).thenReturn(-1);

        final DnsDiscoveryConfigExtended configExtended = new DnsDiscoveryConfigExtended(configFile, configEnvironment);
        configExtended.reloadInterval();

        assertEquals(30, configExtended.getReloadInterval());
    }
}
