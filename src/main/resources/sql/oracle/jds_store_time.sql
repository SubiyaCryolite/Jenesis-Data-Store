CREATE TABLE jds_store_time (
  composite_key NVARCHAR2(128),
  field_id      NUMBER(19),
  value         NUMBER(19),
  PRIMARY KEY (field_id, composite_key),
  FOREIGN KEY (composite_key) REFERENCES jds_entity_overview (composite_key) ON DELETE CASCADE
)