# example-flink-apps

## Overview

This project shows a local Flink pipeline to ingest Pageview data from Kafka, transform it to an aggregated stream, and
then use Kafka Connectors to run sink jobs to local S3 (localstack) as parquet files partitioned by process date.

## Prerequisites

Setup IDE (IntelliJ), SBT and use **Java 11** for this project.

- Docker:
    - Recommended Docker runtime is Colima (but Docker/Rancher Desktop should also be fine, the code does handle this).
- If you use [colima](https://github.com/abiosoft/colima):
    - Start colima with network address `colima start --network-address`
        - After this, `colima ls -j | jq -r .address` should show an IP address from which we can use to ping the local
          containers

### Pipeline setup steps

Infra setup:

1. Once Docker, SBT and JDK has been setup. Run `make localdev-up` to spin up the local containers:
    - Kafka broker & zookeeper
    - Kafka Connect (for S3 sink)
    - Confluent Schema Registry
    - Localstack (for local S3 infra)
2. Once the containers are ready, run `make apply-localstack` to create the local S3 buckets in localstack

Run the pipeline:
3. Once infra has been setup, on a terminal window, run `make run-data-gen` to start the Pageview data generator
   process.
4. Navigate to the [PageviewAgg class](src/main/scala/com/viethungha/flink/examples/PageviewAgg.scala) and run it from
   IntelliJ. Since some dependencies are in the "provided" scope (as this is meant to be run on Kinesis Data Analytics),
   edit
   IntelliJ's run configuration to `Add dependencies with 'provided' scope to classpath` (screenshot below).
![img.png](img.png)
5. Once the data generation and aggregation pipelines are running, we can create connectors to sink the Kafka data in S3, by running `make create-connectors`

See the file landing in localstack S3:
6. Seeing the data:
   - Raw pageview data -> Run `aws --endpoint-url=http://localhost:4566 s3 ls lakehouse-raw-bucket`
   - Aggregated pageview data -> Run `aws --endpoint-url=http://localhost:4566 s3 ls lakehouse-agg-bucket`

### Teardown environment
- Stop existing pipelines (Pageview data generation and aggregation), and then run `make localdev-down`

## Running this production
- Sink: potentially use Iceberg Sink Connector (or implement the Sink logic in Flink directly) instead of S3 file
  landing (effectively a Hive data lake)
- Streaming application
    - Dead letter queues mechanisms for main pipes (use Flink)
      using [side output](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/side_output/)
