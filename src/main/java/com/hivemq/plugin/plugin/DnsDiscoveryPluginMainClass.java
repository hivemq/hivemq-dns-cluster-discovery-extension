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

package com.hivemq.plugin.plugin;

import com.hivemq.plugin.api.PluginMain;
import com.hivemq.plugin.api.annotations.NotNull;
import com.hivemq.plugin.api.parameter.PluginStartInput;
import com.hivemq.plugin.api.parameter.PluginStartOutput;
import com.hivemq.plugin.api.parameter.PluginStopInput;
import com.hivemq.plugin.api.parameter.PluginStopOutput;
import com.hivemq.plugin.api.services.Services;
import com.hivemq.plugin.callbacks.DnsClusterDiscovery;
import com.hivemq.plugin.configuration.ConfigurationReader;
import com.hivemq.plugin.configuration.DnsDiscoveryConfigExtended;

/**
 * This is the main class of the  dns discovery plugin, which is instantiated during the HiveMQ start up process.
 *
 * @author Anja Helmbrecht-Schaar
 */
public class DnsDiscoveryPluginMainClass implements PluginMain {

    @Override
    public void pluginStart(@NotNull PluginStartInput pluginStartInput, @NotNull PluginStartOutput pluginStartOutput) {
        try {

            final ConfigurationReader configurationReader = new ConfigurationReader(pluginStartInput.getPluginInformation());
            if (configurationReader.get() == null) {
                pluginStartOutput.preventPluginStartup("Unspecified error occurred while reading configuration");
                return;
            }
            Services.clusterService().addDiscoveryCallback(new DnsClusterDiscovery(new DnsDiscoveryConfigExtended(configurationReader)));
        } catch (final Exception e) {
            pluginStartOutput.preventPluginStartup("Unknown error while starting the plugin" + ((e.getMessage() != null) ? ": " + e.getMessage() : ""));
            return;
        }
    }

    @Override
    public void pluginStop(@NotNull PluginStopInput pluginStopInput, @NotNull PluginStopOutput pluginStopOutput) {
        //nothing to do here
    }
}

