ALTER TABLE app_users ADD COLUMN profile_link1 varchar(2048);
ALTER TABLE app_users ADD COLUMN profile_link2 varchar(2048);
ALTER TABLE app_users ADD COLUMN profile_link3 varchar(2048);

CREATE TABLE direct_messages (
 id uuid PRIMARY KEY,
 sender_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 recipient_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 content text NOT NULL,
 read boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL
);
CREATE INDEX idx_direct_messages_pair_created ON direct_messages(sender_id, recipient_id, created_at);
CREATE INDEX idx_direct_messages_recipient_read ON direct_messages(recipient_id, read);
