-- 修复本机/测试环境 live MySQL `showroom_product_revision` 缺少产品双语字段的问题
-- 目标库：ruoyi-vue-pro

SET @table_exists := (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'showroom_product_revision'
);

SET @alter_sql := IF(
  @table_exists = 0,
  'SELECT ''showroom_product_revision_missing''',
  CONCAT(
    'ALTER TABLE showroom_product_revision ',
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = ''showroom_product_revision'' AND column_name = ''target_market_en''
      ),
      '',
      'ADD COLUMN target_market_en TEXT NULL AFTER target_market, '
    ),
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = ''showroom_product_revision'' AND column_name = ''pipeline_layout_en''
      ),
      '',
      'ADD COLUMN pipeline_layout_en TEXT NULL AFTER pipeline_layout, '
    ),
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = ''showroom_product_revision'' AND column_name = ''registration_certificate_en''
      ),
      '',
      'ADD COLUMN registration_certificate_en TEXT NULL AFTER registration_certificate, '
    ),
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = ''showroom_product_revision'' AND column_name = ''indication_content_en''
      ),
      '',
      'ADD COLUMN indication_content_en TEXT NULL AFTER indication_content, '
    ),
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = ''showroom_product_revision'' AND column_name = ''core_selling_points_en''
      ),
      '',
      'ADD COLUMN core_selling_points_en TEXT NULL AFTER core_selling_points, '
    ),
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = ''showroom_product_revision'' AND column_name = ''model_specification_en''
      ),
      '',
      'ADD COLUMN model_specification_en TEXT NULL AFTER model_specification, '
    ),
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = ''showroom_product_revision'' AND column_name = ''clinical_effect_en''
      ),
      '',
      'ADD COLUMN clinical_effect_en TEXT NULL AFTER clinical_effect, '
    ),
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = ''showroom_product_revision'' AND column_name = ''fim_status_en''
      ),
      '',
      'ADD COLUMN fim_status_en VARCHAR(255) NULL AFTER fim_status, '
    )
  )
);

SET @alter_sql := TRIM(@alter_sql);
SET @alter_sql := REGEXP_REPLACE(@alter_sql, ',\\s*$', '');
SET @alter_sql := IF(@alter_sql = 'ALTER TABLE showroom_product_revision', 'SELECT ''showroom_product_revision_columns_already_present''', @alter_sql);

PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
