import sbt.*

object Dependencies {
  object V {
    val flink                = "1.19.1"
    val kafka                = "3.8.0"
    val jsonSchemaSerializer = "7.4.1"
    val schemaRegistry       = "7.7.0"
    val jackson              = "2.18.0"
    val jsonSchema           = "0.7.11"
    val flinkKafka           = "3.2.0-1.19"
  }

  private val flinkDeps = Seq(
    "org.apache.flink" % "flink-streaming-java"        % V.flink % "provided",
    "org.apache.flink" % "flink-core"                  % V.flink % "provided",
    "org.apache.flink" % "flink-table-api-java-bridge" % V.flink % "provided",
    "org.apache.flink" % "flink-java"                  % V.flink,
    "org.apache.flink" % "flink-connector-base"        % V.flink,
    "org.apache.flink" % "flink-clients"               % V.flink,
    "org.apache.flink" % "flink-runtime-web"           % V.flink,
    "org.apache.flink" % "flink-table-runtime"         % V.flink,
    "org.apache.flink" % "flink-connector-kafka"       % V.flinkKafka
  )

  private val kafkaDeps = Seq(
    "org.apache.kafka" % "kafka-clients"                % V.kafka,
    "io.confluent"     % "kafka-schema-registry-client" % V.schemaRegistry,
    "io.confluent"     % "kafka-json-schema-serializer" % V.jsonSchemaSerializer
  )

  private val jsonDeps = Seq(
    "com.fasterxml.jackson.core"    % "jackson-databind"     % V.jackson,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % V.jackson,
    "com.github.andyglow"          %% "scala-jsonschema"     % V.jsonSchema
  )

  val deps: Seq[ModuleID] = flinkDeps ++ kafkaDeps ++ jsonDeps
}
