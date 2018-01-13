CREATE TABLE jds_store_long_array (
  field_id BIGINT,
  uuid     NVARCHAR(96) NOT NULL,
  sequence INTEGER,
  value    BIGINT,
  PRIMARY KEY (field_id, uuid, sequence),
  CONSTRAINT fk_jds_store_long_array_parent_uuid FOREIGN KEY (uuid) REFERENCES jds_entity_overview (uuid)
    ON DELETE CASCADE
);