-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261021_mes_craft_library; type=schema; riskLevel=medium
-- 简易解析发布回执：与业务创建同事务提交，按租户、用户和请求 UUID 持久化幂等。
-- 复用已有 form:parser:production-batch-record:publish 权限，不新增角色或套餐授权。
CREATE TABLE IF NOT EXISTS mes_simple_parser_publish_receipt (
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  request_id CHAR(36) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  result_json LONGTEXT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (tenant_id, user_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
