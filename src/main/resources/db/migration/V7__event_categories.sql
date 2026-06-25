CREATE TABLE event_categories (
 event_id uuid NOT NULL REFERENCES events(id) ON DELETE CASCADE,
 category varchar(255)
);
CREATE INDEX idx_event_categories_category ON event_categories(lower(category));
