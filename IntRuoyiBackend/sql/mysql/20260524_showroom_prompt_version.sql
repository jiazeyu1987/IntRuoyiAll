-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Add showroom image prompt version persistence and batch-task prompt locking.
-- Safe to run repeatedly on MySQL runtime schemas.

DROP PROCEDURE IF EXISTS ensure_showroom_column;
DELIMITER $$
CREATE PROCEDURE ensure_showroom_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = target_table
        AND COLUMN_NAME = target_column
  ) THEN
    SET @ddl_statement = ddl_statement;
    PREPARE stmt FROM @ddl_statement;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL ensure_showroom_column(
  'showroom_product_cover_batch_task',
  'prompt_version_id',
  'ALTER TABLE `showroom_product_cover_batch_task` ADD COLUMN `prompt_version_id` bigint DEFAULT NULL AFTER `cover_generation_mode`'
);

CREATE TABLE IF NOT EXISTS `showroom_image_prompt_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scene_code` varchar(64) NOT NULL,
  `version_no` int NOT NULL,
  `template_text` longtext NOT NULL,
  `change_note` varchar(255) DEFAULT NULL,
  `placeholder_codes_json` varchar(1000) NOT NULL,
  `use_count` int NOT NULL DEFAULT 0,
  `last_used_at` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_showroom_image_prompt_scene_version` (`scene_code`, `version_no`),
  KEY `idx_showroom_image_prompt_scene` (`scene_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Showroom image prompt version';

INSERT INTO `showroom_image_prompt_version`
(`scene_code`, `version_no`, `template_text`, `change_note`, `placeholder_codes_json`, `use_count`, `creator`, `updater`, `tenant_id`)
SELECT
  'PRODUCT_COVER',
  1,
  '生成一张横向医疗器械产品展示图，用于产品列表卡片缩略图。\n背景：\n极简圆角卡片风格背景，整体为很浅的冰蓝色到白色渐变，四周有轻微柔和蓝色光晕，背景干净、通透、明亮，带高级医疗科技感。不要真实展台，不要桌面，不要道具，不要复杂场景，不要文字。\n主体：\n主体是“{{product_name_cn}}”，英文名参考“{{product_name_en}}”。\n如果该产品属于导丝类，只展示一根指引导丝；如果不是导丝类，只展示一个对应的医疗器械产品主体。完整展示，居中偏上放置，轮廓清晰，质感精致，具有医疗器械产品图风格。若有参考产品图，则以前景产品的真实外形、颜色和结构为准，不要随意改造。\n构图与大小：\n产品大小控制在画面宽度的45%到55%，高度约占画面30%到40%，不要太大，不要贴边，四周保留充足留白，视觉比例接近医疗展厅产品卡片中的缩略图效果。\n风格：\n简洁、克制、现代、专业、柔和打光、轻微悬浮感，高端医疗器械目录图风格。\n避免：\n人物、多个物体、复杂背景、重阴影、夸张反光、文字、logo、水印、产品过大、产品贴边、背景过花。\n约束：\n仅根据“{{product_name_cn}}”对应产品生成单个主体，不要替换为其他产品，不要生成方图，不要输出任何说明文字。\n只进行一次原生图片生成。\n最终只返回一个本地 PNG 绝对路径，不要输出其他内容。',
  'V1 seeded from legacy product cover prompt',
  '["product_name_cn","product_name_en"]',
  0,
  'showroom-seed',
  'showroom-seed',
  0
WHERE NOT EXISTS (
  SELECT 1
  FROM `showroom_image_prompt_version`
  WHERE `scene_code` = 'PRODUCT_COVER'
);

UPDATE `showroom_product_cover_batch_task`
SET `prompt_version_id` = (
  SELECT `id`
  FROM `showroom_image_prompt_version`
  WHERE `scene_code` = 'PRODUCT_COVER'
  ORDER BY `version_no` ASC
  LIMIT 1
)
WHERE `prompt_version_id` IS NULL
  AND `status` IN ('WAITING', 'RUNNING');

DROP PROCEDURE IF EXISTS ensure_showroom_column;
