package com.wildlife.wildlife_conservationbackend.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class LogSanitizer {
    public static final int MAX_BODY_BYTES = 16_384;
    private static final int MAX_DEPTH = 16;
    private static final int MAX_ARRAY_ITEMS = 20;
    private static final int MAX_HEADER_VALUES = 5;
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");
    private static final Set<String> SAFE_HEADERS = Set.of("content-type", "content-length", "accept",
            "accept-encoding", "accept-language", "user-agent", "host", "origin", "x-request-id", "x-trace-id",
            "x-b3-traceid", "vary", "cache-control", "content-disposition", "x-content-type-options");
    private static final Set<String> PRIVATE_FIELDS = Set.of("email", "name", "phone", "phonenumber", "pwd", "pass");
    private static final Set<String> OMITTED_FIELDS = Set.of("location", "latitude", "longitude", "trackpoints",
            "pathpoints", "waypoints", "observations");

    private final ObjectMapper objectMapper;

    public String clean(String value, int limit) {
        if (value == null) {
            return "";
        }
        String bounded = value.substring(0, Math.min(value.length(), limit));
        return bounded.replaceAll("[\\p{Cntrl}]", "_") + (value.length() > limit ? " [truncated]" : "");
    }

    public Map<String, String> headers(Map<String, ? extends Collection<String>> headers) {
        Map<String, String> safe = new LinkedHashMap<>();
        headers.entrySet().stream().limit(50).forEach(entry -> {
            String name = clean(entry.getKey(), 100);
            String value = SAFE_HEADERS.contains(name.toLowerCase(Locale.ROOT))
                    ? clean(String.join(", ", entry.getValue().stream().limit(MAX_HEADER_VALUES).toList()), 512)
                    : "[REDACTED]";
            safe.put(name, value);
        });
        return safe;
    }

    public String body(byte[] bytes, String contentType, boolean truncated) {
        if (bytes.length == 0) {
            return "[empty or not consumed]";
        }
        if (contentType == null || !isJson(contentType)) {
            return "[omitted: binary, multipart or non-JSON content]";
        }
        if (truncated) {
            return "[omitted: JSON body exceeds " + MAX_BODY_BYTES + " bytes]";
        }
        try {
            Object value = objectMapper.readValue(bytes, Object.class);
            if (!(value instanceof Map<?, ?>)) {
                return "[omitted: JSON object expected]";
            }
            String result = objectMapper.writeValueAsString(redact(value, 0));
            return result.length() <= MAX_BODY_BYTES ? result : "[omitted: sanitized JSON exceeds log limit]";
        } catch (JacksonException exception) {
            return "[omitted: invalid JSON]";
        }
    }

    private boolean isJson(String contentType) {
        String type = contentType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        return "application/json".equals(type) || type.startsWith("application/") && type.endsWith("+json");
    }

    private Object redact(Object value, int depth) {
        if (depth >= MAX_DEPTH) {
            return "[omitted: nested content]";
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, field) -> {
                String name = String.valueOf(key);
                String normalized = NON_ALPHANUMERIC.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("");
                if (isPrivate(normalized)) {
                    result.put(name, "[REDACTED]");
                } else if (OMITTED_FIELDS.contains(normalized)) {
                    result.put(name, "[OMITTED]");
                } else {
                    result.put(name, redact(field, depth + 1));
                }
            });
            return result;
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            list.stream().limit(MAX_ARRAY_ITEMS).forEach(item -> result.add(redact(item, depth + 1)));
            if (list.size() > MAX_ARRAY_ITEMS) {
                result.add("[omitted: " + (list.size() - MAX_ARRAY_ITEMS) + " additional items]");
            }
            return result;
        }
        return value;
    }

    private boolean isPrivate(String name) {
        return PRIVATE_FIELDS.contains(name) || name.contains("password") || name.contains("secret")
                || name.contains("token") || name.contains("apikey") || name.contains("authorization") || name.contains("cookie");
    }
}
