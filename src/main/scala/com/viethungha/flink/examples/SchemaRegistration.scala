package com.viethungha.flink.examples

import com.viethungha.flink.examples.models.PageviewEvent
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient

object SchemaRegistration {

  def main(args: Array[String]): Unit = {
    val csrEndpoint = "http://192.168.106.2:8081/"
    val csrClient = new CachedSchemaRegistryClient(csrEndpoint, 10)

    csrClient.register(PageviewEvent.title, PageviewEvent.jsonSchema)

    println(csrClient.getLatestSchemaMetadata(PageviewEvent.title))
  }
}
