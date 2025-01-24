package com.infomedia.abacox.users.component.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class GenericExcelGenerator {

    private static final Set<Class<?>> SIMPLE_TYPES = new HashSet<>(Arrays.asList(
            String.class, Boolean.class, boolean.class,
            Integer.class, int.class, Long.class, long.class,
            Double.class, double.class, Float.class, float.class,
            LocalDate.class, LocalDateTime.class, UUID.class
    ));

    private static class FieldInfo {
        Field field;
        String displayName;
        Field[] fieldPath;

        FieldInfo(Field field, String displayName, Field[] fieldPath) {
            this.field = field;
            this.displayName = displayName;
            this.fieldPath = fieldPath;
        }
    }

    public static <T> InputStream generateExcelInputStream(List<T> entities) throws IOException {
        return generateExcelInputStream(entities, Collections.emptySet(), Collections.emptyMap(), null);
    }

    public static <T> InputStream generateExcelInputStream(List<T> entities, Set<String> excludedFields) throws IOException {
        return generateExcelInputStream(entities, excludedFields, Collections.emptyMap(), null);
    }

    public static <T> InputStream generateExcelInputStream(List<T> entities, Set<String> excludedFields, List<String> alternativeHeaders) throws IOException {
        return generateExcelInputStream(entities, excludedFields, Collections.emptyMap(), alternativeHeaders);
    }

    public static <T> InputStream generateExcelInputStream(List<T> entities, Map<String, String> alternativeNames) throws IOException {
        return generateExcelInputStream(entities, Collections.emptySet(), alternativeNames, null);
    }

    public static <T> InputStream generateExcelInputStream(List<T> entities, List<String> alternativeHeaders) throws IOException {
        return generateExcelInputStream(entities, Collections.emptySet(), Collections.emptyMap(), alternativeHeaders);
    }

    public static <T> InputStream generateExcelInputStream(List<T> entities, Set<String> excludedFields,
                                                         Map<String, String> alternativeNames) throws IOException {
        return generateExcelInputStream(entities, excludedFields, alternativeNames, null);
    }

    public static <T> InputStream generateExcelInputStream(List<T> entities, Set<String> excludedFields,
                                                         Map<String, String> alternativeNames, List<String> alternativeHeaders) throws IOException {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entity list cannot be empty");
        }

        Class<?> entityClass = entities.get(0).getClass();
        List<FieldInfo> fields = getExportableFields(entityClass, "", null, excludedFields, alternativeNames);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(entityClass.getSimpleName());

        createHeaderRow(workbook, sheet, fields, alternativeHeaders);
        createDataRows(sheet, entities, fields);
        setColumnWidths(sheet, fields.size());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public static <T> void generateExcel(List<T> entities, String filePath) throws IOException {
        generateExcel(entities, filePath, Collections.emptySet(), Collections.emptyMap(), null);
    }

    public static <T> void generateExcel(List<T> entities, String filePath, Set<String> excludedFields) throws IOException {
        generateExcel(entities, filePath, excludedFields, Collections.emptyMap(), null);
    }

    public static <T> void generateExcel(List<T> entities, String filePath, Map<String, String> alternativeNames) throws IOException {
        generateExcel(entities, filePath, Collections.emptySet(), alternativeNames, null);
    }

    public static <T> void generateExcel(List<T> entities, String filePath, List<String> alternativeHeaders) throws IOException {
        generateExcel(entities, filePath, Collections.emptySet(), Collections.emptyMap(), alternativeHeaders);
    }

    public static <T> void generateExcel(List<T> entities, String filePath, Set<String> excludedFields,
                                       Map<String, String> alternativeNames) throws IOException {
        generateExcel(entities, filePath, excludedFields, alternativeNames, null);
    }

    public static <T> void generateExcel(List<T> entities, String filePath, Set<String> excludedFields,
                                       Map<String, String> alternativeNames, List<String> alternativeHeaders) throws IOException {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entity list cannot be empty");
        }

        Class<?> entityClass = entities.get(0).getClass();
        List<FieldInfo> fields = getExportableFields(entityClass, "", null, excludedFields, alternativeNames);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(entityClass.getSimpleName());

            createHeaderRow(workbook, sheet, fields, alternativeHeaders);
            createDataRows(sheet, entities, fields);
            setColumnWidths(sheet, fields.size());

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    private static List<FieldInfo> getExportableFields(Class<?> clazz, String prefix, Field[] parentPath,
                                                      Set<String> excludedFields, Map<String, String> alternativeNames) {
        List<FieldInfo> fields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (shouldSkipField(field, parentPath, excludedFields)) {
                continue;
            }

            field.setAccessible(true);

            if (isSimpleType(field.getType())) {
                String originalDisplayName = prefix + formatFieldName(field.getName());
                String displayName = alternativeNames.getOrDefault(originalDisplayName, originalDisplayName);
                Field[] fieldPath = appendToPath(parentPath, field);
                fields.add(new FieldInfo(field, displayName, fieldPath));
            } else if (isJpaEntity(field.getType()) && !Collection.class.isAssignableFrom(field.getType())) {
                String newPrefix = prefix + formatFieldName(field.getName()) + " - ";
                Field[] newPath = appendToPath(parentPath, field);
                fields.addAll(getExportableFields(field.getType(), newPrefix, newPath, excludedFields, alternativeNames));
            }
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            fields.addAll(getExportableFields(superclass, prefix, parentPath, excludedFields, alternativeNames));
        }

        return fields;
    }

    private static boolean shouldSkipField(Field field, Field[] parentPath, Set<String> excludedFields) {
        if (field.getName().equals("serialVersionUID")) {
            return true;
        }

        ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
        if (excelColumn != null && excelColumn.ignore()) {
            return true;
        }

        String fullPath = buildFieldPath(parentPath, field);
        return excludedFields.contains(fullPath);
    }

    private static String buildFieldPath(Field[] parentPath, Field currentField) {
        if (parentPath == null || parentPath.length == 0) {
            return currentField.getName();
        }

        StringBuilder path = new StringBuilder();
        for (Field field : parentPath) {
            if (path.length() > 0) {
                path.append(".");
            }
            path.append(field.getName());
        }
        return path + "." + currentField.getName();
    }

    private static Field[] appendToPath(Field[] currentPath, Field newField) {
        if (currentPath == null) {
            return new Field[]{newField};
        }
        Field[] newPath = Arrays.copyOf(currentPath, currentPath.length + 1);
        newPath[currentPath.length] = newField;
        return newPath;
    }

    private static void createHeaderRow(Workbook workbook, Sheet sheet, List<FieldInfo> fields, List<String> alternativeHeaders) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < fields.size(); i++) {
            Cell cell = headerRow.createCell(i);
            String headerValue = (alternativeHeaders != null && i < alternativeHeaders.size())
                    ? alternativeHeaders.get(i)
                    : fields.get(i).displayName;
            cell.setCellValue(headerValue);
            cell.setCellStyle(headerStyle);
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    private static <T> void createDataRows(Sheet sheet, List<T> entities, List<FieldInfo> fields) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int rowNum = 1;

        for (T entity : entities) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < fields.size(); i++) {
                Cell cell = row.createCell(i);
                setFieldValue(cell, entity, fields.get(i), dateFormatter, dateTimeFormatter);
            }
        }
    }

    private static void setColumnWidths(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            int maxWidth = 256 * 30; // 30 characters maximum width for all columns
            int minWidth = 256 * 10; // 10 characters minimum width
            int newWidth = Math.min(Math.max(currentWidth, minWidth), maxWidth);
            sheet.setColumnWidth(i, newWidth);
        }
    }

    private static <T> void setFieldValue(Cell cell, T entity, FieldInfo fieldInfo,
                                        DateTimeFormatter dateFormatter,
                                        DateTimeFormatter dateTimeFormatter) {
        try {
            Object value = getFieldValue(entity, fieldInfo.fieldPath);

            if (value == null) {
                cell.setCellValue("");
                return;
            }

            switch (fieldInfo.field.getType().getSimpleName()) {
                case "String":
                    cell.setCellValue((String) value);
                    break;
                case "LocalDate":
                    cell.setCellValue(((LocalDate) value).format(dateFormatter));
                    break;
                case "LocalDateTime":
                    cell.setCellValue(((LocalDateTime) value).format(dateTimeFormatter));
                    break;
                case "boolean":
                case "Boolean":
                    cell.setCellValue((Boolean) value);
                    break;
                case "int":
                case "Integer":
                    cell.setCellValue((Integer) value);
                    break;
                case "long":
                case "Long":
                    cell.setCellValue((Long) value);
                    break;
                case "double":
                case "Double":
                    cell.setCellValue((Double) value);
                    break;
                case "UUID":
                    cell.setCellValue(value.toString());
                    break;
                default:
                    if (value instanceof Enum) {
                        cell.setCellValue(((Enum<?>) value).name());
                    } else {
                        cell.setCellValue(value.toString());
                    }
            }
        } catch (Exception e) {
            cell.setCellValue("Error: " + e.getMessage());
        }
    }

    private static Object getFieldValue(Object entity, Field[] fieldPath) throws IllegalAccessException {
        Object currentObject = entity;

        for (Field field : fieldPath) {
            if (currentObject == null) {
                return null;
            }

            field.setAccessible(true);
            currentObject = field.get(currentObject);
        }

        return currentObject;
    }

    private static boolean isSimpleType(Class<?> type) {
        return SIMPLE_TYPES.contains(type) || type.isEnum();
    }

    private static boolean isJpaEntity(Class<?> clazz) {
        return clazz.isAnnotationPresent(jakarta.persistence.Entity.class);
    }

    private static String formatFieldName(String fieldName) {
        String[] words = fieldName.split("(?<=\\p{Ll})(?=\\p{Lu})|(?<=\\p{L})(?=\\p{Lu}\\p{Ll})");
        return Arrays.stream(words)
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
    public @interface ExcelColumn {
        String name() default "";
        boolean ignore() default false;
        int order() default Integer.MAX_VALUE;
    }
}