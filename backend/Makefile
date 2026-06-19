# Psy backend — dev helpers. Run from the backend/ directory.
# Quick start:  make dev   (starts Postgres + runs the server)
SHELL := /bin/bash
-include .env   # optional local overrides (gitignored): PORT, JWT_SECRET, GOOGLE_CLIENT_ID, DATABASE_URL

PORT             ?= 8080
DATABASE_URL     ?= postgres://psy:psy@localhost:5433/psy?sslmode=disable
JWT_SECRET       ?= dev-secret-change-me
GOOGLE_CLIENT_ID ?= 885786271406-u79onredebj2udsoaen122vphe3pk8hd.apps.googleusercontent.com

.PHONY: help db-up db-down db-reset db-logs psql server dev test build vet tidy

help: ## Show available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

db-up: ## Start Postgres (Docker, :5433, persistent volume) and wait until ready
	docker compose up -d
	@echo "waiting for postgres..."; until docker compose exec -T postgres pg_isready -U psy -d psy >/dev/null 2>&1; do sleep 1; done; echo "postgres ready on :5433"

db-down: ## Stop Postgres (keep data)
	docker compose down

db-reset: ## Stop Postgres and DELETE all data (drops the volume)
	docker compose down -v

db-logs: ## Tail Postgres logs
	docker compose logs -f postgres

psql: ## Open a psql shell in the Postgres container
	docker compose exec postgres psql -U psy -d psy

server: ## Run the Go server (requires Postgres up)
	PORT=$(PORT) DATABASE_URL="$(DATABASE_URL)" JWT_SECRET=$(JWT_SECRET) GOOGLE_CLIENT_ID=$(GOOGLE_CLIENT_ID) go run ./cmd/server

dev: db-up server ## Start Postgres then run the server (one command)

test: ## Run Go tests (set TEST_DATABASE_URL to run DB-gated tests)
	go test ./...

build: ## Compile everything
	go build ./...

vet: ## go vet
	go vet ./...

tidy: ## go mod tidy
	go mod tidy
