-- release-target-preflight: migrationId=20261127_dcc_registration_certificate_retained_delete; allowedEnvironments=test,backup,prod
-- Multiple certificates per product are valid. Missing or inconsistent product identity still blocks release.
WITH sources AS (
  SELECT tenant_id,project_code_id,product_catalog_id,id AS relation_id,'CONFIRMED' AS source
  FROM dcc_data_relation WHERE deleted=0 AND relation_status='CONFIRMED'
  UNION ALL
  SELECT r.tenant_id,r.project_code_id,r.product_catalog_id,r.id,'APPROVED'
  FROM dcc_project_product_relation r JOIN dcc_project_product_create_request q
    ON q.tenant_id=r.tenant_id AND q.id=r.request_id AND q.deleted=0 AND q.status='COMPLETED'
      AND q.generated_project_code_id=r.project_code_id AND q.generated_product_catalog_id=r.product_catalog_id
      AND q.relation_id=r.id
  WHERE r.deleted=0 AND r.relation_status='ACTIVE'
), projects AS (
  SELECT r.tenant_id,r.project_code_id,MIN(r.product_catalog_id) AS product_catalog_id,
    COUNT(DISTINCT r.product_catalog_id) AS product_count,
    COUNT(*)-COUNT(DISTINCT r.source) AS duplicate_sources,
    MIN(p.product_code) AS product_code,MIN(p.product) AS product_name
  FROM sources r LEFT JOIN dcc_product_catalog p
    ON p.tenant_id=r.tenant_id AND p.id=r.product_catalog_id AND p.deleted=0
  GROUP BY r.tenant_id,r.project_code_id
), candidates AS (
  SELECT c.tenant_id,c.id AS certificate_id,c.project_code_id,p.product_catalog_id,
    p.product_count,p.duplicate_sources,p.product_code,p.product_name
  FROM dcc_registration_certificate c LEFT JOIN projects p
    ON p.tenant_id=c.tenant_id AND p.project_code_id=c.project_code_id
  WHERE c.deleted=0 AND c.project_code_id IS NOT NULL
    AND (c.current_version_id IS NOT NULL OR EXISTS (
      SELECT 1 FROM dcc_registration_certificate_access_request a
      WHERE a.tenant_id=c.tenant_id AND a.certificate_id=c.id AND a.deleted=0
        AND a.request_type='UPLOAD_CERTIFICATE' AND a.status IN ('SUBMITTED','BPM_BOUND')))
)
SELECT CASE
  WHEN NOT EXISTS (SELECT 1 FROM information_schema.TRIGGERS
    WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='trg_dcc_reg_cert_master_immutable_bu'
      AND EVENT_OBJECT_TABLE='dcc_registration_certificate' AND EVENT_MANIPULATION='UPDATE' AND ACTION_TIMING='BEFORE'
      AND SHA2(REGEXP_REPLACE(ACTION_STATEMENT,'[[:space:]`]+',''),256) IN ('4e9deddd5938f846958dc12749e030e42a54cfde41694e7e8b70617beb10e91d','eaa0df4bc4292ebd45e863bda911ee2510e1d0815d125f4593a792253c6698be')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261127_dcc_registration_certificate_retained_delete:UNSUPPORTED_MASTER_TRIGGER'
  WHEN EXISTS (SELECT 1 FROM candidates WHERE product_count IS NULL OR product_count<>1 OR duplicate_sources>0
      OR product_code IS NULL OR product_name IS NULL OR product_code='' OR product_name='')
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261127_dcc_registration_certificate_retained_delete:UNRESOLVED_PRODUCT'
  WHEN EXISTS (
    SELECT 1 FROM dcc_registration_certificate c
    JOIN dcc_registration_certificate_snapshot s ON s.tenant_id=c.tenant_id AND s.id=c.current_snapshot_id
    LEFT JOIN projects p ON p.tenant_id=c.tenant_id AND p.project_code_id=c.project_code_id
    WHERE c.deleted=0 AND JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.ownProduct'))='true'
      AND (c.project_code_id IS NULL OR p.product_count IS NULL OR p.product_count<>1
        OR (JSON_TYPE(JSON_EXTRACT(s.facts_json,'$.projectProduct'))='OBJECT'
          AND (NOT(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.productCatalogId'))<=>CAST(p.product_catalog_id AS CHAR))
            OR NOT(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.projectCodeId'))<=>CAST(c.project_code_id AS CHAR))))))
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261127_dcc_registration_certificate_retained_delete:SNAPSHOT_PRODUCT_MISMATCH'
  WHEN EXISTS (SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_product')
    AND (
      (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_registration_certificate_product' AND COLUMN_NAME IN
        ('tenant_id','product_catalog_id','certificate_id','project_code_id','product_code','product_name','state','created_by','created_at'))<>9
      OR NOT COALESCE((SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_product' AND INDEX_NAME='PRIMARY'), '')
        IN ('tenant_id,product_catalog_id','tenant_id,certificate_id,product_catalog_id')
      OR NOT COALESCE((SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_product' AND INDEX_NAME='uk_reg_product_project' AND NON_UNIQUE=0), '')
        IN ('tenant_id,project_code_id','tenant_id,certificate_id,project_code_id'))
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261127_dcc_registration_certificate_retained_delete:UNSUPPORTED_BINDING_SCHEMA'
  ELSE 'TARGET_PREFLIGHT_PASS:20261127_dcc_registration_certificate_retained_delete'
END;
