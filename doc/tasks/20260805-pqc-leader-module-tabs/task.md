# 20260805 PQC 组长功能模块页签

## Task Goal

将 `PQC组长` 页面内部按功能模块拆成独立 tab，当前先落地 `PQC管理` 与 `看板`，避免把 PQC 组长的功能模块继续堆叠在同一页面纵向区块中。

## Milestones

- [x] 创建任务记录并确认现有 PQC 组长页面入口
- [x] 编写最小静态合同，锁定 `PQC管理` 与 `看板` 功能模块 tab
- [x] 实现 PQC 组长页面内部功能模块 tab，生产组长页面不受影响
- [x] 运行定向验证并记录 RED/GREEN/REGRESSION
- [x] 更新验证报告与收尾状态

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\pqc-leader-module-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\PqcLeaderWorkbenchPage.vue IntRuoyiFronted\src\views\mes\pro\processpool\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\tests\e2e\pqc-leader-module-tabs-static.spec.js doc\tasks\20260805-pqc-leader-module-tabs`

## Current Status

ready_for_closeout

- 已完成 PQC 组长页面内部 `PQC管理` / `看板` 功能模块 tab、定向验证和验证报告。
- cleanup、提交和推送仍需在共享工作区既有非本任务脏改动门禁下单独处理，避免混入并行任务产物。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，按页面内部正式功能模块拆分展示，不用隐藏区块或占位按钮冒充模块入口。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#前端角色内容页签拆分口径门禁`：区分主导航页签、角色切换页签与页面内部功能模块页签；本任务只改 PQC 组长页面内部模块。
- `docs/frontend-development.md#前端静态契约隔离门禁`：新增任务专用静态合同，避免用无关大契约证明当前 UI 结构。
