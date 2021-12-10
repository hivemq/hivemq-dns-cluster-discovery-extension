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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ConfigurationFileReaderTest {

    private @NotNull ConfigurationFileReader configurationFileReader;
    private @NotNull Path configPath;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        configurationFileReader = new ConfigurationFileReader(tempDir.toFile());
        configPath = tempDir.resolve(ConfigurationFileReader.CONFIG_PATH);
    }

    @Test
    void whenNoFile_thenUseDefaults() {
        final DnsDiscoveryConfigFile config = configurationFileReader.get();

        assertEquals(-1, config.getFileResolutionTimeout());
        assertNull(config.getFileDiscoveryAddress());
    }

    @Test
    void whenConfigIsCorrect_thenUseValues() throws Exception {
        Files.writeString(configPath, "discoveryAddress:task.hivemq\nresolutionTimeout:10");

        final DnsDiscoveryConfigFile config = configurationFileReader.get();

        assertEquals(10, config.getFileResolutionTimeout());
        assertEquals("task.hivemq", config.getFileDiscoveryAddress());
    }

    @Test
    void whenTypoInResolutionTimeout_thenThrowException() throws Exception {
        Files.writeString(configPath, "discoveryAddress:\nresolutionTimeout:30Seconds");

        final UnsupportedOperationException e =
                assertThrows(UnsupportedOperationException.class, () -> configurationFileReader.get().getFileResolutionTimeout());
        assertTrue(e.getMessage().contains("Cannot convert '30Seconds' to int"));
    }

    @Test
    void whenTypoInReloadInterval_thenThrowException() throws Exception {
        Files.writeString(configPath, "discoveryAddress:\nreloadInterval:30Seconds");

        final UnsupportedOperationException e =
                assertThrows(UnsupportedOperationException.class, () -> configurationFileReader.get().getFileReloadInterval());
        assertTrue(e.getMessage().contains("Cannot convert '30Seconds' to int"));
    }
}