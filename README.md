# Universal Tracker

An open-source, metadata-driven data platform. Define modules, collections, and custom fields to build any structured data system — trackers, CRMs, inventories, registries, project boards — without writing code.

Built on **Spring Boot 4 + PostgreSQL** using the **EAV (Entity–Attribute–Value) pattern** with typed storage, configurable validation, dynamic views, and an aggregation engine.

---

## What It Does

A Tracker lets you model *any* structured data domain through configuration instead of code. Rather than building a new database schema for every use case, you define **modules** (domains), **collections** (tables), and **custom fields** (columns) at runtime — the platform handles storage, validation, querying, and aggregation automatically.

**Example use cases you can spin up without touching code:**

- Bug / issue tracker
- CRM with contacts, deals, and pipelines
- Inventory management system
- Student or employee registry
- Project boards with custom statuses
- Habit / fitness / personal tracking dashboards
- Asset management for IT teams

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.3 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway (with PostgreSQL support) |
| Validation | Spring Boot Starter Validation |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Code Gen | Lombok |
| Containerization | Docker (multi-stage build) |
| Orchestration | Docker Compose |
| DB Functions | PLpgSQL (6.8% of codebase) |

---

## Architecture

```
┌─────────────────────────────────────────────┐
│                  API Layer                   │
│           (Spring Boot WebMVC)               │
│          REST endpoints + Swagger            │
├─────────────────────────────────────────────┤
│              Service Layer                   │
│    Validation engine · Aggregation engine    │
│    Dynamic views · Field type resolution     │
├─────────────────────────────────────────────┤
│               Data Layer                     │
│          Spring Data JPA + Flyway            │
├─────────────────────────────────────────────┤
│              PostgreSQL                      │
│   EAV tables · Typed storage · PLpgSQL       │
│   functions for aggregation & queries        │
└─────────────────────────────────────────────┘
```

---

## The Technical Structure: EAV Pattern

An **Entity–Attribute–Value (EAV)** model is a data model optimized for the space-efficient storage of sparse or ad-hoc property/data values. It is intended for situations where runtime usage patterns are arbitrary, subject to user variation, or otherwise unforeseeable using a fixed design.

In practice, production EAV systems have more than 3 tables. Magento (Adobe Commerce) optimizes by providing **separate value tables corresponding to data types** — such as `eav_entity_datetime`, `eav_entity_varchar`, `eav_entity_int`, and `eav_entity_decimal`. This is called **typed storage**: instead of shoving everything into a single `VARCHAR` column, you split values across type-specific tables so the database can do proper sorting, indexing, and aggregation natively.

Universal Tracker does the same thing with its typed storage approach in PostgreSQL + PLpgSQL functions.

### The Three Core Components

**Entity** — the "thing" being described (a product, a patient, a ticket, a customer). In clinical data, the entity is typically a clinical event. In more general-purpose settings, the entity is a foreign key into an "objects" table that records common information about every object in the database — at the minimum, a preferred name and brief description, as well as the category/class of entity to which it belongs.

**Attribute** — the "property" or "field" (name, price, color, due_date). Attributes are stored as *data*, not as columns. This is the key insight: new attributes = new rows, not new migrations.

**Value** — the actual data for a specific entity–attribute pair. In a row-modelled table, a value's data type is pre-determined by the column definition. In an EAV table, a value's data type depends on the attribute recorded in that particular row.

### How It Looks in Practice

**Traditional relational table** — fixed schema, every new field requires a migration:

```
┌────────────────────────────────────────────────────┐
│                    products                         │
├──────┬──────────┬───────┬───────┬─────────┬────────┤
│  id  │   name   │ price │ color │ weight  │  ...   │
├──────┼──────────┼───────┼───────┼─────────┼────────┤
│  1   │ Widget A │ 29.99 │ blue  │  0.5kg  │  ...   │
│  2   │ Widget B │ 49.99 │  red  │  1.2kg  │  ...   │
└──────┴──────────┴───────┴───────┴─────────┴────────┘
         ↑ Adding a new field = ALTER TABLE migration
```

**EAV model** — flexible schema, new fields are just new rows:

```
 entities              attributes             values (typed)
┌────┬──────────┐   ┌────┬──────────┬──────┐   ┌──────────┬─────┬──────┬───────┐
│ id │   name   │   │ id │   name   │ type │   │entity_id │attr │ type │ value │
├────┼──────────┤   ├────┼──────────┼──────┤   ├──────────┼─────┼──────┼───────┤
│  1 │ Widget A │   │ 10 │ price    │ DEC  │   │    1     │ 10  │ dec  │ 29.99 │
│  2 │ Widget B │   │ 11 │ color    │ TEXT │   │    1     │ 11  │ text │ blue  │
└────┴──────────┘   │ 12 │ weight   │ DEC  │   │    1     │ 12  │ dec  │  0.5  │
                    │ 13 │ rating   │ INT  │   │    2     │ 10  │ dec  │ 49.99 │
                    └────┴──────────┴──────┘   │    2     │ 11  │ text │  red  │
                     ↑ New field = INSERT row   └──────────┴─────┴──────┴───────┘
                       (no migration needed)     ↑ Values split by type for proper
                                                   indexing, sorting & aggregation
```

### Why Typed Storage Matters

A naive EAV implementation stores every value as a string. This breaks sorting (is "9" > "10"?), prevents database-level aggregation (`SUM`, `AVG`), and wastes index potential.

Universal Tracker follows the Magento approach with **type-specific value tables**:

| Value Table | Stores | Enables |
|------------|--------|---------|
| `value_text` | Strings, descriptions | Full-text search, LIKE queries |
| `value_int` | Integers, counts | Numeric sorting, `SUM`/`COUNT` |
| `value_decimal` | Prices, measurements | Precise math, `AVG`/`SUM` |
| `value_datetime` | Dates, timestamps | Range queries, date math |
| `value_boolean` | Flags, toggles | Efficient filtering |

This means when you aggregate "total revenue by month," PostgreSQL runs a native `SUM` on actual `DECIMAL` values — not string parsing. The PLpgSQL functions in this project handle the routing: they know which value table to join based on the attribute's declared type.

### The Trade-Off

EAV trades **query simplicity for schema flexibility**. A simple `SELECT * FROM products WHERE price > 20` in a traditional schema becomes a multi-join operation in EAV. This project addresses that trade-off through its aggregation engine and PLpgSQL stored functions, which encapsulate the join complexity so the API consumer doesn't have to think about it.

---

## Project Structure

```
universal-tracker/
├── .mvn/wrapper/            # Maven wrapper config
├── src/                     # Application source code
│   ├── main/
│   │   ├── java/            # Java source (92.7% of codebase)
│   │   │   └── com/codify/  # Root package
│   │   └── resources/
│   │       └── db/migration/ # Flyway SQL migrations (PLpgSQL)
│   └── test/                # Test suite
├── .env.example             # Environment variable template
├── Dockerfile               # Multi-stage Docker build
├── docker-compose.yml       # Container orchestration
├── deploy.sh                # One-command deployment script
├── pom.xml                  # Maven dependencies & build config
├── mvnw / mvnw.cmd          # Maven wrapper (Linux / Windows)
└── .gitignore
```

---

## Getting Started

### Prerequisites

- **Java 17+**
- **PostgreSQL** (running locally or remotely)
- **Maven 3.9+** (or use the included `mvnw` wrapper)
- **Docker** (optional, for containerized deployment)

### Option 1: Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/FrankMwangi8118/universal-tracker.git
   cd universal-tracker
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   ```
   Edit `.env` with your PostgreSQL credentials:
   ```env
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=tracker
   DB_USERNAME=tracker_user
   DB_PASSWORD=your_secure_password
   ```

3. **Create the database**
   ```bash
   createdb tracker
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

5. **Access the API**
   - Application: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Health check: `http://localhost:8080/api/health`

### Option 2: Docker Deployment

1. **Clone and configure**
   ```bash
   git clone https://github.com/FrankMwangi8118/universal-tracker.git
   cd universal-tracker
   cp .env.example .env
   # Edit .env with your DB credentials
   ```

2. **Create the Docker network** (if not already existing)
   ```bash
   docker network create tracker-net
   ```

3. **Deploy with one command**
   ```bash
   chmod +x deploy.sh
   ./deploy.sh
   ```

   This will build the image, stop any old containers, start fresh, and show you the logs.

The Dockerfile uses a **multi-stage build** for a lean production image: Maven builds the JAR in a build stage, then only the JRE and JAR are copied to the runtime stage. The app runs as a non-root user (`tracker`) for security, with container-optimized JVM settings.

---

## API Documentation

Once the application is running, interactive API documentation is available via **Swagger UI** at:

```
http://localhost:8080/swagger-ui.html
```

This provides a full list of endpoints for managing modules, collections, field definitions, records, and aggregations — all explorable and testable directly from the browser.

---

## Core Concepts

| Concept | Description |
|---------|-------------|
| **Module** | A top-level domain (e.g., "Sales," "HR," "Inventory"). Groups related collections together. |
| **Collection** | A data table within a module (e.g., "Contacts," "Deals," "Products"). Holds records. |
| **Field Definition** | A custom column on a collection — name, type (text, number, date, boolean, etc.), validation rules, and display settings. Created at runtime. |
| **Record** | A single row/entry in a collection, with values stored against the defined fields. |
| **Dynamic View** | A configurable query/filter over a collection — think saved filters or dashboard widgets. |
| **Aggregation** | Computed summaries (count, sum, average, etc.) over collection data, powered by PLpgSQL functions. |

---

## Key Features

- **No-code data modeling** — define any schema through API calls, not migrations
- **Typed storage** — field values are stored with proper types (not just strings), enabling correct sorting, filtering, and aggregation
- **Configurable validation** — set required fields, min/max values, regex patterns, and more per field definition
- **Dynamic views** — save and reuse filtered/sorted perspectives on your data
- **Aggregation engine** — compute sums, counts, averages, and custom aggregations via PLpgSQL
- **Flyway migrations** — database schema versioning and repeatable migrations
- **OpenAPI / Swagger** — full interactive API documentation out of the box
- **Docker-ready** — multi-stage Dockerfile with non-root user, health checks, and JVM container tuning
- **One-command deploy** — `./deploy.sh` handles build, stop, start, and log tailing

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL hostname | `host.docker.internal` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `tracker` |
| `DB_USERNAME` | Database user | `tracker_user` |
| `DB_PASSWORD` | Database password | *(required)* |

---

## Docker Details

**Build image:**
```bash
docker build -t universal-tracker:latest .
```

**Runtime characteristics:**
- Base image: `eclipse-temurin:17-jre-jammy`
- Runs as non-root user `tracker`
- JVM flags: `UseContainerSupport`, `MaxRAMPercentage=75%`
- Exposes port `8080`
- Health check: `curl http://localhost:8080/api/health` every 30s

---

## Development

**Run tests:**
```bash
./mvnw test
```

**Build without tests:**
```bash
./mvnw package -DskipTests
```

**Access the Maven wrapper on Windows:**
```cmd
mvnw.cmd spring-boot:run
```

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

## Roadmap

- [ ] Authentication & authorization (JWT / OAuth2)
- [ ] Multi-tenant support
- [ ] Webhook / event system for record changes
- [ ] Import/export (CSV, JSON)
- [ ] Frontend dashboard (React or similar)
- [ ] Relationship fields (foreign keys between collections)
- [ ] Audit log / change history
- [ ] Caching layer (Redis)

---

## License

This project is open source. See the repository for license details.

---

**Built by [@FrankMwangi8118](https://github.com/FrankMwangi8118)**
