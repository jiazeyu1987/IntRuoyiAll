-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Backfill active scheduling route versions for legacy MES routes.
-- The statement is idempotent: routes that already have any non-deleted version are left unchanged.

INSERT INTO `mes_pro_route_version` (
  `route_id`,
  `version_no`,
  `active`,
  `source_route_version_id`,
  `route_snapshot_json`,
  `remark`,
  `creator`,
  `create_time`,
  `updater`,
  `update_time`,
  `deleted`,
  `tenant_id`
)
SELECT
  r.`id`,
  'V1',
  b'1',
  NULL,
  JSON_OBJECT(
    'routeId', r.`id`,
    'routeCode', r.`code`,
    'routeName', r.`name`,
    'description', r.`description`,
    'status', r.`status`,
    'remark', r.`remark`
  ),
  'legacy route version backfill',
  COALESCE(r.`creator`, ''),
  NOW(),
  COALESCE(r.`updater`, ''),
  NOW(),
  b'0',
  r.`tenant_id`
FROM `mes_pro_route` r
LEFT JOIN `mes_pro_route_version` v
  ON v.`tenant_id` = r.`tenant_id`
  AND v.`route_id` = r.`id`
  AND v.`deleted` = b'0'
WHERE r.`deleted` = b'0'
  AND v.`id` IS NULL;
