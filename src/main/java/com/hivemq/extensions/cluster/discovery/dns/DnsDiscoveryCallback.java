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

import com.hivemq.extension.sdk.api.services.cluster.ClusterDiscoveryCallback;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryInput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryOutput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterNodeAddress;
import com.hivemq.extensions.cluster.discovery.dns.configuration.DnsDiscoveryConfigExtended;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.SingletonDnsServerAddressStreamProvider;
import io.netty.util.concurrent.Future;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.hivemq.extensions.cluster.discovery.dns.ExtensionConstants.EXTENSION_NAME;

/**
 * Cluster discovery using DNS resolution of round-robin A records.
 * Uses non-blocking netty API for DNS resolution, reads discovery parameters as environment variables.
 *
 * @author Daniel Krüger
 * @author Lukas Brand
 */
class DnsDiscoveryCallback implements ClusterDiscoveryCallback {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DnsDiscoveryCallback.class);

    private final @NotNull DnsDiscoveryConfigExtended configuration;
    private final @NotNull DnsDiscoveryMetrics metrics;
    private final @NotNull NioEventLoopGroup eventLoopGroup;
    private final @NotNull InetAddressValidator addressValidator;

    private final @NotNull AtomicInteger addressesCount = new AtomicInteger(0);
    private final @NotNull AtomicReference<List<String>> foundHostsRef = new AtomicReference<>(List.of());

    private @Nullable ClusterNodeAddress ownAddress;

    DnsDiscoveryCallback(
            final @NotNull DnsDiscoveryConfigExtended configuration, final @NotNull DnsDiscoveryMetrics metrics) {
        this.eventLoopGroup = new NioEventLoopGroup();
        this.addressValidator = InetAddressValidator.getInstance();
        this.configuration = configuration;
        this.metrics = metrics;

        metrics.registerAddressCountGauge(addressesCount::get);
    }

    @Override
    public void init(
            final @NotNull ClusterDiscoveryInput clusterDiscoveryInput,
            final @NotNull ClusterDiscoveryOutput clusterDiscoveryOutput) {
        ownAddress = clusterDiscoveryInput.getOwnAddress();
        clusterDiscoveryOutput.setReloadInterval(configuration.getReloadInterval());
        loadClusterNodeAddresses(clusterDiscoveryOutput);
    }

    @Override
    public void reload(
            final @NotNull ClusterDiscoveryInput clusterDiscoveryInput,
            final @NotNull ClusterDiscoveryOutput clusterDiscoveryOutput) {
        loadClusterNodeAddresses(clusterDiscoveryOutput);
        clusterDiscoveryOutput.setReloadInterval(configuration.getReloadInterval());
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
                metrics.getQuerySuccessCount().inc();
            }
        } catch (final TimeoutException | InterruptedException e) {
            log.error("{}: Timeout while getting other node addresses.", EXTENSION_NAME);
            metrics.getQueryFailedCount().inc();
            addressesCount.set(0);
        }
    }

    private @Nullable List<ClusterNodeAddress> loadOtherNodes() throws TimeoutException, InterruptedException {
        if (ownAddress == null) {
            return null;
        }

        final Optional<String> discoveryAddress = configuration.getDiscoveryAddress();
        if (discoveryAddress.isEmpty()) {
            log.warn("{}: Discovery address not set, skipping DNS query.", EXTENSION_NAME);
            return null;
        }
        final int discoveryTimeout = configuration.getResolutionTimeout();

        // initialize netty DNS resolver
        final DnsNameResolverBuilder dnsNameResolverBuilder =
                new DnsNameResolverBuilder(eventLoopGroup.next()).channelType(NioDatagramChannel.class)
                        .optResourceEnabled(false);

        // use custom DNS server address if necessary
        final Optional<InetSocketAddress> dnsServerAddress = configuration.getDnsServerAddress();
        dnsServerAddress.ifPresent(inetSocketAddress -> dnsNameResolverBuilder.nameServerProvider(new SingletonDnsServerAddressStreamProvider(
                inetSocketAddress)));

        try (final DnsNameResolver resolver = dnsNameResolverBuilder.build()) {
            final Future<List<InetAddress>> addresses = resolver.resolveAll(discoveryAddress.get());
            final List<ClusterNodeAddress> clusterNodeAddresses =
                    addresses.get(discoveryTimeout, TimeUnit.SECONDS)
                            .stream()
                            // skip any possibly unresolved elements
                            .filter(Objects::nonNull)
                            // check if the discoveryAddress address we got from the DNS is a valid IP address
                            .filter((address) -> addressValidator.isValid(address.getHostAddress()))
                            .map((address) -> new ClusterNodeAddress(address.getHostAddress(), ownAddress.getPort()))
                            .collect(Collectors.toList());

            final List<String> foundHosts = new ArrayList<>();
            final List<String> lastFoundHosts = foundHostsRef.get();
            clusterNodeAddresses.forEach((address) -> {
                final String host = address.getHost();
                foundHosts.add(host);
                if (!lastFoundHosts.contains(host)) {
                    log.debug("{}: Discovered new address '{}'.", EXTENSION_NAME, host);
                }
            });
            lastFoundHosts.forEach(host -> {
                if (!foundHosts.contains(host)) {
                    log.debug("{}: Discovered address '{}' is gone.", EXTENSION_NAME, host);
                }
            });
            foundHostsRef.set(foundHosts);
            addressesCount.set(clusterNodeAddresses.size());

            return clusterNodeAddresses;
        } catch (final ExecutionException e) {
            log.error("{}: Failed to resolve DNS record for address '{}'.", EXTENSION_NAME, discoveryAddress);
            metrics.getQueryFailedCount().inc();
            addressesCount.set(0);
        }
        return null;
    }
}
