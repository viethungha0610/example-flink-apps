# Instructions

## Local environment

1. Start colima `colima start --network-address`
2. Start `make localdev-up`
   - After this, `colima status` should show an IP from which we can use to ping the local containers 

## Production considerations (if I had more time)
- Sink: potentially use Iceberg Sink Connector (or implement the Sink logic in Flink directly) instead of S3 file landing (effectively a Hive data lake)
- Streaming application
  - Dead letter queues mechanisms for main pipes
