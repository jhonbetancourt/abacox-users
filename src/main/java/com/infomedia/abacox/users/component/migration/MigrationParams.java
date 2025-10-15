package com.infomedia.abacox.users.component.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrationParams {
    private SourceDbConfig sourceDbConfig;
    private List<TableMigrationConfig> tablesToMigrate; // Ordered list
}