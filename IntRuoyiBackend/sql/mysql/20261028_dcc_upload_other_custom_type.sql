-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260526_dcc_other_template_category,20260719_dcc_file_type_taxonomy,20261015_dcc_category_approval_source_mode,20261015_dcc_application_custom_matrix; type=data; riskLevel=medium
-- Configure the formal "其他" category, preserving its existing permissions and tenant ownership.
-- Preserve unique mappings; unmapped or shared "其他" mappings receive a dedicated three-level path.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS configure_dcc_upload_other_custom_type;
DELIMITER $$
CREATE PROCEDURE configure_dcc_upload_other_custom_type()
BEGIN
  DECLARE invalid_path_count INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (
    SELECT 1 FROM dcc_file_category c JOIN system_tenant t ON t.id=c.tenant_id AND t.status=0 AND t.deleted=0
    WHERE c.name='其他' AND c.active=1 AND c.deleted=0
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing active formal other category; run required category seed first';
  END IF;
  IF EXISTS (
    SELECT c.tenant_id FROM dcc_file_category c
    JOIN system_tenant t ON t.id=c.tenant_id AND t.status=0 AND t.deleted=0
    WHERE c.name='其他' AND c.active=1 AND c.deleted=0
    GROUP BY c.tenant_id
    HAVING COUNT(*)>1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Formal other category is ambiguous within its tenant';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_upload_other;
  CREATE TEMPORARY TABLE tmp_dcc_upload_other AS
    SELECT c.id category_id,c.tenant_id,c.file_type_taxonomy_id
    FROM dcc_file_category c
    JOIN system_tenant t ON t.id=c.tenant_id AND t.status=0 AND t.deleted=0
    WHERE c.name='其他' AND c.active=1 AND c.deleted=0;
  -- A shared leaf is not selectable; move only "其他", leaving every other category on its original leaf.
  UPDATE tmp_dcc_upload_other source JOIN dcc_file_category other
    ON other.tenant_id=source.tenant_id AND other.file_type_taxonomy_id=source.file_type_taxonomy_id
    AND other.id<>source.category_id AND other.active=1 AND other.deleted=0
    SET source.file_type_taxonomy_id=NULL;
  IF EXISTS (
    SELECT 1 FROM tmp_dcc_upload_other c
    LEFT JOIN dcc_file_type_taxonomy f ON f.id=c.file_type_taxonomy_id
      AND f.tenant_id=c.tenant_id AND f.active=1 AND f.deleted=0
    WHERE c.file_type_taxonomy_id IS NOT NULL AND (f.id IS NULL OR f.level_no NOT BETWEEN 3 AND 5 OR EXISTS (
      SELECT 1 FROM dcc_file_type_taxonomy child WHERE child.tenant_id=c.tenant_id AND child.parent_id=f.id AND child.deleted=0
    ))
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing other category taxonomy must be an enabled formal leaf of at least three levels';
  END IF;
  WITH RECURSIVE configured_path AS (
    SELECT c.category_id,c.tenant_id,f.id,f.parent_id,f.level_no,f.level_no leaf_level,f.active,f.deleted,1 depth
    FROM tmp_dcc_upload_other c JOIN dcc_file_type_taxonomy f ON f.id=c.file_type_taxonomy_id AND f.tenant_id=c.tenant_id
    UNION ALL
    SELECT path.category_id,path.tenant_id,parent.id,parent.parent_id,parent.level_no,path.leaf_level,parent.active,parent.deleted,path.depth+1
    FROM configured_path path JOIN dcc_file_type_taxonomy parent ON parent.id=path.parent_id AND parent.tenant_id=path.tenant_id
    WHERE path.parent_id<>0 AND path.depth<6
  )
  SELECT COUNT(*) INTO invalid_path_count FROM (
    SELECT category_id FROM configured_path GROUP BY category_id
    HAVING MAX(parent_id=0)<>1 OR MAX(depth) NOT BETWEEN 3 AND 5 OR MAX(depth)<>MAX(leaf_level)
      OR SUM(active<>1 OR deleted<>0 OR level_no+depth<>leaf_level+1)>0
  ) invalid_paths;
  IF invalid_path_count<>0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Other category requires an enabled complete three-to-five-level path';
  END IF;
  INSERT INTO dcc_file_type_taxonomy
    (parent_id,level_no,code,name,active,sort,tenant_id,creator,updater,create_time,update_time,deleted)
    SELECT 0,1,'DCC_UPLOAD_OTHER_ROOT','通用文件',1,999,c.tenant_id,'dcc-upload-other','dcc-upload-other',NOW(),NOW(),0
    FROM tmp_dcc_upload_other c WHERE c.file_type_taxonomy_id IS NULL AND NOT EXISTS (
      SELECT 1 FROM dcc_file_type_taxonomy f WHERE f.tenant_id=c.tenant_id AND f.code='DCC_UPLOAD_OTHER_ROOT' AND f.deleted=0
    );
  INSERT INTO dcc_file_type_taxonomy
    (parent_id,level_no,code,name,active,sort,tenant_id,creator,updater,create_time,update_time,deleted)
    SELECT root.id,2,'DCC_UPLOAD_OTHER_GROUP','其他类型',1,999,c.tenant_id,'dcc-upload-other','dcc-upload-other',NOW(),NOW(),0
    FROM tmp_dcc_upload_other c JOIN dcc_file_type_taxonomy root
      ON root.tenant_id=c.tenant_id AND root.code='DCC_UPLOAD_OTHER_ROOT' AND root.deleted=0
    WHERE c.file_type_taxonomy_id IS NULL AND NOT EXISTS (
      SELECT 1 FROM dcc_file_type_taxonomy f WHERE f.tenant_id=c.tenant_id AND f.code='DCC_UPLOAD_OTHER_GROUP' AND f.deleted=0
    );
  INSERT INTO dcc_file_type_taxonomy
    (parent_id,level_no,code,name,active,sort,tenant_id,creator,updater,create_time,update_time,deleted)
    SELECT grp.id,3,'DCC_UPLOAD_OTHER_LEAF','其他',1,999,c.tenant_id,'dcc-upload-other','dcc-upload-other',NOW(),NOW(),0
    FROM tmp_dcc_upload_other c JOIN dcc_file_type_taxonomy grp
      ON grp.tenant_id=c.tenant_id AND grp.code='DCC_UPLOAD_OTHER_GROUP' AND grp.deleted=0
    WHERE c.file_type_taxonomy_id IS NULL AND NOT EXISTS (
      SELECT 1 FROM dcc_file_type_taxonomy f WHERE f.tenant_id=c.tenant_id AND f.code='DCC_UPLOAD_OTHER_LEAF' AND f.deleted=0
    );
  IF EXISTS (
    SELECT 1 FROM tmp_dcc_upload_other c
    LEFT JOIN dcc_file_type_taxonomy root ON root.tenant_id=c.tenant_id AND root.code='DCC_UPLOAD_OTHER_ROOT' AND root.deleted=0
    LEFT JOIN dcc_file_type_taxonomy grp ON grp.tenant_id=c.tenant_id AND grp.code='DCC_UPLOAD_OTHER_GROUP' AND grp.deleted=0
    LEFT JOIN dcc_file_type_taxonomy leaf ON leaf.tenant_id=c.tenant_id AND leaf.code='DCC_UPLOAD_OTHER_LEAF' AND leaf.deleted=0
    WHERE c.file_type_taxonomy_id IS NULL AND (
      root.id IS NULL OR root.parent_id<>0 OR root.level_no<>1 OR root.name<>'通用文件' OR root.active<>1
      OR grp.id IS NULL OR grp.parent_id<>root.id OR grp.level_no<>2 OR grp.name<>'其他类型' OR grp.active<>1
      OR leaf.id IS NULL OR leaf.parent_id<>grp.id OR leaf.level_no<>3 OR leaf.name<>'其他' OR leaf.active<>1
      OR EXISTS (SELECT 1 FROM dcc_file_type_taxonomy child WHERE child.tenant_id=c.tenant_id AND child.parent_id=leaf.id AND child.deleted=0)
    )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dedicated upload-other taxonomy codes are occupied by conflicting configuration';
  END IF;
  IF EXISTS (
    SELECT 1 FROM tmp_dcc_upload_other source
    JOIN dcc_file_type_taxonomy leaf ON leaf.tenant_id=source.tenant_id AND leaf.code='DCC_UPLOAD_OTHER_LEAF' AND leaf.deleted=0
    JOIN dcc_file_category other ON other.tenant_id=source.tenant_id AND other.file_type_taxonomy_id=leaf.id
      AND other.id<>source.category_id AND other.active=1 AND other.deleted=0
    WHERE source.file_type_taxonomy_id IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dedicated upload-other taxonomy already belongs to another active category';
  END IF;
  UPDATE dcc_file_category target JOIN tmp_dcc_upload_other source ON source.category_id=target.id
    LEFT JOIN dcc_file_type_taxonomy leaf ON leaf.tenant_id=source.tenant_id AND leaf.code='DCC_UPLOAD_OTHER_LEAF' AND leaf.deleted=0
    SET target.file_type_taxonomy_id=COALESCE(source.file_type_taxonomy_id,leaf.id),
        target.approval_source_mode='APPLICATION_CUSTOM',target.updater='dcc-upload-other',target.update_time=NOW();
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_upload_other;
  COMMIT;
END$$
DELIMITER ;
CALL configure_dcc_upload_other_custom_type();
DROP PROCEDURE IF EXISTS configure_dcc_upload_other_custom_type;
