# 20260808-frontline-pqc-tab-description-redbox-only

## Task Goal

根据截图反馈，调整一线 PQC 的 tab 描述展示：tab 卡片只显示红框里的正式标题/描述内容，隐藏红框外的额外说明文本，不改变工序选择、PQC 填写、设备和提交链路。

## Milestones

- [x] 定位截图命中的一线 PQC tab 描述组件、样式和相邻静态合同。
- [x] 先补充任务专用 RED 静态合同，证明红框外额外说明仍会渲染。
- [x] 最小修改目标组件，让 tab 描述只显示红框内内容。
- [x] 运行 GREEN、相邻回归和格式检查，记录验证结果。
- [x] 完成收尾记录与可复用经验检查。

## Expected Verification

- 任务专用静态合同 RED -> GREEN。
- 受影响页面相邻一线 PQC 标题身份静态合同通过；相邻旧布局/方法弹框合同若失败，记录与本次 tab 描述无关的既有阻塞点。
- `pnpm ts:check` 如可运行则通过；如遇无关历史失败，记录首个阻塞点。
- `git diff --check` 通过。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接收敛目标 tab 描述渲染边界并用静态合同锁定。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/frontend-development.md#用户可见描述与内部编码隔离门禁`：截图可见文案必须定位真实 DOM/源码锚点，描述字段仅用于展示，编码和 ID 仅用于 key、编辑定位或提交身份。
- `docs/frontend-development.md#前端静态契约隔离门禁`：若旧大合同或相邻合同存在无关失败，使用任务专用最小静态合同记录 RED/GREEN，不能用无关失败冒充当前通过。
- `doc/tasks/20260808-frontline-pqc-redbox-header-hide/`：相邻历史任务已证明 PQC 红框显示需求需要用稳定 DOM 锚点约束，不得改动正式填写控件和提交链路。

## Cleanup Candidates

- doc/tasks/20260808-frontline-pqc-tab-description-redbox-only/frontend-feature-evidence.md
- doc/tasks/20260808-frontline-pqc-tab-description-redbox-only/bug-regression-evidence.md

## Closeout

- `task-closeout-cleanup` preview/apply 均通过，blocked/warnings 均为 none。
- 已删除中间 evidence 文件，保留 `task.md`、`execution-log.md` 和 `verification-report.md`。
