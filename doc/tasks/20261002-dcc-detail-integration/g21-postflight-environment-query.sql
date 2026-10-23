-- Read and freeze before migration; use same connection environment during candidate execution.
SELECT JSON_OBJECT('kind','environment','database',DATABASE(),'serverUuid',@@server_uuid,'databaseCharset',DEFAULT_CHARACTER_SET_NAME,
 'databaseCollation',DEFAULT_COLLATION_NAME,'utf8mb4DefaultCollation',@@default_collation_for_utf8mb4,
 'defaultStorageEngine',@@default_storage_engine) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=DATABASE();
