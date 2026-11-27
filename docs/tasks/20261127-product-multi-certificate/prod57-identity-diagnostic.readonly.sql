START TRANSACTION READ ONLY;
SELECT JSON_OBJECT('kind','identity','database',DATABASE(),'serverUuid',@@server_uuid,'version',@@version,'readOnly',@@read_only,'superReadOnly',@@super_read_only,'isolation',@@transaction_isolation,'observedAt',UTC_TIMESTAMP());
-- READ-ONLY DIAGNOSTIC ONLY. Not a migration or replacement admission.
-- Run on test58 through the existing read-only executor, record sourceRef and target identity.
-- CTE sources/projects/candidates copied exactly from sourceRef 77d4f35f8 target preflight.
-- Returns all identity failures, including failures hidden by the first CASE result.
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
), details AS (
 SELECT c.tenant_id,c.id AS certificate_id,c.status AS certificate_status,c.project_code_id,
   c.current_version_id,c.current_snapshot_id,s.version_id AS snapshot_version_id,
   JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.ownProduct')) AS snapshot_own_product,
   JSON_TYPE(JSON_EXTRACT(s.facts_json,'$.projectProduct')) AS snapshot_product_shape,
   JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.projectCodeId')) AS snapshot_project_id,
   JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.productCatalogId')) AS snapshot_product_id,
   p.product_catalog_id AS resolved_product_id,p.product_count,p.duplicate_sources,p.product_code,p.product_name,
   CASE WHEN m.certificate_id IS NOT NULL AND (m.product_count IS NULL OR m.product_count<>1 OR m.duplicate_sources>0
       OR m.product_code IS NULL OR m.product_name IS NULL OR m.product_code='' OR m.product_name='') THEN 1 ELSE 0 END AS unresolved_product,
   CASE WHEN s.id IS NOT NULL AND JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.ownProduct'))='true'
     AND (c.project_code_id IS NULL OR p.product_count IS NULL OR p.product_count<>1
       OR (JSON_TYPE(JSON_EXTRACT(s.facts_json,'$.projectProduct'))='OBJECT'
         AND (NOT(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.productCatalogId'))<=>CAST(p.product_catalog_id AS CHAR))
           OR NOT(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.projectCodeId'))<=>CAST(c.project_code_id AS CHAR)))))
     THEN 1 ELSE 0 END AS snapshot_product_mismatch
 FROM dcc_registration_certificate c
 LEFT JOIN dcc_registration_certificate_snapshot s ON s.tenant_id=c.tenant_id AND s.id=c.current_snapshot_id
 LEFT JOIN projects p ON p.tenant_id=c.tenant_id AND p.project_code_id=c.project_code_id
 LEFT JOIN candidates m ON m.tenant_id=c.tenant_id AND m.certificate_id=c.id
 WHERE c.deleted=0
)
SELECT JSON_OBJECT('kind','certificate','tenant_id',tenant_id,'certificate_id',certificate_id,'certificate_status',certificate_status,'project_code_id',project_code_id,'current_version_id',current_version_id,'current_snapshot_id',current_snapshot_id,'snapshot_version_id',snapshot_version_id,'snapshot_own_product',snapshot_own_product,'snapshot_product_shape',snapshot_product_shape,'snapshot_project_id',snapshot_project_id,'snapshot_product_id',snapshot_product_id,'resolved_product_id',resolved_product_id,'product_count',product_count,'duplicate_sources',duplicate_sources,'product_code',product_code,'product_name',product_name,'unresolved_product',unresolved_product,'snapshot_product_mismatch',snapshot_product_mismatch,'reasons',CONCAT_WS('|',
   CASE WHEN unresolved_product=1 AND product_count IS NULL THEN 'NO_ELIGIBLE_PROJECT_PRODUCT_SOURCE' END,
   CASE WHEN unresolved_product=1 AND product_count=0 THEN 'SOURCE_PRODUCT_ID_MISSING' END,
   CASE WHEN unresolved_product=1 AND product_count>1 THEN 'AMBIGUOUS_PROJECT_PRODUCTS' END,
   CASE WHEN unresolved_product=1 AND duplicate_sources>0 THEN 'DUPLICATE_EFFECTIVE_SOURCES' END,
   CASE WHEN unresolved_product=1 AND product_count=1 AND (product_code IS NULL OR product_name IS NULL OR product_code='' OR product_name='') THEN 'CATALOG_MISSING_DELETED_OR_CODE_NAME_EMPTY' END,
   CASE WHEN snapshot_product_mismatch=1 AND project_code_id IS NULL THEN 'SNAPSHOT_OWN_PRODUCT_WITHOUT_PROJECT' END,
   CASE WHEN snapshot_product_mismatch=1 AND project_code_id IS NOT NULL AND (product_count IS NULL OR product_count<>1) THEN 'SNAPSHOT_PROJECT_IDENTITY_UNRESOLVED' END,
   CASE WHEN snapshot_product_mismatch=1 AND snapshot_product_shape='OBJECT' AND NOT(snapshot_product_id<=>CAST(resolved_product_id AS CHAR)) THEN 'SNAPSHOT_PRODUCT_ID_DIFFERS' END,
   CASE WHEN snapshot_product_mismatch=1 AND snapshot_product_shape='OBJECT' AND NOT(snapshot_project_id<=>CAST(project_code_id AS CHAR)) THEN 'SNAPSHOT_PROJECT_ID_DIFFERS' END
 )) FROM details WHERE unresolved_product=1 OR snapshot_product_mismatch=1 ORDER BY tenant_id,certificate_id;
-- release-target-preflight: migrationId=20261127_dcc_registration_certificate_multi_products; allowedEnvironments=test,backup,prod
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
), blocked_projects AS (SELECT DISTINCT tenant_id,project_code_id FROM candidates WHERE product_count IS NULL OR product_count<>1 OR duplicate_sources>0 OR product_code IS NULL OR product_name IS NULL OR product_code='' OR product_name='')
SELECT JSON_OBJECT('kind','project','tenantId',b.tenant_id,'projectCodeId',b.project_code_id,'exists',p.id IS NOT NULL,'deleted',CAST(p.deleted AS UNSIGNED)) FROM blocked_projects b LEFT JOIN dcc_project_code p ON p.tenant_id=b.tenant_id AND p.id=b.project_code_id ORDER BY b.tenant_id,b.project_code_id;
-- release-target-preflight: migrationId=20261127_dcc_registration_certificate_multi_products; allowedEnvironments=test,backup,prod
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
), blocked_projects AS (SELECT DISTINCT tenant_id,project_code_id FROM candidates WHERE product_count IS NULL OR product_count<>1 OR duplicate_sources>0 OR product_code IS NULL OR product_name IS NULL OR product_code='' OR product_name='')
SELECT JSON_OBJECT('kind','data-relation','tenantId',r.tenant_id,'id',r.id,'projectCodeId',r.project_code_id,'productCatalogId',r.product_catalog_id,'status',r.relation_status,'deleted',CAST(r.deleted AS UNSIGNED)) FROM dcc_data_relation r JOIN blocked_projects b ON b.tenant_id=r.tenant_id AND b.project_code_id=r.project_code_id ORDER BY r.tenant_id,r.id;
-- release-target-preflight: migrationId=20261127_dcc_registration_certificate_multi_products; allowedEnvironments=test,backup,prod
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
), blocked_projects AS (SELECT DISTINCT tenant_id,project_code_id FROM candidates WHERE product_count IS NULL OR product_count<>1 OR duplicate_sources>0 OR product_code IS NULL OR product_name IS NULL OR product_code='' OR product_name='')
SELECT JSON_OBJECT('kind','approved-relation','tenantId',r.tenant_id,'id',r.id,'projectCodeId',r.project_code_id,'productCatalogId',r.product_catalog_id,'status',r.relation_status,'deleted',CAST(r.deleted AS UNSIGNED),'requestId',r.request_id,'requestExists',q.id IS NOT NULL,'requestStatus',q.status,'requestDeleted',CAST(q.deleted AS UNSIGNED),'generatedProjectCodeId',q.generated_project_code_id,'generatedProductCatalogId',q.generated_product_catalog_id,'requestRelationId',q.relation_id) FROM dcc_project_product_relation r JOIN blocked_projects b ON b.tenant_id=r.tenant_id AND b.project_code_id=r.project_code_id LEFT JOIN dcc_project_product_create_request q ON q.tenant_id=r.tenant_id AND q.id=r.request_id ORDER BY r.tenant_id,r.id;
SELECT JSON_OBJECT('kind','binding-schema','exists',EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_product'));
ROLLBACK;