CREATE FUNCTION procJdsStoreInteger(pEntityGuid VARCHAR(48), pFieldId BIGINT, pValue INTEGER)
RETURNS INTEGER AS $$
BEGIN
	INSERT INTO JdsStoreInteger(EntityGuid, FieldId, Value)
    VALUES (pEntityGuid, pFieldId, pValue)
    ON conflict (EntityGuid,FieldId) do
    UPDATE SET Value = pValue;
    RETURN 1;
END;
$$ LANGUAGE plpgsql;