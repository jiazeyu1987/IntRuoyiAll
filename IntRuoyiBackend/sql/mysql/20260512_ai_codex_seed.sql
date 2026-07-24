-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=seed; riskLevel=low
-- AI Codex CLI seed data for MySQL.
-- Safe to run repeatedly: inserts only when the target row does not already exist.

UPDATE `ai_api_key`
SET `tenant_id` = 1, `updater` = '1', `update_time` = NOW()
WHERE `platform` = 'CodexCli';

UPDATE `ai_model`
SET `tenant_id` = 1, `updater` = '1', `update_time` = NOW()
WHERE `platform` = 'CodexCli' AND `type` = 1;

INSERT INTO `system_dict_data`
(`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT COALESCE((SELECT MAX(`id`) FROM `system_dict_data`), 0) + 1, 99, 'Codex CLI', 'CodexCli', 'ai_platform', 0, '', '', 'Local Codex CLI platform', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'ai_platform' AND `value` = 'CodexCli'
);

INSERT INTO `ai_api_key`
(`name`, `api_key`, `platform`, `url`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT '本机 Codex CLI', '', 'CodexCli', '', 0, '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `ai_api_key` WHERE `platform` = 'CodexCli'
);

INSERT INTO `ai_model`
(`key_id`, `name`, `model`, `platform`, `type`, `sort`, `status`, `temperature`, `max_tokens`, `max_contexts`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `id`, 'Codex CLI 默认聊天模型', 'codex-cli', 'CodexCli', 1, 0, 0, 0.7, 4096, 10,
       '1', NOW(), '1', NOW(), b'0', 1
FROM `ai_api_key`
WHERE `platform` = 'CodexCli'
  AND NOT EXISTS (
    SELECT 1 FROM `ai_model` WHERE `platform` = 'CodexCli' AND `type` = 1
  )
LIMIT 1;
