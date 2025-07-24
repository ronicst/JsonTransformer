package com.example.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

public class TransformerUtilsTest {
    private ObjectMapper mapper;
    private ObjectNode root;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        root = mapper.createObjectNode();
        root.put("simple", "value");
        ObjectNode nested = mapper.createObjectNode();
        nested.put("field", "nestedValue");
        root.set("nested", nested);
        root.putArray("arr").add("a").add("b");
    }

    @Test
    public void testGetValueByPath_simple() {
        JsonNode val = TransformerUtils.getValueByPath(root, "simple");
        assertNotNull(val);
        assertEquals("value", val.asText());
    }

    @Test
    public void testGetValueByPath_nested() {
        JsonNode val = TransformerUtils.getValueByPath(root, "nested.field");
        assertNotNull(val);
        assertEquals("nestedValue", val.asText());
    }

    @Test
    public void testGetValueByPath_array() {
        JsonNode val = TransformerUtils.getValueByPath(root, "arr[1]");
        assertNotNull(val);
        assertEquals("b", val.asText());
    }

    @Test
    public void testSetValueByPath() {
        TransformerUtils.setValueByPath(root, "new.path", mapper.convertValue("val", JsonNode.class));
        assertEquals("val", root.get("new").get("path").asText());
    }

    @Test
    public void testConcatenateFields() {
        root.put("f1", "A");
        root.put("f2", "B");
        String result = TransformerUtils.concatenateFields(root, Arrays.asList("f1", "f2"), ", ");
        assertEquals("A, B", result);
    }

    @Test
    public void testCompareDecisions_same() {
        assertEquals("X", TransformerUtils.compareDecisions("X", 1, "X", 2));
    }

    @Test
    public void testCompareDecisions_diff() {
        assertEquals("A", TransformerUtils.compareDecisions("A", 1, "B", 2));
        assertEquals("B", TransformerUtils.compareDecisions("A", 3, "B", 2));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(TransformerUtils.isEmpty(null));
        assertTrue(TransformerUtils.isEmpty(mapper.nullNode()));
        assertTrue(TransformerUtils.isEmpty(mapper.createArrayNode()));
        assertTrue(TransformerUtils.isEmpty(mapper.createObjectNode()));
        assertTrue(TransformerUtils.isEmpty(mapper.convertValue("", JsonNode.class)));
        assertFalse(TransformerUtils.isEmpty(mapper.convertValue("abc", JsonNode.class)));
    }
} 