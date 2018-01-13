CREATE PROCEDURE proc_store_date_time(puuid IN NVARCHAR2, pfield_id IN NUMBER, pvalue IN TIMESTAMP)
AS
  BEGIN
    MERGE INTO jds_store_date_time dest
    USING DUAL
    ON (puuid = uuid AND pfield_id = field_id)
    WHEN MATCHED THEN
      UPDATE SET value = pvalue
    WHEN NOT MATCHED THEN
      INSERT (uuid, field_id, value)   VALUES (puuid, pfield_id, pvalue);
  END proc_store_date_time;