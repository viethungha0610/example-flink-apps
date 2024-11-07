package com.viethungha.flink.examples.models

import com.github.andyglow.json.JsonFormatter
import com.github.andyglow.jsonschema.AsValue
import io.confluent.kafka.schemaregistry.json.JsonSchema

case class PageviewEvent(
  user_id: String,
  postcode: String,
  webpage: String,
  timestamp: Long
)

object PageviewEvent {
  val title: String = this.getClass.getSimpleName.stripSuffix("$")
  private val internalSchema: json.Schema[PageviewEvent] =
    json.Json.objectSchema[PageviewEvent]().withTitle(title)
  private val internalSchemaStr: String =
    JsonFormatter.format(AsValue.schema(internalSchema, json.schema.Version.Draft04()))
  val jsonSchema: JsonSchema = new JsonSchema(internalSchemaStr)
}
