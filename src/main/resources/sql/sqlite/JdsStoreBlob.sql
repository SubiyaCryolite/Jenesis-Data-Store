CREATE TABLE JdsStoreBlob(
	FieldId         BIGINT,
	Uuid      TEXT,
	Value           BLOB,
	PRIMARY KEY (FieldId,Uuid),
	FOREIGN KEY (Uuid) REFERENCES JdsEntityOverview(Uuid) DEFERRABLE INITIALLY DEFERRED --we use REPLACE INTO, so hopefully this maintains integrity
);