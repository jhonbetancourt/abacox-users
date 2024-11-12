package com.infomedia.abacox.users.dto.superclass;

import com.infomedia.abacox.users.constants.DateTimePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class AuditedDto {
    @JsonFormat(pattern = DateTimePattern.DATE_TIME)
    @Schema(description = "fecha de creación", example = "2021-08-01T00:00:00")
    LocalDateTime createdDate;
    @Schema(description = "creado por", example = "admin")
    String createdBy;
    @JsonFormat(pattern = DateTimePattern.DATE_TIME)
    @Schema(description = "fecha de modificación", example = "2021-08-01T00:00:00")
    LocalDateTime lastModifiedDate;
    @Schema(description = "modificado por", example = "admin")
    String lastModifiedBy;
}
