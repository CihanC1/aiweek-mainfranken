ALTER TABLE app_users ADD COLUMN email varchar(320) UNIQUE;
ALTER TABLE app_users ADD COLUMN password_hash varchar(255);
ALTER TABLE app_users ADD COLUMN enabled boolean NOT NULL DEFAULT true;
ALTER TABLE app_users ADD COLUMN role varchar(32) NOT NULL DEFAULT 'USER';

CREATE TABLE auth_sessions (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 access_token_hash varchar(64) NOT NULL UNIQUE, refresh_token_hash varchar(64) NOT NULL UNIQUE,
 access_expires_at timestamptz NOT NULL, refresh_expires_at timestamptz NOT NULL,
 revoked boolean NOT NULL DEFAULT false, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL
);
CREATE INDEX idx_auth_session_refresh ON auth_sessions(refresh_token_hash);
CREATE TABLE password_reset_tokens (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 token_hash varchar(64) NOT NULL UNIQUE, expires_at timestamptz NOT NULL, used boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL
);
