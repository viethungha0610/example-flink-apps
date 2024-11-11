package com.viethungha.flink.examples.datagen

import com.fasterxml.jackson.databind.JsonNode
import com.viethungha.flink.examples._
import com.viethungha.flink.examples.models.PageviewEvent
import io.confluent.kafka.schemaregistry.json.{JsonSchema, JsonSchemaUtils}
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import java.util.Properties
import scala.io.Source
import scala.util.Random

object PageviewDataGen {

  def main(args: Array[String]): Unit = {
    println(getAddress)
    getAddress match {
      case Some(ip) =>
        println("Running pipeline with Colima (network address enabled)")
        runPipeline(ip)
      case None =>
        println("Running pipeline with Docker local")
        runPipeline("127.0.0.1")
    }
  }

  private def createKafkaProducer(
    bootstrapServers: String,
    schemaRegistryUrl: String
  ): KafkaProducer[String, JsonNode] = {
    val props = new Properties()

    // Standard Kafka Producer properties
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(
      AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY,
      "io.confluent.kafka.serializers.subject.TopicNameStrategy"
    )
    props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, "true")
    // TODO - use this KafkaJsonSchemaSerializer here so that messages have structured schema, which will be useful downstream if we want to land these in an Iceberg table
    //  e.g. Iceberg Kafka Connector requires messages with Schema to build Struct columns
    props.put(
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      "io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer"
    )
    props.put(
      ProducerConfig.MAX_REQUEST_SIZE_CONFIG,
      "8388608"
    )
    props.put("schema.registry.url", schemaRegistryUrl)

    // Create and return the Kafka producer
    new KafkaProducer[String, JsonNode](props)
  }

  private val domains = List("com", "org", "net", "io", "co.uk")
  private val paths   = List("index.html", "about.html", "contact.html", "products.html", "services.html")

  private def randomString(length: Int): String = {
    val chars = ('a' to 'z') ++ ('0' to '9')
    (1 to length).map(_ => chars(Random.nextInt(chars.length))).mkString
  }

  private def generateRandomUrl(): String = {
    val subdomain = randomString(5)
    val domain    = randomString(7)
    val tld       = domains(Random.nextInt(domains.length))
    val path      = paths(Random.nextInt(paths.length))
    s"www.$subdomain.$domain.$tld/$path"
  }

  private def readPostcodes(): List[String] = {
    val resourceStream = getClass.getResourceAsStream("/postcodes.csv")
    val bufferedSource = Source.fromInputStream(resourceStream)
    val postcodes = bufferedSource
      .getLines()
      .drop(1)
      .map { line =>
        val cols = line.split(",").map(_.trim.stripSuffix("\"").stripPrefix("\""))
        cols(0) // Extracts the first column (postcode)
      }
      .toList
    bufferedSource.close()
    postcodes
  }

  private def generateRandomTimestamp(): Long = {
    val currentTimeMillis = System.currentTimeMillis()
    val durationMillis    = 5 * 60 * 1000 // 5 mins
    val durationAgoMillis = currentTimeMillis - durationMillis

    val randomMillis = durationAgoMillis + Random.nextLong(durationMillis)
    randomMillis
  }

  private def generateRandomPageviewEvent(postcodes: List[String]): PageviewEvent = {

//    val timestamp = generateRandomTimestamp()
    val timestamp = Instant.now().toEpochMilli
    val datetime = Instant
      .ofEpochMilli(timestamp)
      .atZone(ZoneId.of("UTC"))
      .format(
        DateTimeFormatter.ISO_DATE_TIME
      )

    PageviewEvent(
      user_id = math.abs(Random.nextInt()).toString,
      postcode = postcodes(Random.nextInt(postcodes.length)),
      webpage = generateRandomUrl(),
      timestamp = timestamp,
      datetime = datetime
    )
  }

  private def produceJsonToKafka(
    producer: KafkaProducer[String, JsonNode],
    topic: String,
    payloadList: List[JsonNode],
    schema: JsonSchema
  ): Unit = {
    // Iterate through the list of JSON objects
    payloadList.foreach { json =>
      // Create a Kafka producer record, using a key (null here, can be replaced)
      val serialized = JsonSchemaUtils.envelope(schema, json)
      val record = new ProducerRecord[String, JsonNode](
        topic,
        null,
        serialized
      )

      // Send the record asynchronously to Kafka
      producer.send(
        record,
        (metadata: RecordMetadata, exception: Exception) =>
          if (exception == null)
            println(
              s"Produced record to topic ${metadata.topic}, partition ${metadata.partition}, offset ${metadata.offset}, value ${json}"
            )
          else
            println(s"Failed to produce record: ${exception.getMessage}")
      )
    }

    // Optionally flush and close the producer if needed (if one-time use)
    producer.flush()
  }

  private def runPipeline(ip: String): Unit = {
    val producer = createKafkaProducer(
      s"$ip:9092",
      s"http://$ip:8081"
    )

    val postcodes = readPostcodes()
    while (true) {
      val samples = (1 to Random.nextInt(50)).map { _ =>
        mapper.valueToTree[JsonNode](generateRandomPageviewEvent(postcodes))
      }.toList
      produceJsonToKafka(producer, PageviewEvent.title, samples, PageviewEvent.jsonSchema)
      Thread.sleep(10000)
    }
  }
}
