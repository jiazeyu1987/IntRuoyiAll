SET SESSION TRANSACTION READ ONLY;
START TRANSACTION READ ONLY;

SET @snapshot_max_master_id := (SELECT MAX(id) FROM dcc_controlled_file_master);
SET @snapshot_max_file_id := (SELECT MAX(id) FROM dcc_controlled_file);

SELECT 'SNAPSHOT' AS check_code, DATABASE() AS database_name, @@transaction_read_only AS read_only,
       @snapshot_max_master_id AS max_master_id, @snapshot_max_file_id AS max_file_id, NOW() AS captured_at;

SELECT 'BASELINE' AS check_code,
       (SELECT COUNT(*) FROM dcc_controlled_file_master WHERE deleted = 0 AND id <= @snapshot_max_master_id) AS master_count,
       (SELECT COUNT(*) FROM dcc_controlled_file WHERE deleted = 0 AND id <= @snapshot_max_file_id) AS file_count,
       (SELECT COUNT(*) FROM dcc_controlled_file WHERE deleted = 0 AND source_file_id IS NOT NULL
          AND id <= @snapshot_max_file_id) AS effective_source_reference_count;

SELECT 'I01_IDENTITY_INCOMPLETE_OR_MIXED' AS check_code, COUNT(*) AS blocker_count
FROM (
    SELECT tenant_id, master_id
    FROM dcc_controlled_file
    WHERE deleted = 0 AND id <= @snapshot_max_file_id
    GROUP BY tenant_id, master_id
    HAVING master_id IS NULL
        OR COUNT(DISTINCT dcc_project_code_id) <> 1
        OR COUNT(DISTINCT file_type_taxonomy_id) <> 1
        OR COUNT(DISTINCT UPPER(TRIM(file_number))) <> 1
        OR SUM(dcc_project_code_id IS NULL) > 0
        OR SUM(file_type_taxonomy_id IS NULL) > 0
        OR SUM(TRIM(COALESCE(file_number, '')) = '') > 0
) blocker;

SELECT 'I02_TAXONOMY_INVALID_OR_NON_LEAF' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file f
LEFT JOIN dcc_file_type_taxonomy t
  ON t.id = f.file_type_taxonomy_id AND t.tenant_id = f.tenant_id
WHERE f.deleted = 0 AND f.id <= @snapshot_max_file_id
  AND (t.id IS NULL OR t.deleted <> 0 OR t.active <> 1 OR EXISTS (
      SELECT 1 FROM dcc_file_type_taxonomy child
      WHERE child.tenant_id = t.tenant_id AND child.parent_id = t.id
        AND child.deleted = 0 AND child.active = 1));

WITH stable_identity AS (
    SELECT tenant_id, master_id,
           MIN(dcc_project_code_id) AS project_id,
           MIN(file_type_taxonomy_id) AS taxonomy_id,
           MIN(UPPER(TRIM(file_number))) AS normalized_file_number
    FROM dcc_controlled_file
    WHERE deleted = 0 AND id <= @snapshot_max_file_id
    GROUP BY tenant_id, master_id
    HAVING master_id IS NOT NULL
       AND COUNT(DISTINCT dcc_project_code_id) = 1
       AND COUNT(DISTINCT file_type_taxonomy_id) = 1
       AND COUNT(DISTINCT UPPER(TRIM(file_number))) = 1
       AND SUM(dcc_project_code_id IS NULL) = 0
       AND SUM(file_type_taxonomy_id IS NULL) = 0
       AND SUM(TRIM(COALESCE(file_number, '')) = '') = 0
)
SELECT 'I03_DUPLICATE_TARGET_IDENTITY' AS check_code, COUNT(*) AS blocker_count
FROM (
    SELECT tenant_id, project_id, taxonomy_id, normalized_file_number
    FROM stable_identity
    GROUP BY tenant_id, project_id, taxonomy_id, normalized_file_number
    HAVING COUNT(*) > 1
) blocker;

SELECT 'I04_FORMAL_POINTER_DRIFT' AS check_code, COUNT(*) AS blocker_count
FROM (
    SELECT m.tenant_id, m.id
    FROM dcc_controlled_file_master m
    LEFT JOIN dcc_controlled_file f
      ON f.master_id = m.id AND f.tenant_id = m.tenant_id
     AND f.deleted = 0 AND f.id <= @snapshot_max_file_id
    WHERE m.deleted = 0 AND m.id <= @snapshot_max_master_id
    GROUP BY m.tenant_id, m.id, m.current_active_controlled_file_id
    HAVING SUM(f.status = 'ACTIVE') > 1
        OR SUM(f.id = m.current_active_controlled_file_id AND f.status = 'ACTIVE')
           <> CASE WHEN m.current_active_controlled_file_id IS NULL THEN 0 ELSE 1 END
) blocker;

SELECT 'I05_INVALID_OR_DUPLICATE_VERSION' AS check_code,
       SUM(invalid_version) + SUM(duplicate_version) AS blocker_count
FROM (
    SELECT tenant_id, master_id, version_no,
           MAX(version_no IS NULL OR TRIM(version_no) = ''
               OR TRIM(version_no) NOT REGEXP '^[Vv]?[0-9]+([.][0-9]+)*$') AS invalid_version,
           (COUNT(*) > 1) AS duplicate_version
    FROM dcc_controlled_file
    WHERE deleted = 0 AND id <= @snapshot_max_file_id
    GROUP BY tenant_id, master_id, version_no
) blocker;

SELECT 'I06_CHECKOUT_INVALID' AS check_code, COUNT(*) AS blocker_count
FROM (
    SELECT f.tenant_id, f.master_id
    FROM dcc_controlled_file f
    LEFT JOIN system_users u ON u.id = f.checked_out_by AND u.tenant_id = f.tenant_id AND u.deleted = 0
    WHERE f.deleted = 0 AND f.id <= @snapshot_max_file_id AND f.checked_out_by IS NOT NULL
    GROUP BY f.tenant_id, f.master_id
    HAVING COUNT(*) > 1 OR SUM(f.checked_out_time IS NULL) > 0
        OR SUM(u.id IS NULL OR u.status <> 0) > 0
        OR SUM(f.status IN ('ACTIVE', 'SUPERSEDED', 'OBSOLETE')) > 0
) blocker;

SELECT 'I07_SOURCE_REFERENCE_TOTAL' AS check_code, COUNT(*) AS item_count
FROM dcc_controlled_file
WHERE deleted = 0 AND id <= @snapshot_max_file_id AND source_file_id IS NOT NULL;

SELECT 'I07_SOURCE_OWNERSHIP_OR_HASH_BLOCKED' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file f
LEFT JOIN infra_file sf ON sf.id = f.source_file_id
LEFT JOIN dcc_controlled_file_source_ownership o
  ON o.tenant_id = f.tenant_id AND o.controlled_file_id = f.id AND o.deleted = 0
WHERE f.deleted = 0 AND f.id <= @snapshot_max_file_id AND f.source_file_id IS NOT NULL
  AND (sf.id IS NULL OR sf.deleted <> 0 OR sf.config_id IS NULL OR TRIM(COALESCE(sf.path, '')) = ''
       OR o.id IS NULL OR o.source_file_id <> f.source_file_id
       OR o.source_sha256 IS NULL OR CHAR_LENGTH(o.source_sha256) <> 64);

SELECT 'I07_SOURCE_RECORD_MISSING' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file f LEFT JOIN infra_file sf ON sf.id = f.source_file_id
WHERE f.deleted = 0 AND f.id <= @snapshot_max_file_id AND f.source_file_id IS NOT NULL AND sf.id IS NULL;

SELECT 'I07_SOURCE_RECORD_DELETED' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file f JOIN infra_file sf ON sf.id = f.source_file_id
WHERE f.deleted = 0 AND f.id <= @snapshot_max_file_id AND f.source_file_id IS NOT NULL AND sf.deleted <> 0;

SELECT 'I07_SOURCE_LOCATION_INCOMPLETE' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file f JOIN infra_file sf ON sf.id = f.source_file_id
WHERE f.deleted = 0 AND f.id <= @snapshot_max_file_id AND f.source_file_id IS NOT NULL
  AND sf.deleted = 0 AND (sf.config_id IS NULL OR TRIM(COALESCE(sf.path, '')) = '');

SELECT 'I07_OWNERSHIP_MISSING' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file f LEFT JOIN dcc_controlled_file_source_ownership o
  ON o.tenant_id = f.tenant_id AND o.controlled_file_id = f.id AND o.deleted = 0
WHERE f.deleted = 0 AND f.id <= @snapshot_max_file_id AND f.source_file_id IS NOT NULL AND o.id IS NULL;

SELECT 'I07_OWNERSHIP_POINTER_MISMATCH' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file f JOIN dcc_controlled_file_source_ownership o
  ON o.tenant_id = f.tenant_id AND o.controlled_file_id = f.id AND o.deleted = 0
WHERE f.deleted = 0 AND f.id <= @snapshot_max_file_id AND f.source_file_id IS NOT NULL
  AND NOT (o.source_file_id <=> f.source_file_id);

SELECT 'I07_OWNERSHIP_HASH_INVALID' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file f JOIN dcc_controlled_file_source_ownership o
  ON o.tenant_id = f.tenant_id AND o.controlled_file_id = f.id AND o.deleted = 0
WHERE f.deleted = 0 AND f.id <= @snapshot_max_file_id AND f.source_file_id IS NOT NULL
  AND (o.source_sha256 IS NULL OR CHAR_LENGTH(o.source_sha256) <> 64);

SELECT 'I07_GLOBAL_SHARED_SOURCE_GROUPS' AS check_code, COUNT(*) AS blocker_count,
       COALESCE(SUM(reference_count), 0) AS blocked_reference_count
FROM (
    SELECT source_file_id, COUNT(*) AS reference_count
    FROM dcc_controlled_file
    WHERE deleted = 0 AND id <= @snapshot_max_file_id AND source_file_id IS NOT NULL
    GROUP BY source_file_id HAVING COUNT(*) > 1
) blocker;

SELECT 'I07_CROSS_TENANT_SHARED_SOURCE_GROUPS' AS check_code, COUNT(*) AS blocker_count,
       COALESCE(SUM(reference_count), 0) AS blocked_reference_count
FROM (
    SELECT source_file_id, COUNT(*) AS reference_count
    FROM dcc_controlled_file
    WHERE deleted = 0 AND id <= @snapshot_max_file_id AND source_file_id IS NOT NULL
    GROUP BY source_file_id HAVING COUNT(DISTINCT tenant_id) > 1
) blocker;

SELECT 'I08_PROCESS_OR_SIGNATURE_ORPHAN' AS check_code,
       (SELECT COUNT(*) FROM dcc_controlled_file_signature s
        LEFT JOIN dcc_controlled_file f ON f.id = s.controlled_file_id AND f.tenant_id = s.tenant_id
        WHERE s.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id))
       +
       (SELECT COUNT(*) FROM dcc_controlled_file_route_snapshot r
        LEFT JOIN dcc_controlled_file f ON f.id = r.controlled_file_id AND f.tenant_id = r.tenant_id
       WHERE r.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id)) AS blocker_count;

SELECT 'I08_SIGNATURE_ORPHAN' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file_signature s LEFT JOIN dcc_controlled_file f
  ON f.id = s.controlled_file_id AND f.tenant_id = s.tenant_id
WHERE s.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id);

SELECT 'I08_ROUTE_SNAPSHOT_ORPHAN' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file_route_snapshot r LEFT JOIN dcc_controlled_file f
  ON f.id = r.controlled_file_id AND f.tenant_id = r.tenant_id
WHERE r.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id);

SELECT 'I09_PLATFORM_ACTIVE_DRIFT' AS check_code, COUNT(*) AS blocker_count
FROM (
    SELECT m.tenant_id, m.id
    FROM dcc_controlled_file_master m
    LEFT JOIN controlled_content_version_ref ref
      ON ref.tenant_id = m.tenant_id AND ref.content_type = 'DCC_CONTROLLED_FILE'
     AND ref.native_master_id = m.id AND ref.canonical_status = 'ACTIVE' AND ref.deleted = 0
    WHERE m.deleted = 0 AND m.id <= @snapshot_max_master_id
    GROUP BY m.tenant_id, m.id, m.current_active_controlled_file_id
    HAVING COUNT(ref.id) > 1
        OR NOT (m.current_active_controlled_file_id <=> MIN(ref.native_version_id))
) blocker;

SELECT 'I10_HISTORICAL_RELATION_ORPHAN' AS check_code,
       (SELECT COUNT(*) FROM dcc_controlled_file_related_file x LEFT JOIN dcc_controlled_file f
          ON f.id = x.controlled_file_id AND f.tenant_id = x.tenant_id
        WHERE x.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id))
       + (SELECT COUNT(*) FROM dcc_controlled_file_distribution x LEFT JOIN dcc_controlled_file f
          ON f.id = x.controlled_file_id AND f.tenant_id = x.tenant_id
        WHERE x.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id))
       + (SELECT COUNT(*) FROM dcc_controlled_file_training x LEFT JOIN dcc_controlled_file f
          ON f.id = x.controlled_file_id AND f.tenant_id = x.tenant_id
        WHERE x.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id))
       + (SELECT COUNT(*) FROM dcc_controlled_file_print_record x LEFT JOIN dcc_controlled_file f
          ON f.id = x.controlled_file_id AND f.tenant_id = x.tenant_id
        WHERE x.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id))
       + (SELECT COUNT(*) FROM dcc_controlled_file_access_log x LEFT JOIN dcc_controlled_file f
          ON f.id = x.controlled_file_id AND f.tenant_id = x.tenant_id
        WHERE x.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id)) AS blocker_count;

SELECT 'I10_RELATED_FILE_ORPHAN' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file_related_file x LEFT JOIN dcc_controlled_file f
  ON f.id = x.controlled_file_id AND f.tenant_id = x.tenant_id
WHERE x.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id);

SELECT 'I10_DISTRIBUTION_ORPHAN' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file_distribution x LEFT JOIN dcc_controlled_file f
  ON f.id = x.controlled_file_id AND f.tenant_id = x.tenant_id
WHERE x.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id);

SELECT 'I10_TRAINING_ORPHAN' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file_training x LEFT JOIN dcc_controlled_file f
  ON f.id = x.controlled_file_id AND f.tenant_id = x.tenant_id
WHERE x.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id);

SELECT 'I10_PRINT_ORPHAN' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file_print_record x LEFT JOIN dcc_controlled_file f
  ON f.id = x.controlled_file_id AND f.tenant_id = x.tenant_id
WHERE x.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id);

SELECT 'I10_ACCESS_LOG_ORPHAN' AS check_code, COUNT(*) AS blocker_count
FROM dcc_controlled_file_access_log x LEFT JOIN dcc_controlled_file f
  ON f.id = x.controlled_file_id AND f.tenant_id = x.tenant_id
WHERE x.deleted = 0 AND (f.id IS NULL OR f.deleted <> 0 OR f.id > @snapshot_max_file_id);

SELECT 'GOVERNANCE_SCHEMA_PRESENT' AS check_code, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN (
    'dcc_controlled_file_source_governance_batch',
    'dcc_controlled_file_source_governance_item',
    'dcc_controlled_file_source_global_claim');

WITH shared_sources AS (
    SELECT source_file_id
    FROM dcc_controlled_file
    WHERE deleted = 0 AND id <= @snapshot_max_file_id AND source_file_id IS NOT NULL
    GROUP BY source_file_id HAVING COUNT(*) > 1
), stable_master AS (
    SELECT f.tenant_id, f.master_id
    FROM dcc_controlled_file f
    LEFT JOIN dcc_controlled_file_master m ON m.id = f.master_id AND m.tenant_id = f.tenant_id AND m.deleted = 0
    LEFT JOIN dcc_file_type_taxonomy t ON t.id = f.file_type_taxonomy_id
      AND t.tenant_id = f.tenant_id AND t.deleted = 0 AND t.active = 1
    LEFT JOIN infra_file sf ON sf.id = f.source_file_id AND sf.deleted = 0
    LEFT JOIN dcc_controlled_file_source_ownership o ON o.tenant_id = f.tenant_id
      AND o.controlled_file_id = f.id AND o.deleted = 0
    LEFT JOIN shared_sources shared ON shared.source_file_id = f.source_file_id
    WHERE f.deleted = 0 AND f.id <= @snapshot_max_file_id AND f.master_id IS NOT NULL
    GROUP BY f.tenant_id, f.master_id, m.current_active_controlled_file_id
    HAVING COUNT(DISTINCT f.dcc_project_code_id) = 1
       AND COUNT(DISTINCT f.file_type_taxonomy_id) = 1
       AND COUNT(DISTINCT UPPER(TRIM(f.file_number))) = 1
       AND SUM(f.dcc_project_code_id IS NULL OR f.file_type_taxonomy_id IS NULL
               OR TRIM(COALESCE(f.file_number, '')) = '') = 0
       AND SUM(t.id IS NULL OR EXISTS (SELECT 1 FROM dcc_file_type_taxonomy child
           WHERE child.tenant_id = t.tenant_id AND child.parent_id = t.id
             AND child.deleted = 0 AND child.active = 1)) = 0
       AND SUM(f.version_no IS NULL OR TRIM(f.version_no) = ''
               OR TRIM(f.version_no) NOT REGEXP '^[Vv]?[0-9]+([.][0-9]+)*$') = 0
       AND COUNT(*) = COUNT(DISTINCT f.version_no)
       AND SUM(f.status NOT IN ('ACTIVE', 'SUPERSEDED', 'OBSOLETE')) = 0
       AND SUM(f.status = 'ACTIVE') <= 1
       AND SUM(f.id = m.current_active_controlled_file_id AND f.status = 'ACTIVE')
           = CASE WHEN m.current_active_controlled_file_id IS NULL THEN 0 ELSE 1 END
       AND SUM(f.source_file_id IS NULL OR sf.id IS NULL OR o.id IS NULL
               OR o.source_file_id <> f.source_file_id OR o.source_sha256 IS NULL
               OR CHAR_LENGTH(o.source_sha256) <> 64) = 0
       AND SUM(shared.source_file_id IS NOT NULL) = 0
)
SELECT 'AUTO_MAP_CURRENT' AS check_code, COUNT(*) AS candidate_master_count FROM stable_master;

ROLLBACK;
