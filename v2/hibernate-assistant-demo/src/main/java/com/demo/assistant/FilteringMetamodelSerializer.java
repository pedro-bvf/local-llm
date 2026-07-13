package com.demo.assistant;

import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.tool.language.internal.MetamodelJsonSerializerImpl;
import org.hibernate.tool.language.spi.MetamodelSerializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link MetamodelSerializer} — the official Hibernate Assistant SPI extension point — that
 * controls exactly what the LLM is told about the domain. It <b>reuses Hibernate's own
 * {@link MetamodelJsonSerializerImpl}</b> for the format and simply removes the entities/fields
 * that must stay hidden.
 *
 * <p>This is the architectural advantage over an MCP wired straight to the database: there, the
 * model sees every table and column. Here the assistant is primed only with the curated view
 * this serializer emits — a hidden field or table does not exist in the model's world. The
 * decision lives in your domain layer, not in trusting the model or guarding raw SQL.
 */
public class FilteringMetamodelSerializer implements MetamodelSerializer {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final List<String> TYPE_GROUPS = List.of("entities", "mappedSuperclasses", "embeddables");

  private final MetamodelSerializer delegate;            // Hibernate's own serializer
  private final Set<String> hiddenTypes;                 // entity / embeddable names to drop entirely
  private final Map<String, Set<String>> hiddenFields;   // type name -> attribute names to drop

  public FilteringMetamodelSerializer(Set<String> hiddenTypes, Map<String, Set<String>> hiddenFields) {
    this(MetamodelJsonSerializerImpl.INSTANCE, hiddenTypes, hiddenFields);
  }

  public FilteringMetamodelSerializer(
      MetamodelSerializer delegate,
      Set<String> hiddenTypes,
      Map<String, Set<String>> hiddenFields) {
    this.delegate = delegate;
    this.hiddenTypes = hiddenTypes == null ? Set.of() : hiddenTypes;
    this.hiddenFields = hiddenFields == null ? Map.of() : hiddenFields;
  }

  @Override
  public String toString(Metamodel metamodel) {
    // 1) let Hibernate Assistant produce the full metamodel JSON, 2) strip the hidden parts
    final JsonNode root = MAPPER.readTree(delegate.toString(metamodel));
    if (root instanceof ObjectNode obj) {
      for (final String group : TYPE_GROUPS) {
        if (obj.path(group).isArray()) {
          obj.set(group, filterTypes((ArrayNode) obj.get(group)));
        }
      }
    }
    return MAPPER.writeValueAsString(root);
  }

  private ArrayNode filterTypes(ArrayNode types) {
    final ArrayNode kept = MAPPER.createArrayNode();
    for (final JsonNode type : types) {
      final String name = type.path("name").asString();
      if (hiddenTypes.contains(name)) {
        continue; // whole entity / table hidden from the LLM
      }
      final Set<String> hidden = hiddenFields.get(name);
      if (hidden != null && !hidden.isEmpty()
          && type.path("attributes").isArray()
          && type instanceof ObjectNode objType) {
        final ArrayNode keptAttrs = MAPPER.createArrayNode();
        for (final JsonNode attr : type.get("attributes")) {
          if (!hidden.contains(attr.path("name").asString())) {
            keptAttrs.add(attr);
          }
        }
        objType.set("attributes", keptAttrs);
      }
      kept.add(type);
    }
    return kept;
  }
}
