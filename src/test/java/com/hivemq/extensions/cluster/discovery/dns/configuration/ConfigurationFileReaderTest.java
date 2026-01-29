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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationFileReaderTest {

    private @NotNull ConfigurationFileReader configurationFileReader;
    private @NotNull Path configPath;

    @TempDir
    private @NotNull Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        configurationFileReader = new ConfigurationFileReader(tempDir.toFile());
        configPath = tempDir.resolve(ConfigurationFileReader.CONFIG_PATH);
        Files.createDirectories(configPath.getParent());
    }

    @Test
    void whenNoFile_thenUseDefaults() {
        final var config = configurationFileReader.get();
        assertThat(config.getFileResolutionTimeout()).isEqualTo(-1);
        assertThat(config.getFileDiscoveryAddress()).isNull();
    }

    @Test
    void whenConfigIsCorrect_thenUseValues() throws Exception {
        Files.writeString(configPath, """
                discoveryAddress:task.hivemq
                resolutionTimeout:10""");

        final var config = configurationFileReader.get();
        assertThat(config.getFileResolutionTimeout()).isEqualTo(10);
        assertThat(config.getFileDiscoveryAddress()).isEqualTo("task.hivemq");
    }

    @Test
    void whenTypoInResolutionTimeout_thenThrowException() throws Exception {
        Files.writeString(configPath, """
                discoveryAddress:
                resolutionTimeout:30Seconds""");

        assertThatThrownBy(() -> configurationFileReader.get().getFileResolutionTimeout()).isInstanceOf(
                UnsupportedOperationException.class).hasMessageContaining("Cannot convert '30Seconds' to int");
    }

    @Test
    void whenTypoInReloadInterval_thenThrowException() throws Exception {
        Files.writeString(configPath, """
                discoveryAddress:
                reloadInterval:30Seconds""");

        assertThatThrownBy(() -> configurationFileReader.get().getFileReloadInterval()).isInstanceOf(
                UnsupportedOperationException.class).hasMessageContaining("Cannot convert '30Seconds' to int");
    }

    @Test
    void whenConfigAtLegacyLocation_thenUseValues() throws Exception {
        final var legacyPath = tempDir.resolve(ConfigurationFileReader.LEGACY_CONFIG_PATH);
        Files.writeString(legacyPath, """
                discoveryAddress:legacy.hivemq
                resolutionTimeout:20""");

        // create a new reader so ConfigResolver picks up the legacy file
        final var legacyReader = new ConfigurationFileReader(tempDir.toFile());
        final var config = legacyReader.get();
        assertThat(config.getFileResolutionTimeout()).isEqualTo(20);
        assertThat(config.getFileDiscoveryAddress()).isEqualTo("legacy.hivemq");
    }
}
