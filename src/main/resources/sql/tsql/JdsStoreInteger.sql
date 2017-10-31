CREATE TABLE JdsStoreInteger(
    FieldId         BIGINT,
    Uuid      NVARCHAR(48),
    Value           INTEGER,
    PRIMARY KEY (FieldId,Uuid),
    CONSTRAINT fk_JdsStoreInteger_ParentUuid FOREIGN KEY (Uuid) REFERENCES JdsEntityOverview(Uuid) ON DELETE CASCADE
);