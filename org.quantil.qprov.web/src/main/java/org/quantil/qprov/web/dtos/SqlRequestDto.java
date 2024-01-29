package org.quantil.qprov.web.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for SQL requests.
 */
@EqualsAndHashCode
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlRequestDto {

    private String query;
}
