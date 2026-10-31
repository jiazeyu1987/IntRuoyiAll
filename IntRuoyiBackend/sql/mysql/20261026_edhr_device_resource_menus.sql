-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_device_management_directory; type=menu; riskLevel=low
-- Reparent existing resource pages, retaining page/button identities and original absolute URLs.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS move_edhr_device_resource_menus;
DELIMITER $$
CREATE PROCEDURE move_edhr_device_resource_menus()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_device_resources;
  CREATE TEMPORARY TABLE tmp_edhr_device_resources (
    id BIGINT PRIMARY KEY, old_parent BIGINT, old_path VARCHAR(200),
    path VARCHAR(200), component VARCHAR(255), sort INT)
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  INSERT INTO tmp_edhr_device_resources VALUES
    (5170,5101,'workstation','/mes/md/workstation','mes/md/workstation/index',80),
    (5160,5101,'workstation/workshop','/mes/md/workstation/workshop','mes/md/workstation/workshop/index',90),
    (5630,5500,'template','/mes/qc/template','mes/qc/template/index',100),
    (5610,5500,'indicator','/mes/qc/indicator','mes/qc/indicator/index',110);
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND parent_id=0
      AND path='/mes/edhr-batch-processing' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=5300 AND parent_id=900220
      AND path='/mes/dv' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=5320 AND parent_id=5300
      AND component='mes/dv/machinery/index' AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing eDHR equipment directory or machinery page';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_edhr_device_resources p LEFT JOIN system_menu m ON m.id=p.id
      WHERE m.id IS NULL OR m.deleted<>b'0' OR m.status<>0 OR m.type<>2
        OR BINARY m.component<>BINARY p.component
        OR NOT ((m.parent_id=p.old_parent AND BINARY m.path=BINARY p.old_path)
          OR (m.parent_id=5300 AND BINARY m.path=BINARY p.path))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Resource menu missing or identity changed';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_edhr_device_resources p JOIN system_menu m
      ON BINARY m.path=BINARY p.path WHERE m.deleted=b'0' AND m.id<>p.id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Resource menu absolute route conflict';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;
  UPDATE system_menu m JOIN tmp_edhr_device_resources p ON m.id=p.id
    SET m.parent_id=5300,m.path=p.path,m.sort=p.sort,
      m.updater='edhr-resource-menu',m.update_time=NOW();
  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_resource_descendants;
  CREATE TEMPORARY TABLE tmp_edhr_resource_descendants (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_edhr_resource_descendants
  WITH RECURSIVE descendants AS (
    SELECT id FROM tmp_edhr_device_resources
    UNION ALL
    SELECT m.id FROM system_menu m JOIN descendants d ON m.parent_id=d.id WHERE m.deleted=b'0'
  ) SELECT DISTINCT id FROM descendants;
  -- Grant navigation ancestors only to existing page/button holders; no new business grants.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT s.role_id,a.id,'edhr-resource-menu',NOW(),'edhr-resource-menu',NOW(),b'0',s.tenant_id
  FROM system_role_menu s JOIN tmp_edhr_resource_descendants d ON d.id=s.menu_id
  CROSS JOIN (SELECT 5300 AS id UNION ALL SELECT 900220) a
  WHERE s.deleted=b'0' AND NOT EXISTS (SELECT 1 FROM system_role_menu t WHERE t.deleted=b'0'
    AND t.role_id=s.role_id AND t.tenant_id=s.tenant_id AND t.menu_id=a.id);
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',5300),updater='edhr-resource-menu',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('5300' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_resource_descendants d
      WHERE JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(d.id AS JSON),'$'));
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',900220),updater='edhr-resource-menu',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('900220' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_resource_descendants d
      WHERE JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(d.id AS JSON),'$'));
  IF (SELECT COUNT(*) FROM system_menu m JOIN tmp_edhr_device_resources p ON m.id=p.id
      WHERE m.parent_id=5300 AND BINARY m.path=BINARY p.path AND m.sort=p.sort)<>4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Resource menu move postcondition failed';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE tmp_edhr_resource_descendants;
  DROP TEMPORARY TABLE tmp_edhr_device_resources;
END$$
DELIMITER ;
CALL move_edhr_device_resource_menus();
DROP PROCEDURE move_edhr_device_resource_menus;
