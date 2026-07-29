# eDHR 辅助填写卡片密度调整

## Task Goal

按用户截图要求调整 eDHR 填写辅助模式字段卡片：每个卡片内输入框高度增加 50%，整个字段卡片高度缩减到当前约 80%，在不改变字段值、校验、保存、提交和工序/填写人切换逻辑的前提下提升可填写区域可见性。

## Milestones

1. `pending`：完成任务启动、脏工作区基线和适用经验门禁记录。
2. `pending`：定位辅助填写卡片和输入控件样式，补充 RED 静态合同。
3. `pending`：实施最小 CSS/模板调整，不引入 fallback 或行为降级。
4. `pending`：运行目标静态合同、相邻回归和类型检查。
5. `pending`：补齐前端证据、cleanup、提交并推送。

## Expected Verification

- `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-edhr-assist-card-density/frontend-feature-evidence.md`

## Applicable Gates

- 前端静态契约隔离门禁：本次使用 eDHR 辅助填写卡片密度专用静态合同证明输入框高度与卡片高度，不用无关全量检查替代当前需求。
- eDHR 辅助模式当前工序 assistRows 路由门禁：只调整运行页展示层尺寸，不改写 `assistRows`、`ASSIST_GRID_U` rowKey、原始行列、工序切换或 `openTask` 链路。
- Element Plus 选择框显示门禁：输入框、数字输入、日期选择和选择框必须在卡片内显式收敛宽高，不能因增高后溢出、挤压单位或截断占位文本。
- 脏工作区基线门禁：任务启动时已有未提交源码、测试和任务文档，必须先独立基线提交，再在基线之上实施本次改动。
- PowerShell 分号串联测试退出码门禁：验证命令逐条执行并记录每条结果。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接收敛辅助填写卡片和控件的布局尺寸契约。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
