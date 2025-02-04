package com.infomedia.abacox.users.component.export.excel;

import jakarta.validation.ValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseUtils {
    public static Map<String, String> parseAlternativeHeaders(String alternativeHeaders) {
        Map<String, String> headersMap = new HashMap<>();
        if (alternativeHeaders == null || alternativeHeaders.trim().isEmpty()) {
            return headersMap;
        }

        try {
            // Matches 'key':'value' pattern, handling spaces and escaped quotes
            Pattern pattern = Pattern.compile("'([^']+)':'([^']+)'");
            Matcher matcher = pattern.matcher(alternativeHeaders);

            while (matcher.find()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                if (!key.isEmpty() && !value.isEmpty()) {
                    headersMap.put(key, value);
                }
            }
        } catch (Exception e) {
            throw new ValidationException("Invalid alternative headers format, please use 'key1':'value1','key2':'value2',...");
        }

        return headersMap;
    }
}
