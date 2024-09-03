# Stop the service
stop:
	docker compose stop $(SERVICE_NAME)

# Remove the service (optional, in case you want to ensure it's fully removed before recreating)
rm:
	docker compose rm $(SERVICE_NAME)

# Recreate the service
up: stop rm
	docker compose up -d $(SERVICE_NAME)

# Combined target to stop, remove, and recreate the service
update: stop rm up


.PHONY: help
help:
	@echo "  up | startup		fullrun"
	@echo "  update"

.PHONY: startup
startup: up

start:
	docker compose up chatspuffer

minstall:
	mvn clean install

build:
	mvn clean package

dbuild:
	docker rm chatspuffer
	docker rmi chatspuffer
	docker build -t chatspuffer .
	docker run --name chatspuffer -d chatspuffer


