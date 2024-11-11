# Image for Kafka Connect
FROM confluentinc/cp-kafka-connect:7.5.0

RUN curl -L -o /tmp/confluentinc-kafka-connect-s3-10.5.17.zip \
    https://d2p6pa21dvn84.cloudfront.net/api/plugins/confluentinc/kafka-connect-s3/versions/10.5.17/confluentinc-kafka-connect-s3-10.5.17.zip

RUN confluent-hub install --no-prompt /tmp/confluentinc-kafka-connect-s3-10.5.17.zip
