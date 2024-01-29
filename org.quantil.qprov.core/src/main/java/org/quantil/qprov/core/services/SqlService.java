/*******************************************************************************
 * Copyright (c) 2024 the QProv contributors.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
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
 *******************************************************************************/

package org.quantil.qprov.core.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SqlService {

    protected static final Logger logger = LogManager.getLogger();

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    /**
     * Execute a given SQL query in read only mode against the specified provenance database
     *
     * @param query the SQL query to execute
     * @return the result of the SQL query
     * @throws SQLException if the given SQL query or database configuration is invalid
     */
    public String executeSQLQuery(String query) throws SQLException {
        logger.debug("Creating connection for datasource at URL: {}", datasourceUrl);

        // create a read only connection to execute select statements
        Connection connection = DriverManager.getConnection(datasourceUrl);
        connection.setReadOnly(true);

        // TODO

       return "TODO";
    }
}
