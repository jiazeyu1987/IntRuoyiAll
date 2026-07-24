# 执行日志：DCC 单文件基础信息维护后端

BDD: 文控直改单文件基础信息 -> Given 用户具备系统角色 `doc_control` 且文件存在 / When 调用单文件基础信息维护接口 / Then 后端更新产品名称、文件名称、产品编号、文件编号、文件类别和受控目录，不启动 BPM、不改变状态。

BDD: 非文控账号被拒绝 -> Given 用户没有系统角色 `doc_control` / When 调用单文件基础信息维护接口 / Then 请求被拒绝，不更新受控文件。

BDD: 目录必须位于类别绑定范围 -> Given 文控选择的受控目录不在目标文件类别绑定目录范围内 / When 保存基础信息 / Then 后端明确失败，不更新文件。

BDD: 文件链冲突必须失败 -> Given 目标类别和文件名已存在不兼容的文件编号、版本或当前有效文件 / When 保存基础信息 / Then 后端明确失败，不合并或降级处理。

RED: mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileMetadataUpdateControllerTest,DccControlledFileQueryServiceTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, expected missing backend metadata update contract, productName DO/VO field, mapper method, and doc_control-only error code.

GREEN: mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileMetadataUpdateControllerTest,DccControlledFileQueryServiceTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS, 60 tests.

GREEN: mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileMetadataUpdateServiceTest,DccControlledFileMetadataUpdateControllerTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS, 116 tests.

GREEN: backend-api evidence validator -> PASS.

GREEN: database-schema evidence validator -> PASS.

GREEN: git diff --check -> PASS, only CRLF normalization warnings.

BLOCKED: cross-repo Playwright positive prerequisite probe -> BLOCKED, official role simple-list API did not return enabled role code doc_control for local test tenant; no role assignment or metadata save was performed.

BLOCKED: cross-repo Playwright source-role copy probe -> BLOCKED, source tenant 芋道源码 role page did not have code doc_control; no target tenant role creation, role assignment, or metadata save was performed.

INFO: 用户要求在 `芋道源码` 与测试租户都新增 `doc_control` 角色；按项目基线，未修改受保护的 `芋道源码` 租户，仅在本机测试租户创建 `文控/code=doc_control`。

GREEN: docker exec int-ruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -e "ALTER TABLE dcc_controlled_file ADD COLUMN product_name varchar(255) DEFAULT NULL AFTER product_code" -> PASS, 本机 Docker MySQL 运行库补齐本任务 schema 字段。

GREEN: cross-repo Playwright create-test-role positive path -> PASS, 创建/确认测试租户角色 `910217/doc_control`，临时赋给 `测试租户/aoteman`，metadata PUT 保存文件 `2054545668044046254` 产品名称成功，随后恢复文件 productName 与用户原角色 `111,910209`。

GREEN: docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -N -e "SELECT id, tenant_id, name, HEX(name), code, status, deleted FROM system_role WHERE tenant_id=122 AND code='doc_control';" -> PASS, 角色 `910217/doc_control` 名称为 `文控`，UTF-8 hex 为 `E69687E68EA7`。

GREEN: docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -N -e "SELECT u.id, u.username, u.tenant_id, GROUP_CONCAT(ur.role_id ORDER BY ur.role_id) AS role_ids FROM system_users u LEFT JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=b'0' WHERE u.tenant_id=122 AND u.username='aoteman' GROUP BY u.id,u.username,u.tenant_id; SELECT id, product_name, HEX(COALESCE(product_name,'')) FROM dcc_controlled_file WHERE id=2054545668044046254; SHOW COLUMNS FROM dcc_controlled_file LIKE 'product_name';" -> PASS, `测试租户/aoteman` 角色为 `111,910209`，测试文件 `product_name` 为 `NULL`，字段 `product_name` 存在。

GREEN: final reverify `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileMetadataUpdateServiceTest,DccControlledFileMetadataUpdateControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 116 tests.

GREEN: `python -m pytest script/tests/test_dcc_sql_scripts.py -q` -> PASS, 8 passed; schema script test verifies `product_name` in base schema, runtime repair schema, standalone migration, and DCC test schema.

GREEN: task-closeout-cleanup preview -> PASS, no blocked cleanup paths; preview kept task records and reported only task-specific auxiliary artifacts as cleanup candidates.
