# GracefulShutdown

Distributed transaction pipeline with three Spring Boot services:

- **central**: transaction generator + TCP server + metrics/dashboard
- **adapter**: TCP client to Central, HTTP client to PROCR, graceful drain/shutdown
- **procr**: transaction decision service

## Current status

All three modules are implemented and buildable.

## Runtime topology

```text
Central (TCP server :5009, Web :5000)
        ⇅ JSON over TCP
Adapter (Web :9090)
        ⇅ JSON over HTTP
PROCR   (Web :8080)
```

## Repository layout

```text
gracefulShutdown/
├── adapter/
├── central/
├── procr/
├── db/
├── log/
├── logs/
├── LEARNINGS.md
├── LICENSE
└── README.md
```

## Build

```bash
cd central && mvn clean package
cd ../adapter && mvn clean package
cd ../procr && mvn clean package
```

Or from root:

```bash
./build-all.sh
```

## Run

From root:

```bash
./start-all.sh
```

Stop:

```bash
./kill.sh
```

## Key ports

| Service | Purpose | Port |
|---|---|---:|
| central | Web/API | 5000 |
| central | TCP transaction socket | 5009 |
| adapter | Web/health/metrics | 9090 |
| procr | Web/process/health | 8080 |

## Graceful shutdown behavior (current)

- Spring shutdown mode enabled across services (`server.shutdown=graceful`)
- Lifecycle phase timeout set to `10s`
- Adapter sends `SESSION_END`, performs a mandatory wait, drains pending work, then closes threads/connections

## Core config locations

- `central/src/main/resources/application.properties`
- `adapter/src/main/resources/application.properties`
- `procr/src/main/resources/application.properties`

## Main docs

- Central requirements/review: `central/docs/`
- Adapter requirements/review: `adapter/docs/`
- PROCR requirements/guide: `procr/docs/`
- Consolidated notes: `LEARNINGS.md`

## License

MIT — see `LICENSE`.

---

Last updated: 2026-10-24
