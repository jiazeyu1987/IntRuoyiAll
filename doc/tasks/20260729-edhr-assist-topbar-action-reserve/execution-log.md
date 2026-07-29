# eDHR 辅助填写顶部按钮区域预留执行日志

## Intent

- User request: 顶部 3 个按钮只占据 2/3 的位置，右边 1/3 空余出来做其他按钮区域。
- Scope: eDHR 填写辅助模式顶部栏布局；不改变切换弹窗、数据加载、保存、提交、权限和批记录/FormCenter 链路。

## Preflight

- Read `docs/task-closeout-rules.md`.
- Read `docs/frontend-development.md`.
- Read `docs/e2e-rules.md`.
- Read `docs/powershell-encoding.md`.
- Read `docs/powershell-memory.md`.
- Read `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`.
- Read `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`.
- Read `docs/experience-index.md`.
- Read matched frontend style evidence `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` and checked referenced toolbar/components style.
- Baseline evidence before current task: `7fb94427 chore: preserve pre-task dirty worktree baseline`; residual existing `ExecutionPage.vue` dirty change preserved as `10dd6c25 chore: preserve residual execution page baseline`.

## BDD

- `BDD: assist topbar leaves action reserve -> Given` eDHR 填写页处于填写辅助模式且顶部栏展示任务/批次、工序、填写人 3 个切换按钮；`When` 页面渲染顶部栏；`Then` 3 个切换按钮位于左侧 2/3 宽度的上下文区域，右侧 1/3 保留为空白操作按钮区域，且 3 个切换按钮仍可点击打开各自切换弹窗。

## TDD Evidence

- `RED: node tests/e2e/edhr-assist-topbar-action-reserve-static.spec.js -> FAIL, expected reason: 顶部 3 个切换按钮尚未包在左侧 2/3 上下文区域。`
- `GREEN: pending`

## Milestones

- `completed`：规则、技能和适用样式经验已读取。
- `completed`：任务文档已创建，BDD 和设计约束已记录。
- `completed`：新增 RED 静态合同并确认当前实现失败。
- `pending`：实施布局调整。
- `pending`：验证与收尾。

## Blockers

- None.
