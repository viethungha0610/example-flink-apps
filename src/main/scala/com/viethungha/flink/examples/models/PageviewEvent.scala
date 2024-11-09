package com.viethungha.flink.examples.models

import com.github.andyglow.json.JsonFormatter
import com.github.andyglow.jsonschema.AsValue
import com.viethungha.flink.examples.mapper
import io.confluent.kafka.schemaregistry.json.JsonSchema
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema
import org.apache.flink.util.Collector
import org.apache.kafka.clients.consumer.ConsumerRecord

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

  // TODO - potentially use the io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer
  //  but since we know have the case class definition directly, we can just directly deserialize the messages by
  //  skipping the first 5 bytes
  val kafkaDeserializationSchema: KafkaRecordDeserializationSchema[PageviewEvent] =
    new KafkaRecordDeserializationSchema[PageviewEvent] {
      override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]], out: Collector[PageviewEvent]): Unit =
        out.collect {
          // Skip the first 5 bytes (magic bytes + id) to get the record directly
          mapper.readValue(record.value().drop(5), classOf[PageviewEvent])

        }

      override def getProducedType: TypeInformation[PageviewEvent] = TypeInformation.of(new TypeHint[PageviewEvent] {})
    }
}
