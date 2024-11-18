package com.viethungha.flink.examples.functions

import com.viethungha.flink.examples.PageviewAgg
import com.viethungha.flink.examples.models.{AggregatedPageviewEvent, PageviewEvent}
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util
import java.util.Collections
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava}

class PageviewProcessWindowFunctionTest extends AnyWordSpecLike with Matchers with BeforeAndAfter {

  private val minutesInMillis = 60000L

  val flinkCluster = new MiniClusterWithClientResource(
    new MiniClusterResourceConfiguration.Builder().setNumberSlotsPerTaskManager(2).setNumberTaskManagers(1).build()
  )

  before {
    flinkCluster.before()
  }

  after {
    flinkCluster.after()
  }

  "PageviewProcessWindowFunction" should {
    "aggregate pageview events correctly" in {

      val env = StreamExecutionEnvironment.getExecutionEnvironment
      env.setParallelism(2)

      val baseTimestamp = 0L

      val baseStream = env
        .fromData[PageviewEvent](
          List(
            PageviewEvent("1", "EC1", "https://example.com", baseTimestamp),
            PageviewEvent("2", "EC1", "https://example.com", baseTimestamp  + (0.2 * minutesInMillis).toLong),
            PageviewEvent("4", "EC1", "https://example.com", baseTimestamp  + (0.5 * minutesInMillis).toLong),
            PageviewEvent("7", "SE18", "https://example.com", baseTimestamp + (1.5 * minutesInMillis).toLong),
            PageviewEvent("3", "SW1", "https://example.com", baseTimestamp  + (2.05 * minutesInMillis).toLong),
            PageviewEvent("5", "SW1", "https://example.com", baseTimestamp  + (2.5 * minutesInMillis).toLong)
          ).asJavaCollection
        )
        .assignTimestampsAndWatermarks(
          PageviewAgg.pageviewWatermarkStrategy
        )

      val windowedStream = PageviewAgg.windowPageviewStream(baseStream)

      env.execute()

      val output = windowedStream.executeAndCollect(10)

      output.size() shouldBe 3
      output.asScala.toSet shouldBe Set(
        AggregatedPageviewEvent("EC1", 3L, 0L, 60000L, "1970-01-01"),       // first minute
        AggregatedPageviewEvent("SE18", 1L, 60000L, 120000L, "1970-01-01"), // second minute
        AggregatedPageviewEvent("SW1", 2L, 120000L, 180000L, "1970-01-01")  // third minute
      )
    }
  }
}
