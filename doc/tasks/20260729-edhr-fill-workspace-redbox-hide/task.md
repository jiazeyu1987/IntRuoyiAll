# eDHR 填写页红框信息隐藏

## Task Goal

修复 eDHR 执行填写页中截图红框区域仍显示的问题：非追踪填写模式下隐藏外层标题/操作工具栏、辅助填写顶栏、完成提示条和左侧待保存变更摘要，只保留填写所需的显示模式、填写模式、真实告警、保存草稿、提交执行和最大化入口。

## Milestones

1. `in_progress`：确认页面入口、既有静态合同和截图红框对应 DOM。
2. `pending`：补充 RED 静态回归，锁定红框区域不得显示。
3. `pending`：实施最小前端修复，不引入 fallback 或静默降级。
4. `pending`：运行目标静态合同和相邻回归验证。
5. `pending`：更新证据、收尾状态与提交记录。

## Expected Verification

- `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-static.spec.js`
- 如存在前置阻塞，记录精确缺失项和影响。

## Applicable Gates

- 前端静态契约隔离门禁：本次使用 eDHR 填写页专用静态合同证明红框信息隐藏，不用无关全量检查替代当前需求。
- 脏工作区基线门禁：任务开始时存在既有未提交测试改动，必须单独基线提交并记录 hash。
- PowerShell 分号串联测试退出码门禁：验证命令逐条执行并记录每条结果。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接收敛填写页非追踪模式 DOM 渲染边界。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
