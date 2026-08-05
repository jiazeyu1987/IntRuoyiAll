# 生产组长工作台异常与看板页签拆分

## Task Goal

将生产组长工作台中的“日结待处理看板”和“订单异常上报”拆分为同级独立 Tab，保持现有看板统计、异常上报表单和数据加载逻辑不降级。

## Milestones

- [x] 建立任务记录并记录 BDD/TDD 验收口径。
- [ ] 定位生产组长工作台页面结构、现有 Tab 和静态契约。
- [ ] 先补充失败的静态契约，锁定“看板”和“异常”独立 Tab 行为。
- [ ] 最小修改前端页面结构，使看板和异常成为独立 Tab。
- [ ] 运行目标静态契约、相邻契约和前端类型检查。
- [ ] 完成收尾、经验沉淀、提交并推送。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/team-leader-workbench-exception-dashboard-tabs-static.spec.cjs`
- 受影响相邻静态契约（定位后记录具体命令）
- `pnpm ts:check`（如遇历史无关 blocker，按前端静态契约隔离门禁记录）
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-teamleader-exception-dashboard-tabs/frontend-feature-evidence.md`

## Current Status

in_progress

已完成脏工作区基线提交 `4009002aa`，用于隔离本任务开始前已有改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按页面信息架构把两个功能区提升为独立 Tab，不改变 API 契约。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：若全量类型检查或既有大契约先失败在无关历史问题上，必须新增任务专用最小静态契约覆盖当前行为，并记录全量回归 blocker。
- 前端截图样式块静态契约门禁：本任务来自截图反馈，但目标是结构拆分，不做无关视觉重设计；如调整 Tab 样式需用选择器级静态契约锁定。
- 技能证据文件清理前归档门禁：`frontend-feature-evidence.md` 通过 validator 后，关键结论必须复制到默认保留的验证报告或执行日志，再运行 cleanup。
