package com.viethungha.flink

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.sys.process.Process

package object examples {
  val colimaList   = Process("colima ls -j")
  val queryAddress = Process("jq -r .address")

  def getAddress: Option[String] = {
    val result = (colimaList #| queryAddress).!!.trim
    if (result != "null" && result != "") Some(result) else None
  }

  val mapper: JsonMapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
}
