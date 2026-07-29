# eDHR 辅助填写顶部按钮区域预留

## Task Goal

按用户截图要求调整 eDHR 填写辅助模式顶部栏：左侧 3 个切换按钮整体只占顶部栏 2/3 宽度，右侧 1/3 预留为空白操作按钮区域；不改变任务、工序、填写人切换逻辑和权限链路。

## Milestones

1. `completed`：读取任务、前端、E2E、PowerShell 编码、Git 编排规则和前端功能交付技能。
2. `pending`：创建任务文档并记录 BDD、适用经验门禁与脏工作区基线证据。
3. `pending`：新增 RED 静态合同，锁定顶部栏 2/3 + 1/3 布局。
4. `pending`：实施最小模板/CSS 调整。
5. `pending`：运行目标合同、相邻回归、类型检查和前端证据校验。
6. `pending`：完成 cleanup、经验沉淀、提交并推送。

## Expected Verification

- `node tests/e2e/edhr-assist-topbar-action-reserve-static.spec.js`
- `node tests/e2e/edhr-assist-process-switch-dialog-grid-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-edhr-assist-topbar-action-reserve/frontend-feature-evidence.md`

## Applicable Gates

- 前端样式经验：保持 IntRuoyi 蓝/中性、紧凑操作台风格；用户明确要求右侧 1/3 为空余按钮区域，因此允许保留可扩展空白但不得做装饰性大空区。
- 前端静态契约隔离门禁：本次只改顶部栏布局，使用专用静态合同锁定 2/3 与 1/3 结构，不用无关宽合同替代。
- eDHR 辅助模式当前工序 assistRows 路由门禁：只调整顶部展示布局，不改写 `assistRows`、工序切换、填写人切换、`openTask` 或批记录/FormCenter 链路。
- 脏工作区基线门禁：任务启动前已有非本任务改动，已作为独立基线提交保存，当前任务在 clean worktree 上开始。
- PowerShell 分号串联测试退出码门禁：验证命令逐条执行并记录每条退出结果。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接调整顶部栏布局结构并用静态合同锁定。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
