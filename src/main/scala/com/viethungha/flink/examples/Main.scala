package com.viethungha.flink.examples

import org.apache.flink.configuration.{Configuration, RestOptions}
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment

import scala.jdk.CollectionConverters._

object Main {

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

    // streamEnv.execute(this.getClass.getName)
  }
}
