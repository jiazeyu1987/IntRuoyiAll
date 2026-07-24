# 任务：修复 DCC 电子签名芋道源码/admin 验证失败

## 任务目标

- 修复 DCC 电子签名在测试环境和生产验证环境中的部署配置、数据库 schema 与测试租户数据准备问题。
- 所有写入和数据准备仅发生在测试租户；最终使用 `芋道源码/admin` 走真实前端路径做只读验证。
- 不使用 mock、静默跳过、降级、默认成功或绕过前端入口。

## Worktree

- 后端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-dcc-admin-e2e-repair\ruoyi-vue-pro`
- 前端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-dcc-admin-e2e-repair\yudao-ui-admin-vue3`
- 分支：`codex/20260527-dcc-admin-e2e-repair`

## 前置任务检查

- 已检查当前后端 worktree 的最近任务文档。
- `doc/tasks/20260527-runtime-control-real-data-e2e/task.md` 已完成。
- `doc/tasks/20260527-edhr-int-main-integration/task.md` 仍有前端融合 M4 未完成；该任务属于 eDHR 集成 worktree，和本次 DCC 电子签名修复无共享写入范围。本次按用户明确要求继续 DCC 修复，不修改该旧任务文档和代码。

## BDD 场景

- BDD: 测试环境 DCC 后端可启动 -> Given 部署模板包含 DCC 签名证据 HMAC secret 和 key version When 测试环境启动后端 Then `/actuator/health` 必须返回健康且日志中不得出现 signature evidence configuration missing。
- BDD: 电子签名 schema 可供新接口读取 -> Given 目标数据库应用 DCC 电子签名加固迁移 When 查询授权、签名证据和策略表字段 Then 授权接口必须包含 authorizationState/locked 等字段，签名表必须包含 evidence_hash/evidence_status 等字段。
- BDD: 测试租户签名证据可验证 -> Given 仅在测试租户准备或生成 DCC 签名记录 When 查询签名记录和导出证据 Then 至少存在一条 `VALID` 证据记录且导出校验为 `VALID`。
- BDD: 芋道源码/admin 只读验证通过 -> Given 使用 `芋道源码/admin` 登录生产前端 When 通过真实页面查看 DCC 电子签名授权、签名记录和证据状态 Then 页面/API 数据符合新契约且验证过程不发送 DCC 写请求。

## 里程碑

- [x] M1：记录现有失败、补齐任务文档和 RED 检查。
- [x] M2：修复部署配置并完成本地 GREEN 验证。
- [x] M3：在测试环境应用 schema/config，恢复后端健康。
- [x] M4：只在测试租户完成数据准备与测试租户 E2E。
- [x] M5：推广到 `芋道源码/admin` 验证环境后做只读 E2E，直到通过。
- [x] M6：整理证据、提交当前任务直接改动。

## 预期验证

- `node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\assert-dcc-deploy-config.mjs`
- `python -X utf8 -m pytest script\tests\test_dcc_sql_scripts.py -q`
- `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileSignatureEvidenceServiceTest,DccElectronicSignatureManagementServiceTest" test`
- 测试环境：`http://172.30.30.58:48081/actuator/health` 返回健康。
- 测试租户真实前端：Playwright 输出 `TEST_TENANT_DCC_SIGNATURE_PASS`，并断言目标文件签名导出摘要 `allRequiredEvidenceValid=true`。
- 芋道源码/admin：Playwright 只读 E2E 输出 `YUDAO_ADMIN_DCC_SIGNATURE_PASS`，并断言无 DCC 非 GET 请求。

## 当前状态

- 状态：completed
- 当前阶段：M6
- 当前结论：测试环境部署配置、DCC 电子签名 schema、测试租户真实签名数据与 `芋道源码/admin` 严格只读验证均已通过。测试租户通过真实前端审批任务生成 `signatureId=128`，证据状态 `VALID`，导出摘要 `allRequiredEvidenceValid=true`；随后 `芋道源码/admin` 访问测试租户 `visitTenantId=122` 的只读 E2E 输出 `YUDAO_ADMIN_DCC_SIGNATURE_PASS`，且 DCC 写请求数为 0。
- 收尾说明：测试环境额外补齐缺失 schema 表 `dcc_external_file_review` 和 `dcc_approval_print_template`；未修改 `芋道源码` 租户数据，最终 admin 验证仅做只读访问。
- `int_main` 融合：后端 `int_main` 已融合 `codex/20260527-dcc-admin-e2e-repair` 至 `4871b0f02c`；前端 `int_main` 已融合至 `fd0d1be9`。合入后后端 E2E 脚本改为从当前后端仓库解析相邻 `yudao-ui-admin-vue3`，不再依赖临时 worktree 固定路径。
- `int_main` 复验：从 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 运行 `芋道源码/admin` 严格只读 E2E，通过并输出 `YUDAO_ADMIN_DCC_SIGNATURE_PASS`，`mutatingDccRequests=0`。
- 最终主干复验：后续 `int_main` 又快进融合 NAS 任务后，DCC 收口提交仍为当前主干祖先；已在最新 `int_main` 上重新运行 `芋道源码/admin` 严格只读 E2E，结果仍为 `YUDAO_ADMIN_DCC_SIGNATURE_PASS`，`mutatingDccRequests=0`。

## Cleanup Keep

- doc/tasks/20260527-dcc-admin-e2e-repair/backend-api-evidence.md
- doc/tasks/20260527-dcc-admin-e2e-repair/bug-regression-evidence.md
- doc/tasks/20260527-dcc-admin-e2e-repair/database-schema-evidence.md
- doc/tasks/20260527-dcc-admin-e2e-repair/scripts/assert-dcc-deploy-config.mjs
- doc/tasks/20260527-dcc-admin-e2e-repair/scripts/dcc-admin-readonly-e2e.mjs
- doc/tasks/20260527-dcc-admin-e2e-repair/scripts/dcc-test-tenant-signature-e2e.mjs
