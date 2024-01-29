package org.quantil.qprov.web.controller;

import java.sql.SQLException;

import org.quantil.qprov.core.services.SqlService;
import org.quantil.qprov.web.Constants;
import org.quantil.qprov.web.dtos.QubitDto;
import org.quantil.qprov.web.dtos.SqlRequestDto;
import org.quantil.qprov.web.dtos.SqlResultDto;
import org.quantil.qprov.web.dtos.VirtualMachineDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(name = Constants.TAG_SQL)
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PATH_SQL)
@AllArgsConstructor
@Slf4j
public class SqlController {

    protected static final Logger logger = LogManager.getLogger();

    private final SqlService sqlService;

    @Operation(responses = {
            @ApiResponse(responseCode = "200"),},
            description = "Execute the given SQL query in read only mode and return the result from the database.")
    @PostMapping
    public ResponseEntity<EntityModel<SqlResultDto>> executeSQL(@RequestBody SqlRequestDto sqlRequestDto) {
        logger.debug("Executing SQL query: {}", sqlRequestDto.getQuery());

        try {
            String result = sqlService.executeSQLQuery(sqlRequestDto.getQuery());
            logger.debug("Query result: {}", result);

            return new ResponseEntity<>(EntityModel.of(new SqlResultDto(result)), HttpStatus.OK);
        } catch (SQLException e) {
            logger.error("Exception while executing SQL query. State: {}; Message: {}", e.getSQLState(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
