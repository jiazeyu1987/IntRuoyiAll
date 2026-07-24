# 任务：NAS 权限恢复真实 E2E 后端修复

## 任务目标

- 支撑 NAS 管理真实 E2E 写路径完成权限快照、身份映射、恢复预览、应用恢复和恢复后 DCC 目录权限校验。
- 修复真实 E2E 暴露的后端表结构、恢复调度和目录访问规则接口契约问题。
- 保持无 fallback：缺少启用的 `其他`、表结构、恢复任务执行器或主体映射时必须 fail fast。

## BDD 场景

- BDD: 测试租户恢复计划自动执行 -> Given 测试租户完成 NAS 转移并创建权限恢复计划 / When 后台调度运行 / Then READY 计划会在租户上下文内执行并最终进入 COMPLETED 或明确失败。
- BDD: 恢复后的 DCC 目录权限可读取 -> Given 恢复服务写入 `subjectType=USER` 的目录访问规则 / When 前端读取 `/dcc/directories/{id}/access-rules` / Then 接口返回字符串主体类型，不因数字转换报 500。
- BDD: 真实版本与 hash 可持久化 -> Given 恢复计划保存语义版本、映射版本和 `sha256:` 前缀 hash / When 写入 MySQL / Then 字段长度足够且初始化 SQL 与测试 schema 一致。
- BDD: 禁用 SID 映射可重新激活 -> Given 同一测试租户已存在 `INACTIVE` 的 NAS SID 映射 / When 用户在真实 E2E 中保存该 SID 到 DCC 主体 / Then 后端更新原映射行并重新置为 `MAPPED`，不得插入重复 SID 行。

## 里程碑

- [x] M1：用失败单测/SQL 断言复现真实 E2E 阻塞。
- [x] M2：修复 NAS ACL 恢复表结构列宽。
- [x] M3：新增租户维度恢复调度器。
- [x] M4：统一 DCC 目录访问规则 `subjectType` 为字符串契约。
- [x] M5：补齐 SQL 数据迁移，归一历史数字主体类型。
- [x] M6：完成后端目标回归与测试服部署验证。
- [x] M7：按 no-fallback 口径修正恢复调度器异常行为为 fail-fast。

## 预期验证

- GREEN: `python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py script\tests\test_dcc_directory_access_rule_subject_type_contract_sql.py -q`
- GREEN: `mvn -pl yudao-module-dcc -am '-Dtest=DccControlledFileNasTransferServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccNasPermissionSchemaTest,DccNasPermissionSnapshotControllerTest,DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreExecutionSchedulerTest,DccNasPermissionRestoreControllerTest,DccDirectoryControllerTest,DccDirectoryAdminServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- GREEN: 测试租户真实 E2E `PASS: test-write taskId=42, restoreId=12, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`
- GREEN: 测试租户身份映射真实 E2E `PASS: test-mapping taskId=39, restoreId=10, directories=2, rules=47, unmapped=1, savedMappings=1, blockers=0`
- GREEN: 测试租户 blocker 真实 E2E `PASS: test-blocker taskId=41, unmapped=0, blockers=1`
- GREEN: 芋道源码/admin 只读 E2E `PASS: admin-readonly baseUrl=http://172.30.30.58:8081`

## Subagent-Driven Review

- Pauli：前端只读复核，结论 conditional go；指出 mapping fixture 直接修改共享 identity mapping 的污染风险。主 reviewer 已修复为克隆当前任务 descriptor/ACE 并复验通过。
- Archimedes：后端只读复核，结论 conditional go；确认 `INACTIVE` SID 重新激活修复逻辑自洽，release 阻塞为芋道源码/admin 缺少启用的 `其他`。

## 当前状态

- 状态：completed。
- 后端代码、测试租户真实写路径、身份映射保存分支、blocker/禁用应用恢复分支、fixture 非污染复验与芋道源码/admin 只读验证均已通过。
- 阻塞项：无。此前芋道源码/admin 缺少启用的 DCC 类别 `其他`，已由 `doc/tasks/20260527-dcc-other-category-runtime-apply/` 在允许的数据补齐任务中解除。

## Cleanup Keep

- `doc/tasks/20260527-nas-permission-real-e2e-suite/qa-test-suite-evidence.md`
- `doc/tasks/20260527-nas-permission-real-e2e-suite/verification-report.md`
