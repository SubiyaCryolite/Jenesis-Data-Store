CREATE PROCEDURE procStoreText(pEntityGuid IN NVARCHAR2, pFieldId IN NUMBER, pValue IN SMALLINT)
AS
BEGIN
	MERGE INTO JdsStoreText dest
	USING DUAL ON (pEntityGuid = EntityGuid AND pFieldId = FieldId)
	WHEN MATCHED THEN
		UPDATE SET Value = pValue
	WHEN NOT MATCHED THEN
		INSERT(EntityGuid,FieldId,Value)   VALUES(pEntityGuid,  pFieldId, pValue);
END procStoreText;