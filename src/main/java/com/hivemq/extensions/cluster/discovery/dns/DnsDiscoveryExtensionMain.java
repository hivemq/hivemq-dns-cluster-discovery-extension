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

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.cluster.discovery.dns.configuration.ConfigurationException;
import com.hivemq.extensions.cluster.discovery.dns.configuration.ConfigurationFileReader;
import com.hivemq.extensions.cluster.discovery.dns.configuration.DnsDiscoveryConfigExtended;
import com.hivemq.extensions.cluster.discovery.dns.configuration.DnsDiscoveryConfigFile;
import com.hivemq.extensions.cluster.discovery.dns.metrics.DnsDiscoveryMetrics;

import java.io.File;

/**
 * This is the main class of the dns discovery extension, which is instantiated during the HiveMQ start up process.
 *
 * @author Anja Helmbrecht-Schaar
 */
public class DnsDiscoveryExtensionMain implements ExtensionMain {

    private @Nullable DnsDiscoveryCallback dnsDiscoveryCallback;

    @Override
    public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput) {
        try {
            final File extensionHomeFolder = extensionStartInput.getExtensionInformation().getExtensionHomeFolder();
            final ConfigurationFileReader configurationFileReader = new ConfigurationFileReader(extensionHomeFolder);
            final DnsDiscoveryConfigFile dnsFileConfig = configurationFileReader.get();
            final DnsDiscoveryConfigExtended extendedConfig = DnsDiscoveryConfigExtended.createInstance(dnsFileConfig);
            final DnsDiscoveryMetrics metrics = new DnsDiscoveryMetrics(Services.metricRegistry());

            dnsDiscoveryCallback = new DnsDiscoveryCallback(extendedConfig, metrics);

            Services.clusterService().addDiscoveryCallback(dnsDiscoveryCallback);
        } catch (final ConfigurationException e) {
            extensionStartOutput.preventExtensionStartup("Error while reading the configuration" +
                    ((e.getMessage() != null) ? ": " + e.getMessage() : ""));
        } catch (final Exception e) {
            extensionStartOutput.preventExtensionStartup("Unknown error while starting the extension" +
                    ((e.getMessage() != null) ? ": " + e.getMessage() : ""));
        }
    }

    @Override
    public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput) {
        if (dnsDiscoveryCallback != null) {
            Services.clusterService().removeDiscoveryCallback(dnsDiscoveryCallback);
        }
    }
}
