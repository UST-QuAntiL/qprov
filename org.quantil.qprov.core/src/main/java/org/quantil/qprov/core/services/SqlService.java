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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SqlService {

    protected static final Logger logger = LogManager.getLogger();

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUser;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

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
        Connection connection = DriverManager.getConnection(datasourceUrl + "?user=" + datasourceUser + "&password=" + datasourcePassword);
        connection.setReadOnly(true);

        // retrieve results from database
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        connection.close();
        logger.debug("Retrieved result set from database...");

        // parse result to JSON including meta data (see https://www.baeldung.com/java-jdbc-convert-resultset-to-json)
        ResultSetMetaData md = resultSet.getMetaData();
        int numCols = md.getColumnCount();
        List<String> colNames = IntStream.range(0, numCols)
                .mapToObj(i -> {
                    try {
                        return md.getColumnName(i + 1);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return "?";
                    }
                })
                .toList();

        JsonArray resultJson = new JsonArray();
        while (resultSet.next()) {
            JsonObject row = new JsonObject();
            colNames.forEach(cn -> {
                try {
                    if (Objects.nonNull(resultSet.getObject(cn))) {
                        logger.debug("Adding entry: Key: {}, Value: {}", cn, resultSet.getObject(cn).toString());
                        if (resultSet.getObject(cn) instanceof String || resultSet.getObject(cn) instanceof Timestamp) {
                            row.addProperty(cn, resultSet.getObject(cn).toString());
                        } else {
                            row.add(cn, JsonParser.parseString(resultSet.getObject(cn).toString()));
                        }
                    } else {
                        logger.warn("Adding entry with null value for key: {}", cn);
                        row.addProperty(cn, "null");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            resultJson.add(row);
        }
        logger.debug("Query result: {}", resultJson);
       return resultJson.toString();
    }
}
