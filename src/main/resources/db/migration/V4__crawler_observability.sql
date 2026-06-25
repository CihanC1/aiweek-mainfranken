ALTER TABLE event_sources ADD COLUMN fetch_strategy varchar(32) NOT NULL DEFAULT 'HTTP';
ALTER TABLE event_sources ADD COLUMN last_checked_at timestamptz;
ALTER TABLE event_sources ADD COLUMN last_changed_at timestamptz;
ALTER TABLE event_sources ADD COLUMN last_success_at timestamptz;
ALTER TABLE event_sources ADD COLUMN last_error text;
ALTER TABLE event_sources ADD COLUMN content_hash varchar(64);
ALTER TABLE event_sources ADD COLUMN etag varchar(255);
ALTER TABLE event_sources ADD COLUMN remote_last_modified_at timestamptz;
ALTER TABLE event_sources ADD COLUMN check_interval_minutes integer NOT NULL DEFAULT 30;
ALTER TABLE event_sources ADD COLUMN consecutive_failures integer NOT NULL DEFAULT 0;
ALTER TABLE event_sources ADD COLUMN next_check_at timestamptz;

CREATE TABLE source_pages (
 id uuid PRIMARY KEY,
 source_id uuid NOT NULL REFERENCES event_sources(id) ON DELETE CASCADE,
 url varchar(2048) NOT NULL,
 page_type varchar(32) NOT NULL,
 active boolean NOT NULL,
 last_checked_at timestamptz,
 last_changed_at timestamptz,
 content_hash varchar(64),
 last_error text,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 CONSTRAINT uq_source_page_url UNIQUE(source_id,url)
);
CREATE INDEX idx_source_pages_source_active ON source_pages(source_id, active);

CREATE TABLE import_runs (
 id uuid PRIMARY KEY,
 started_at timestamptz NOT NULL,
 finished_at timestamptz,
 status varchar(32) NOT NULL,
 source_count integer NOT NULL,
 discovered_count integer NOT NULL,
 created_count integer NOT NULL,
 updated_count integer NOT NULL,
 unchanged_count integer NOT NULL,
 failed_count integer NOT NULL,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL
);

CREATE TABLE source_import_runs (
 id uuid PRIMARY KEY,
 import_run_id uuid NOT NULL REFERENCES import_runs(id) ON DELETE CASCADE,
 source_id uuid NOT NULL REFERENCES event_sources(id) ON DELETE CASCADE,
 status varchar(32) NOT NULL,
 fetched_url_count integer NOT NULL,
 event_count integer NOT NULL,
 created_count integer NOT NULL,
 updated_count integer NOT NULL,
 unchanged_count integer NOT NULL,
 duration_ms bigint NOT NULL,
 error_message text,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL
);
CREATE INDEX idx_source_import_runs_source_created ON source_import_runs(source_id, created_at);

ALTER TABLE notifications ADD COLUMN dedupe_key varchar(255) UNIQUE;
