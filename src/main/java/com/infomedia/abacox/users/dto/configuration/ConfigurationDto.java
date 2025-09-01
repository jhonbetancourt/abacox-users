package com.infomedia.abacox.users.dto.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ConfigurationDto {
    private Boolean singleSession;
    private Integer sessionMaxAge;
}