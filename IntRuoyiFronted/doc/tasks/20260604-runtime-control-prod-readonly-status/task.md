# 任务：显示正式服只读状态与探针入口

## 任务目标

让运行控制台在正式服写动作继续受保护的前提下，清晰展示正式服状态卡片、只读探针结果和探针刷新入口，不能把正式服保护状态误表现为状态不可见。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-runtime-control-ops-cards-visible/task.md`
- 状态：`blocked`
- 处理：旧任务因当前需求切换已显式阻断；另一个并行旧任务 `doc/tasks/20260604-runtime-control-rollback-target-ui/task.md` 也已标记 `blocked`。本任务只修改运行控制台只读状态与探针展示、前端测试和证据。

## BDD 场景

- BDD: 正式服状态卡片可见 -> Given 后端返回正式服组件状态 / When 操作员打开运行控制台 / Then 页面显示正式服后端、前端、展厅和 OnlyOffice 状态卡片。
- BDD: 正式服探针入口可用 -> Given 操作员需要刷新探针 / When 查看探针面板 / Then 页面显示运行探针入口并展示正式服探针结果，不因为生产写动作保护而隐藏。
- BDD: 正式服写动作保护仍明确 -> Given 操作员尝试生产相关动作 / When 未输入 `PROD` 或候选不可用 / Then 页面继续阻止提交并展示阻断原因。

## Milestones

- [x] M1：收口旧任务文档并建立本任务文档。
- [x] M2：新增静态测试覆盖正式服状态与探针入口。
- [x] M3：确认前端现有状态/探针展示已由后端状态驱动，保持写动作保护。
- [x] M4：运行前端验证并记录证据。
- [x] M5：收尾预览并提交前端改动。

## Expected Verification

- RED/GREEN：`node tests/e2e/runtime-control-prod-readonly-status-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：frontend feature evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。接口错误继续按现有错误机制展示，不伪造正式服健康。
- `是否从根因和长期维护角度解决`：是。把正式服只读状态/探针展示与生产写动作保护分开表达。
- `是否存在临时补丁或绕过`：否。不新增测试专用控件，不绕过真实 API。

## 当前状态

completed

## 验证结果

- VERIFY：上一前端任务已标记 `blocked`，本任务开始。
- BDD: 正式服状态卡片可见 -> Given 后端返回正式服组件状态 / When 操作员打开运行控制台 / Then 页面显示正式服后端、前端、展厅和 OnlyOffice 状态卡片。
- BDD: 正式服探针入口可用 -> Given 操作员需要刷新探针 / When 查看探针面板 / Then 页面显示运行探针入口并展示正式服探针结果，不因为生产写动作保护而隐藏。
- BDD: 正式服写动作保护仍明确 -> Given 操作员尝试生产相关动作 / When 未输入 `PROD` 或候选不可用 / Then 页面继续阻止提交并展示阻断原因。
- RED: 后端 `RuntimeControlServiceImplTest.getOverviewShouldReadProductionStatusWhenWriteAccessIsDisabled` -> FAIL，旧后端契约导致前端只能显示正式服 `BLOCKED/access-disabled`；前端现有展示为后端数据驱动，无需生产代码修复。
- GREEN: `node tests\e2e\runtime-control-prod-readonly-status-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\runtime-control-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS。
- CHECK: `pnpm ts:check` -> FAIL，Node 默认堆约 4GB OOM，非类型错误。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-runtime-control-prod-readonly-status/frontend-feature-evidence.md` -> PASS。
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-prod-readonly-status --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。

## 剩余阻塞

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-prod-readonly-status/frontend-feature-evidence.md`
