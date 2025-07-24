package com.example.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Main class for configuration-driven JSON transformation.
 * Reads a config describing how to map/transform fields from an input JSON event,
 * applies the rules, and outputs a structured result.
 * Throws a RuntimeException if required fields, sources, or scores are missing or empty.
 */
public class JsonTransformer {
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Transforms the input JSON according to the provided config.
     * @param input The input event as a JsonNode.
     * @param config The transformation config as a JsonNode.
     * @return The transformed output as a JsonNode.
     * @throws RuntimeException if any required field, source, or score is missing or empty.
     */
    public JsonNode transform(JsonNode input, JsonNode config) {
        ObjectNode output = mapper.createObjectNode();
        JsonNode outputFields = config.get("outputFields");
        Iterator<Map.Entry<String, JsonNode>> fields = outputFields.fields();
        boolean strictSourcesRequired = config.has("strictSourcesRequired") && config.get("strictSourcesRequired").asBoolean(false);
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String outField = entry.getKey();
            JsonNode rule = entry.getValue();
            boolean required = rule.has("required") && rule.get("required").asBoolean(false);
            JsonNode value = null;
            boolean hasValue = false;
            if (rule.has("source")) {
                value = TransformerUtils.getValueByPath(input, rule.get("source").asText());
                hasValue = value != null && !TransformerUtils.isEmpty(value);
                if (hasValue) {
                    output.set(outField, value);
                }
                if (required && !hasValue) {
                    throw new RuntimeException(outField + " defined in the config mapping is null or empty");
                }
            } else if (rule.has("operation")) {
                String op = rule.get("operation").asText();
                List<String> sources = new ArrayList<>();
                for (JsonNode s : rule.get("sources")) sources.add(s.asText());
                List<Boolean> requiredSources = new ArrayList<>();
                if (rule.has("requiredSources")) {
                    for (JsonNode rs : rule.get("requiredSources")) requiredSources.add(rs.asBoolean(false));
                }
                // If strictSourcesRequired is true, treat all sources as required for required fields
                if (strictSourcesRequired && required) {
                    requiredSources.clear();
                    for (int i = 0; i < sources.size(); i++) requiredSources.add(true);
                }
                // Check each required source
                for (int i = 0; i < sources.size(); i++) {
                    boolean srcRequired = (i < requiredSources.size()) ? requiredSources.get(i) : false;
                    if (srcRequired) {
                        JsonNode srcVal = TransformerUtils.getValueByPath(input, sources.get(i));
                        if (srcVal == null || TransformerUtils.isEmpty(srcVal)) {
                            throw new RuntimeException(outField + ": required source '" + sources.get(i) + "' defined in the config mapping is null or empty");
                        }
                    }
                }
                List<String> scorePaths = new ArrayList<>();
                if (rule.has("scores")) {
                    for (JsonNode s : rule.get("scores")) scorePaths.add(s.asText());
                }
                List<Boolean> requiredScores = new ArrayList<>();
                if (rule.has("requiredScores")) {
                    for (JsonNode rs : rule.get("requiredScores")) requiredScores.add(rs.asBoolean(false));
                }
                // Check each required score
                for (int i = 0; i < scorePaths.size(); i++) {
                    boolean scoreRequired = (i < requiredScores.size()) ? requiredScores.get(i) : false;
                    if (scoreRequired) {
                        JsonNode scoreVal = TransformerUtils.getValueByPath(input, scorePaths.get(i));
                        if (scoreVal == null || TransformerUtils.isEmpty(scoreVal)) {
                            throw new RuntimeException(outField + ": required score '" + scorePaths.get(i) + "' defined in the config mapping is null or empty");
                        }
                    }
                }
                switch (op) {
                    case "concatenate": {
                        String sep = rule.has("separator") ? rule.get("separator").asText() : ", ";
                        String concat = OperationsUtils.concatenate(input, sources, sep);
                        hasValue = concat != null && !concat.trim().isEmpty();
                        if (hasValue) output.put(outField, concat);
                        value = hasValue ? output.get(outField) : null;
                        break;
                    }
                    case "conditional_decision": {
                        String result = OperationsUtils.conditionalDecision(input, sources, scorePaths);
                        hasValue = result != null && !result.trim().isEmpty();
                        if (hasValue) output.put(outField, result);
                        value = hasValue ? output.get(outField) : null;
                        break;
                    }
                    case "fallback": {
                        JsonNode val = OperationsUtils.fallback(input, sources);
                        hasValue = val != null && !TransformerUtils.isEmpty(val);
                        if (hasValue) output.set(outField, val);
                        value = hasValue ? val : null;
                        break;
                    }
                }
                if (required && (!hasValue || (value != null && value.isArray() && TransformerUtils.isArrayAllEmpty(value)))) {
                    throw new RuntimeException(outField + " defined in the config mapping is null or empty");
                }
            }
        }
        return output;
    }

    /**
     * Main entry point for running the transformer from the command line.
     * Reads input_event.json and config/sample_config.json, prints the output or error.
     * @param args Command-line arguments (not used).
     * @throws Exception if file reading or transformation fails.
     */
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode input = mapper.readTree(new File("input_event.json"));
        JsonNode config = mapper.readTree(new File("config/sample_config.json"));
        JsonTransformer transformer = new JsonTransformer();
        JsonNode output = transformer.transform(input, config);
        System.out.println("Transformed output:\n" + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output));
    }
} 