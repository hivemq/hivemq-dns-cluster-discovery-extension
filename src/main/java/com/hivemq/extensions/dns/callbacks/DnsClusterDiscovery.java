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

package com.hivemq.extensions.dns.callbacks;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.services.cluster.ClusterDiscoveryCallback;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryInput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryOutput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterNodeAddress;
import com.hivemq.extensions.dns.configuration.DnsDiscoveryConfigExtended;
import com.hivemq.extensions.dns.metrics.DnsDiscoveryMetrics;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.SingletonDnsServerAddressStreamProvider;
import io.netty.util.concurrent.Future;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * Cluster discovery using DNS resolution of round-robin A records.
 * Uses non-blocking netty API for DNS resolution, reads discovery parameters as environment variables.
 *
 * @author Daniel Krüger
 * @author Lukas Brand
 */
public class DnsClusterDiscovery implements ClusterDiscoveryCallback {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DnsClusterDiscovery.class);

    private final @NotNull DnsDiscoveryConfigExtended discoveryConfiguration;
    private final @NotNull DnsDiscoveryMetrics dnsDiscoveryMetrics;
    private final @NotNull NioEventLoopGroup eventLoopGroup;
    private final @NotNull InetAddressValidator addressValidator;

    private final @NotNull AtomicInteger addressesCount = new AtomicInteger(0);

    private @Nullable ClusterNodeAddress ownAddress;


    public DnsClusterDiscovery(final @NotNull DnsDiscoveryConfigExtended discoveryConfiguration,
                               final @NotNull DnsDiscoveryMetrics dnsDiscoveryMetrics) {
        this.eventLoopGroup = new NioEventLoopGroup();
        this.addressValidator = InetAddressValidator.getInstance();
        this.discoveryConfiguration = discoveryConfiguration;
        this.dnsDiscoveryMetrics = dnsDiscoveryMetrics;

        dnsDiscoveryMetrics.registerAddressCountGauge(addressesCount::get);
    }

    @Override
    public void init(final @NotNull ClusterDiscoveryInput clusterDiscoveryInput,
                     final @NotNull ClusterDiscoveryOutput clusterDiscoveryOutput) {
        ownAddress = clusterDiscoveryInput.getOwnAddress();
        loadClusterNodeAddresses(clusterDiscoveryOutput);
    }

    @Override
    public void reload(final @NotNull ClusterDiscoveryInput clusterDiscoveryInput,
                       final @NotNull ClusterDiscoveryOutput clusterDiscoveryOutput) {
        loadClusterNodeAddresses(clusterDiscoveryOutput);
    }

    @Override
    public void destroy(final @NotNull ClusterDiscoveryInput clusterDiscoveryInput) {
        eventLoopGroup.shutdownGracefully();
    }


    private void loadClusterNodeAddresses(final @NotNull ClusterDiscoveryOutput clusterDiscoveryOutput) {
        try {
            final List<ClusterNodeAddress> clusterNodeAddresses = loadOtherNodes();
            if (clusterNodeAddresses != null) {
                clusterDiscoveryOutput.provideCurrentNodes(clusterNodeAddresses);
                dnsDiscoveryMetrics.getResolutionRequestCounter().inc();
            }
        } catch (TimeoutException | InterruptedException e) {
            log.error("Timeout while getting other node addresses");
            dnsDiscoveryMetrics.getResolutionRequestFailedCounter().inc();
        }
    }

    private @Nullable List<ClusterNodeAddress> loadOtherNodes() throws TimeoutException, InterruptedException {

        final String discoveryAddress = discoveryConfiguration.discoveryAddress();
        if (discoveryAddress == null) {
            log.warn("Discovery address not set, skipping dns query.");
            return null;
        }
        final int discoveryTimeout = discoveryConfiguration.resolutionTimeout();

        // initialize netty DNS resolver
        final DnsNameResolverBuilder dnsNameResolverBuilder = new DnsNameResolverBuilder(eventLoopGroup.next())
                .channelType(NioDatagramChannel.class)
                .optResourceEnabled(false);

        // use custom DNS server address if necessary
        final Map<String, Integer> dnsServerAddress = discoveryConfiguration.dnsServerAddress();
        if (dnsServerAddress != null) {
            final String address = dnsServerAddress.keySet().iterator().next();
            final int port = dnsServerAddress.get(address);

            final InetSocketAddress dnsInetSocketAddress = new InetSocketAddress(address, port);
            dnsNameResolverBuilder.nameServerProvider(new SingletonDnsServerAddressStreamProvider(dnsInetSocketAddress));
        }

        try (final DnsNameResolver resolver = dnsNameResolverBuilder.build()) {

            final Future<List<InetAddress>> addresses = resolver.resolveAll(discoveryAddress);
            final List<ClusterNodeAddress> clusterNodeAddresses = addresses.get(discoveryTimeout, TimeUnit.SECONDS)
                    .stream()
                    // Skip any possibly unresolved elements
                    .filter(Objects::nonNull)
                    // Check if the discoveryAddress address we got from the DNS is a valid IP address
                    .filter((address) -> addressValidator.isValid(address.getHostAddress()))
                    .map((address) -> new ClusterNodeAddress(address.getHostAddress(), ownAddress.getPort()))
                    .collect(Collectors.toList());

            clusterNodeAddresses.forEach((address) -> log.debug("Found address: '{}'", address.getHost()));
            addressesCount.set(clusterNodeAddresses.size());

            return clusterNodeAddresses;
        } catch (final ExecutionException ex) {
            log.error("Failed to resolve DNS record for address '{}'.", discoveryAddress, ex);
            dnsDiscoveryMetrics.getResolutionRequestFailedCounter().inc();
        }
        return null;
    }

}
