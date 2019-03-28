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

package com.hivemq.extensions;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.callbacks.DnsClusterDiscovery;
import com.hivemq.extensions.configuration.ConfigurationReader;
import com.hivemq.extensions.configuration.DnsDiscoveryConfigExtended;

/**
 * This is the main class of the dns discovery extension, which is instantiated during the HiveMQ start up process.
 *
 * @author Anja Helmbrecht-Schaar
 */
public class DnsDiscoveryExtensionMainClass implements ExtensionMain {

    private DnsClusterDiscovery dnsClusterDiscovery;

    @Override
    public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput) {

        try {
            final ConfigurationReader configurationReader = new ConfigurationReader(extensionStartInput.getExtensionInformation());
            if (configurationReader.get() == null) {
                extensionStartOutput.preventExtensionStartup("Unspecified error occurred while reading configuration");
                return;
            }

            dnsClusterDiscovery = new DnsClusterDiscovery(new DnsDiscoveryConfigExtended(configurationReader));

            try {
                Services.clusterService().addDiscoveryCallback(dnsClusterDiscovery);
            } catch (final UnsupportedOperationException e) {
                extensionStartOutput.preventExtensionStartup(e.getMessage());
            }

        } catch (final Exception e) {
            extensionStartOutput.preventExtensionStartup("Unknown error while starting the extension" + ((e.getMessage() != null) ? ": " + e.getMessage() : ""));
        }
    }

    @Override
    public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput) {

        if (dnsClusterDiscovery != null) {
            Services.clusterService().removeDiscoveryCallback(dnsClusterDiscovery);
        }
    }
}

