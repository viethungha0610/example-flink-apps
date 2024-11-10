package com.viethungha.flink.examples.models

import com.sksamuel.avro4s.AvroSchema
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroSerializationSchema
import org.apache.kafka.clients.producer.ProducerRecord

case class AggregatedPageviewEvent(
  postcode: String,
  viewCount: Long,
  windowStart: Long,
  windowEnd: Long,
  year_month_day: String
)

object AggregatedPageviewEvent {
  val schema: Schema = AvroSchema[AggregatedPageviewEvent]

  def convertToAvro(event: AggregatedPageviewEvent): GenericRecord = {
    val record = new GenericData.Record(schema)
    record.put("postcode", event.postcode)
    record.put("viewCount", event.viewCount)
    record.put("windowStart", event.windowStart)
    record.put("windowEnd", event.windowEnd)
    record.put("year_month_day", event.year_month_day)
    record
  }

  /**
    * Custom Avro serialization schema for this class
    * @param topic name of the target Kafka topic
    * @param csrUrl url of Confluent Schema Registry
    */
  class CustomKafkaAvroSerializer(topic: String, csrUrl: String)
      extends KafkaRecordSerializationSchema[AggregatedPageviewEvent] {

    private val serializer: ConfluentRegistryAvroSerializationSchema[GenericRecord] =
      ConfluentRegistryAvroSerializationSchema.forGeneric(s"$topic-value", AggregatedPageviewEvent.schema, csrUrl)

    override def serialize(
      element: AggregatedPageviewEvent,
      context: KafkaRecordSerializationSchema.KafkaSinkContext,
      timestamp: java.lang.Long
    ): ProducerRecord[Array[Byte], Array[Byte]] =
      new ProducerRecord[Array[Byte], Array[Byte]](
        topic,
        null,
        serializer.serialize(AggregatedPageviewEvent.convertToAvro(element))
      )
  }
}
