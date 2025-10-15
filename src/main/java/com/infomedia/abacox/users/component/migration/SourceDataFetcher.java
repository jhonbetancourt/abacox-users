// File: com/infomedia/abacox/telephonypricing/component/migration/SourceDataFetcher.java
package com.infomedia.abacox.users.component.migration;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Log4j2
public class SourceDataFetcher {

    private static final String SQL_SERVER_PRODUCT_NAME = "Microsoft SQL Server";
    private static final String POSTGRESQL_PRODUCT_NAME = "PostgreSQL";
    private static final String MYSQL_PRODUCT_NAME = "MySQL";
    private static final String ORACLE_PRODUCT_NAME = "Oracle";

    private enum PagingDialect {
        NONE,
        SQL_SERVER_2012, // OFFSET / FETCH
        SQL_SERVER_2005, // ROW_NUMBER()
        POSTGRESQL,      // LIMIT / OFFSET
        MYSQL,           // LIMIT / OFFSET (LIMIT offset, count)
        ORACLE_12C,      // OFFSET / FETCH
        ORACLE_PRE12C    // ROWNUM
    }

    /**
     * Fetches full row data for a specific list of source IDs.
     * This is used in multi-pass strategies where you first discover IDs and then fetch details.
     */
    public List<Map<String, Object>> fetchFullDataForIds(SourceDbConfig config, String tableName, Set<String> columnsToSelect, String sourceIdColumn, List<Object> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        String columnsSql = buildColumnsSql(columnsToSelect, PagingDialect.NONE); // Quoting doesn't matter here as much
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = String.format("SELECT %s FROM %s WHERE %s IN (%s)", columnsSql, tableName, sourceIdColumn, placeholders);

        log.debug("Fetching full data for {} IDs from table {}", ids.size(), tableName);

        try (Connection connection = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
             PreparedStatement ps = connection.prepareStatement(sql)) {

            MigrationUtils.setPreparedStatementParameters(ps, ids);

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                Map<String, Integer> columnIndexMap = buildColumnIndexMap(metaData);
                while (rs.next()) {
                    results.add(extractRow(rs, columnsToSelect, columnIndexMap, PagingDialect.NONE));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to fetch full data for ID batch from table {}: {}", tableName, e.getMessage(), e);
            throw e;
        }
        return results;
    }

    // ... (all other existing methods like buildPagedQuery, etc.) ...
    /**
     * Fetches data from the source table in batches and processes each batch using the provided consumer.
     * Uses database-specific paging if possible, otherwise reads the full result set
     * while still processing in batches on the application side.
     *
     * @param config           Source database configuration.
     * @param tableName        Name of the source table (can include schema like 'dbo.MyTable').
     * @param requestedColumns Columns requested by the migration mapping.
     * @param sourceIdColumn   The column used as the primary identifier in the source table (MUST be orderable for paging).
     * @param orderByClause    Optional clause for ordering results, e.g., "creation_date DESC".
     * @param maxEntriesToMigrate Optional limit on the total number of rows to fetch.
     * @param batchSize        The desired number of rows per batch for processing.
     * @param batchProcessor   A Consumer that will process each List (batch) of fetched rows.
     * @throws SQLException If database access fails or the sourceIdColumn is missing when required for paging.
     */
    public void fetchData(SourceDbConfig config, String tableName, Set<String> requestedColumns, String sourceIdColumn,
                          String orderByClause, Integer maxEntriesToMigrate,
                          int batchSize, Consumer<List<Map<String, Object>>> batchProcessor) throws SQLException {

        long totalRowsFetched = 0;

        log.debug("Loading JDBC driver: {}", config.getDriverClassName());
        try {
            Class.forName(config.getDriverClassName());
        } catch (ClassNotFoundException e) {
            log.error("Could not load JDBC driver: {}", config.getDriverClassName(), e);
            throw new SQLException("JDBC Driver not found: " + config.getDriverClassName(), e);
        }

        log.info("Connecting to source database: {}", config.getUrl());
        try (Connection connection = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword())) {

            // 1. Get Actual Columns & Metadata
            DatabaseMetaData dbMetaData = connection.getMetaData();
            Set<String> actualColumns = getActualSourceColumns(connection, dbMetaData, tableName);
            if (actualColumns.isEmpty()) {
                log.warn("Could not retrieve column metadata or table '{}' has no columns/does not exist.", tableName);
                return; // Nothing to fetch
            }

            // 2. Determine Columns to Select (Intersection, case-insensitive)
            Set<String> requestedUpper = requestedColumns.stream().map(String::toUpperCase).collect(Collectors.toSet());
            Set<String> columnsToSelect = actualColumns.stream()
                    .filter(actualCol -> requestedUpper.contains(actualCol.toUpperCase()))
                    .collect(Collectors.toSet());

            // 3. Log skipped columns
            logSkippedColumns(requestedColumns, columnsToSelect, tableName);

            // 4. Validate Essential ID Column (and ensure it's included in selection)
            String actualSourceIdColumn = null;
            if (sourceIdColumn != null && !sourceIdColumn.trim().isEmpty()) {
                actualSourceIdColumn = validateAndGetActualIdColumn(actualColumns, sourceIdColumn, tableName);
                columnsToSelect.add(actualSourceIdColumn); // Ensure the ID column is always selected if provided
            } else {
                log.debug("No sourceIdColumn provided for table '{}'. Paging will be disabled if attempted.", tableName);
            }

            // Also add any columns from a custom order by clause to the selection
            if (orderByClause != null && !orderByClause.trim().isEmpty()) {
                String[] orderByParts = orderByClause.split(",");
                for (String part : orderByParts) {
                    String columnNameCandidate = part.trim().split("\\s+")[0];
                    // Remove potential quotes to perform a case-insensitive match
                    String cleanCandidate = columnNameCandidate.replaceAll("[\\[\\]`\"]", "");
                    if (!cleanCandidate.isEmpty()) {
                        // Find the actual case-sensitive column name from the list of columns we know exist
                        String actualCol = actualColumns.stream()
                                .filter(c -> c.equalsIgnoreCase(cleanCandidate))
                                .findFirst()
                                .orElse(null);
                        if (actualCol != null) {
                            columnsToSelect.add(actualCol);
                            log.debug("Ensuring orderBy column '{}' (from clause '{}') is selected.", actualCol, columnNameCandidate);
                        } else {
                            log.warn("Column '{}' from orderBy clause not found in source table '{}' and will not be added to SELECT list. The ORDER BY may fail.", cleanCandidate, tableName);
                        }
                    }
                }
            }


            // 5. Proceed only if columns exist
            if (columnsToSelect.isEmpty()) {
                log.warn("No requested columns found in source table '{}'. Skipping fetch.", tableName);
                return;
            }

            // 6. Determine Database Dialect and Paging Strategy
            String dbProductName = "";
            int dbMajorVersion = 0;
            int dbMinorVersion = 0;
            PagingDialect dialect = PagingDialect.NONE;
            boolean usePaging = false;

            try {
                dbProductName = dbMetaData.getDatabaseProductName();
                dbMajorVersion = dbMetaData.getDatabaseMajorVersion();
                dbMinorVersion = dbMetaData.getDatabaseMinorVersion();
                log.info("Detected database: {} Version: {}.{}", dbProductName, dbMajorVersion, dbMinorVersion);

                if (SQL_SERVER_PRODUCT_NAME.equalsIgnoreCase(dbProductName)) {
                    if (dbMajorVersion >= 11) { // SQL Server 2012+
                        dialect = PagingDialect.SQL_SERVER_2012;
                        usePaging = true;
                        log.info("Using SQL Server 2012+ (OFFSET/FETCH) paging.");
                    } else if (dbMajorVersion >= 9) { // SQL Server 2005, 2008, 2008 R2
                        dialect = PagingDialect.SQL_SERVER_2005;
                        usePaging = true;
                        log.info("Using SQL Server 2005-2008 (ROW_NUMBER) paging.");
                    } else {
                        log.warn("Older SQL Server version ({}) detected. Server-side paging not supported, fetching all results.", dbMajorVersion);
                    }
                } else if (POSTGRESQL_PRODUCT_NAME.equalsIgnoreCase(dbProductName)) {
                    if (dbMajorVersion >= 8) { // LIMIT/OFFSET generally available from 8.x, standard in 9.x+
                        dialect = PagingDialect.POSTGRESQL;
                        usePaging = true;
                        log.info("Using PostgreSQL (LIMIT/OFFSET) paging.");
                    } else {
                        log.warn("Older PostgreSQL version ({}) detected. Server-side paging might not be reliable, fetching all results.", dbMajorVersion);
                    }
                } else if (MYSQL_PRODUCT_NAME.equalsIgnoreCase(dbProductName)) {
                    if (dbMajorVersion >= 4) { // LIMIT/OFFSET available in MySQL 4.x+, very standard in 5.x+
                        dialect = PagingDialect.MYSQL;
                        usePaging = true;
                        log.info("Using MySQL (LIMIT/OFFSET) paging.");
                    } else {
                        log.warn("Very old MySQL version ({}) detected. Server-side paging might not be reliable, fetching all results.", dbMajorVersion);
                    }
                } else if (ORACLE_PRODUCT_NAME.equalsIgnoreCase(dbProductName)) {
                    // Oracle 12c (12.1) introduced standard OFFSET/FETCH
                    if (dbMajorVersion > 12 || (dbMajorVersion == 12 && dbMinorVersion >= 1)) {
                        dialect = PagingDialect.ORACLE_12C;
                        usePaging = true;
                        log.info("Using Oracle 12c+ (OFFSET/FETCH) paging.");
                    } else if (dbMajorVersion >= 9) { // ROWNUM method works on 9i, 10g, 11g
                        dialect = PagingDialect.ORACLE_PRE12C;
                        usePaging = true;
                        log.info("Using Oracle 9i-11g (ROWNUM) paging.");
                    } else {
                        log.warn("Older Oracle version ({}) detected. Server-side paging not supported, fetching all results.", dbMajorVersion);
                    }
                } else {
                    log.warn("Unknown database product: '{}'. Falling back to fetching all results (processing in batches). Consider adding specific paging support.", dbProductName);
                    dialect = PagingDialect.NONE;
                    usePaging = false;
                }

            } catch (SQLException e) {
                log.warn("Could not reliably determine database metadata. Falling back to fetching all results.", e);
                dialect = PagingDialect.NONE;
                usePaging = false;
            }

            // Ensure sourceIdColumn is suitable for ORDER BY if paging is attempted
            if (usePaging && actualSourceIdColumn == null) {
                log.error("CRITICAL: Server-side paging for dialect {} requires a valid 'sourceIdColumn' for ordering, but it's missing or empty in config for table '{}'. Disabling paging.", dialect, tableName);
                usePaging = false;
                dialect = PagingDialect.NONE;
                // DO NOT proceed with paging if order column is invalid.
            }

            // --- Build Final Order By Clause ---
            String finalOrderByClause;
            if (usePaging) {
                String quotedIdColumn = quoteIdentifier(actualSourceIdColumn, dialect);
                if (orderByClause != null && !orderByClause.trim().isEmpty()) {
                    finalOrderByClause = orderByClause;
                    // Append unique ID column as tie-breaker to ensure stable paging, if not already present
                    if (!finalOrderByClause.toLowerCase().contains(actualSourceIdColumn.toLowerCase())) {
                        finalOrderByClause += ", " + quotedIdColumn + " ASC";
                        log.debug("Appended unique ID column '{}' as tie-breaker to custom order by clause.", actualSourceIdColumn);
                    }
                } else {
                    // Default ordering for paging
                    finalOrderByClause = quotedIdColumn + " ASC";
                }
            } else {
                // No paging, just use the clause if provided, otherwise no ordering
                finalOrderByClause = orderByClause;
            }
            log.info("Using effective ORDER BY clause: {}", finalOrderByClause != null ? finalOrderByClause : "[None]");


            final PagingDialect effectiveDialect = dialect; // Final variable for use in helpers
            final boolean effectivelyUsePaging = usePaging; // Final variable

            // --- Batch Fetching Loop ---
            int offset = 0;
            boolean moreData = true;
            String columnsSql = buildColumnsSql(columnsToSelect, effectiveDialect);

            while (moreData) {
                // --- Enforce maxEntriesToMigrate limit ---
                int currentBatchSize = batchSize;
                if (maxEntriesToMigrate != null && maxEntriesToMigrate > 0) {
                    if (totalRowsFetched >= maxEntriesToMigrate) {
                        log.info("Reached max entries limit ({}). Stopping fetch.", maxEntriesToMigrate);
                        moreData = false;
                        continue; // break the loop
                    }
                    int remaining = maxEntriesToMigrate - (int) totalRowsFetched;
                    currentBatchSize = Math.min(batchSize, remaining);
                }

                String sql = buildPagedQuery(effectiveDialect, tableName, columnsSql, finalOrderByClause, offset, currentBatchSize);
                log.debug("Executing query for batch (Dialect: {}): {}", effectiveDialect, sql);

                List<Map<String, Object>> currentBatch = new ArrayList<>(currentBatchSize);
                int rowsInCurrentQuery = 0;

                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    if (effectivelyUsePaging) {
                        setPagingParameters(preparedStatement, effectiveDialect, offset, currentBatchSize);
                    }

                    // Set fetch size hint regardless of server-side paging
                    preparedStatement.setFetchSize(currentBatchSize);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        Map<String, Integer> columnIndexMap = buildColumnIndexMap(metaData);

                        while (resultSet.next()) {
                            Map<String, Object> row = extractRow(resultSet, columnsToSelect, columnIndexMap, effectiveDialect);
                            currentBatch.add(row);
                            rowsInCurrentQuery++;
                            totalRowsFetched++;

                            // Application-level batching if server-side paging is disabled
                            if (!effectivelyUsePaging && currentBatch.size() >= batchSize) {
                                log.debug("Processing application-level batch of size {}", currentBatch.size());
                                batchProcessor.accept(new ArrayList<>(currentBatch)); // Process copy
                                currentBatch.clear();
                            }
                        } // End result set iteration
                    } // ResultSet closed
                } // PreparedStatement closed

                // Process the current batch (either full page or application-level batch)
                if (!currentBatch.isEmpty()) {
                    log.debug("Processing {} batch of size {}", effectivelyUsePaging ? "server-paged" : "final application", currentBatch.size());
                    batchProcessor.accept(new ArrayList<>(currentBatch)); // Process copy
                    currentBatch.clear(); // Good practice
                }

                // Determine if more data exists based on paging strategy
                if (effectivelyUsePaging) {
                    if (rowsInCurrentQuery < currentBatchSize) {
                        moreData = false; // Fetched less than requested, must be the last page
                        log.debug("Last page detected (fetched {} rows, requested {}).", rowsInCurrentQuery, currentBatchSize);
                    } else {
                        offset += rowsInCurrentQuery; // Move to the next page
                        log.debug("Fetched full page ({} rows), preparing for next offset {}.", rowsInCurrentQuery, offset);
                    }
                } else {
                    moreData = false; // If not using paging, we only execute the query once
                }

                // Log progress periodically based on total rows
                if (totalRowsFetched > 0 && (totalRowsFetched % (batchSize * 10) == 0 || !moreData)) { // Log every 10 batches or at the end
                    log.info("Fetched {} total rows from source table {}...", totalRowsFetched, tableName);
                }

            } // End while(moreData) loop

            log.info("Finished fetching data for source table {}. Total rows fetched: {}", tableName, totalRowsFetched);

        } catch (SQLException e) {
            log.error("Error during data fetch lifecycle for source table {}: {}", tableName, e.getMessage(), e);
            throw e; // Re-throw to be handled by the migration service
        }
    }

    private String buildColumnsSql(Set<String> columnsToSelect, PagingDialect dialect) {
        return columnsToSelect.stream()
                .map(col -> quoteIdentifier(col, dialect))
                .collect(Collectors.joining(", "));
    }

    private String buildPagedQuery(PagingDialect dialect, String tableName, String columnsSql,
                                   String orderByClause, int offset, int limit) {

        String safeTableName = quoteIdentifier(tableName, dialect);

        if (dialect != PagingDialect.NONE && (orderByClause == null || orderByClause.trim().isEmpty())) {
            throw new IllegalStateException("ORDER BY clause is required for paging dialect " + dialect + " but was not provided or resolved.");
        }

        switch (dialect) {
            case SQL_SERVER_2012:
                return String.format("SELECT %s FROM %s ORDER BY %s OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        columnsSql, safeTableName, orderByClause);
            case SQL_SERVER_2005:
                return String.format("WITH NumberedRows AS (SELECT %s, ROW_NUMBER() OVER (ORDER BY %s) AS rn FROM %s) " +
                                "SELECT %s FROM NumberedRows WHERE rn > ? AND rn <= ?",
                        columnsSql, orderByClause, safeTableName, columnsSql);
            case POSTGRESQL:
                return String.format("SELECT %s FROM %s ORDER BY %s LIMIT ? OFFSET ?",
                        columnsSql, safeTableName, orderByClause);
            case MYSQL:
                return String.format("SELECT %s FROM %s ORDER BY %s LIMIT ?, ?",
                        columnsSql, safeTableName, orderByClause);
            case ORACLE_12C:
                return String.format("SELECT %s FROM %s ORDER BY %s OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        columnsSql, safeTableName, orderByClause);
            case ORACLE_PRE12C:
                return String.format("SELECT %s FROM ( SELECT inner_.*, ROWNUM rnum FROM ( SELECT %s FROM %s ORDER BY %s ) inner_ WHERE ROWNUM <= ? ) WHERE rnum > ?",
                        columnsSql, columnsSql, safeTableName, orderByClause);
            case NONE:
            default:
                String sql = String.format("SELECT %s FROM %s", columnsSql, safeTableName);
                if (orderByClause != null && !orderByClause.trim().isEmpty()) {
                    sql += " ORDER BY " + orderByClause;
                }
                return sql;
        }
    }

    private void setPagingParameters(PreparedStatement ps, PagingDialect dialect, int offset, int limit) throws SQLException {
        switch (dialect) {
            case SQL_SERVER_2012: // OFFSET ?, FETCH ?
            case ORACLE_12C:      // OFFSET ?, FETCH NEXT ?
                ps.setInt(1, offset);
                ps.setInt(2, limit);
                break;
            case SQL_SERVER_2005: // WHERE rn > ? AND rn <= ?
                ps.setInt(1, offset);
                ps.setInt(2, offset + limit);
                break;
            case POSTGRESQL:      // LIMIT ?, OFFSET ?
                ps.setInt(1, limit);
                ps.setInt(2, offset);
                break;
            case MYSQL:           // LIMIT ?, ? (offset, count)
                ps.setInt(1, offset);
                ps.setInt(2, limit);
                break;
            case ORACLE_PRE12C:   // WHERE ROWNUM <= ?, WHERE rnum > ?
                ps.setInt(1, offset + limit); // Outer ROWNUM condition
                ps.setInt(2, offset);         // Inner rnum condition
                break;
            case NONE:
            default:
                break;
        }
    }

    private String quoteIdentifier(String identifier, PagingDialect dialect) {
        if (identifier == null || identifier.isEmpty()) {
            return identifier;
        }
        String quoteCharStart;
        String quoteCharEnd;

        switch (dialect) {
            case SQL_SERVER_2012:
            case SQL_SERVER_2005:
                quoteCharStart = "[";
                quoteCharEnd = "]";
                break;
            case MYSQL:
                quoteCharStart = "`";
                quoteCharEnd = "`";
                break;
            case POSTGRESQL:
            case ORACLE_12C:
            case ORACLE_PRE12C:
            case NONE: // Default to standard SQL quotes
            default:
                quoteCharStart = "\"";
                quoteCharEnd = "\"";
                break;
        }

        if (identifier.startsWith(quoteCharStart) && identifier.endsWith(quoteCharEnd)) {
            return identifier;
        }

        if (identifier.contains(".")) {
            String[] parts = identifier.split("\\.", 2);
            String part1 = parts[0].startsWith(quoteCharStart) ? parts[0] : quoteCharStart + parts[0] + quoteCharEnd;
            String part2 = parts[1].startsWith(quoteCharStart) ? parts[1] : quoteCharStart + parts[1] + quoteCharEnd;
            return part1 + "." + part2;
        } else {
            return quoteCharStart + identifier + quoteCharEnd;
        }
    }

    private void logSkippedColumns(Set<String> requestedColumns, Set<String> columnsToSelect, String tableName) {
        Set<String> selectedUpper = columnsToSelect.stream().map(String::toUpperCase).collect(Collectors.toSet());
        requestedColumns.forEach(requestedCol -> {
            if (!selectedUpper.contains(requestedCol.toUpperCase())) {
                log.warn("Requested column '{}' not found in source table '{}' (case-insensitive check) and will be skipped.", requestedCol, tableName);
            }
        });
    }

    private String validateAndGetActualIdColumn(Set<String> actualColumns, String requestedSourceIdColumn, String tableName) throws SQLException {
        final String requestedUpper = requestedSourceIdColumn.toUpperCase();
        Optional<String> actualIdColumnOpt = actualColumns.stream()
                .filter(col -> col.toUpperCase().equals(requestedUpper))
                .findFirst();

        if (actualIdColumnOpt.isEmpty()) {
            log.error("CRITICAL: Configured source ID column '{}' does not exist (case-insensitive) in source table '{}'. Cannot use for paging.", requestedSourceIdColumn, tableName);
            throw new SQLException("Source ID column '" + requestedSourceIdColumn + "' not found in table '" + tableName + "'.");
        }
        String actualCol = actualIdColumnOpt.get();
        log.debug("Validated source ID column: requested='{}', actual='{}'", requestedSourceIdColumn, actualCol);
        return actualCol;
    }

    private Map<String, Integer> buildColumnIndexMap(ResultSetMetaData metaData) throws SQLException {
        Map<String, Integer> columnIndexMap = new HashMap<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String label = metaData.getColumnLabel(i);
            if (label == null || label.isEmpty()) {
                label = metaData.getColumnName(i);
            }
            columnIndexMap.put(label.toUpperCase(), i);
        }
        return columnIndexMap;
    }

    private Map<String, Object> extractRow(ResultSet resultSet, Set<String> columnsToSelect,
                                           Map<String, Integer> columnIndexMap, PagingDialect dialect) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        for (String selectedColumn : columnsToSelect) {
            Integer index = columnIndexMap.get(selectedColumn.toUpperCase());
            if (index != null) {
                Object value = resultSet.getObject(index);
                value = convertSqlTypes(value, dialect);
                row.put(selectedColumn, value);
            } else {
                log.warn("Column '{}' was expected in ResultSet based on selection but not found by label/name in metadata mapping. Skipping column for this row.", selectedColumn);
            }
        }
        return row;
    }

    private Object convertSqlTypes(Object value, PagingDialect dialect) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        } else if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        } else if (value instanceof Time) {
            return ((Time) value).toLocalTime();
        }
        return value;
    }

    private Set<String> getActualSourceColumns(Connection connection, DatabaseMetaData metaData, String tableName) throws SQLException {
        Set<String> columnNames = new HashSet<>();
        String catalog = null;
        String schemaPattern = null;
        String actualTableName = tableName;

        try {
            catalog = connection.getCatalog();
        } catch (SQLException e) {
            log.warn("Could not retrieve catalog name: {}", e.getMessage());
        }

        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            schemaPattern = parts[0];
            actualTableName = parts[1];
            log.debug("Using explicit schema pattern '{}' and table name '{}'", schemaPattern, actualTableName);
        } else {
            log.debug("No schema specified for table '{}', using driver default schema pattern (null).", actualTableName);
        }

        try (ResultSet rs = metaData.getColumns(catalog, schemaPattern, actualTableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                if (columnName != null) {
                    columnNames.add(columnName);
                }
            }
            log.debug("Found columns for table '{}' (Catalog: {}, Schema: '{}'): {}", tableName, catalog, schemaPattern != null ? schemaPattern : "default", columnNames);
        } catch (SQLException e) {
            log.error("Could not retrieve column metadata for table '{}' (Catalog: {}, Schema: {}, Table: {}): {}",
                    tableName, catalog, schemaPattern, actualTableName, e.getMessage());
            throw e;
        }

        if (columnNames.isEmpty() && schemaPattern != null) {
            log.warn("No columns found with schema pattern '{}' for table '{}'. Retrying without schema pattern (using default).", schemaPattern, actualTableName);
            try (ResultSet rs = metaData.getColumns(catalog, null, actualTableName, null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    if (columnName != null) {
                        columnNames.add(columnName);
                    }
                }
                log.debug("Found columns for table '{}' (without explicit schema pattern): {}", tableName, columnNames);
            } catch (SQLException e) {
                log.error("Retry without schema pattern failed for table '{}': {}", tableName, e.getMessage());
            }
        }

        if (columnNames.isEmpty()) {
            log.warn("No columns found for table '{}' using schema pattern '{}'. Ensure table exists and is accessible.", actualTableName, schemaPattern != null ? schemaPattern : "default/null");
        }

        return columnNames;
    }
}