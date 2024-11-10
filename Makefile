localdev-up:
	docker-compose up -d

localdev-down:
	docker-compose down

run-data-gen:
	sbt "runMain com.viethungha.flink.examples.datagen.PageviewDataGen"

run-pageview-agg:
	sbt "runMain com.viethungha.flink.examples.PageviewAgg"

.PHONY: localdev-up localdev-down run-data-gen