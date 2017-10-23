CREATE TABLE JdsStoreTime(
    FieldId     BIGINT,
    EntityGuid  VARCHAR(48),
    Value       TIME,
    PRIMARY KEY (FieldId,EntityGuid),
    FOREIGN KEY (EntityGuid) REFERENCES JdsStoreEntityOverview(EntityGuid) ON DELETE CASCADE
);