CREATE PROCEDURE proc_store_double(@composite_key NVARCHAR(128), @field_id BIGINT, @sequence INTEGER, @value FLOAT)
AS
  BEGIN
    MERGE jds_store_double AS dest
    USING (VALUES (@composite_key, @field_id, @sequence, @value)) AS src(composite_key, field_id, sequence, value)
    ON (src.composite_key = dest.composite_key AND src.field_id = dest.field_id)
    WHEN MATCHED THEN
      UPDATE SET dest.sequence = src.sequence, dest.value = src.value
    WHEN NOT MATCHED THEN
      INSERT (composite_key, field_id, sequence, value)
      VALUES (src.composite_key, src.field_id, src.sequence, src.value);
  END