CREATE FUNCTION procStoreTime(pEntityGuid VARCHAR(48), pFieldId BIGINT, pValue TIME WITHOUT TIME ZONE)
RETURNS VOID AS $$
BEGIN
	INSERT INTO JdsStoreTime(EntityGuid, FieldId, Value)
    VALUES (pEntityGuid, pFieldId, pValue)
    ON CONFLICT (EntityGuid,FieldId) DO UPDATE SET Value = pValue;
END;
$$ LANGUAGE plpgsql;