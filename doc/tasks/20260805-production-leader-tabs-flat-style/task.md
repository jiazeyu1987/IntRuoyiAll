# 20260805 生产组长模块 Tab 扁平样式

## Task Goal

将 `生产组长` 页面下的功能模块 tab 调整为与 PQC 组长模块一致的紧凑下划线样式，并让模块 tab 直接嵌入当前内容卡片顶部，避免 tab 与列表之间出现独立空白头部区域。并发功能已补入 `看板 / 异常`，本样式覆盖当前六个生产组长模块。

## Milestones

- [x] 创建任务记录并确认当前生产组长页面入口
- [x] 编写最小静态合同，锁定生产组长模块 tab 扁平样式和嵌入布局
- [x] 实现生产组长模块 tab 样式与布局调整
- [x] 运行聚焦验证并记录 RED/GREEN
- [x] 完成 frontend feature evidence 校验
- [ ] 解决并发回归、独立提交并推送
- [ ] cleanup apply 与最终完成

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-function-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\pqc-leader-module-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\mes-process-pool-team-leader-static.spec.js`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=E:\IntRuoyi; python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-production-leader-tabs-flat-style/frontend-feature-evidence.md`
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\tests\e2e\production-leader-tabs-flat-style-static.spec.js IntRuoyiFronted\tests\e2e\production-leader-function-tabs-static.spec.js IntRuoyiFronted\tests\e2e\pqc-leader-module-tabs-static.spec.js doc\tasks\20260805-production-leader-tabs-flat-style`

## Current Status

blocked

- 生产组长模块 tab 样式和布局实现已完成，聚焦静态合同、相邻模块合同、PQC tab 合同、TypeScript 检查和 evidence validator 已通过。
- `mes-process-pool-team-leader-static.spec.js` 当前失败于并发多维筛选任务将 `resetSubmissionMultiFilter` 改为同步清空、不再 `await getSubmissionList()`；该失败不属于本样式任务，不能通过修改无关行为或放宽合同绕过。
- 同一 `TeamLeaderWorkbenchPage.vue` 存在 PQC 人员、标准多维筛选、看板/异常等并发未提交改动，无法可靠区分并独立提交本任务 hunks。
- 当前 `int_main` 还包含非本任务提交 `172c55077` 且领先 `origin/int_main` 1 个提交；本任务未推送该并发提交。
- 因回归门禁和提交/推送前置未满足，未运行 cleanup apply，临时 `frontend-feature-evidence.md` 保留。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，按现有模块 tab 结构复用同一 flat 样式和内容卡片嵌入布局。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#前端截图样式块静态契约门禁`：截图样式调整必须锁定目标选择器、active bar、间距和旧结构负向断言。
- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用静态合同覆盖当前 UI 结构，避免用无关大契约证明完成。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：共享分支存在并发提交和其它任务改动时，不做宽泛提交/推送。
