-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261021_mes_craft_library; type=schema; riskLevel=medium
-- 单独保留设备身份，不改写已发布路线 JSON/hash 或订单、批记录。
-- LEGACY_BASELINE 是迁移时的设备资料基线，不声称恢复从未记录的历史名称。
CREATE TABLE IF NOT EXISTS mes_pro_route_device_identity_snapshot (
  tenant_id BIGINT NOT NULL,
  route_version_id BIGINT NOT NULL,
  identities_json LONGTEXT NOT NULL,
  identities_sha256 CHAR(64) NOT NULL,
  capture_source VARCHAR(32) NOT NULL,
  captured_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  captured_by VARCHAR(64) NOT NULL DEFAULT '',
  PRIMARY KEY (tenant_id, route_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 只补尚未建立基线的版本；重复执行不会覆盖已保存的设备身份。
INSERT INTO mes_pro_route_device_identity_snapshot
  (tenant_id, route_version_id, identities_json, identities_sha256, capture_source, captured_by)
SELECT baseline.tenant_id, baseline.id, baseline.identities, SHA2(baseline.identities,256),
       'LEGACY_BASELINE', '20261118-migration'
FROM (
  SELECT v.tenant_id, v.id, CAST(COALESCE((
    SELECT JSON_OBJECTAGG(CAST(refs.device_id AS CHAR), JSON_OBJECT(
      'deviceId', refs.device_id, 'deviceCode', d.device_code, 'deviceName', d.device_name))
    FROM JSON_TABLE(v.route_snapshot_json,
      '$.configSnapshots.productionProcessConfigs[*].deviceSelectionGroups[*].deviceIds[*]'
      COLUMNS(device_id BIGINT PATH '$')) refs
    LEFT JOIN mes_pro_process_pool_team_device d
      ON d.id=refs.device_id AND d.tenant_id=v.tenant_id AND d.deleted=0
  ),JSON_OBJECT()) AS CHAR CHARACTER SET utf8mb4) identities
  FROM mes_pro_route_version v
  WHERE v.deleted=0 AND JSON_VALID(v.route_snapshot_json)=1
) baseline
LEFT JOIN mes_pro_route_device_identity_snapshot existing
  ON existing.tenant_id=baseline.tenant_id AND existing.route_version_id=baseline.id
WHERE existing.route_version_id IS NULL;
