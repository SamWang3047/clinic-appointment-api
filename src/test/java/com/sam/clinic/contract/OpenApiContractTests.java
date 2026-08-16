package com.sam.clinic.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class OpenApiContractTests {

	private static final Path CONTRACT_PATH = Path.of(
			"docs", "specs", "openapi", "clinic-api.yaml");
	private static final Set<String> HTTP_METHODS = Set.of(
			"get", "post", "put", "patch", "delete", "head", "options", "trace");

	@Test
	void contractHasValidInternalReferencesAndUniqueOperationIds() throws IOException {
		Map<String, Object> contract = loadContract();
		assertThat(contract.get("openapi")).isEqualTo("3.1.0");

		List<String> references = new ArrayList<>();
		collectReferences(contract, references);
		assertThat(references).isNotEmpty();
		assertThat(references)
				.as("every local OpenAPI reference must resolve")
				.allSatisfy(reference -> assertThat(resolveReference(contract, reference))
						.as(reference)
						.isNotNull());

		List<String> operationIds = collectOperationIds(contract);
		assertThat(operationIds).isNotEmpty();
		assertThat(operationIds)
				.as("operation IDs must be unique")
				.doesNotHaveDuplicates();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> loadContract() throws IOException {
		LoaderOptions options = new LoaderOptions();
		options.setAllowDuplicateKeys(false);
		Yaml yaml = new Yaml(new SafeConstructor(options));
		try (InputStream input = Files.newInputStream(CONTRACT_PATH)) {
			Object document = yaml.load(input);
			assertThat(document).isInstanceOf(Map.class);
			return (Map<String, Object>) document;
		}
	}

	private static void collectReferences(Object value, List<String> references) {
		if (value instanceof Map<?, ?> map) {
			map.forEach((key, child) -> {
				if ("$ref".equals(key) && child instanceof String reference && reference.startsWith("#/")) {
					references.add(reference);
				}
				collectReferences(child, references);
			});
		}
		else if (value instanceof Iterable<?> iterable) {
			iterable.forEach(child -> collectReferences(child, references));
		}
	}

	private static Object resolveReference(Map<String, Object> contract, String reference) {
		Object current = contract;
		for (String encodedPart : reference.substring(2).split("/")) {
			if (!(current instanceof Map<?, ?> map)) {
				return null;
			}
			String part = encodedPart.replace("~1", "/").replace("~0", "~");
			current = map.get(part);
			if (current == null) {
				return null;
			}
		}
		return current;
	}

	private static List<String> collectOperationIds(Map<String, Object> contract) {
		Object pathsValue = contract.get("paths");
		assertThat(pathsValue).isInstanceOf(Map.class);
		List<String> operationIds = new ArrayList<>();
		Set<String> operationsWithoutIds = new HashSet<>();

		((Map<?, ?>) pathsValue).forEach((path, pathValue) -> {
			if (!(pathValue instanceof Map<?, ?> pathItem)) {
				return;
			}
			pathItem.forEach((method, operationValue) -> {
				if (!HTTP_METHODS.contains(method) || !(operationValue instanceof Map<?, ?> operation)) {
					return;
				}
				Object operationId = operation.get("operationId");
				if (operationId instanceof String text && !text.isBlank()) {
					operationIds.add(text);
				}
				else {
					operationsWithoutIds.add(method.toString().toUpperCase() + " " + path);
				}
			});
		});

		assertThat(operationsWithoutIds)
				.as("every HTTP operation must declare an operationId")
				.isEmpty();
		return operationIds;
	}
}
