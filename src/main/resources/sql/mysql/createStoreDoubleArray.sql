CREATE TABLE JdsStoreDoubleArray(
    FieldId     BIGINT,
    EntityGuid  VARCHAR(48),
    Sequence    INT,
    Value       DOUBLE,
    PRIMARY KEY (FieldId,EntityGuid,Sequence),
    FOREIGN KEY (EntityGuid) REFERENCES JdsStoreEntityOverview(EntityGuid) ON DELETE CASCADE
);