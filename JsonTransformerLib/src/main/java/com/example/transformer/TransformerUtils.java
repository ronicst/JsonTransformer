package com.example.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Iterator;

/**
 * Utility methods for extracting, setting, and checking values in JSON nodes using Jackson.
 * Includes helpers for path navigation, concatenation, comparison, and emptiness checks.
 */
public class TransformerUtils {
    /**
     * Gets a nested value from a JsonNode by a dot/bracket path (e.g., a.b[0].c).
     * @param node The root JsonNode.
     * @param path The dot/bracket path string.
     * @return The value at the path, or null if not found.
     */
    public static JsonNode getValueByPath(JsonNode node, String path) {
        String[] parts = path.split("\\.");
        JsonNode current = node;
        for (String part : parts) {
            if (current == null) return null;
            if (part.contains("[")) {
                String field = part.substring(0, part.indexOf('['));
                int idx = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));
                current = current.path(field);
                if (current.isArray() && current.size() > idx) {
                    current = current.get(idx);
                } else {
                    return null;
                }
            } else {
                current = current.path(part);
            }
        }
        return current.isMissingNode() ? null : current;
    }

    /**
     * Sets a value in an ObjectNode at the given dot path, creating intermediate objects as needed.
     * @param node The root ObjectNode.
     * @param path The dot path string.
     * @param value The value to set.
     */
    public static void setValueByPath(ObjectNode node, String path, JsonNode value) {
        String[] parts = path.split("\\.");
        ObjectNode current = node;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.has(part) || !current.get(part).isObject()) {
                current.set(part, current.objectNode());
            }
            current = (ObjectNode) current.get(part);
        }
        current.set(parts[parts.length - 1], value);
    }

    /**
     * Concatenates the values of multiple fields from a JsonNode, separated by sep.
     * Skips empty or missing fields.
     * @param node The root JsonNode.
     * @param fields List of field paths to concatenate.
     * @param sep Separator string.
     * @return The concatenated string.
     */
    public static String concatenateFields(JsonNode node, List<String> fields, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String fieldPath : fields) {
            JsonNode val = getValueByPath(node, fieldPath);
            if (val != null && !isEmpty(val)) {
                if (sb.length() > 0) sb.append(sep);
                sb.append(val.asText());
            }
        }
        return sb.toString();
    }

    /**
     * Compares two decisions by their scores, returning the one with the lower score, or the same if equal.
     * @param dec1 First decision.
     * @param score1 First score.
     * @param dec2 Second decision.
     * @param score2 Second score.
     * @return The selected decision.
     */
    public static String compareDecisions(String dec1, int score1, String dec2, int score2) {
        if (dec1.equals(dec2)) return dec1;
        return (score1 < score2) ? dec1 : dec2;
    }

    /**
     * Checks if a JsonNode is empty (null, empty string, empty array, or empty object).
     * @param value The JsonNode to check.
     * @return True if empty, false otherwise.
     */
    public static boolean isEmpty(JsonNode value) {
        if (value == null || value.isNull()) return true;
        if (value.isTextual() && value.asText().trim().isEmpty()) return true;
        if (value.isArray() && value.size() == 0) return true;
        if (value.isObject() && !value.fieldNames().hasNext()) return true;
        return false;
    }

    /**
     * Checks if all elements in an array node are empty.
     * @param array The array JsonNode.
     * @return True if all elements are empty or array is empty/null, false otherwise.
     */
    public static boolean isArrayAllEmpty(JsonNode array) {
        if (array == null || !array.isArray() || array.size() == 0) return true;
        for (JsonNode elem : array) {
            if (!isEmpty(elem)) return false;
        }
        return true;
    }
} 