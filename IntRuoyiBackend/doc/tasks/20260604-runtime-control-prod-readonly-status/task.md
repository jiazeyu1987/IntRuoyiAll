# 任务：开放正式服只读状态与探针检查

## 任务目标

让运行控制台在正式服写动作继续受保护的前提下，可以查看正式服当前状态，并允许探针对正式服目标执行只读健康检查。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260604-runtime-control-rollback-target-backend/task.md`
- 状态：`blocked`
- 处理：旧任务因当前需求切换已显式阻断；本任务只修改运行控制台只读状态、探针契约、相关测试和证据。

## BDD 场景

- BDD: 正式服写动作继续受保护 -> Given 正式服发布、重启、回滚或恢复缺少 `PROD` 确认或有效候选 / When 操作员提交动作 / Then 后端继续阻断动作且不执行远端脚本。
- BDD: 正式服状态可见 -> Given `prod.accessEnabled=false` 用于保护写动作 / When 运行控制台加载概览 / Then 后端仍返回正式服组件状态卡片，并执行只读状态脚本。
- BDD: 正式服探针可检查 -> Given 探针配置包含正式服目标 / When 操作员运行探针 / Then 后端对正式服目标执行只读 HTTP 探针并返回真实 PASS/NO_GO/BLOCKED 结果。

## Milestones

- [x] M1：收口旧任务文档并建立本任务文档。
- [x] M2：新增 RED 测试覆盖 `prod.accessEnabled=false` 下状态概览与探针仍可读。
- [x] M3：实现只读状态/探针与写动作保护解耦。
- [x] M4：运行后端验证并记录 RED/GREEN 证据。
- [x] M5：收尾预览并提交后端改动。

## Expected Verification

- RED/GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeProbeServiceImplTest,RuntimeControlHighRiskActionContractTest" test`
- GREEN：backend API evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。状态脚本或探针失败必须以真实异常/失败状态呈现，不返回默认成功。
- `是否从根因和长期维护角度解决`：是。将只读状态/探针权限与正式服写动作门禁拆开，避免用 `accessEnabled` 同时控制读写。
- `是否存在临时补丁或绕过`：否。不绕过 `PROD` 确认、候选校验或责任人门禁。

## 当前状态

completed

## 验证结果

- VERIFY：上一后端任务已标记 `blocked`，本任务开始。
- BDD: 正式服状态可见 -> Given `prod.accessEnabled=false` 用于保护写动作 / When 运行控制台加载概览 / Then 后端仍返回正式服组件状态卡片，并执行只读状态脚本。
- BDD: 正式服探针可检查 -> Given 探针配置包含正式服目标 / When 操作员运行探针 / Then 后端对正式服目标执行只读 HTTP 探针并返回真实 PASS/NO_GO/BLOCKED 结果。
- BDD: 正式服写动作继续受保护 -> Given 正式服发布、重启、回滚或恢复缺少 `PROD` 确认或有效候选 / When 操作员提交动作 / Then 后端继续阻断动作且不执行远端脚本。
- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeProbeServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> FAIL，`getOverviewShouldReadProductionStatusWhenWriteAccessIsDisabled` 期望正式服状态 `running`，旧实现返回 `BLOCKED`。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeProbeServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> PASS，47 tests passed。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260604-runtime-control-prod-readonly-status/backend-api-evidence.md` -> PASS。
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-prod-readonly-status --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。

## Blockers

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-prod-readonly-status/backend-api-evidence.md`
