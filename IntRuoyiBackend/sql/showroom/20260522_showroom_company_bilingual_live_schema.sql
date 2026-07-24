-- 修复本机/测试环境 live MySQL `showroom_company_revision` 缺少公司双语字段的问题
-- 目标库：ruoyi-vue-pro

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'showroom_company_revision'
        AND column_name = 'development_history_en'
    ),
    'SELECT ''development_history_en_exists''',
    'ALTER TABLE showroom_company_revision ADD COLUMN development_history_en TEXT NULL AFTER development_history'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'showroom_company_revision'
        AND column_name = 'park_introduction_en'
    ),
    'SELECT ''park_introduction_en_exists''',
    'ALTER TABLE showroom_company_revision ADD COLUMN park_introduction_en TEXT NULL AFTER park_introduction'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'showroom_company_revision'
        AND column_name = 'incubation_platform_en'
    ),
    'SELECT ''incubation_platform_en_exists''',
    'ALTER TABLE showroom_company_revision ADD COLUMN incubation_platform_en TEXT NULL AFTER incubation_platform'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'showroom_company_revision'
        AND column_name = 'subsidiary_overview_en'
    ),
    'SELECT ''subsidiary_overview_en_exists''',
    'ALTER TABLE showroom_company_revision ADD COLUMN subsidiary_overview_en TEXT NULL AFTER subsidiary_overview'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'showroom_company_revision'
        AND column_name = 'stock_info_en'
    ),
    'SELECT ''stock_info_en_exists''',
    'ALTER TABLE showroom_company_revision ADD COLUMN stock_info_en TEXT NULL AFTER stock_info'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'showroom_company_revision'
        AND column_name = 'core_manufacturing_capability_en'
    ),
    'SELECT ''core_manufacturing_capability_en_exists''',
    'ALTER TABLE showroom_company_revision ADD COLUMN core_manufacturing_capability_en TEXT NULL AFTER core_manufacturing_capability'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'showroom_company_revision'
        AND column_name = 'honors_awards_en'
    ),
    'SELECT ''honors_awards_en_exists''',
    'ALTER TABLE showroom_company_revision ADD COLUMN honors_awards_en TEXT NULL AFTER honors_awards'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
