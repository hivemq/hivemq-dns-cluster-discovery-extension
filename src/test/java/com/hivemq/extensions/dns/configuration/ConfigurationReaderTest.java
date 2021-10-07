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
package com.hivemq.extensions.dns.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConfigurationReaderTest {

    @Mock
    DnsDiscoveryConfigExtended defaultConfig;

    @Mock
    private ExtensionInformation extensionInformation;


    @BeforeEach
    public void init() {
        initMocks(this);
        when(defaultConfig.resolutionTimeout()).thenReturn(30);
        when(defaultConfig.discoveryAddress()).thenReturn("");

    }


    @Test
    public void test_ReadConfiguration_no_file_use_Defaults(final @NotNull @TempDir Path tempDir) {
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(tempDir.toAbsolutePath().toFile());

        final ConfigurationReader configurationReader = new ConfigurationReader(extensionInformation);

        assertEquals(configurationReader.get().resolutionTimeout(), defaultConfig.resolutionTimeout());
        assertNull(configurationReader.get().discoveryAddress());
    }

    @Test
    public void test_successfully_read_config(final @NotNull @TempDir Path tempDir) throws Exception {
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(tempDir.toAbsolutePath().toFile());
        Files.writeString(tempDir.resolve(ConfigurationReader.CONFIG_PATH),
                "discoveryAddress:task.hivemq\n" + "resolutionTimeout:10",
                StandardOpenOption.CREATE);


        final ConfigurationReader configurationReader = new ConfigurationReader(extensionInformation);
        configurationReader.get();

        assertEquals(10, configurationReader.get().resolutionTimeout());
        assertEquals("task.hivemq", configurationReader.get().discoveryAddress());

    }

    @Test
    public void test_typo_resolutionTimeOut(final @NotNull @TempDir Path tempDir) throws Exception {
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(tempDir.toAbsolutePath().toFile());

        final ConfigurationReader configurationReader = new ConfigurationReader(extensionInformation);

        Files.writeString(tempDir.resolve(ConfigurationReader.CONFIG_PATH),
                "discoveryAddress:\n" + "resolutionTimeout:30Seconds",
                StandardOpenOption.CREATE);

        assertThrows(UnsupportedOperationException.class, () -> {
            try {
                configurationReader.get();
                assertEquals(configurationReader.get().resolutionTimeout(), defaultConfig.resolutionTimeout());
            } catch (UnsupportedOperationException e) {
                assertTrue(e.getMessage().contains("Cannot convert '30Seconds' to int"));
                throw e;
            }
        });
    }

    @Test
    public void test_typo_reloadInterval(final @NotNull @TempDir Path tempDir) throws Exception {
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(tempDir.toAbsolutePath().toFile());

        final ConfigurationReader configurationReader = new ConfigurationReader(extensionInformation);

        Files.writeString(tempDir.resolve(ConfigurationReader.CONFIG_PATH),
                "discoveryAddress:\n" + "reloadInterval:30Seconds",
                StandardOpenOption.CREATE);

        assertThrows(UnsupportedOperationException.class, () -> {
            try {
                configurationReader.get();
                assertEquals(configurationReader.get().reloadInterval(), defaultConfig.resolutionTimeout());
            } catch (UnsupportedOperationException e) {
                assertTrue(e.getMessage().contains("Cannot convert '30Seconds' to int"));
                throw e;
            }
        });

    }


}