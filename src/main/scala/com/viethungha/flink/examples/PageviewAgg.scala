package com.viethungha.flink.examples

import com.viethungha.flink.examples.functions.PageviewProcessWindowFunction
import com.viethungha.flink.examples.models.{AggregatedPageviewEvent, PageviewEvent}
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.configuration.{Configuration, RestOptions}
import org.apache.flink.connector.kafka.sink.KafkaSink
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.assigners._

import java.time.Duration
import scala.jdk.CollectionConverters._

object PageviewAgg {
  def main(args: Array[String]): Unit = {

    val localhost        = getAddress.getOrElse("localhost")
    val bootstrapServers = s"$localhost:9092"
    val csrUrl           = s"http://$localhost:8081"
    println(s"Running pageview aggregate pipeline locally at $localhost, Kafka at $bootstrapServers and CSR at $csrUrl")

    val conf = Configuration.fromMap(
      Map(
        RestOptions.ENABLE_FLAMEGRAPH.key() -> "true",
        RestOptions.PORT.key()              -> 11000.toString
      ).asJava
    )
    val streamEnv = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf)
    streamEnv.setParallelism(1)
    streamEnv.disableOperatorChaining()

    val kafkaSource = KafkaSource
      .builder[PageviewEvent]()
      .setBootstrapServers(bootstrapServers) // TODO - get proper address
      .setTopics("PageviewEvent")
      .setGroupId("pageview-agg")
      .setStartingOffsets(OffsetsInitializer.earliest())
      .setDeserializer(PageviewEvent.kafkaDeserializationSchema)
      .build()

    val sourceStream: SingleOutputStreamOperator[PageviewEvent] =
      streamEnv
        .fromSource(
          kafkaSource,
          WatermarkStrategy
            .forBoundedOutOfOrderness[PageviewEvent](Duration.ofMinutes(5))
            .withTimestampAssigner(new SerializableTimestampAssigner[PageviewEvent] {
              override def extractTimestamp(pageview: PageviewEvent, recordTimestamp: Long): Long =
                pageview.timestamp
            }),
          "Kafka source"
        )
        .uid("kafka-pageview-source")

    val windowedStream = sourceStream
      .keyBy((value: PageviewEvent) => value.postcode)
      .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
      .process(new PageviewProcessWindowFunction())

    // Sink back to Kafka
    val kafkaSink = KafkaSink
      .builder[AggregatedPageviewEvent]()
      .setBootstrapServers(bootstrapServers)
      .setRecordSerializer(
        new AggregatedPageviewEvent.CustomKafkaAvroSerializer("AggregatedPageviewEvent", csrUrl)
      )
      .build()

    windowedStream.sinkTo(kafkaSink)

    // Execution plan
    println(streamEnv.getExecutionPlan)

    streamEnv.execute()
  }
}
