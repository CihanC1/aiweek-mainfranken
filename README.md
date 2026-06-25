# MainFrankenIT

Quarkus/PostgreSQL backend for discovering technology events in the Main-Franken region. Scraping, normalization, search, duplicate/update detection, and recommendation scoring are deterministic; AI is reserved for conversational adapters.

## Run locally

1. Create PostgreSQL database `mainfrankenit` on port 5432 (credentials default to `mainfrankenit/mainfrankenit`).
2. Run `mvn quarkus:dev` and open `http://localhost:8080/api/docs`.

Configuration can be overridden with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ORIGINS`,
`EVENT_SOURCES_FILE`, `EVENT_IMPORT_ENABLED`, `EVENT_IMPORT_RUN_AT_START`, and
`EVENT_IMPORT_INTERVAL`.

## Event sources

Put crawl/import sources in `src/main/resources/event-sources.csv` using this format:

```csv
# name;url;parserKey;active
AI Week Mainfranken;https://www.ai-week.de/programm.php;ai-week;true
```

The file is loaded on backend startup and upserts sources by name. For AI Week, normal
programme links are accepted; the importer automatically fetches the public timetable export.
A detail link such as `https://www.ai-week.de/programm.php#/veranstaltung/83` imports only
that one event, while `https://www.ai-week.de/programm.php` imports the full programme.
To use a different file outside the app bundle, set `EVENT_SOURCES_FILE=/path/to/event-sources.csv`.

The backend imports active sources automatically on startup and then repeats the import every
`EVENT_IMPORT_INTERVAL` (`30m` by default). Set `EVENT_IMPORT_ENABLED=false` to disable automatic
imports, or `EVENT_IMPORT_RUN_AT_START=false` if startup should only load the source list.

## Frontend locally

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173`. The strict production build is `npm run build`, unit tests are `npm test`, and browser tests are `npm run e2e`.

## Run with Docker

Copy `.env.example` to `.env`, choose a password, then run `docker compose up --build`. PostgreSQL is exposed at `localhost:5433`, the API at `http://localhost:8082`, and the web app at `http://localhost:3000`.

## Architecture

- `domain`: entities, value enums, deterministic models
- `application`: normalization, import, search, recommendation, chat and notification use cases
- `adapter.in.rest`: validated HTTP APIs and consistent errors
- `adapter.out.scraper`: source-specific Jsoup parsers behind a port
- `adapter.out.whatsapp`: replaceable mock/provider adapter
- `db/migration`: schema and development seed data
