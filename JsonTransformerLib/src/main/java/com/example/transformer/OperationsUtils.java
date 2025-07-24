package com.example.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Utility methods for transformation operations (concatenate, conditionalDecision, fallback).
 * Used by JsonTransformer to apply config-driven logic.
 */
public class OperationsUtils {
    /**
     * Concatenates the values of the given sources from the input node, separated by sep.
     * @param input The root JsonNode.
     * @param sources List of source paths.
     * @param sep Separator string.
     * @return The concatenated string.
     */
    public static String concatenate(JsonNode input, List<String> sources, String sep) {
        return TransformerUtils.concatenateFields(input, sources, sep);
    }

    /**
     * Compares two decisions and their scores, returning the one with the lower score, or the same if equal.
     * @param input The root JsonNode.
     * @param sources List of decision paths.
     * @param scorePaths List of score paths.
     * @return The selected decision string.
     */
    public static String conditionalDecision(JsonNode input, List<String> sources, List<String> scorePaths) {
        String dec1 = TransformerUtils.getValueByPath(input, sources.get(0)).asText("");
        String dec2 = TransformerUtils.getValueByPath(input, sources.get(1)).asText("");
        int score1 = TransformerUtils.getValueByPath(input, scorePaths.get(0)).asInt(Integer.MAX_VALUE);
        int score2 = TransformerUtils.getValueByPath(input, scorePaths.get(1)).asInt(Integer.MAX_VALUE);
        return TransformerUtils.compareDecisions(dec1, score1, dec2, score2);
    }

    /**
     * Returns the first non-empty value from the given sources, or null if all are empty.
     * @param input The root JsonNode.
     * @param sources List of source paths.
     * @return The first non-empty JsonNode, or null.
     */
    public static JsonNode fallback(JsonNode input, List<String> sources) {
        JsonNode val1 = TransformerUtils.getValueByPath(input, sources.get(0));
        if (val1 == null || TransformerUtils.isEmpty(val1)) {
            JsonNode val2 = TransformerUtils.getValueByPath(input, sources.get(1));
            return val2;
        } else {
            return val1;
        }
    }
} 