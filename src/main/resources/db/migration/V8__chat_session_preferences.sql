ALTER TABLE chat_sessions ADD COLUMN preferred_city varchar(255);
ALTER TABLE chat_sessions ADD COLUMN preferred_attendance_mode varchar(32);

CREATE TABLE chat_session_tags (
    session_id uuid NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    tag varchar(255)
);

CREATE TABLE chat_session_event_types (
    session_id uuid NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    event_type varchar(32)
);
