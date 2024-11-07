package com.viethungha.flink

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.io.Source

package object examples {

  val mapper: JsonMapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    .build()

  def fromFile(path: String): String = {
    val source = Source.fromFile(path)
    val j = source.mkString
    source.close()
    j
  }
}
