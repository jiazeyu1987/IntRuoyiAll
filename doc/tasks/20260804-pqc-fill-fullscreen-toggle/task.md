# 20260804 PQC填写页最大化切换

## Task Goal

PQC账号进入 `PQC填写` 页面时，页面内顶部按钮默认显示“最大化”而不是“主页”；点击后进入浏览器全屏最大化布局，按钮切换为“主页”；再次点击“主页”退出全屏并恢复原页面布局。最大化后的 PQC 填写主界面需与用户截图中的全屏操作台一致。

## Milestones

- [x] 定位 PQC填写页面入口、组件边界和现有全屏相关实现
- [x] 编写最小静态合同，先证明当前 PQC 页面仍默认显示“主页”且缺少全屏切换状态
- [x] 实现 PQC填写页默认“最大化”按钮、浏览器全屏进入/退出和按钮文案切换
- [x] 运行定向静态合同和相邻 PQC 布局合同，记录 RED/GREEN/REGRESSION 证据
- [x] 补齐技能 evidence、验证报告和收尾状态
- [x] 按真实前端路径补跑 PQC填写最大化 E2E，并记录当前环境业务数据前置缺口
- [ ] 创建本地任务自有 PQC E2E 账号、活跃订单和待检任务前置条件
- [ ] 使用 PQC E2E 账号重跑真实 Playwright 验证

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-html-alignment-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs`（当前失败在既有 `EdhrBatchRecordTabs.vue` 缺少“历史批记录”页签，非本次 PQC 最大化改动）
- `workdir=E:\IntRuoyi; node doc\tasks\20260804-pqc-fill-fullscreen-toggle\pqc-fill-fullscreen-real.e2e.cjs`（当前 BLOCKED：本地环境提示“当前没有活跃订单，PQC 不能选择订单”）
- `workdir=E:\IntRuoyi; PQC_FULLSCREEN_E2E_USERNAME=<task-owned-pqc-user>; node doc\tasks\20260804-pqc-fill-fullscreen-toggle\pqc-fill-fullscreen-real.e2e.cjs`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260804-pqc-fill-fullscreen-toggle/database-schema-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-pqc-fill-fullscreen-toggle/frontend-feature-evidence.md`

## Current Status

in_progress

Creating the task-owned local PQC account and active-order fixture requested by the user, then rerunning real Playwright E2E with that identity.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，直接在 PQC 填写工作台组件内建模最大化状态和浏览器全屏生命周期，不用隐藏主页按钮或静态文案冒充切换。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- 已读取 `docs/experience-index.md`，命中 `PQC 填写` / `pqcItemDetails` / `rawPayload.pqcPieceValues` 经验；本任务只改显示与全屏交互，不改变 PQC 检验事实、提交 payload、项目级设备/标准/方法来源。
- 已读取 `docs/frontend-development.md` 的 Element Plus 全屏弹框挂载门禁；本任务涉及浏览器 `requestFullscreen()`，若后续发现最大化状态内弹框不可见，必须确保弹框保留在 fullscreen 容器子树内且不得用 z-index 兜底。
- 已执行 `project-experience-consolidation` 检查；本次经验已由 `docs/frontend-development.md#Element Plus 全屏弹框挂载门禁` 覆盖，无需新增长期经验文档。
