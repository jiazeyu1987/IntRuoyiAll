# 任务：运行控制后端恢复集候选契约

## 任务目标

补全运行控制后端 API 契约：恢复候选必须基于完整恢复集，动作请求支持 `selectedRecoverySetCandidateId`，回滚候选必须展示并校验显式兼容性证据，旧备份或缺少证据的发布包必须不可用。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260604-runtime-control-rollback-target-backend/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改运行控制后端 DTO、候选解析、动作执行参数和相关测试。

## BDD 场景

- BDD: 后端屏蔽不完整恢复集 -> Given 备份 manifest 缺少 `recoverySet` 或必需组件 / When 查询恢复候选 / Then 候选状态为 `BLOCKED` 且不可执行。
- BDD: 后端暴露完整恢复集字段 -> Given 备份 manifest 包含完整 `recoverySet` / When 查询恢复候选 / Then 返回恢复集 ID、状态、程序版本、Redis 策略、配置清单、manifest hash 和组件摘要。
- BDD: 后端恢复动作使用恢复集候选 -> Given 操作员提交 `restore-data` 和 `selectedRecoverySetCandidateId` / When 后端校验通过 / Then 脚本参数投射恢复集 ID、hash、imageTag 和 Redis 策略。
- BDD: 后端屏蔽缺少兼容性证据的回滚包 -> Given 发布包没有 `rollback-compatibility.json` 或 `status != COMPATIBLE` / When 查询回滚候选或执行回滚 / Then 候选不可用且动作被阻断。

## Milestones

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：新增 RED 后端契约测试。
- [x] M3：实现 DTO、候选解析和动作参数。
- [x] M4：运行后端目标测试与 evidence 校验。
- [x] M5：收尾预览并提交后端改动。
- [x] M6：补充本机 `芋道源码/admin` 只读真实 E2E 复核。

## Expected Verification

- RED/GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeControlHighRiskActionContractTest" test`
- GREEN：backend API evidence validator。
- GREEN：`git diff --check`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。旧 manifest、缺少恢复集、缺少兼容性证据或字段不完整时直接阻塞。
- `是否从根因和长期维护角度解决`：是。将恢复集和兼容性证据提升为后端候选与动作契约。
- `是否存在临时补丁或绕过`：否。不绕过已有运行控制门禁。

## 当前状态

completed

## 验证结果

- RED：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlCanonicalContractTest,RuntimeRestoreCandidateServiceImplTest,RuntimeRollbackCandidateServiceImplTest" test` -> FAIL，缺少 `selectedRecoverySetCandidateId`、恢复集字段与回滚兼容性字段。
- GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlCanonicalContractTest,RuntimeRestoreCandidateServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeControlServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeOpsGuideServiceImplTest,RuntimeOpsResponsibilityServiceImplTest" test` -> PASS，84 tests。
- GREEN：`python -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_backup_ops_manifest_tooling.py script/tests/test_mark_tested_current_release_tooling.py script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py -q` -> PASS，122 tests。
- GREEN：backend API evidence validator -> PASS。
- GREEN：UTF-8 readback -> PASS。
- GREEN：`git diff --check` -> PASS。
- GREEN：task-closeout-cleanup preview -> PASS，keep only，无 delete/blocked/warnings。
- GREEN：本机只读真实 E2E `node tests/e2e/runtime-control-yudao-admin-readonly.e2e.js` -> PASS，回滚候选 22 条、恢复集候选 8 条，`YUDAO_ADMIN_READONLY_PASS`，运行控制接口无非 GET 写请求。

## Blockers

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-recovery-set-contract/backend-api-evidence.md`
