package com.viethungha.flink.examples.datagen

import com.viethungha.flink.examples.getAddress
import com.viethungha.flink.examples.models.AggregatedPageviewEvent
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient

object SchemaRegistration {

  def main(args: Array[String]): Unit = {
    val csrUrl    = s"http://${getAddress.getOrElse("localhost")}:8081"
    val csrClient = new CachedSchemaRegistryClient(csrUrl, 10)

    csrClient.register("AggregatedPageviewEvent-value", new AvroSchema(AggregatedPageviewEvent.schema.toString))
  }

}
