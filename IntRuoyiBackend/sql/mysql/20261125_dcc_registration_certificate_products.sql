-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261030_dcc_registration_certificate_regulatory_facts,20260920_dcc_project_product_create_approval; type=schema; riskLevel=high
-- Run the companion read-only preflight first. Conflicts require a business decision; never delete/overwrite them.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_registration_certificate_product (
 tenant_id BIGINT NOT NULL, product_catalog_id BIGINT NOT NULL, certificate_id BIGINT NOT NULL,
 project_code_id BIGINT NOT NULL, product_code VARCHAR(128) NOT NULL, product_name VARCHAR(255) NOT NULL,
 state VARCHAR(16) NOT NULL, created_by BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,product_catalog_id),
 UNIQUE KEY uk_reg_product_project(tenant_id,project_code_id),
 KEY idx_reg_product_certificate(tenant_id,certificate_id),
 CONSTRAINT chk_reg_product_state CHECK(state IN ('RESERVED','BOUND'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='One certificate per DCC product, including approval reservations';
DROP TEMPORARY TABLE IF EXISTS reg_product_sources;
CREATE TEMPORARY TABLE reg_product_sources AS
 SELECT tenant_id,project_code_id,product_catalog_id,id AS relation_id,'CONFIRMED' AS source
 FROM dcc_data_relation WHERE deleted=0 AND relation_status='CONFIRMED'
 UNION ALL
 SELECT r.tenant_id,r.project_code_id,r.product_catalog_id,r.id,'APPROVED'
 FROM dcc_project_product_relation r JOIN dcc_project_product_create_request q
 ON q.tenant_id=r.tenant_id AND q.id=r.request_id AND q.deleted=0 AND q.status='COMPLETED'
 AND q.generated_project_code_id=r.project_code_id AND q.generated_product_catalog_id=r.product_catalog_id AND q.relation_id=r.id
 WHERE r.deleted=0 AND r.relation_status='ACTIVE';
DROP TEMPORARY TABLE IF EXISTS reg_project_products;
CREATE TEMPORARY TABLE reg_project_products AS
 SELECT r.tenant_id,r.project_code_id,MIN(r.product_catalog_id) AS product_catalog_id,
 COUNT(DISTINCT r.product_catalog_id) AS product_count,
 COUNT(*)-COUNT(DISTINCT r.source) AS duplicate_sources,
 CAST(CONCAT('[',GROUP_CONCAT(JSON_OBJECT('source',r.source,'relationId',CAST(r.relation_id AS CHAR)) ORDER BY r.source,r.relation_id SEPARATOR ','),']') AS JSON) AS bindings,
 MIN(p.product_code) AS product_code,MIN(p.product) AS product_name
 FROM reg_product_sources r LEFT JOIN dcc_product_catalog p ON p.tenant_id=r.tenant_id AND p.id=r.product_catalog_id AND p.deleted=0
 GROUP BY r.tenant_id,r.project_code_id;
DROP TEMPORARY TABLE IF EXISTS reg_product_migration;
CREATE TEMPORARY TABLE reg_product_migration AS
 SELECT c.tenant_id,c.id AS certificate_id,c.project_code_id,p.product_catalog_id,p.product_count,p.duplicate_sources,p.product_code,p.product_name,
 CASE WHEN c.current_version_id IS NULL THEN 'RESERVED' ELSE 'BOUND' END AS state
 FROM dcc_registration_certificate c
 LEFT JOIN reg_project_products p ON p.tenant_id=c.tenant_id AND p.project_code_id=c.project_code_id
 WHERE c.deleted=0 AND c.project_code_id IS NOT NULL AND
 (c.current_version_id IS NOT NULL OR EXISTS(SELECT 1 FROM dcc_registration_certificate_access_request a
 WHERE a.tenant_id=c.tenant_id AND a.certificate_id=c.id AND a.deleted=0 AND a.request_type='UPLOAD_CERTIFICATE' AND a.status IN ('SUBMITTED','BPM_BOUND')));
-- Visible conflict report is emitted before SIGNAL. No business data is changed by validation.
SELECT * FROM reg_product_migration WHERE product_count IS NULL OR product_count<>1 OR duplicate_sources>0 OR product_code IS NULL OR product_name IS NULL;
SELECT tenant_id,product_catalog_id,GROUP_CONCAT(certificate_id ORDER BY certificate_id) AS conflicting_certificates
 FROM reg_product_migration GROUP BY tenant_id,product_catalog_id HAVING COUNT(DISTINCT certificate_id)>1;
DROP PROCEDURE IF EXISTS validate_reg_products_20261125;
DELIMITER $$
CREATE PROCEDURE validate_reg_products_20261125()
BEGIN
 IF EXISTS(SELECT 1 FROM reg_product_migration WHERE product_count IS NULL OR product_count<>1 OR duplicate_sources>0 OR product_code IS NULL OR product_name IS NULL OR product_code='' OR product_name='') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unresolved DCC product identity; review the registration product preflight';
 END IF;
 IF EXISTS(SELECT 1 FROM dcc_registration_certificate c
  JOIN dcc_registration_certificate_snapshot s ON s.tenant_id=c.tenant_id AND s.id=c.current_snapshot_id
  LEFT JOIN reg_project_products p ON p.tenant_id=c.tenant_id AND p.project_code_id=c.project_code_id
  WHERE c.deleted=0 AND JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.ownProduct'))='true'
  AND (c.project_code_id IS NULL OR p.product_count IS NULL OR p.product_count<>1
   OR (JSON_TYPE(JSON_EXTRACT(s.facts_json,'$.projectProduct'))='OBJECT'
    AND (NOT(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.productCatalogId'))<=>CAST(p.product_catalog_id AS CHAR))
     OR NOT(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.projectProduct.projectCodeId'))<=>CAST(c.project_code_id AS CHAR)))))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Approved snapshot product identity differs from its project; business resolution required';
 END IF;
 IF EXISTS(SELECT 1 FROM reg_product_migration GROUP BY tenant_id,product_catalog_id HAVING COUNT(DISTINCT certificate_id)>1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A product is linked to multiple certificates; business resolution required';
 END IF;
 IF EXISTS(SELECT 1 FROM reg_product_migration m JOIN dcc_registration_certificate_product b ON b.tenant_id=m.tenant_id AND (b.product_catalog_id=m.product_catalog_id OR b.project_code_id=m.project_code_id) WHERE b.certificate_id<>m.certificate_id OR b.product_catalog_id<>m.product_catalog_id OR b.project_code_id<>m.project_code_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing certificate product binding conflicts with migration';
 END IF;
 IF EXISTS(SELECT 1 FROM dcc_data_relation r JOIN reg_product_migration m ON m.tenant_id=r.tenant_id AND m.product_catalog_id=r.product_catalog_id WHERE r.deleted=0 AND r.relation_status='CONFIRMED' AND r.registration_certificate_id IS NOT NULL AND r.registration_certificate_id<>m.certificate_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Product relation references a different certificate; business resolution required';
 END IF;
END$$
DELIMITER ;
CALL validate_reg_products_20261125();
DROP PROCEDURE validate_reg_products_20261125;
INSERT INTO dcc_registration_certificate_product(tenant_id,product_catalog_id,certificate_id,project_code_id,product_code,product_name,state,created_by)
 SELECT m.tenant_id,m.product_catalog_id,m.certificate_id,m.project_code_id,m.product_code,m.product_name,m.state,0 FROM reg_product_migration m
 WHERE NOT EXISTS(SELECT 1 FROM dcc_registration_certificate_product b WHERE b.tenant_id=m.tenant_id AND b.product_catalog_id=m.product_catalog_id);
-- Explicit one-time shape migration. Existing identity and regulatory facts are retained.
DROP TRIGGER IF EXISTS trg_dcc_reg_cert_snapshot_immutable_bu;
UPDATE dcc_registration_certificate_snapshot s
 JOIN dcc_registration_certificate_version v ON v.tenant_id=s.tenant_id AND v.id=s.version_id
 JOIN dcc_registration_certificate c ON c.tenant_id=v.tenant_id AND c.id=v.certificate_id
 LEFT JOIN reg_project_products p ON p.tenant_id=c.tenant_id AND p.project_code_id=c.project_code_id AND p.product_count=1
 SET s.facts_json=JSON_SET(s.facts_json,'$.projectProducts',JSON_ARRAY(
 CASE WHEN JSON_TYPE(JSON_EXTRACT(s.facts_json,'$.projectProduct'))='OBJECT' THEN JSON_EXTRACT(s.facts_json,'$.projectProduct')
 ELSE JSON_OBJECT('projectCodeId',CAST(c.project_code_id AS CHAR),'productCatalogId',CAST(p.product_catalog_id AS CHAR),
 'productCode',p.product_code,'productName',p.product_name,'bindings',p.bindings) END)),s.update_time=s.update_time
 WHERE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.ownProduct'))='true'
 AND (JSON_TYPE(JSON_EXTRACT(s.facts_json,'$.projectProduct'))='OBJECT' OR p.product_catalog_id IS NOT NULL)
 AND JSON_EXTRACT(s.facts_json,'$.projectProducts') IS NULL;
UPDATE dcc_registration_certificate_snapshot s
 SET s.facts_json=JSON_SET(s.facts_json,'$.projectProduct',JSON_EXTRACT(s.facts_json,'$.projectProducts[0]')),s.update_time=s.update_time
 WHERE JSON_LENGTH(JSON_EXTRACT(s.facts_json,'$.projectProducts'))>0 AND NOT(JSON_TYPE(JSON_EXTRACT(s.facts_json,'$.projectProduct'))<=>'OBJECT');
DELIMITER $$
CREATE TRIGGER trg_dcc_reg_cert_snapshot_immutable_bu BEFORE UPDATE ON dcc_registration_certificate_snapshot FOR EACH ROW
BEGIN
 DECLARE linked_version_status VARCHAR(32);
 IF NOT(OLD.id<=>NEW.id) OR NOT(OLD.version_id<=>NEW.version_id) OR NOT(OLD.tenant_id<=>NEW.tenant_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Cross-version snapshot reattachment is forbidden';
 END IF;
 SELECT status INTO linked_version_status FROM dcc_registration_certificate_version WHERE id=OLD.version_id AND tenant_id=OLD.tenant_id AND deleted=b'0';
 IF linked_version_status IS NULL OR linked_version_status<>'DRAFT' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Formal registration certificate snapshot is append-only';
 END IF;
END$$
DELIMITER ;
DROP TEMPORARY TABLE reg_product_migration;
DROP TEMPORARY TABLE reg_project_products;
DROP TEMPORARY TABLE reg_product_sources;
