/*
 * Copyright 2020-2024 Hichem BOURADA and other authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jeeware.cloud.lock4j.spring.autoconfigure;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.StreamUtils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Create the lock table and if required upsert function by executing sql schema
 * script at initialization.
 * 
 * @author hbourada
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcLockRepositoryInitializer implements InitializingBean {

    private static final String PLATFORM_ALL = "all";

    private static final String PLATFORM_PLACEHOLDER = "@@platform@@";

    private static final String TABLE_PLACEHOLDER = "@@table@@";

    private static final String FUNCTION_PLACEHOLDER = "@@function@@";

    @NonNull
    protected final DataSource dataSource;

    @NonNull
    protected final ResourceLoader resourceLoader;

    @NonNull
    protected final DistributedLockProperties properties;

    @Override
    public void afterPropertiesSet() throws Exception {
        DistributedLockProperties.Jdbc jdbc = properties.getJdbc();
        Charset charset = jdbc.getScriptCharset();
        Resource resource = findSchemaResource();
        String script;
        try (InputStream inputStream = resource.getInputStream()) {
            script = StreamUtils.copyToString(inputStream, charset);
        }
        script = replaceSchemaScript(script);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setScripts(new ByteArrayResource(script.getBytes(charset)));
        populator.setSqlScriptEncoding(charset.name());
        populator.setContinueOnError(jdbc.isScriptContinueOnError());
        populator.setSeparator(jdbc.getScriptSeparator());
        DatabasePopulatorUtils.execute(populator, this.dataSource);
        log.info("Successfully executed script resource [{}]", resource);
    }

    protected Resource findSchemaResource() {
        String location = properties.getJdbc().getSchemaLocation();
        if (location.contains(PLATFORM_PLACEHOLDER)) {
            String path = location.replace(PLATFORM_PLACEHOLDER, getDatabaseName());
            Resource resource = resourceLoader.getResource(path);
            if (resource.exists()) {
                return resource;
            }
            return resourceLoader.getResource(location.replace(PLATFORM_PLACEHOLDER, PLATFORM_ALL));
        }
        return resourceLoader.getResource(location);
    }

    protected String replaceSchemaScript(String script) {
        DistributedLockProperties.Jdbc jdbc = properties.getJdbc();
        String replacement = script;
        if (script.contains(TABLE_PLACEHOLDER)) {
            replacement = script.replace(TABLE_PLACEHOLDER, jdbc.getTableName());
        }
        if (script.contains(FUNCTION_PLACEHOLDER)) {
            replacement = replacement
                    .replace(FUNCTION_PLACEHOLDER, jdbc.getFunctionName());
        }
        return replacement;
    }

    protected String getDatabaseName() {
        try {
            String productName = JdbcUtils.commonDatabaseName(
                    JdbcUtils.extractDatabaseMetaData(this.dataSource, "getDatabaseProductName").toString());
            DatabaseDriver databaseDriver = DatabaseDriver.fromProductName(productName);
            if (databaseDriver == DatabaseDriver.UNKNOWN) {
                throw new IllegalStateException("Unable to detect database type");
            }
            return databaseDriver.getId();
        } catch (MetaDataAccessException ex) {
            throw new IllegalStateException("Unable to detect database type", ex);
        }
    }
}
