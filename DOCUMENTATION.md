# JsonTransformerLib Documentation

## Overview
JsonTransformerLib is a generic, configuration-driven JSON transformation service for Java, using Jackson. It enables flexible, no-code transformation of JSON events (e.g., from Kafka) based on a JSON configuration file.

## Configuration Schema

### Top-level
- `outputFields`: Object. Each key is an output field, value is a mapping rule.
- (Optional) `strictSourcesRequired`: Boolean. If true, all sources for required fields must be present and non-empty.

### Field Mapping Rule
- `source`: String. Dot/bracket path to a value in the input JSON.
- `operation`: String. One of `concatenate`, `conditional_decision`, `fallback` (or custom).
- `sources`: Array of strings. Paths to input fields for multi-source operations.
- `requiredSources`: Array of booleans. Each entry corresponds to a source; if true, that source must be present and non-empty.
- `scores`: Array of strings. Used in `conditional_decision`.
- `requiredScores`: Array of booleans. Each entry corresponds to a score; if true, that score must be present and non-empty.
- `separator`: String. Used in `concatenate`.
- `required`: Boolean. If true, the output field itself must not be empty.

### Example
```json
{
  "outputFields": {
    "address": {
      "operation": "concatenate",
      "sources": [
        "message.originalRequestData.contacts[0].addresses[0].street",
        "message.originalRequestData.contacts[0].addresses[0].street2",
        "message.originalRequestData.contacts[0].addresses[0].postTown",
        "message.originalRequestData.contacts[0].addresses[0].postal",
        "message.originalRequestData.contacts[0].addresses[0].countryCode"
      ],
      "requiredSources": [true, false, false, false, true],
      "separator": ", "
    },
    "newDecision": {
      "operation": "conditional_decision",
      "sources": [
        "message.responseHeader.overallResponse.decision",
        "message.clientResponsePayload.orchestrationDecisions[0].decision"
      ],
      "requiredSources": [true, true],
      "scores": [
        "message.responseHeader.overallResponse.score",
        "message.clientResponsePayload.orchestrationDecisions[0].score"
      ],
      "requiredScores": [true, true],
      "required": true
    }
  }
}
```

## Supported Operations

### 1. Direct Mapping
```json
"responseCode": {"source": "message.responseHeader.responseCode", "required": true}
```

### 2. Concatenate
```json
"address": {
  "operation": "concatenate",
  "sources": [ ... ],
  "requiredSources": [true, false, ...],
  "separator": ", "
}
```

### 3. Conditional Decision
```json
"newDecision": {
  "operation": "conditional_decision",
  "sources": [ ... ],
  "requiredSources": [true, true],
  "scores": [ ... ],
  "requiredScores": [true, true],
  "required": true
}
```

### 4. Fallback
```json
"newDecisionReasons": {
  "operation": "fallback",
  "sources": [ ... ],
  "requiredSources": [false, false],
  "required": false
}
```

## Error Handling
- If a required field, source, or score is missing or empty, the transformation aborts and throws a `RuntimeException` with a clear message, e.g.:
  - `address: required source 'message.originalRequestData.contacts[0].addresses[0].street' defined in the config mapping is null or empty`
  - `newDecision: required score 'message.responseHeader.overallResponse.score' defined in the config mapping is null or empty`
- If a field is not required and missing, it is omitted from the output.

## Extending the System
- **Add new operations:**
  - Implement a static method in `OperationsUtils`.
  - Add a new case in the `switch` statement in `JsonTransformer`.
- **Add new config options:**
  - Update the config schema and parsing logic as needed.
- **Add new tests:**
  - Place new tests in `src/test/java/com/example/transformer/`.

## Running & Testing
- **Build:**
  ```sh
  mvn clean compile
  ```
- **Run:**
  ```sh
  mvn exec:java -Dexec.mainClass=com.example.transformer.App
  ```
- **Test:**
  ```sh
  mvn test
  ```

## Example Output
```json
{
  "address": "Main St, Apt 1, Townsville, 12345, US",
  "newDecision": "ACCEPT01"
}
```

## Contact
For questions or contributions, open an issue or PR. 