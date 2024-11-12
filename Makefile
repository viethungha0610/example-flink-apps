localdev-up:
	docker-compose up -d

localdev-down:
	docker-compose down

run-data-gen:
	sbt "runMain com.viethungha.flink.examples.PageviewDataGen"

run-pageview-agg:
	sbt "runMain com.viethungha.flink.examples.PageviewAgg"

apply-localstack:
	cd infra && terraform init && terraform apply -auto-approve

plan-localstack:
	cd infra && terraform init && terraform apply -auto-approve

create-connectors:
	docker container exec connect curl -X PUT -H "Content-Type: application/json" --data @/tmp/connectors/PageviewEvent.json http://localhost:8083/connectors/PageviewEvent/config && \
    docker container exec connect curl -X PUT -H "Content-Type: application/json" --data @/tmp/connectors/AggregatedPageviewEvent.json http://localhost:8083/connectors/AggregatedPageviewEvent/config

.PHONY: localdev-up localdev-down run-data-gen apply-localstack plan-localstack create-connectors