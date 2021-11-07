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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurationReaderTest {

    private @NotNull ConfigurationReader configurationReader;
    private @NotNull Path configPath;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        final ExtensionInformation extensionInformation = mock(ExtensionInformation.class);
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(tempDir.toFile());
        configurationReader = new ConfigurationReader(extensionInformation);
        configPath = tempDir.resolve(ConfigurationReader.CONFIG_PATH);
    }

    @Test
    void whenNoFile_thenUseDefaults() {
        final DnsDiscoveryConfig config = configurationReader.get();

        assertEquals(30, config.resolutionTimeout());
        assertNull(config.discoveryAddress());
    }

    @Test
    void whenConfigIsCorrect_thenUseValues() throws Exception {
        Files.writeString(configPath, "discoveryAddress:task.hivemq\nresolutionTimeout:10");

        final DnsDiscoveryConfig config = configurationReader.get();

        assertEquals(10, config.resolutionTimeout());
        assertEquals("task.hivemq", config.discoveryAddress());
    }

    @Test
    void whenTypoInResolutionTimeout_thenThrowException() throws Exception {
        Files.writeString(configPath, "discoveryAddress:\nresolutionTimeout:30Seconds");

        final UnsupportedOperationException e =
                assertThrows(UnsupportedOperationException.class, () -> configurationReader.get().resolutionTimeout());
        assertTrue(e.getMessage().contains("Cannot convert '30Seconds' to int"));
    }

    @Test
    void whenTypoInReloadInterval_thenThrowException() throws Exception {
        Files.writeString(configPath, "discoveryAddress:\nreloadInterval:30Seconds");

        final UnsupportedOperationException e =
                assertThrows(UnsupportedOperationException.class, () -> configurationReader.get().reloadInterval());
        assertTrue(e.getMessage().contains("Cannot convert '30Seconds' to int"));
    }
}