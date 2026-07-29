# eDHR 填写页红框信息隐藏

## Task Goal

修复 eDHR 执行填写页中截图红框区域仍显示的问题：非追踪填写模式下隐藏外层标题/操作工具栏、辅助标题、还差项、完成提示条和左侧待保存变更摘要；按用户补充要求保留“任务 / 批次、工序、填写人”三张切换卡，以及显示模式、填写模式、真实告警、保存草稿、提交执行和最大化入口。继续按最新截图反馈隐藏每张辅助填写卡片内部红框位置的辅助元信息，只保留字段名称、填写控件、控件单位和真实校验错误。

追加修复：最大化填写工作区后，点击左侧保存草稿或提交执行触发的签名/结果弹框不得被浏览器全屏层遮挡。

## Milestones

1. `completed`：确认页面入口、既有静态合同和截图红框对应 DOM。
2. `completed`：补充 RED 静态回归，锁定红框区域隐藏且三张切换卡保留。
3. `completed`：实施最小前端修复，不引入 fallback 或静默降级。
4. `completed`：运行目标静态合同和相邻回归验证。
5. `completed`：更新证据、收尾状态与提交记录。
6. `completed`：补充卡片内部元信息隐藏合同，移除辅助填写卡片内的可选/已填、自动映射、位置和说明文本。
7. `completed`：补充最大化弹框遮挡回归，确保保存/提交弹框挂载在全屏工作区内部。

## Expected Verification

- `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- `node --check tests/e2e/edhr-assist-fill-mode-real-flow.e2e.js`
- `pnpm ts:check`

## Applicable Gates

- 前端静态契约隔离门禁：本次使用 eDHR 填写页专用静态合同证明红框信息隐藏，不用无关全量检查替代当前需求。
- 脏工作区基线门禁：任务开始时存在既有未提交测试改动，必须单独基线提交并记录 hash。
- PowerShell 分号串联测试退出码门禁：验证命令逐条执行并记录每条结果。
- Windows 换行与脚本行为同步门禁：最大化遮挡补充在现有 eDHR 填写页静态合同中，目标断言独立覆盖全屏弹框挂载位置。
- Element Plus 全屏弹框挂载门禁：使用浏览器 fullscreen 的局部工作区内，保存/提交弹框必须挂载在 fullscreen 元素子树内并禁用 body teleport。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接收敛填写页非追踪模式 DOM 渲染边界。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Final Verification Result

- `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> PASS
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS
- `node tests/e2e/edhr-fill-workspace-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-edhr-fill-workspace-redbox-hide/frontend-feature-evidence.md` -> PASS
- `task_closeout.py --task-id 20260729-edhr-fill-workspace-redbox-hide --mode preview` -> ready
- `task_closeout.py --task-id 20260729-edhr-fill-workspace-redbox-hide --mode apply` -> applied, deleted none

## Cleanup Keep

- doc/tasks/20260729-edhr-fill-workspace-redbox-hide/frontend-feature-evidence.md
- doc/tasks/20260729-edhr-fill-workspace-redbox-hide/bug-regression-evidence.md
