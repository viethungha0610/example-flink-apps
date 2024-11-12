package com.viethungha.flink.examples.functions

import com.viethungha.flink.examples.models.{AggregatedPageviewEvent, PageviewEvent}
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import java.lang
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import scala.jdk.CollectionConverters._

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
