# 生产组长工作台异常与看板页签拆分

## Task Goal

将生产组长工作台中的“日结待处理看板”和“订单异常上报”拆分为同级独立 Tab，保持现有看板统计、异常上报表单和数据加载逻辑不降级。

## Milestones

- [x] 建立任务记录并记录 BDD/TDD 验收口径。
- [x] 定位生产组长工作台页面结构、现有 Tab 和静态契约。
- [x] 先补充失败的静态契约，锁定“看板”和“异常”独立 Tab 行为。
- [x] 最小修改前端页面结构，使看板和异常成为独立 Tab。
- [x] 运行目标静态契约、相邻契约、前端类型检查和真实页面只读验收。
- [ ] 完成收尾、经验沉淀、提交并推送。

## Expected Verification

- `node tests/e2e/production-leader-function-tabs-static.spec.js`
- `node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `node tests/e2e/team-leader-workbench-static.spec.cjs`
- `node tests/e2e/production-personnel-management-static.spec.cjs`
- `node tests/e2e/production-personnel-audit-inline-static.spec.cjs`
- `pnpm ts:check`
- 真实 Playwright 只读访问 `http://127.0.0.1:8081/mes/pro/process-pool/production-leader`，逐一切换六个功能 Tab，确认看板和异常内容互斥展示，且目标写请求为 0。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-teamleader-exception-dashboard-tabs/frontend-feature-evidence.md`
- `git diff --check`

## Current Status

ready_for_closeout

实现、任务专用验证、相邻专用合同、类型检查、真实页面只读验收、evidence validator、cleanup apply 和分支端口守卫已通过，等待本任务收尾记录提交与推送。任务开始前脏工作区基线提交为 `4009002aa`；本任务实现随后被共享分支并发基线提交 `f6ea8f545` 一并收录，未改写或拆分该提交历史。

`mes-process-pool-team-leader-static.spec.js` 当前失败于并发标准列表任务引入的矛盾断言：该断言要求重置后立即查询，而正式标准列表合同要求重置后保持空条件并清空列表。目标六 Tab 合同、PQC 模块合同、PQC 标准列表合同、多维筛选渲染合同和 `pnpm ts:check` 均通过；按前端静态契约隔离门禁记录为非本任务 blocker。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按页面信息架构把两个功能区提升为独立 Tab，不改变 API 契约。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：若全量类型检查或既有大契约先失败在无关历史问题上，必须新增任务专用最小静态契约覆盖当前行为，并记录全量回归 blocker。
- 前端截图样式块静态契约门禁：本任务来自截图反馈，但目标是结构拆分，不做无关视觉重设计；如调整 Tab 样式需用选择器级静态契约锁定。
- 技能证据文件清理前归档门禁：`frontend-feature-evidence.md` 通过 validator 后，关键结论必须复制到默认保留的验证报告或执行日志，再运行 cleanup。

## Cleanup Candidates

- doc/tasks/20260805-teamleader-exception-dashboard-tabs/frontend-feature-evidence.md
- doc/tasks/20260805-teamleader-exception-dashboard-tabs/production-leader-tabs-real.e2e.cjs
- output/playwright/20260805-teamleader-exception-dashboard-tabs
