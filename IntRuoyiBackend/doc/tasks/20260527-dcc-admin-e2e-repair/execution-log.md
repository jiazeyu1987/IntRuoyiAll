# DCC 电子签名芋道源码/admin 修复执行日志

## BDD

- BDD: 测试环境 DCC 后端可启动 -> Given 部署模板包含 DCC 签名证据 HMAC secret 和 key version When 测试环境启动后端 Then `/actuator/health` 必须返回健康且日志中不得出现 signature evidence configuration missing。
- BDD: 电子签名 schema 可供新接口读取 -> Given 目标数据库应用 DCC 电子签名加固迁移 When 查询授权、签名证据和策略表字段 Then 授权接口必须包含 authorizationState/locked 等字段，签名表必须包含 evidence_hash/evidence_status 等字段。
- BDD: 测试租户签名证据可验证 -> Given 仅在测试租户准备或生成 DCC 签名记录 When 查询签名记录和导出证据 Then 至少存在一条 `VALID` 证据记录且导出校验为 `VALID`。
- BDD: 芋道源码/admin 只读验证通过 -> Given 使用 `芋道源码/admin` 登录生产前端 When 通过真实页面查看 DCC 电子签名授权、签名记录和证据状态 Then 页面/API 数据符合新契约且验证过程不发送 DCC 写请求。

## Evidence

- 2026-05-27：芋道源码/admin 只读 E2E 失败。生产环境登录成功，但签名记录为 0，授权接口仍缺少 `authorizationState`、`locked` 等新字段，签名记录摘要为空。
- 2026-05-27：测试/生产数据库检查显示 `dcc_electronic_signature_authorization` 缺少 `authorization_state`，`dcc_controlled_file_signature` 缺少 `evidence_hash`，签名与授权数据仅存在于测试租户 `tenant_id=122`。
- 2026-05-27：测试环境后端日志显示 `DccSignatureEvidenceProperties.validateRuntimeConfig` 抛出 `DCC electronic signature evidence configuration is missing`，后端健康检查不可用。

## TDD

- RED: `node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\assert-dcc-deploy-config.mjs` -> FAIL, expected reason: compose missing `--dcc.signature.evidence.*` args and publish env missing `DCC_SIGNATURE_EVIDENCE_*` keys.
- GREEN: `node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\assert-dcc-deploy-config.mjs` -> PASS, output `DCC_DEPLOY_CONFIG_PASS`.
- GREEN: `python -X utf8 -m pytest script\tests\test_dcc_sql_scripts.py -q` -> PASS, 4 tests.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileSignatureEvidenceServiceTest,DccElectronicSignatureManagementServiceTest" test` -> PASS, 29 tests.
GREEN: `node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\assert-dcc-deploy-config.mjs` -> PASS, output `DCC_DEPLOY_CONFIG_PASS`.

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 27 passed.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccElectronicSignatureManagementServiceTest,DccElectronicSignatureAuthorizationServiceTest,DccControlledFileSignatureEvidenceServiceTest" test` -> PASS, 42 tests.

GREEN: `mvn -f pom.xml -pl yudao-server -am -DskipTests package` -> PASS.

GREEN: 测试环境部署 `intruoyi-backend:20260527_dcc_admin_e2e_repair` 与 `intruoyi-frontend:20260527_dcc_admin_e2e_repair` -> PASS，`http://172.30.30.58:48081/actuator/health` 返回 `{"status":"UP"}`。

RED: `$env:DCC_ADMIN_E2E_REQUIRE_VALID_SIGNATURE='true'; node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-admin-readonly-e2e.mjs` -> FAIL, expected reason: 测试租户只有历史 `HISTORICAL_UNBOUND` 签名，`NO_VALID_SIGNATURE_EVIDENCE` 与 `SIGNATURE_EXPORT_SUMMARY_NOT_VALID` 阻塞严格导出验证。

RED: `node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-test-tenant-signature-e2e.mjs` -> FAIL, expected reason: 登录页默认租户仍为 `芋道源码`，`aoteman` 在错误租户下登录失败。

FIX: `dcc-test-tenant-signature-e2e.mjs` 显式选择 `测试租户`，再使用真实前端打开测试租户 DCC 审批任务、填写当前登录密码、点击 `审核通过` / `确认签名`，最后只用 GET 校验签名记录与导出摘要。

RED: 测试租户真实前端详情页 -> FAIL, expected reason: 测试库缺少 `dcc_external_file_review`，页面暴露 SQL 错误，无法渲染审批动作。

FIX: 在测试库应用仓库 SQL `sql/mysql/20260527_dcc_external_file_review.sql` 的 schema，验证 `information_schema.tables` 中 `dcc_external_file_review` 计数为 1。

RED: 测试租户真实前端详情页 -> FAIL, expected reason: 测试库缺少 `dcc_approval_print_template`，页面暴露 SQL 错误，无法渲染审批动作。

FIX: 在测试库应用仓库 SQL `sql/mysql/20260527_dcc_approval_print_template.sql` 的建表部分，验证 `information_schema.tables` 中 `dcc_approval_print_template` 计数为 1；未执行菜单插入，避免本次测试环境 schema 修复扩散到无关权限数据。

RED: `node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-test-tenant-signature-e2e.mjs` with file `2054545668044046013` -> FAIL, expected reason: 真实前端已生成 `signatureId=127` 且 `evidenceStatus=VALID`，但该旧文件仍含历史 `HISTORICAL_UNBOUND` 签名，导出摘要 `allRequiredEvidenceValid=false`。

GREEN: `node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-test-tenant-signature-e2e.mjs` with file `2054545668044045914` -> PASS, output `TEST_TENANT_DCC_SIGNATURE_PASS`;真实前端生成 `signatureId=128`、`evidenceStatus=VALID`、`evidenceHashShort=2505c5cf3c7a`，导出摘要 `allRequiredEvidenceValid=true`。

GREEN: `$env:DCC_ADMIN_E2E_REQUIRE_VALID_SIGNATURE='true'; node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-admin-readonly-e2e.mjs` -> PASS, output `YUDAO_ADMIN_DCC_SIGNATURE_PASS`; `芋道源码/admin` 登录后选择 `visitTenantId=122`，签名记录总数 69，首页第一条为 `signatureId=128` / `VALID`，导出摘要 `allRequiredEvidenceValid=true`，`mutatingDccRequests=0`，`failedResponses=0`。

GREEN: `git merge --ff-only codex/20260527-dcc-admin-e2e-repair` on backend `int_main` -> PASS, backend main reached `4871b0f02c`; frontend `int_main` reached `fd0d1be9` after matching fast-forward merge.

FIX: `dcc-admin-readonly-e2e.mjs` and `dcc-test-tenant-signature-e2e.mjs` now resolve the adjacent frontend repo from the current backend repo path, so post-merge E2E runs from `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` instead of the temporary worktree path.

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` on backend `int_main` -> PASS, 28 passed.

GREEN: `node --test scripts\dcc-tenant-visit-header.test.mjs` on frontend `int_main` -> PASS, 2 tests passed.

GREEN: `$env:DCC_ADMIN_E2E_REQUIRE_VALID_SIGNATURE='true'; node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-admin-readonly-e2e.mjs` from backend `int_main` -> PASS, output `YUDAO_ADMIN_DCC_SIGNATURE_PASS`; `visitTenantId=122`，签名记录总数 69，首页第一条 `signatureId=128` / `VALID`，导出摘要 `allRequiredEvidenceValid=true`，`mutatingDccRequests=0`，`failedResponses=0`。

GREEN: `git merge-base --is-ancestor f8c2bf7cbe int_main` after subsequent NAS fast-forward -> PASS, DCC backend closeout commit remains included in latest `int_main`.

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` on latest backend `int_main` -> PASS, 28 passed.

GREEN: latest backend `int_main` strict admin E2E `$env:DCC_ADMIN_E2E_REQUIRE_VALID_SIGNATURE='true'; node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-admin-readonly-e2e.mjs` -> PASS, output `YUDAO_ADMIN_DCC_SIGNATURE_PASS`; `visitTenantId=122`，签名记录总数 69，首页第一条 `signatureId=128` / `VALID`，导出摘要 `allRequiredEvidenceValid=true`，`mutatingDccRequests=0`，`failedResponses=0`。
