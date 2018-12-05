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

import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.FileWriter;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConfigurationReaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Mock
    DnsDiscoveryConfigExtended defaultConfig;
    private ConfigurationReader configurationReader;
    @Mock
    private ExtensionInformation extensionInformation;

    @Before
    public void init() {
        initMocks(this);
        when(defaultConfig.resolutionTimeout()).thenReturn(30);
        when(defaultConfig.discoveryAddress()).thenReturn("");

    }


    @Test
    public void test_ReadConfiguration_no_file_use_Defaults() throws Exception {
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(temporaryFolder.getRoot());
        configurationReader = new ConfigurationReader(extensionInformation);
        Assert.assertEquals(configurationReader.get().resolutionTimeout(), defaultConfig.resolutionTimeout());
        Assert.assertEquals(configurationReader.get().discoveryAddress(), null);
    }

    @Test
    public void test_successfully_read_config() throws Exception {
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(temporaryFolder.getRoot());
        FileWriter out = new FileWriter(temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH));
        out.write("discoveryAddress:task.hivemq\n" +
                "resolutionTimeout:10");
        out.flush();
        out.close();

        configurationReader = new ConfigurationReader(extensionInformation);
        configurationReader.get();

        Assert.assertEquals(configurationReader.get().resolutionTimeout(), 10);
        Assert.assertEquals(configurationReader.get().discoveryAddress(), "task.hivemq");

    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_typo_resolutionTimeOut() throws Exception {
        when(extensionInformation.getExtensionHomeFolder()).thenReturn(temporaryFolder.getRoot());
        try (FileWriter out = new FileWriter(temporaryFolder.newFile(ConfigurationReader.CONFIG_PATH))) {

            out.write("discoveryAddress:\n" +
                    "resolutionTimeout:30Seconds");
            out.flush();
            out.close();

            configurationReader = new ConfigurationReader(extensionInformation);
            configurationReader.get();

            Assert.assertEquals(configurationReader.get().resolutionTimeout(), defaultConfig.resolutionTimeout());
        } catch (UnsupportedOperationException e) {
            Assert.assertTrue(e.getMessage().contains("Cannot convert '30Seconds' to int"));
            throw e;
        }
    }

}