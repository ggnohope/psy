# --- build ---
FROM golang:1.26 AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
# Pure-Go (pgx); static binary, migrations are embedded via //go:embed.
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -o /out/server ./cmd/server

# --- run ---
FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /out/server /server
EXPOSE 8080
USER nonroot:nonroot
ENTRYPOINT ["/server"]
