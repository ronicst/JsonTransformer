package com.example.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonTransformerTest {
    private ObjectMapper mapper;
    private JsonTransformer transformer;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        transformer = new JsonTransformer();
    }

    @Test
    public void testSuccessfulTransformation() throws Exception {
        String inputJson = """
        {
          \"message\": {
            \"responseHeader\": {
              \"responseCode\": \"R0201\",
              \"responseType\": \"INFO\",
              \"responseMessage\": \"Workflow Complete.\",
              \"tenantID\": \"tenant1\",
              \"expRequestId\": \"REQ123\",
              \"overallResponse\": {
                \"decision\": \"ACCEPT01\",
                \"score\": 1,
                \"decisionReasons\": [\"reason1\"]
              }
            },
            \"clientResponsePayload\": {
              \"orchestrationDecisions\": [
                {\"decision\": \"ACCEPT02\", \"score\": 2, \"decisionReasons\": [\"reason2\"]}
              ]
            },
            \"originalRequestData\": {
              \"contacts\": [
                {\"addresses\": [
                  {\"street\": \"Main St\", \"street2\": \"Apt 1\", \"postTown\": \"Townsville\", \"postal\": \"12345\", \"countryCode\": \"US\"}
                ]}
              ]
            }
          }
        }
        """;
        String configJson = """
        {
          \"outputFields\": {
            \"responseCode\": {\"source\": \"message.responseHeader.responseCode\", \"required\": true},
            \"responseType\": {\"source\": \"message.responseHeader.responseType\", \"required\": true},
            \"responseMessage\": {\"source\": \"message.responseHeader.responseMessage\", \"required\": false},
            \"tenantID\": {\"source\": \"message.responseHeader.tenantID\", \"required\": true},
            \"expRequestId\": {\"source\": \"message.responseHeader.expRequestId\", \"required\": true},
            \"overallResponse.decision\": {\"source\": \"message.responseHeader.overallResponse.decision\", \"required\": true},
            \"newDecision\": {
              \"operation\": \"conditional_decision\",
              \"sources\": [
                \"message.responseHeader.overallResponse.decision\",
                \"message.clientResponsePayload.orchestrationDecisions[0].decision\"
              ],
              \"scores\": [
                \"message.responseHeader.overallResponse.score\",
                \"message.clientResponsePayload.orchestrationDecisions[0].score\"
              ],
              \"required\": true
            },
            \"newDecisionReasons\": {
              \"operation\": \"fallback\",
              \"sources\": [
                \"message.responseHeader.overallResponse.decisionReasons\",
                \"message.clientResponsePayload.orchestrationDecisions[0].decisionReasons\"
              ],
              \"required\": false
            },
            \"address\": {
              \"operation\": \"concatenate\",
              \"sources\": [
                \"message.originalRequestData.contacts[0].addresses[0].street\",
                \"message.originalRequestData.contacts[0].addresses[0].street2\",
                \"message.originalRequestData.contacts[0].addresses[0].postTown\",
                \"message.originalRequestData.contacts[0].addresses[0].postal\",
                \"message.originalRequestData.contacts[0].addresses[0].countryCode\"
              ],
              \"separator\": \", \",
              \"required\": true
            }
          }
        }
        """;
        JsonNode input = mapper.readTree(inputJson);
        JsonNode config = mapper.readTree(configJson);
        JsonNode output = transformer.transform(input, config);
        assertEquals("R0201", output.get("responseCode").asText());
        assertEquals("INFO", output.get("responseType").asText());
        assertEquals("tenant1", output.get("tenantID").asText());
        assertEquals("REQ123", output.get("expRequestId").asText());
        assertEquals("ACCEPT01", output.get("overallResponse.decision").asText());
        assertEquals("ACCEPT01", output.get("newDecision").asText());
        assertEquals("reason1", output.get("newDecisionReasons").get(0).asText());
        assertEquals("Main St, Apt 1, Townsville, 12345, US", output.get("address").asText());
    }

    @Test
    public void testMissingRequiredFieldThrowsError() throws Exception {
        String inputJson = "{\"message\":{\"responseHeader\":{}}}";
        String configJson = "{\"outputFields\":{\"responseCode\":{\"source\":\"message.responseHeader.responseCode\",\"required\":true}}}";
        JsonNode input = mapper.readTree(inputJson);
        JsonNode config = mapper.readTree(configJson);
        try {
            transformer.transform(input, config);
            fail("Expected RuntimeException for missing required field");
        } catch (RuntimeException ex) {
            assertTrue(ex.getMessage().contains("responseCode defined in the config mapping is null or empty"));
        }
    }

    @Test
    public void testOptionalFieldMissingDoesNotThrow() throws Exception {
        String inputJson = "{\"message\":{\"responseHeader\":{}}}";
        String configJson = "{\"outputFields\":{\"responseMessage\":{\"source\":\"message.responseHeader.responseMessage\",\"required\":false}}}";
        JsonNode input = mapper.readTree(inputJson);
        JsonNode config = mapper.readTree(configJson);
        JsonNode output = transformer.transform(input, config);
        assertFalse(output.has("responseMessage"));
    }

    @Test
    public void testInvalidPathDoesNotThrowForOptional() throws Exception {
        String inputJson = "{\"message\":{}}";
        String configJson = "{\"outputFields\":{\"address\":{\"source\":\"message.nonexistent.path\",\"required\":false}}}";
        JsonNode input = mapper.readTree(inputJson);
        JsonNode config = mapper.readTree(configJson);
        JsonNode output = transformer.transform(input, config);
        assertFalse(output.has("address"));
    }
} 