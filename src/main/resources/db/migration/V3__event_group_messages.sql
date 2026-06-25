CREATE TABLE event_group_messages (
 id uuid PRIMARY KEY,
 group_id uuid NOT NULL REFERENCES event_groups(id) ON DELETE CASCADE,
 user_id uuid REFERENCES app_users(id) ON DELETE SET NULL,
 role varchar(32) NOT NULL,
 content text NOT NULL,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL
);
CREATE INDEX idx_event_group_messages_group_created ON event_group_messages(group_id, created_at);
