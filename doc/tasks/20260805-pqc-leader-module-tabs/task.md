# 20260805 PQC 组长功能模块页签

## Task Goal

将 `PQC组长` 页面内部按功能模块拆成独立 tab，当前先落地 `PQC管理` 与 `看板`，并按截图反馈把该组 tab 改成文控权限页的下划线 tab 样式；tab 下方直接衔接筛选/列表，不再保留独立空白标题卡片。

## Milestones

- [x] 创建任务记录并确认现有 PQC 组长页面入口
- [x] 编写最小静态合同，锁定 `PQC管理` 与 `看板` 功能模块 tab
- [x] 实现 PQC 组长页面内部功能模块 tab，生产组长页面不受影响
- [x] 运行定向验证并记录 RED/GREEN/REGRESSION
- [x] 更新验证报告与收尾状态
- [x] 按截图调整 PQC 模块 tab 样式与列表贴合布局
- [x] 复跑聚焦静态合同、相邻合同和类型检查

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\pqc-leader-module-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\PqcLeaderWorkbenchPage.vue IntRuoyiFronted\src\views\mes\pro\processpool\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\tests\e2e\pqc-leader-module-tabs-static.spec.js doc\tasks\20260805-pqc-leader-module-tabs`

## Current Status

blocked

- 已根据截图追加样式调整：红框 PQC 模块 tab 对齐黄框 DCC 下划线 tab 视觉，且 `PQC管理` tab 下直接进入筛选/列表。
- 本轮只改 PQC 组长 tab 展示外壳与静态合同，不改 API、请求参数、后端、动态菜单或数据来源。
- frontend evidence validator、cleanup preview/apply 已完成，仅删除本任务临时 `frontend-feature-evidence.md`。
- Git closeout 仍受共享分支并行提交影响；`TeamLeaderWorkbenchPage.vue` 的源码变更已被最近提交 `c17cbef6f feat: split production leader workbench into module tabs` 一并纳入 HEAD，当前不执行提交/推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，按页面内部正式功能模块拆分展示，不用隐藏区块或占位按钮冒充模块入口。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#前端角色内容页签拆分口径门禁`：区分主导航页签、角色切换页签与页面内部功能模块页签；本任务只改 PQC 组长页面内部模块。
- `docs/frontend-development.md#前端静态契约隔离门禁`：新增任务专用静态合同，避免用无关大契约证明当前 UI 结构。
- `docs/frontend-development.md#前端截图样式块静态契约门禁`：截图样式调整必须锁定目标选择器、active bar、间距和旧结构负向断言。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：发现最近基线提交包含当前任务源码/测试和其它任务文件后，记录异常并停止宽泛提交/推送。
