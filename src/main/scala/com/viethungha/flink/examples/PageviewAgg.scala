package com.viethungha.flink.examples

import com.viethungha.flink.examples.models.{AggregatedPageviewEvent, PageviewEvent}
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.configuration.{Configuration, RestOptions}
import org.apache.flink.connector.kafka.sink.KafkaSink
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.assigners._
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import java.lang
import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, ZoneId}
import scala.jdk.CollectionConverters._

object PageviewAgg {

  private class PageviewProcessWindowFunction
      extends ProcessWindowFunction[PageviewEvent, AggregatedPageviewEvent, String, TimeWindow] {
    override def process(
      key: String,
      context: ProcessWindowFunction[PageviewEvent, AggregatedPageviewEvent, String, TimeWindow]#Context,
      elements: lang.Iterable[PageviewEvent],
      out: Collector[AggregatedPageviewEvent]
    ): Unit =
      out.collect(
        AggregatedPageviewEvent(
          postcode = key,
          viewCount = elements.asScala.size,
          windowStart = context.window().getStart,
          windowEnd = context.window().getEnd,
          year_month_day = Instant
            .ofEpochMilli(context.window().getStart)
            .atZone(ZoneId.of("UTC"))
            .format(
              DateTimeFormatter.ISO_LOCAL_DATE
            )
        )
      )
  }

  def main(args: Array[String]): Unit = {

    val localhost = getAddress.getOrElse("localhost")
    println(s"Running pipeline locally at $localhost")
    val bootstrapServers = s"$localhost:9092"
    val csrUrl           = s"http://$localhost:8081"
    val classpath = System.getProperty("java.class.path")
    println("Classpath:")
    classpath.split(java.io.File.pathSeparator).foreach(println)

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
      .setStartingOffsets(OffsetsInitializer.latest())
      .setDeserializer(PageviewEvent.kafkaDeserializationSchema)
      .build()

    val sourceStream: SingleOutputStreamOperator[PageviewEvent] =
      streamEnv
        .fromSource(
          kafkaSource,
          WatermarkStrategy
            .forBoundedOutOfOrderness[PageviewEvent](Duration.ofMillis(500)) // important to include the type
            .withTimestampAssigner(new SerializableTimestampAssigner[PageviewEvent] {
              override def extractTimestamp(pageview: PageviewEvent, recordTimestamp: Long): Long =
                pageview.timestamp
            }),
          "Kafka source"
        )
        .uid("Kafka source")

    val windowedStream = sourceStream
      .keyBy((value: PageviewEvent) => value.postcode)
      .window(TumblingEventTimeWindows.of(Duration.ofSeconds(20)))
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
