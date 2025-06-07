import sbt.*

object Dependencies {
  object V {
    val flink           = "1.19.1"
    val kafka           = "3.8.0"
    val kafkaSerializer = "7.4.1"
    val schemaRegistry  = "7.7.0"
    val jackson         = "2.18.0"
    val jsonSchema      = "0.7.11"
    val flinkKafka      = "3.2.0-1.19"
    val avro4s          = "4.1.2"
    val scalaTest       = "3.2.19"
  }

  private val testDeps = Seq(
    "org.scalatest"     %% "scalatest"        % V.scalaTest         % Test,
    "org.scalatestplus" %% "mockito-5-12"     % s"${V.scalaTest}.0" % Test,
    "org.apache.flink"   % "flink-test-utils" % V.flink             % Test,
    "org.apache.flink"   % "flink-runtime"    % V.flink             % Test
  )

  private val flinkDeps = Seq(
    // Some dependencies are provided since in prod, the assembled jar can be deployed on AWS managed flink (formerly Kinesis Data Analytics)
    "org.apache.flink" % "flink-streaming-java"          % V.flink % "provided",
    "org.apache.flink" % "flink-core"                    % V.flink % "provided",
    "org.apache.flink" % "flink-table-api-java-bridge"   % V.flink % "provided",
    "org.apache.flink" % "flink-java"                    % V.flink,
    "org.apache.flink" % "flink-clients"                 % V.flink,
    "org.apache.flink" % "flink-runtime-web"             % V.flink,
    "org.apache.flink" % "flink-table-runtime"           % V.flink,
    "org.apache.flink" % "flink-avro-confluent-registry" % V.flink,
    "org.apache.flink" % "flink-connector-base"          % V.flink,
    "org.apache.flink" % "flink-connector-kafka"         % V.flinkKafka
  )

  private val kafkaDeps = Seq(
    "org.apache.kafka" % "kafka-clients"                % V.kafka,
    "io.confluent"     % "kafka-schema-registry-client" % V.schemaRegistry,
    "io.confluent"     % "kafka-json-schema-serializer" % V.kafkaSerializer,
    "io.confluent"     % "kafka-avro-serializer"        % V.kafkaSerializer
  )

  private val jsonDeps = Seq(
    "com.fasterxml.jackson.core"    % "jackson-databind"     % V.jackson,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % V.jackson,
    "com.github.andyglow"          %% "scala-jsonschema"     % V.jsonSchema,
    "com.sksamuel.avro4s"          %% "avro4s-core"          % V.avro4s
  )

  val deps: Seq[ModuleID] = flinkDeps ++ kafkaDeps ++ jsonDeps ++ testDeps
}
