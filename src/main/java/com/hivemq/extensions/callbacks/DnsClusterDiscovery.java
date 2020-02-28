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

package com.hivemq.extensions.callbacks;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.services.cluster.ClusterDiscoveryCallback;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryInput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryOutput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterNodeAddress;
import com.hivemq.extensions.configuration.DnsDiscoveryConfigExtended;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.concurrent.Future;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


/**
 * Cluster discovery using DNS resolution of round-robin A records.
 * Uses non-blocking netty API for DNS resolution, reads discovery parameters as environment variables.
 *
 * @author Daniel Krüger
 * @author Simon Baier
 */
public class DnsClusterDiscovery implements ClusterDiscoveryCallback {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DnsClusterDiscovery.class);
    public static final int BACKOFF_DISABLED = -1;

    @NotNull
    private final DnsDiscoveryConfigExtended discoveryConfiguration;
    @NotNull
    private final NioEventLoopGroup eventLoopGroup;
    @NotNull
    private final InetAddressValidator addressValidator;
    @Nullable
    private ClusterNodeAddress ownAddress;

    private int iteration;
    private int initialReloadInterval;
    private int maxReloadInterval;
    private boolean backOffEnabled;

    private List<ClusterNodeAddress> previousClusterNodes;

    public DnsClusterDiscovery(final @NotNull DnsDiscoveryConfigExtended discoveryConfiguration) {
        this.eventLoopGroup = new NioEventLoopGroup();
        this.addressValidator = InetAddressValidator.getInstance();
        this.discoveryConfiguration = discoveryConfiguration;
        this.iteration = 1;
    }

    @Override
    public void init(final @NotNull ClusterDiscoveryInput clusterDiscoveryInput, final @NotNull ClusterDiscoveryOutput clusterDiscoveryOutput) {
        this.maxReloadInterval = discoveryConfiguration.maxDiscoveryInterval();
        this.initialReloadInterval = discoveryConfiguration.initialDiscoveryInterval();
        this.backOffEnabled = initialReloadInterval < maxReloadInterval;
        ownAddress = clusterDiscoveryInput.getOwnAddress();
        loadClusterNodeAddresses(clusterDiscoveryOutput);
        clusterDiscoveryOutput.setReloadInterval(getReloadInterval());
    }

    @Override
    public void reload(final @NotNull ClusterDiscoveryInput clusterDiscoveryInput, final @NotNull ClusterDiscoveryOutput clusterDiscoveryOutput) {
        loadClusterNodeAddresses(clusterDiscoveryOutput);

        clusterDiscoveryOutput.setReloadInterval(getReloadInterval());
    }

    @Override
    public void destroy(final @NotNull ClusterDiscoveryInput clusterDiscoveryInput) {
        eventLoopGroup.shutdownGracefully();
    }

    private void loadClusterNodeAddresses(final @NotNull ClusterDiscoveryOutput clusterDiscoveryOutput) {
        try {
            final List<ClusterNodeAddress> clusterNodeAddresses = loadOtherNodes();

            if (backOffEnabled && previousClusterNodes != null && clusterNodeAddresses != null
                    && previousClusterNodes.size() != clusterNodeAddresses.size()) {
                if (!CollectionUtils.isEqualCollection(previousClusterNodes, clusterNodeAddresses)) {
                    // Reset the exponential back-off on topology changes to detect new nodes faster
                    iteration = 1;
                }
            }
            previousClusterNodes = clusterNodeAddresses;

            if (clusterNodeAddresses != null && !clusterNodeAddresses.isEmpty()) {
                clusterDiscoveryOutput.provideCurrentNodes(clusterNodeAddresses);
            }
        } catch (TimeoutException | InterruptedException e) {
            log.error("Timeout while getting other node addresses");
        }
    }

    private List<ClusterNodeAddress> loadOtherNodes() throws TimeoutException, InterruptedException {

        final String discoveryAddress = discoveryConfiguration.discoveryAddress();
        if (discoveryAddress == null) {
            return null;
        }

        final int discoveryTimeout = discoveryConfiguration.resolutionTimeout();

        // initialize netty DNS resolver
        try (DnsNameResolver resolver = new DnsNameResolverBuilder(eventLoopGroup.next()).channelType(NioDatagramChannel.class).build()) {

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
            return clusterNodeAddresses;
        } catch (ExecutionException ex) {
            log.error("Failed to resolve DNS record for address '{}'.", discoveryAddress, ex);
        }
        return null;
    }

    private int getReloadInterval() {
        if(!backOffEnabled) {
            // exponential back-off disabled
            return maxReloadInterval;
        }
        if (iteration != BACKOFF_DISABLED) {
            final int backOff = (int) Math.pow(2, iteration);

            if (backOff >= maxReloadInterval) {
                iteration = BACKOFF_DISABLED;
            } else {
                iteration++;
            }
            return Math.max(initialReloadInterval, Math.min(backOff, maxReloadInterval));
        }
        return maxReloadInterval;
    }
}
