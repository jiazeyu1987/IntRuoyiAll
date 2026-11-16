-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=high
-- Configuration isolation only. Original ids stay stable within each tenant; objects are not copied by this SQL.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS `infra_tenant_file_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名',
  `storage` tinyint NOT NULL COMMENT '存储器',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `master` bit(1) NOT NULL COMMENT '是否为主配置',
  `config` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储配置',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`,`tenant_id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件配置表';
CREATE TABLE IF NOT EXISTS infra_tenant_file_config_snapshot (
 tenant_id bigint NOT NULL PRIMARY KEY,
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
DROP PROCEDURE IF EXISTS migrate_tenant_file_config;
DELIMITER $$
CREATE PROCEDURE migrate_tenant_file_config()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 IF (SELECT COUNT(*) FROM infra_file_config WHERE master=1 AND deleted=0) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_FILE_DEFAULT_CONFIG_REQUIRED';
 END IF;
INSERT INTO infra_tenant_file_config (tenant_id,id,name,storage,remark,master,config,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.`id`,s.`name`,s.`storage`,s.`remark`,s.`master`,s.`config`,s.`creator`,s.`create_time`,s.`updater`,s.`update_time`,s.`deleted` FROM system_tenant t CROSS JOIN infra_file_config s
WHERE NOT EXISTS (SELECT 1 FROM infra_tenant_file_config_snapshot x WHERE x.tenant_id=t.id);
 INSERT INTO infra_tenant_file_config_snapshot (tenant_id)
 SELECT id FROM system_tenant t WHERE NOT EXISTS (SELECT 1 FROM infra_tenant_file_config_snapshot x WHERE x.tenant_id=t.id);
 COMMIT;
END$$
DELIMITER ;
CALL migrate_tenant_file_config();
DROP PROCEDURE migrate_tenant_file_config;
