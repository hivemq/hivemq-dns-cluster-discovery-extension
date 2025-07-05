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

import org.aeonbits.owner.ConfigFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration reader that reads properties from config file.
 *
 * @author Daniel Krüger
 */
public class ConfigurationFileReader {

    static final @NotNull String CONFIG_PATH = "dnsdiscovery.properties";

    private final @NotNull File extensionHomeFolder;

    public ConfigurationFileReader(final @NotNull File extensionHomeFolder) {
        this.extensionHomeFolder = extensionHomeFolder;
        ConfigFactory.setProperty("configFile", new File(extensionHomeFolder, CONFIG_PATH).getAbsolutePath());
    }

    /**
     * Method that loads and reloads the configuration for the DNS discovery properties, by (re)creating the
     * configuration.
     *
     * @return DnsDiscoveryConfigFile The configuration from the config file or default values.
     */
    public @NotNull DnsDiscoveryConfigFile get() {
        final var propertiesFile = new File(extensionHomeFolder, CONFIG_PATH);
        try (final var inputStream = new FileInputStream(propertiesFile)) {
            final var properties = new Properties();
            properties.load(inputStream);
            return ConfigFactory.create(DnsDiscoveryConfigFile.class, properties);
        } catch (final IOException e) {
            return ConfigFactory.create(DnsDiscoveryConfigFile.class);
        }
    }
}
