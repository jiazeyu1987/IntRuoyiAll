-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_mes_kingdee_production_material_list; type=schema; riskLevel=low
-- 生产用料清单补齐 ERP 图号字段；应发数量、需求日期、发料方式已在基表中存在。

SET @mes_kingdee_production_material_list_exists = (
    SELECT COUNT(1)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_kingdee_production_material_list'
);
SET @mes_kingdee_production_material_list_exists_sql = IF(
    @mes_kingdee_production_material_list_exists = 1,
    'SELECT 1',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''missing required table mes_kingdee_production_material_list'''
);
PREPARE mes_kingdee_production_material_list_exists_stmt FROM @mes_kingdee_production_material_list_exists_sql;
EXECUTE mes_kingdee_production_material_list_exists_stmt;
DEALLOCATE PREPARE mes_kingdee_production_material_list_exists_stmt;

SET @mes_kingdee_production_material_list_drawing_number_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_kingdee_production_material_list'
      AND COLUMN_NAME = 'drawing_number'
);
SET @mes_kingdee_production_material_list_drawing_number_sql = IF(
    @mes_kingdee_production_material_list_drawing_number_exists = 0,
    'ALTER TABLE `mes_kingdee_production_material_list` ADD COLUMN `drawing_number` varchar(128) DEFAULT NULL COMMENT ''图号'' AFTER `child_unit_name`',
    'SELECT 1'
);
PREPARE mes_kingdee_production_material_list_drawing_number_stmt FROM @mes_kingdee_production_material_list_drawing_number_sql;
EXECUTE mes_kingdee_production_material_list_drawing_number_stmt;
DEALLOCATE PREPARE mes_kingdee_production_material_list_drawing_number_stmt;
