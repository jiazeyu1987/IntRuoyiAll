-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=high
-- Run with the paired application release. Retain global source tables as initial templates.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `system_tenant_dict_type`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '字典名称',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '字典类型',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0正常 1停用）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `tenant_id` bigint NOT NULL,
  KEY `idx_tenant` (`tenant_id`),
  `active_type` varchar(100) GENERATED ALWAYS AS (IF(deleted = 0, type, NULL)) STORED,
  `active_name` varchar(100) GENERATED ALWAYS AS (IF(deleted = 0, name, NULL)) STORED,
  UNIQUE KEY `uk_tenant_type` (`tenant_id`,`active_type`),
  KEY `idx_tenant_name` (`tenant_id`,`active_name`),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '字典类型表';

CREATE TABLE IF NOT EXISTS `system_tenant_dict_data`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `sort` int NOT NULL DEFAULT 0 COMMENT '字典排序',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '字典标签',
  `value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '字典类型',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0正常 1停用）',
  `color_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '颜色类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'css 样式',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL,
  KEY `idx_tenant` (`tenant_id`),
  `active_value` varchar(100) GENERATED ALWAYS AS (IF(deleted = 0, value, NULL)) STORED,
  UNIQUE KEY `uk_tenant_value` (`tenant_id`,`dict_type`,`active_value`),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '字典数据表';

CREATE TABLE IF NOT EXISTS system_tenant_dict_snapshot (
 tenant_id bigint NOT NULL PRIMARY KEY,
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

DROP PROCEDURE IF EXISTS migrate_tenant_dictionaries;
DELIMITER $$
CREATE PROCEDURE migrate_tenant_dictionaries()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 -- Dictionary names are display labels: distinct source types may share a name.
 -- Correct tables created before a failed migration without merging either type.
 IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
     AND table_name='system_tenant_dict_type' AND index_name='uk_tenant_name') THEN
  ALTER TABLE system_tenant_dict_type DROP INDEX uk_tenant_name,
    ADD INDEX idx_tenant_name (tenant_id,active_name);
 END IF;
 START TRANSACTION;
 IF (SELECT COUNT(*) FROM system_dict_data d WHERE d.deleted=0 AND NOT EXISTS
    (SELECT 1 FROM system_dict_type t WHERE t.type=d.dict_type AND t.deleted=0)) > 0 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='TENANT_DICTIONARY_SOURCE_TYPE_MISSING';
 END IF;
INSERT INTO system_tenant_dict_type (tenant_id,name,type,status,remark,creator,create_time,updater,update_time,deleted,deleted_time)
SELECT t.id,s.name,s.type,s.status,s.remark,s.creator,s.create_time,s.updater,s.update_time,s.deleted,s.deleted_time FROM system_tenant t CROSS JOIN system_dict_type s
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_dict_snapshot x WHERE x.tenant_id=t.id);
INSERT INTO system_tenant_dict_data (tenant_id,sort,label,value,dict_type,status,color_type,css_class,remark,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.sort,s.label,s.value,s.dict_type,s.status,s.color_type,s.css_class,s.remark,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_dict_data s
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_dict_snapshot x WHERE x.tenant_id=t.id);
 INSERT INTO system_tenant_dict_snapshot (tenant_id)
 SELECT id FROM system_tenant t WHERE NOT EXISTS (SELECT 1 FROM system_tenant_dict_snapshot x WHERE x.tenant_id=t.id);
 COMMIT;
END$$
DELIMITER ;
CALL migrate_tenant_dictionaries();
DROP PROCEDURE migrate_tenant_dictionaries;
