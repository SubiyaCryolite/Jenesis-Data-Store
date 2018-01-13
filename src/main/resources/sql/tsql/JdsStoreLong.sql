CREATE TABLE jds_store_long (
  field_id BIGINT,
  uuid     NVARCHAR(96) NOT NULL,
  value    BIGINT,
  PRIMARY KEY (field_id, uuid),
  CONSTRAINT fk_jds_store_long_parent_uuid FOREIGN KEY (uuid) REFERENCES jds_entity_overview (uuid)
    ON DELETE CASCADE
);