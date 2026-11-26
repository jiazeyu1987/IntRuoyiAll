-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261030_dcc_registration_certificate_regulatory_facts,20260920_dcc_project_product_create_approval; type=schema; riskLevel=high; requiresTargetPreflight=true; applyOrder=1
-- Admission only: never create a binding table or change business rows to resolve conflicting history.
-- The catalog adds this prerequisite to the immutable 20261125 product migration by exact checksum.
-- Repeat the packaged read-only check with writers stopped; a prior partial attempt remains blocked.
DROP PROCEDURE IF EXISTS check_dcc_reg_product_admission_20261127;
DELIMITER $$
CREATE PROCEDURE check_dcc_reg_product_admission_20261127()
BEGIN
  DECLARE admission_result VARCHAR(128);
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
  WHEN EXISTS (SELECT 1 FROM candidates WHERE product_count IS NULL OR product_count<>1 OR duplicate_sources>0
      OR product_code IS NULL OR product_name IS NULL OR product_code='' OR product_name='')
    THEN 'UNRESOLVED_PRODUCT'
  WHEN EXISTS (
    SELECT 1 FROM dcc_registration_certificate c
    JOIN dcc_registration_certificate_snapshot s ON s.tenant_id=c.tenant_id AND s.id=c.current_snapshot_id
    LEFT JOIN projects p ON p.tenant_id=c.tenant_id AND p.project_code_id=c.project_code_id
    WHERE c.deleted=0 AND JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.ownProduct'))='true'
      AND (c.project_code_id IS NULL OR p.product_count IS NULL OR p.product_count<>1
        OR (JSON_TYPE(JSON_EXTRACT(s.facts_json,'$.projectProduct'))='OBJECT'
          AND (NOT(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.productCatalogId'))<=>CAST(p.product_catalog_id AS CHAR))
            OR NOT(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.projectCodeId'))<=>CAST(c.project_code_id AS CHAR))))))
    THEN 'SNAPSHOT_PRODUCT_MISMATCH'
  WHEN EXISTS (SELECT 1 FROM candidates GROUP BY tenant_id,product_catalog_id HAVING COUNT(DISTINCT certificate_id)>1)
    THEN 'MULTIPLE_CERTIFICATES'
  WHEN EXISTS (SELECT 1 FROM dcc_data_relation r JOIN candidates m
      ON m.tenant_id=r.tenant_id AND m.product_catalog_id=r.product_catalog_id
      WHERE r.deleted=0 AND r.relation_status='CONFIRMED' AND r.registration_certificate_id IS NOT NULL
        AND r.registration_certificate_id<>m.certificate_id)
    THEN 'RELATION_CERTIFICATE_MISMATCH'
  WHEN EXISTS (SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_product')
    THEN 'PRIOR_BINDING_TABLE_REQUIRES_REVIEW'
  ELSE 'PASS'
END INTO admission_result;
  IF admission_result<>'PASS' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT=admission_result;
  END IF;
END$$
DELIMITER ;
CALL check_dcc_reg_product_admission_20261127();
DROP PROCEDURE check_dcc_reg_product_admission_20261127;
