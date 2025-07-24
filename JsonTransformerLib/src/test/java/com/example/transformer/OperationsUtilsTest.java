package com.example.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class OperationsUtilsTest {
    private ObjectMapper mapper;
    private ObjectNode root;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        root = mapper.createObjectNode();
        root.put("f1", "A");
        root.put("f2", "B");
        ObjectNode nested = mapper.createObjectNode();
        nested.put("decision", "DEC1");
        nested.put("score", 1);
        root.set("nested1", nested);
        ObjectNode nested2 = mapper.createObjectNode();
        nested2.put("decision", "DEC2");
        nested2.put("score", 2);
        root.set("nested2", nested2);
        root.put("empty", "");
    }

    @Test
    public void testConcatenate() {
        List<String> fields = Arrays.asList("f1", "f2");
        String result = OperationsUtils.concatenate(root, fields, ", ");
        assertEquals("A, B", result);
    }

    @Test
    public void testConditionalDecision_same() {
        List<String> sources = Arrays.asList("nested1.decision", "nested1.decision");
        List<String> scores = Arrays.asList("nested1.score", "nested1.score");
        String result = OperationsUtils.conditionalDecision(root, sources, scores);
        assertEquals("DEC1", result);
    }

    @Test
    public void testConditionalDecision_diff() {
        List<String> sources = Arrays.asList("nested1.decision", "nested2.decision");
        List<String> scores = Arrays.asList("nested1.score", "nested2.score");
        String result = OperationsUtils.conditionalDecision(root, sources, scores);
        assertEquals("DEC1", result); // score1 < score2
        // reverse scores
        result = OperationsUtils.conditionalDecision(root, Arrays.asList("nested2.decision", "nested1.decision"), Arrays.asList("nested2.score", "nested1.score"));
        assertEquals("DEC1", result); // score2 < score1
    }

    @Test
    public void testFallback_firstNotEmpty() {
        List<String> sources = Arrays.asList("f1", "f2");
        JsonNode result = OperationsUtils.fallback(root, sources);
        assertEquals("A", result.asText());
    }

    @Test
    public void testFallback_firstEmpty() {
        List<String> sources = Arrays.asList("empty", "f2");
        JsonNode result = OperationsUtils.fallback(root, sources);
        assertEquals("B", result.asText());
    }

    @Test
    public void testFallback_bothEmpty() {
        List<String> sources = Arrays.asList("empty", "nonexistent");
        JsonNode result = OperationsUtils.fallback(root, sources);
        assertTrue(result == null || result.isNull() || result.isMissingNode());
    }
} 