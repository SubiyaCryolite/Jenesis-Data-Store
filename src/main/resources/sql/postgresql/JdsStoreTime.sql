CREATE TABLE JdsStoreTime(
    FieldId     BIGINT,
    Uuid  VARCHAR(48),
    Value       TIME WITHOUT TIME ZONE,
    PRIMARY KEY (FieldId,Uuid),
    FOREIGN KEY (Uuid) REFERENCES JdsEntityOverview(Uuid) ON DELETE CASCADE
);