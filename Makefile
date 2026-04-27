# Makefile for Philterd Policy Editor

# Variables
APP_NAME = philterd-policy-editor
DOCKER_IMAGE = philterd/philterd-redaction-policy-editor
VERSION = 0.0.1-SNAPSHOT

.PHONY: all build-jar build-docker push-docker clean test

# Default target
all: build-jar build-docker

# Build the JAR using Maven
build-jar:
	mvn clean package

# Run the app
run:
	mvn spring-boot:run

# Build the Docker image
build-docker:
	docker build -t $(DOCKER_IMAGE):$(VERSION) -t $(DOCKER_IMAGE):latest . ;

# Push the Docker image to DockerHub
push-docker:
	docker push $(DOCKER_IMAGE):$(VERSION) ;
	docker push $(DOCKER_IMAGE):latest ;

# Clean the Maven build artifacts
clean:
	mvn clean

# Run tests
test:
	mvn test
