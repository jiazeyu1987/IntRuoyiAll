-- Read-only: unresolved identities and products claimed by multiple certificates. Run before the matching release migration.
SET NAMES utf8mb4;
WITH sources AS (
 SELECT tenant_id,project_code_id,product_catalog_id,id AS relation_id,'CONFIRMED' AS source
 FROM dcc_data_relation WHERE deleted=0 AND relation_status='CONFIRMED'
 UNION ALL
 SELECT r.tenant_id,r.project_code_id,r.product_catalog_id,r.id,'APPROVED'
 FROM dcc_project_product_relation r JOIN dcc_project_product_create_request q
 ON q.tenant_id=r.tenant_id AND q.id=r.request_id AND q.deleted=0 AND q.status='COMPLETED'
 AND q.generated_project_code_id=r.project_code_id AND q.generated_product_catalog_id=r.product_catalog_id AND q.relation_id=r.id
 WHERE r.deleted=0 AND r.relation_status='ACTIVE'
), projects AS (
 SELECT tenant_id,project_code_id,MIN(product_catalog_id) AS product_catalog_id,
 COUNT(DISTINCT product_catalog_id) AS products,COUNT(*)-COUNT(DISTINCT source) AS duplicate_sources
 FROM sources GROUP BY tenant_id,project_code_id
), candidates AS (
 SELECT c.tenant_id,c.id AS certificate_id,c.project_code_id,p.product_catalog_id,p.products,p.duplicate_sources,
 catalog.product_code,catalog.product AS product_name
 FROM dcc_registration_certificate c
 LEFT JOIN projects p ON p.tenant_id=c.tenant_id AND p.project_code_id=c.project_code_id
 LEFT JOIN dcc_product_catalog catalog ON catalog.tenant_id=p.tenant_id AND catalog.id=p.product_catalog_id AND catalog.deleted=0
 WHERE c.deleted=0 AND c.project_code_id IS NOT NULL AND (c.current_version_id IS NOT NULL OR EXISTS(
 SELECT 1 FROM dcc_registration_certificate_access_request a WHERE a.tenant_id=c.tenant_id AND a.certificate_id=c.id
 AND a.deleted=0 AND a.request_type='UPLOAD_CERTIFICATE' AND a.status IN ('SUBMITTED','BPM_BOUND')))
)
SELECT 'UNRESOLVED_PRODUCT' AS issue,tenant_id,product_catalog_id,CAST(certificate_id AS CHAR) AS certificate_ids,
 project_code_id,product_code,product_name
 FROM candidates WHERE products IS NULL OR products<>1 OR duplicate_sources>0 OR COALESCE(product_code,'')='' OR COALESCE(product_name,'')=''
UNION ALL
SELECT 'MULTIPLE_CERTIFICATES',tenant_id,product_catalog_id,GROUP_CONCAT(certificate_id ORDER BY certificate_id),NULL,MIN(product_code),MIN(product_name)
 FROM candidates GROUP BY tenant_id,product_catalog_id HAVING COUNT(DISTINCT certificate_id)>1
UNION ALL
SELECT 'SNAPSHOT_PRODUCT_MISMATCH',c.tenant_id,p.product_catalog_id,CAST(c.id AS CHAR),c.project_code_id,NULL,NULL
 FROM dcc_registration_certificate c
 JOIN dcc_registration_certificate_snapshot s ON s.tenant_id=c.tenant_id AND s.id=c.current_snapshot_id
 LEFT JOIN projects p ON p.tenant_id=c.tenant_id AND p.project_code_id=c.project_code_id
 WHERE c.deleted=0 AND JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.ownProduct'))='true'
 AND (c.project_code_id IS NULL OR p.products IS NULL OR p.products<>1 OR
 (JSON_TYPE(JSON_EXTRACT(s.facts_json,'$.projectProduct'))='OBJECT' AND
 (NOT(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.productCatalogId'))<=>CAST(p.product_catalog_id AS CHAR))
 OR NOT(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.projectCodeId'))<=>CAST(c.project_code_id AS CHAR)))))
UNION ALL
SELECT 'RELATION_CERTIFICATE_MISMATCH',m.tenant_id,m.product_catalog_id,CONCAT(m.certificate_id,',',r.registration_certificate_id),m.project_code_id,m.product_code,m.product_name
 FROM candidates m JOIN dcc_data_relation r ON r.tenant_id=m.tenant_id AND r.product_catalog_id=m.product_catalog_id
 WHERE r.deleted=0 AND r.relation_status='CONFIRMED' AND r.registration_certificate_id IS NOT NULL AND r.registration_certificate_id<>m.certificate_id;
