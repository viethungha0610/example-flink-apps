package com.viethungha.flink.examples

import com.viethungha.flink.examples.models.{AggregatedPageviewEvent, PageviewEvent}
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.configuration.{Configuration, RestOptions}
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.assigners._
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import java.lang
import java.time.Duration
import scala.jdk.CollectionConverters._

object PageviewAgg {

  class PageviewAggregateFunction extends AggregateFunction[PageviewEvent, Long, Long] {
    override def createAccumulator(): Long = 0L

    override def add(value: PageviewEvent, accumulator: Long): Long = accumulator + 1

    override def getResult(accumulator: Long): Long = accumulator

    override def merge(a: Long, b: Long): Long = a + b
  }

  class PageviewDebugProcessWindowFunction extends ProcessWindowFunction[PageviewEvent, String, String, TimeWindow] {
    override def process(
      key: String,
      context: ProcessWindowFunction[PageviewEvent, String, String, TimeWindow]#Context,
      elements: lang.Iterable[PageviewEvent],
      out: Collector[String]
    ): Unit =
      out.collect(
        s"Window: ${context.window().getStart} - ${context.window().getEnd} -- Key: $key -- Count: ${elements.asScala.count(_ => true)}"
      )
  }

  class PageviewProcessWindowFunction
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
          windowEnd = context.window().getEnd
        )
      )
  }

  def main(args: Array[String]): Unit = {
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
      .setBootstrapServers("192.168.106.2:9092") // TODO - get proper address
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
            .forBoundedOutOfOrderness[PageviewEvent](Duration.ofMillis(500))
            .withTimestampAssigner(new SerializableTimestampAssigner[PageviewEvent] {
              override def extractTimestamp(pageview: PageviewEvent, recordTimestamp: Long): Long =
                pageview.timestamp
            }),
          "Kafka source"
        )
        .uid("Kafka source")

    val windowedStream = sourceStream
      .keyBy((value: PageviewEvent) => value.postcode)
      .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
      .process(new PageviewProcessWindowFunction())

    windowedStream.print()

    // Execution plan
    println(streamEnv.getExecutionPlan)

    streamEnv.execute()
  }
}
