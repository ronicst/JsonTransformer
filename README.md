# JsonTransformerLib

A generic, configuration-driven JSON transformation service for Java, using Jackson. Designed to process events (e.g., from Kafka), apply flexible mapping and transformation rules from a JSON config, and output a structured result. No code changes are needed for new or updated transformation logic—just update the config!

## Features
- **Configurable JSON transformation**: Define how to map, concatenate, compare, and fallback fields using a JSON config.
- **Jackson-based**: Uses Jackson for robust JSON parsing and manipulation.
- **Required field enforcement**: Mark any field, source, or score as required; transformation fails with a clear error if missing.
- **Per-source/score requiredness**: Fine-grained control over which sources/scores are mandatory in multi-source operations.
- **Extensible operations**: Easily add new transformation operations.
- **Unit and integration tested**: Includes comprehensive tests for all core logic.

## Project Structure
```
JsonTransformerLib/
  ├── config/
  │     └── sample_config.json      # Example transformation config
  ├── input_event.json              # Example input event
  ├── src/
  │     └── main/java/com/example/transformer/
  │           ├── App.java
  │           ├── JsonTransformer.java
  │           ├── OperationsUtils.java
  │           └── TransformerUtils.java
  │     └── test/java/com/example/transformer/
  │           ├── JsonTransformerTest.java
  │           ├── OperationsUtilsTest.java
  │           └── TransformerUtilsTest.java
  ├── pom.xml                       # Maven build file
  └── README.md                     # This file
```

## Configuration

### Example: `config/sample_config.json`
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

- `requiredSources`: Array of booleans, one per source, indicating if that source is mandatory.
- `requiredScores`: Array of booleans, one per score, indicating if that score is mandatory.
- `required`: If true, the output field itself is required (final value must not be empty).

## Usage

### Build and Run
1. **Build the project:**
   ```sh
   mvn clean compile
   ```
2. **Run the transformer:**
   ```sh
   mvn exec:java -Dexec.mainClass=com.example.transformer.App
   ```
   - By default, reads `input_event.json` and `config/sample_config.json`.
   - Output is printed to the console.

### Error Handling
- If a required field, source, or score is missing or empty, the transformation aborts and prints a clear error message indicating which field/source/score is missing.

## Extending
- Add new operations by implementing static methods in `OperationsUtils` and updating the switch in `JsonTransformer`.
- Add new config options as needed; the system is designed for flexibility.

## Testing
- Run all tests:
  ```sh
  mvn test
  ```
- Tests cover all utility methods, operations, and the main transformation flow, including error cases.

## License
MIT or your preferred license. 