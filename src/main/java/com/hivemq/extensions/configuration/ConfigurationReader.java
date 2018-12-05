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

package com.hivemq.extensions.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration Reader that reads properties from config file
 *
 * @author Daniel Krüger
 */
public class ConfigurationReader {

    static final String CONFIG_PATH = "dnsdiscovery.properties";
    private static final Logger log = LoggerFactory.getLogger(ConfigurationReader.class);
    private final File extensionHomeFolder;

    public ConfigurationReader(final @NotNull ExtensionInformation extensionInformation) {
        this.extensionHomeFolder = extensionInformation.getExtensionHomeFolder();
        ConfigFactory.setProperty("configFile", new File(extensionHomeFolder, CONFIG_PATH).getAbsolutePath());
    }

    /**
     * method that loads and reloads the configuration for the dns discovery properties, by (re)creating the configuration
     *
     * @return DnsDiscoveryConfig
     */
    public DnsDiscoveryConfig get() {
        final File propertiesFile = new File(extensionHomeFolder, CONFIG_PATH);
        try (final InputStream inputStream = new FileInputStream(propertiesFile)) {
            final Properties properties = new Properties();
            properties.load(inputStream);
            return ConfigFactory.create(DnsDiscoveryConfig.class, properties);
        } catch (IOException e) {
            log.warn("No dnsdiscovery.properties file found. Use default settings");
            return ConfigFactory.create(DnsDiscoveryConfig.class);
        }
    }
}
