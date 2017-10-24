CREATE PROCEDURE procStoreTime(@Uuid NVARCHAR(48), @FieldId BIGINT, @Value TIME(7))
AS
BEGIN
	MERGE JdsStoreTime AS dest
	USING (VALUES (@Uuid,  @FieldId, @Value)) AS src([Uuid],  [FieldId], [Value])
	ON (src.Uuid = dest.Uuid AND src.FieldId = dest.FieldId)
	WHEN MATCHED THEN
		UPDATE SET dest.[Value] = src.[Value]
	WHEN NOT MATCHED THEN
		INSERT([Uuid],[FieldId],[Value])   VALUES(src.Uuid,  src.FieldId, src.Value);
END