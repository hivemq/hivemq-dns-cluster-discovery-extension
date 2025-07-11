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
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryInput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryOutput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterNodeAddress;
import com.hivemq.extensions.cluster.discovery.dns.configuration.DnsDiscoveryConfigExtended;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DnsDiscoveryCallbackTest {

    private final @NotNull ClusterDiscoveryInput input = mock();
    private final @NotNull ClusterDiscoveryOutput output = mock();

    private final @NotNull ClusterNodeAddress cla = new ClusterNodeAddress("localhost", 1883);

    private @NotNull DnsDiscoveryCallback dnsDiscoveryCallback;

    @BeforeEach
    void setUp() {
        when(input.getOwnAddress()).thenReturn(cla);

        final var metrics = mock(DnsDiscoveryMetrics.class);
        when(metrics.getQuerySuccessCount()).thenReturn(new Counter());

        final var configuration = mock(DnsDiscoveryConfigExtended.class);
        when(configuration.getDnsServerAddress()).thenReturn(Optional.empty());
        when(configuration.getDiscoveryAddress()).thenReturn(Optional.of("172.16.16.1"));
        when(configuration.getResolutionTimeout()).thenReturn(30);
        when(configuration.getReloadInterval()).thenReturn(60);

        dnsDiscoveryCallback = new DnsDiscoveryCallback(configuration, metrics);
    }

    @Test
    void whenInitAndReload_thenAddressIsProvided() {
        dnsDiscoveryCallback.init(input, output);

        //noinspection unchecked
        final ArgumentCaptor<List<ClusterNodeAddress>> captor = ArgumentCaptor.forClass(List.class);
        verify(output).provideCurrentNodes(captor.capture());
        assertThat(captor.getValue()).containsExactly(new ClusterNodeAddress("172.16.16.1", 1883));

        dnsDiscoveryCallback.reload(input, output);

        verify(output, times(2)).provideCurrentNodes(captor.capture());
        assertThat(captor.getValue()).containsExactly(new ClusterNodeAddress("172.16.16.1", 1883));
    }
}
