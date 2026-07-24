# 任务：修复运行控制台混滚版本缺少发布责任人

## 任务目标

修复运行控制台点击“混滚版本/回滚版本”时提示缺少发布责任人并禁止提交的问题。正式默认发布责任人应为 `admin`，高风险发布/回滚操作在责任矩阵未显式配置时应使用该默认责任人完成校验和审计，不得用空责任人阻断提交。

## 上一任务检查

- 上一个后端任务 `20260602-dcc-download-failure` 原状态为 `in_progress`。
- 已记录为 `blocked`：用户切换到当前运行控制台缺陷，DCC 下载链路验证和提交尚未完成。
- 本任务只修改运行控制台默认责任人相关代码、测试与任务文档，不接管 DCC 下载改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；`admin` 是用户指定的默认发布责任人，不是异常兜底。
- `是否从根因和长期维护角度解决`：是；从责任矩阵读取/初始化契约修复，保证高风险操作有明确责任人。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 默认发布责任人允许提交混滚版本 -> Given 运行控制台责任矩阵没有显式保存 `release-owner` / When 用户点击“混滚版本/回滚版本”并提交操作 / Then 后端应使用默认发布责任人 `admin` 通过责任人校验，并在操作记录中保留责任人信息。

BDD: 显式责任矩阵优先生效 -> Given 运维人员已为 `prod + rollback-app + release-owner` 保存责任人 / When 用户提交混滚版本/回滚版本 / Then 后端应使用显式责任人，不得被默认 `admin` 覆盖。

## 里程碑

- [x] M1：暂停上一未完成后端任务并建立当前任务文档。
- [x] M2：定位发布责任人校验与责任矩阵默认值。
- [x] M3：补充 RED 回归测试复现缺少发布责任人阻断。
- [x] M4：最小实现默认 `admin` 发布责任人并通过 GREEN。
- [x] M5：运行相关回归、记录 bug 证据、执行收尾清理预览并提交。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsResponsibilityServiceImplTest,RuntimeControlServiceImplTest" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-runtime-control-mixed-rollout-default-owner\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-runtime-control-mixed-rollout-default-owner --mode preview`

## Cleanup Keep

- `doc/tasks/20260602-runtime-control-mixed-rollout-default-owner/bug-regression-evidence.md`

## 当前状态

completed

## 阻塞记录

- 2026-06-03：用户切换到“恢复数据不再要求恢复演练证据”的运行控制台需求。当前任务的责任人默认值修复尚未完成，相关已修改代码保持原样不纳入新任务；恢复该任务时需继续 M2-M5 并重新核对工作区差异。
- 2026-06-03：用户要求继续当前任务，已恢复执行默认发布责任人修复。

## 完成记录

- 根因：运行控制台责任矩阵只读取显式 `owner-matrix.json`，未提供发布责任人默认行，导致 `rollback-app/release-owner` 为空时前端与后端均阻断提交。
- 修复：为发布责任人角色加入默认 `admin` 基线，覆盖 `prod/promote-prod`、`backup/promote-backup`、`prod/rollback-app`；显式非空发布责任人仍覆盖默认值，数据责任人不默认放开。
- 验证：`mvn -pl yudao-module-infra "-Dtest=RuntimeOpsResponsibilityServiceImplTest,RuntimeControlServiceImplTest" test` -> PASS，37 tests passed。
- 证据校验：`python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-runtime-control-mixed-rollout-default-owner\bug-regression-evidence.md` -> PASS。
- 收尾预览：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-runtime-control-mixed-rollout-default-owner --mode preview` -> PASS，delete/blocked/warnings 均为空。
