// File: com/infomedia/abacox/users/component/migration/TableMigrationConfig.java
package com.infomedia.abacox.users.component.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableMigrationConfig {

    // --- Core Fields ---
    private String sourceTableName;

    /**
     * A set of source column names to be selected in the fetch query.
     * This is the preferred way to specify columns for fetchOnly mode.
     * If this is provided, `columnMapping` will be ignored for column selection.
     */
    private Set<String> columnsToFetch;

    // --- Optional/Standard Mode Fields ---
    /**
     * (Optional) Informational only for fetchOnly mode. Required for standard migration.
     */
    private String targetEntityClassName;

    /**
     * (Optional) The primary identifier column in the source table.
     * Highly recommended if server-side paging is desired to ensure stable ordering.
     */
    private String sourceIdColumnName;
    private String targetIdFieldName;
    private Map<String, String> columnMapping;

    // --- Fetch Control Fields ---
    private String orderByClause;
    private Integer maxEntriesToMigrate;

    // --- Fetch-Only Mode Fields ---
    /**
     * If true, the executor will only fetch data and pass it to the provided processors.
     * It will NOT attempt to insert any data into the target database itself.
     */
    @Builder.Default
    private boolean fetchOnly = false;

    /**
     * A custom consumer to process a single row (as a Map) when fetchOnly is true.
     */
    private Consumer<Map<String, Object>> rowProcessor;

    /**
     * A custom consumer to process a batch of rows (as a List of Maps) when fetchOnly is true.
     * If both rowProcessor and batchProcessor are provided, batchProcessor takes precedence.
     */
    private Consumer<List<Map<String, Object>>> batchProcessor;


    // --- Advanced Standard Mode Fields (Ignored in fetchOnly mode) ---
    private Runnable postMigrationSuccessAction;

    @Builder.Default
    private boolean selfReferencing = false;
    private String selfReferenceSourceParentIdColumn;
    private String selfReferenceTargetForeignKeyFieldName;

    @Builder.Default
    private boolean treatZeroIdAsNullForForeignKeys = true;

    @Builder.Default
    private boolean processHistoricalActiveness = false;
    private String sourceHistoricalControlIdColumn;
    private String sourceValidFromDateColumn;
}