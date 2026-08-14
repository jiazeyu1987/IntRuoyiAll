# 20260808-frontline-pqc-redbox-header-hide

## Task Goal

根据截图反馈，让一线/PQC 检验卡片红框内的顶部汇总标题与状态摘要不再显示，同时保留检验设备、设备编号、接收标准、检验方法、全部合格/全部不良、逐件选择等正式填写能力。

## Milestones

- [x] 定位截图命中的前端组件、样式和现有静态合同。
- [x] 先补充任务专用 RED 静态合同，证明红框顶部汇总区域仍会渲染。
- [x] 最小修改目标组件，让红框顶部汇总区域不显示且不影响正式填写控件。
- [x] 运行 GREEN、相邻回归和格式检查，记录验证结果。
- [x] 完成收尾记录与可复用经验检查。

## Expected Verification

- 任务专用静态合同 RED -> GREEN。
- 受影响页面相邻静态合同通过。
- `pnpm ts:check` 如可运行则通过；如遇无关历史失败，记录首个阻塞点。
- `git diff --check` 通过。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接调整目标 UI 渲染边界并用静态合同锁定。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/frontend-development.md#前端截图按钮统一静态契约门禁`：截图类“不显示”需求必须先用稳定 DOM/源码锚点锁定目标可见区域，再做最小显示条件或渲染调整；不得改路由、权限或提交链路。
- `docs/frontend-development.md#前端静态契约隔离门禁`：若旧大合同或相邻合同先失败，使用任务专用最小静态合同记录 RED/GREEN，不能用无关失败冒充当前通过。
- `docs/frontend-development.md#前端提交前严格验证与草稿态计算隔离门禁`：PQC 页面改动不得影响正式提交前严格校验、设备必填和逐件结果 payload。
- `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：本轮优先用静态合同证明截图目标；真实 E2E 若缺运行态、账号或入口，必须记录 BLOCKED，不能用 API-only 替代页面路径。

## Closeout

- `frontend-feature-evidence.md` 和 `bug-regression-evidence.md` 已通过对应 validator，并在 cleanup apply 中按规则删除。
- `task-closeout-cleanup` preview/apply 均无 blocked、无 warnings。
- 可复用经验检查已完成：现有 `docs/frontend-development.md#前端截图按钮统一静态契约门禁` 与相关红框 DOM 门禁已覆盖本次经验，无需新增长期经验文档。
