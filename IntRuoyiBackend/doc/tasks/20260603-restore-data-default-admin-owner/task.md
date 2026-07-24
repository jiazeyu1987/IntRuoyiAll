# 任务：恢复数据默认责任人使用 admin

## 任务目标

解决运行控制台点击“恢复数据”时遇到的责任人阻碍：所有默认责任人使用 `admin`；恢复数据缺少显式 `data-owner` 时也应使用默认 `admin` 通过责任人门禁。演练报告和现场快照门禁已在 `20260603-restore-data-without-rehearsal-gate` 任务中取消，本任务不重新引入。

## 上一任务检查

- 上一个后端任务 `20260603-restore-data-without-rehearsal-gate` 已标记 `Completed`，提交 `2d04fc982c`。
- 当前工作区存在 DCC、发布脚本和其他任务目录改动，本任务只修改运行控制台责任人默认值相关代码、测试与任务证据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：是。用户明确要求默认责任人都为 `admin`；本任务只在责任矩阵未显式配置时提供 `admin` 默认值，并保留显式配置优先。
- `是否从根因和长期维护角度解决`：是。通过责任矩阵有效值计算统一默认责任人，而不是前端绕过或接口跳过责任人校验。
- `是否存在临时补丁或绕过`：否。不跳过责任人门禁，只补齐默认责任人。

## BDD 场景

BDD: 恢复数据默认数据责任人为 admin -> Given 责任矩阵没有显式保存 `prod + restore-data + data-owner` / When 用户提交恢复数据 / Then 后端应使用默认 `admin` 通过责任人门禁，并继续校验恢复候选。

BDD: 显式数据责任人优先生效 -> Given 运维人员保存了 `prod + restore-data + data-owner` 的显式责任人 / When 查询责任矩阵或提交恢复数据 / Then 显式责任人应覆盖默认 `admin`。

BDD: 演练和快照仍不阻断恢复候选 -> Given 恢复候选缺少演练报告和现场快照 / When 用户打开恢复数据候选 / Then 候选不应因为这两项被阻断。

## 里程碑

- [x] M1：建立任务文档并确认上一任务已完成。
- [ ] M2：补充 RED 测试，证明恢复数据缺默认 data-owner 时仍被责任人门禁阻断。
- [ ] M3：最小实现 `restore-data/data-owner` 默认 admin。
- [ ] M4：运行责任人、恢复候选和运行控制台相关回归。
- [ ] M5：执行收尾清理预览并提交本任务改动。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsResponsibilityServiceImplTest,RuntimeControlServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeBackupDrillServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260603-restore-data-default-admin-owner\backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-restore-data-default-admin-owner --mode preview`

## 当前状态

in_progress

## Cleanup Keep

- doc/tasks/20260603-restore-data-default-admin-owner/backend-api-evidence.md
