package com.viethungha.flink.examples

import com.viethungha.flink.examples.models.PageviewEvent
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.configuration.{Configuration, RestOptions}
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows

import java.time.{Duration, Instant}
import scala.jdk.CollectionConverters._

object PageviewAgg {

  val pageviewWatermarkStrategy: WatermarkStrategy[PageviewEvent] = WatermarkStrategy
    .forBoundedOutOfOrderness(Duration.ofMinutes(60))
    .withTimestampAssigner(new SerializableTimestampAssigner[PageviewEvent] {
      override def extractTimestamp(element: PageviewEvent, recordTimestamp: Long): Long =
        Instant.ofEpochMilli(element.timestamp).toEpochMilli // TODO - review zone
    })

  class PageviewAggregateFunction extends AggregateFunction[PageviewEvent, Long, Long] {
    override def createAccumulator(): Long = 0L

    override def add(value: PageviewEvent, accumulator: Long): Long = accumulator + 1

    override def getResult(accumulator: Long): Long = accumulator

    override def merge(a: Long, b: Long): Long = a + b
  }

//  class PageviewAggregateFunctionV2 extends RichAggregateFunction[PageviewEvent, Long, AggregatedPageviewEvent] {
//    override def createAccumulator(): Long = 0L
//
//    override def add(value: PageviewEvent, accumulator: Long): Long = accumulator + 1
//
//    override def getResult(accumulator: Long): AggregatedPageviewEvent = {
//      val cxt = getRuntimeContext
//      AggregatedPageviewEvent(postcode = ???, viewCount = accumulator)
//    }
//
//    override def merge(a: Long, b: Long): Long = a + b
//  }

  def main(args: Array[String]): Unit = {
    val conf = Configuration.fromMap(
      Map(
        RestOptions.ENABLE_FLAMEGRAPH.key() -> "true",
        RestOptions.PORT.key()              -> 11000.toString
      ).asJava
    )
    val streamEnv = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf)
    streamEnv.setParallelism(2)
    streamEnv.disableOperatorChaining()

    val kafkaSource = KafkaSource
      .builder[PageviewEvent]()
      .setBootstrapServers("192.168.106.2:9092") // TODO - get proper address
      .setTopics("PageviewEvent")
      .setGroupId("pageview-agg")
      .setStartingOffsets(OffsetsInitializer.earliest())
      .setDeserializer(PageviewEvent.kafkaDeserializationSchema)
      .build()

    val sourceStream: SingleOutputStreamOperator[PageviewEvent] =
      streamEnv.fromSource(kafkaSource, pageviewWatermarkStrategy, "Kafka source").uid("Kafka source")

    val windowedStream = sourceStream
      .keyBy((value: PageviewEvent) => value.postcode)
      .window(TumblingEventTimeWindows.of(Duration.ofSeconds(1)))
      .aggregate(new PageviewAggregateFunction)

    windowedStream.print()

    // Execution plan
    println(streamEnv.getExecutionPlan)

    streamEnv.execute()
  }
}
