package org.quantil.qprov.web.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data transfer object for results of SQL requests.
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
public class SqlResultDto {

    private String result;
}
