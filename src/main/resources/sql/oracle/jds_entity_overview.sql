CREATE TABLE jds_entity_overview (
  composite_key         NVARCHAR2(128),
  uuid                  NVARCHAR2(64),
  uuid_location         NVARCHAR2(45),
  uuid_location_version NUMBER(10),
  parent_uuid           NVARCHAR2(64),
  entity_id             NUMBER(19),
  entity_version        NUMBER(19),
  live                  NUMBER(3),
  last_edit             TIMESTAMP,
  PRIMARY KEY (composite_key)
)