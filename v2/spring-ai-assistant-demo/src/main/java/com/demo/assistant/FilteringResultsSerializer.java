package com.demo.assistant;

import org.hibernate.query.SelectionQuery;
import org.hibernate.tool.language.spi.ResultsSerializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A {@link ResultsSerializer} (the official Hibernate Assistant SPI) that <b>reuses Hibernate's own
 * results serializer</b> and then removes hidden fields from the produced JSON.
 *
 * <p>This closes the gap that metamodel hiding alone leaves open: hiding {@code costPrice} from the
 * {@link FilteringMetamodelSerializer} stops the LLM from <em>naming</em> it, but a perfectly normal
 * {@code SELECT p FROM Product p} returns the whole entity — and Hibernate's serializer renders every
 * mapped attribute, including the hidden one. Filtering the result JSON keeps the sensitive value
 * from ever reaching the model (or the API response).
 */
public class FilteringResultsSerializer implements ResultsSerializer {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final ResultsSerializer delegate;   // Hibernate's own results serializer
  private final Set<String> hiddenFields;     // attribute names to strip from any result object

  public FilteringResultsSerializer(ResultsSerializer delegate, Set<String> hiddenFields) {
    this.delegate = delegate;
    this.hiddenFields = hiddenFields == null ? Set.of() : hiddenFields;
  }

  @Override
  public <T> String toString(List<? extends T> results, SelectionQuery<T> query) throws IOException {
    final String json = delegate.toString(results, query);
    if (hiddenFields.isEmpty()) {
      return json;
    }
    final JsonNode root = MAPPER.readTree(json);
    strip(root);
    return MAPPER.writeValueAsString(root);
  }

  private void strip(JsonNode node) {
    if (node instanceof ObjectNode object) {
      object.remove(hiddenFields);
    }
    for (final JsonNode child : node.values()) {
      strip(child);
    }
  }
}
