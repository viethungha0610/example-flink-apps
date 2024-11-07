localdev-up:
	docker-compose up -d

localdev-down:
	docker-compose down

.PHONY: localdev-up localdev-down