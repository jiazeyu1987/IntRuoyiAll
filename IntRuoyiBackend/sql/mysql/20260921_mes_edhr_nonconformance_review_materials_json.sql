-- eDHR 不合格评审多材料事实清单
-- 首次执行：为评审单保存当前有效材料清单和上传/删除操作记录。
-- 重复执行：information_schema 守护，保持幂等。

SET @schema_name := DATABASE();
SET @column_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'mes_pro_edhr_nonconformance_review'
      AND COLUMN_NAME = 'review_materials_json'
);
SET @ddl := IF(@column_exists = 0,
    'ALTER TABLE mes_pro_edhr_nonconformance_review ADD COLUMN review_materials_json LONGTEXT NULL COMMENT ''评审材料清单JSON'' AFTER review_material_file_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
