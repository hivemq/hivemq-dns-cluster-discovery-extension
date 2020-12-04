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
package com.hivemq.extensions.dns.callbacks;

import com.codahale.metrics.Counter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryInput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryOutput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterNodeAddress;
import com.hivemq.extensions.dns.configuration.DnsDiscoveryConfigExtended;
import com.hivemq.extensions.dns.metrics.DnsDiscoveryMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DnsClusterDiscoveryTest {

    private final @NotNull ClusterNodeAddress cla = new ClusterNodeAddress("localhost", 1883);

    private DnsClusterDiscovery dnsClusterDiscovery;

    @Mock
    private DnsDiscoveryConfigExtended configuration;

    @Mock
    private DnsDiscoveryMetrics metrics;


    @NotNull
    ClusterDiscoveryInput input = new ClusterDiscoveryInput() {
        @Override
        public @NotNull
        ClusterNodeAddress getOwnAddress() {
            return cla;
        }

        @Override
        public @NotNull
        String getOwnClusterId() {
            return "123";
        }

        @Override
        public int getReloadInterval() {
            return 1;
        }
    };

    @NotNull
    ClusterDiscoveryOutput output = new ClusterDiscoveryOutput() {
        //added just to make addresses accessible for testing
        private String addresses;

        @Override
        public void provideCurrentNodes(@Nullable List<ClusterNodeAddress> list) {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(cla);
            addresses = list.iterator().next().getHost();
        }

        @Override
        public String toString() {
            return addresses;
        }

        @Override
        public void setReloadInterval(int i) {
            i = 1;
        }
    };

    @BeforeEach
    public void setUp() {
        initMocks(this);
        dnsClusterDiscovery = new DnsClusterDiscovery(configuration, metrics);
    }

    @Test
    public void testDnsClusterDiscoverySingleNode() {
        when(metrics.getResolutionRequestCounter()).thenReturn(new Counter());

        final String discoveryAddress = "www.hivemq.com";
        final int discoveryTimeout = 30;

        when(configuration.dnsServerAddress()).thenReturn(null);
        when(configuration.discoveryAddress()).thenReturn(discoveryAddress);
        when(configuration.resolutionTimeout()).thenReturn(discoveryTimeout);

        dnsClusterDiscovery.init(input, output);
        dnsClusterDiscovery.reload(input, output);

        // A record for dc-square.de
        assertEquals("212.72.72.12", output.toString());
    }

}
