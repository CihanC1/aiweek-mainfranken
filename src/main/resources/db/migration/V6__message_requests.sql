CREATE TABLE message_requests (
 id uuid PRIMARY KEY,
 requester_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 recipient_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 status varchar(32) NOT NULL,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 CONSTRAINT chk_message_requests_not_self CHECK (requester_id <> recipient_id)
);

CREATE UNIQUE INDEX uq_message_requests_pair ON message_requests (
 (CASE WHEN requester_id::text < recipient_id::text THEN requester_id ELSE recipient_id END),
 (CASE WHEN requester_id::text < recipient_id::text THEN recipient_id ELSE requester_id END)
);
CREATE INDEX idx_message_requests_recipient_status ON message_requests(recipient_id, status);
CREATE INDEX idx_message_requests_requester_status ON message_requests(requester_id, status);

INSERT INTO message_requests(id, requester_id, recipient_id, status, created_at, updated_at)
SELECT gen_random_uuid(), requester_id, recipient_id, 'ACCEPTED', MIN(created_at), MAX(updated_at)
FROM (
 SELECT
  CASE WHEN sender_id::text < recipient_id::text THEN sender_id ELSE recipient_id END AS requester_id,
  CASE WHEN sender_id::text < recipient_id::text THEN recipient_id ELSE sender_id END AS recipient_id,
  created_at,
  updated_at
 FROM direct_messages
) pairs
GROUP BY requester_id, recipient_id
ON CONFLICT DO NOTHING;
