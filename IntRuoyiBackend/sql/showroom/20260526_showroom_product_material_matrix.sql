ALTER TABLE `showroom_version_bundle`
  MODIFY COLUMN `narration_zh_version_id` bigint DEFAULT NULL,
  MODIFY COLUMN `narration_en_version_id` bigint DEFAULT NULL;
