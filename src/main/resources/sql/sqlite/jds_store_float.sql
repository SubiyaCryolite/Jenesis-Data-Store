CREATE TABLE jds_store_float (
  composite_key TEXT,
  field_id      BIGINT,
  sequence      INTEGER,
  value         REAL,
  PRIMARY KEY (field_id, composite_key),
  FOREIGN KEY (composite_key) REFERENCES jds_entity_overview (composite_key)
    ON DELETE CASCADE
    DEFERRABLE INITIALLY DEFERRED --we use REPLACE INTO, so hopefully this maintains integrity
);