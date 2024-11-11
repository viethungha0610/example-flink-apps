localdev-up:
	docker-compose up -d

localdev-down:
	docker-compose down

run-data-gen:
	sbt "runMain com.viethungha.flink.examples.datagen.PageviewDataGen"

run-pageview-agg:
	sbt "runMain com.viethungha.flink.examples.PageviewAgg"

apply-localstack:
	cd infra && terraform init && terraform apply -auto-approve

plan-localstack:
	cd infra && terraform init && terraform apply -auto-approve

create-connectors:
	curl -X PUT -H "Content-Type: application/json" \
      --data @connectors/PageviewEvent.json \
      http://192.168.106.2:8083/connectors/PageviewEvent/config && \
    curl -X PUT -H "Content-Type: application/json" \
	  --data @connectors/AggregatedPageviewEvent.json \
	  http://192.168.106.2:8083/connectors/AggregatedPageviewEvent/config

.PHONY: localdev-up localdev-down run-data-gen apply-localstack plan-localstack