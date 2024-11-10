package com.viethungha.flink.examples.models

case class AggregatedPageviewEvent(
  postcode: String,
  viewCount: Long,
  windowStart: Long,
  windowEnd: Long
)
