# 请求与命令记录

## 2026-07-09 任务：20260709-global-empty-search-reset

### 用户需求

- 所有类似的搜索按钮，如果点击搜索时输入栏为空，就执行对应重置逻辑。

### 已执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery` 与 `frontend-contract.md` -> PASS。
- 创建 `doc/tasks/20260709-global-empty-search-reset/`，记录 BDD、设计约束、经验门禁和前端证据 -> PASS。
- RED: `node tests/e2e/empty-search-reset-static.spec.js` -> FAIL，页面级 `handleQuery` 和通用搜索组件缺少空输入重置契约。
- 实现结果：新增 `src/utils/search.ts`；通用搜索组件空输入搜索执行 `reset()`；具备 `handleQuery` / `resetQuery` 配对的列表页空输入搜索执行 `resetQuery()`，重置刷新使用 `handleQuery(true)` 避免递归。
- 修复类型检查暴露的问题：4 个商城弹窗表格选择组件补齐 `queryFormRef`；系统菜单页模板显式调用 `handleQuery()`。
- GREEN: `node tests/e2e/empty-search-reset-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/table-quick-filter-static.spec.js` -> PASS。
- GREEN: `node node_modules/eslint/bin/eslint.js src/utils/search.ts src/components/Search/src/Search.vue tests/e2e/empty-search-reset-static.spec.js tests/e2e/table-quick-filter-static.spec.js src/views/mall/product/spu/components/SpuTableSelect.vue src/views/mall/promotion/combination/components/CombinationTableSelect.vue src/views/mall/promotion/point/components/PointTableSelect.vue src/views/mall/promotion/seckill/components/SeckillTableSelect.vue src/views/system/menu/index.vue` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。
- GREEN: 前端 evidence 校验与 task-closeout preview/apply -> PASS，已清理临时 evidence 文件。
- Git commit -> BLOCKED，当前前端仓存在大量多任务混合脏改，且本任务批量触达的列表文件与既有未提交改动存在文件级重叠；为避免混入非本任务 hunk，未创建提交。

## 2026-07-09 任务：20260709-workorder-remove-key-column-copy-buttons

### 用户需求

- 删除生产工单列表中工单编号、产品编码、产品名称、规格型号、计划数量每列右侧的复制按钮。

### 已执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery` 技能与前端契约。
- 定位 `src/views/mes/pro/workorder/index.vue` 中生产工单关键列复制按钮、剪贴板 handler 和相关静态契约。
- 创建任务目录 `doc/tasks/20260709-workorder-remove-key-column-copy-buttons/`，写入 `task.md`、`execution-log.md`、`frontend-feature-evidence.md`。
- 新增静态契约 `tests/e2e/workorder-remove-key-column-copy-buttons-static.spec.js`。
- RED: `node tests/e2e/workorder-remove-key-column-copy-buttons-static.spec.js` -> FAIL，工单编号列仍渲染 `work-order-key-copy` 复制按钮。
- 修复：移除工单编号、产品编码、产品名称、规格型号、计划数量列的复制按钮；移除 `handleCopyKeyField`、`handleCopyWorkOrderCode`、剪贴板写入逻辑和 `.work-order-key-copy` 样式。
- GREEN: `node tests/e2e/workorder-remove-key-column-copy-buttons-static.spec.js`、`node tests/e2e/workorder-key-columns-static.spec.js`、`node tests/e2e/workorder-code-copy-button-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-workorder-remove-key-column-copy-buttons/frontend-feature-evidence.md` -> PASS。
- Git commit -> BLOCKED，当前前端仓存在大量既有脏改；为避免混入非本任务 hunk，未提交。

## 2026-07-09 任务：20260709-schedule-order-admission-hide-purple-controls

### 用户需求

- 截图中紫框内的待同步差异弹窗控件不显示。

### 已执行命令

- 读取 `docs/powershell-memory.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery` 技能与前端契约。
- 定位 `src/views/mes/pro/scheduleorder/index.vue` 中“待同步差异”弹窗的额外筛选区和动作区。
- 创建任务目录 `doc/tasks/20260709-schedule-order-admission-hide-purple-controls/`，写入 `task.md`、`execution-log.md`、`frontend-feature-evidence.md`。
- 新增静态契约 `tests/e2e/mes-pro-schedule-order-admission-hide-purple-controls-static.spec.js`。
- RED: `node tests/e2e/mes-pro-schedule-order-admission-hide-purple-controls-static.spec.js` -> FAIL，额外筛选区仍显示 `工单编码`。
- 修复：移除待同步差异弹窗 `extra-filters` 插槽，隐藏工单编码、产品编号、入池状态、阻断原因；移除动作区独立“搜索”按钮；保留重置、汇总标签和“选中工单加入排产工单池”。
- GREEN: `node tests/e2e/mes-pro-schedule-order-admission-hide-purple-controls-static.spec.js`、`node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js`、`node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-admission-hide-purple-controls/frontend-feature-evidence.md` -> PASS。
- `task-closeout-cleanup --task-id 20260709-schedule-order-admission-hide-purple-controls --mode preview` -> PASS，无阻塞；前端 evidence 文件列入 Cleanup Keep。
- Git commit -> BLOCKED，当前前端仓存在大量既有脏改，且本轮触达的页面与既有静态契约文件在任务开始前已非干净状态；为避免混入非本任务 hunk，未提交。

## 2026-07-02 任务：20260702-schedule-order-hide-workorder-code-column

### 用户需求

- `还是显示了`，截图显示排产工单主列表左侧仍展示“工单编码/工单编号”列，需要主列表隐藏该列。

### 已执行命令

- `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> RED FAIL，复现主列表仍包含 `label="工单编码"`。
- `node --check tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS。
- `node --check tests/e2e/mes-schedule-order-workorder-link-static.spec.js` -> PASS。
- `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS。
- `node tests/e2e/mes-schedule-order-workorder-link-static.spec.js` -> PASS。
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260702-schedule-order-hide-workorder-code-column/frontend-feature-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-schedule-order-hide-workorder-code-column --mode preview` -> PASS，无删除项、无阻塞。

## 2026-06-29 任务：20260629-dcc-browser-cache-write-failure

### 用户需求

- 测试服务器的文件查阅提示 `DCC 受控浏览本地缓存写入失败，请检查浏览器本地存储权限。`

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\{SKILL.md,references\bug-contract.md}`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\{SKILL.md,references\frontend-contract.md}`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\browser\{index.vue,state-cache.ts}`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-browser-remember-state-cache-static.spec.js`
- `rg -n "DCC 受控浏览本地缓存写入失败|浏览器本地存储权限|writeDccBrowserMetadataCache|directoryChildrenByParentKey" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- `apply_patch` -> 新建 `doc/tasks/20260629-dcc-browser-cache-write-failure/{task.md,execution-log.md,bug-regression-evidence.md,frontend-feature-evidence.md}`，新增 `tests/e2e/dcc-browser-cache-write-failure-static.spec.js`，并修改 DCC 浏览页缓存实现与现有 remember-state 静态合同。
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-browser-cache-write-failure-static.spec.js` -> RED FAIL；修复后 -> PASS。
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-browser-remember-state-cache-static.spec.js` -> PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-dcc-browser-cache-write-failure\bug-regression-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-dcc-browser-cache-write-failure\frontend-feature-evidence.md` -> PASS。
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tsconfig.relaxed.json` -> FAIL，默认堆上限 OOM。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tsconfig.relaxed.json` -> FAIL，既有无关错误位于 `src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue:158,305`。

## 2026-06-29 任务：20260629-scheduler-workbench-copy-compress

### 用户需求

- 将排产工作台设置区截图里的所有文本精简描述为不超过 4 个字，并直接替换页面文案。

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\clear-frontend-copy\SKILL.md`
- `rg -n "排产设置|班次小时设置|ERP工单同步时间|冒烟测试" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\scheduler-workbench\index.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-{shift-hours,policy-settings,smoke-toggle,density-layout}-static.spec.js`
- `apply_patch` -> 创建 `doc/tasks/20260629-scheduler-workbench-copy-compress/{task.md,execution-log.md}` 并更新工作台页面、静态契约和本段命令记录。

## 2026-06-28 任务：20260628-mes-scheduler-workbench-policy-label-overlap

### 用户需求

- 排产员工作台的策略设置区文字发生重叠，需要修复排版。

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\AGENTS.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\scheduler-workbench\index.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-policy-settings-static.spec.js`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-density-layout-static.spec.js`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-mes-feedback-confirm-batch-cross-page\task.md`
- `apply_patch` -> 创建 `doc/tasks/20260628-mes-scheduler-workbench-policy-label-overlap/{task.md,execution-log.md}`。
- `apply_patch` -> 新增 `tests/e2e/mes-scheduler-workbench-policy-label-layout-static.spec.js`。
- `node tests/e2e/mes-scheduler-workbench-policy-label-layout-static.spec.js` -> RED FAIL，策略区缺少独立标签布局和不可折行约束。
- `apply_patch` -> 更新 `src/views/mes/pro/scheduler-workbench/index.vue`，为策略表单项补齐统一两列布局、标签不可折行和保护规则复选框换行样式。
- `node tests/e2e/mes-scheduler-workbench-policy-label-layout-static.spec.js` -> PASS
- `node tests/e2e/mes-scheduler-workbench-policy-settings-static.spec.js` -> PASS
- `node tests/e2e/mes-scheduler-workbench-density-layout-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/scheduler-workbench --target-text 排产设置` -> PASS
- `只读 Playwright 页面复验` -> PASS，输出截图 `output/playwright/20260628-mes-scheduler-workbench-policy-label-overlap/policy-label-layout-after-fix.png`
- `apply_patch` -> 回写 `bug-regression-evidence.md`、`frontend-feature-evidence.md`、任务状态和本段命令记录。

## 2026-06-28 任务：20260628-route-use-hourly-capacity-integer

### 用户需求

- 将工艺路线排产用途配置表中截图所示的 `产能(h)` 改成整数。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\route-use\RouteUsePage.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-route-use-config-display-static.spec.js`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260624-route-use-config-copy-cleanup\{task.md,execution-log.md}`
- `apply_patch` -> 创建 `doc/tasks/20260628-route-use-hourly-capacity-integer/{task.md,execution-log.md,frontend-feature-evidence.md}` 并记录本轮请求。
- `apply_patch` -> 更新 `tests/e2e/mes-route-use-config-display-static.spec.js`，将 `产能(h)` 静态契约切换为整数输入与正整数保存。
- `node tests/e2e/mes-route-use-config-display-static.spec.js` -> FAIL，当前仍保留 `:precision="6"`。
- `apply_patch` -> 更新 `src/views/mes/pro/route-use/RouteUsePage.vue`，将 `产能(h)` 改为整数输入，并补 `positiveInteger` / `normalizeHourlyCapacity`。
- `node tests/e2e/mes-route-use-config-display-static.spec.js` -> PASS。

## 2026-06-28 任务：20260628-electronic-batch-record-simulate-entry

### 用户需求

- 在电子批记录表单模板操作区增加 `模拟填写` 入口，点击后进入系统现有的左边模拟填写、右边显示填写结果的页面，不重新开发。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `rg -n "模拟填写|TemplateSimulate|BatchExecutionTemplateSimulate|template-simulate" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\router -S`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\edhr-batch\BatchExecutionTemplatePage.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\edhr-batch\BatchExecutionTemplateSimulatePage.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\batchrecordtemplate\index.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\edhr-batch-template-simulate-static.spec.js`
- `apply_patch` -> 创建 `doc/tasks/20260628-electronic-batch-record-simulate-entry/{task.md,execution-log.md,frontend-feature-evidence.md}`，并更新入口页面与现有模拟填写页契约。

## 2026-06-28 任务：20260628-electronic-batch-record-preview-scroll-fix

### 用户需求

- 电子批记录表单预览区右侧滚动条无法稳定点击选中，鼠标滚轮滚动的是最外部滚动条而不是当前预览滚动条。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\components\IFrame\src\IFrame.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\batch-record-preview-toolbar.spec.js`
- `rg -n "wheel|scroll|overflow|pointer-events|sameOriginChromeMode|jmreport-viewer-fit-width" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\components\IFrame D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\batchrecordtemplate -S`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 diff --unified=20 -- src/components/IFrame/src/IFrame.vue src/views/mes/pro/batchrecordtemplate/index.vue tests/e2e/batch-record-preview-toolbar.spec.js tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `apply_patch` -> 创建 `doc/tasks/20260628-electronic-batch-record-preview-scroll-fix/{task.md,execution-log.md,bug-regression-evidence.md}`，并更新滚动契约与页面实现。

## 2026-06-28 任务：20260628-electronic-batch-record-remove-outer-card

### 用户需求

- 电子批记录页面红框里有两层外部卡片，去除外部卡片。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `rg -n "批记录名称|报表名称|产品信息|粗洗工序生产记录|清洗工序生产记录" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 -S`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\batchrecordtemplate\index.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\electronic-batch-record-master-detail-layout-static.spec.js`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\components\ContentWrap\src\ContentWrap.vue`
- `apply_patch` -> 创建 `doc/tasks/20260628-electronic-batch-record-remove-outer-card/{task.md,execution-log.md,frontend-feature-evidence.md}`，更新页面与静态断言。

## 2026-06-26 任务：20260626-mes-schedule-order-toolbar-spacing

### 用户需求

- 优化排产工单页顶部工具栏排版，不要出现按钮挤在一起。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-bu-select-restriction\task.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-audio-modal\task.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-replan-protected-task-readable\task.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-replan-apply-disabled-regression\task.md`
- `rg -n "同步工单|手动重排|批量冻结|批量解冻|批量删除|搜索|重置" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- `rg -n "批量冻结|批量解冻|批量删除|手动重排|同步工单" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\scheduleorder\index.vue -TotalCount 220`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\scheduleorder\index.vue | Select-Object -Skip 2180 -First 280`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-usability-static.spec.js`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-replan-scope-static.spec.js`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 status --short`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 diff --unified=20 -- src/views/mes/pro/scheduleorder/index.vue`
- `apply_patch` -> 创建 `doc/tasks/20260626-mes-schedule-order-toolbar-spacing/{task.md,execution-log.md,frontend-feature-evidence.md}` 与 `tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-toolbar-layout-static.spec.js` -> RED FAIL
- `apply_patch` -> 调整 `src/views/mes/pro/scheduleorder/index.vue` 工具栏结构与样式，补充分组、换行和间距
- `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> BLOCKED（仓库现存全局类型错误）
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/scheduleorder --target-text 排产工单` -> BLOCKED
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/scheduleorder --target-text 同步工单` -> BLOCKED
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-schedule-order-toolbar-spacing\frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260626-mes-schedule-order-toolbar-spacing --mode preview` -> PASS

## 2026-06-26 任务：20260626-mdm-product-reference-id-missing

### 用户需求

- 产品主数据页面点击`引用`按钮时报错：`Required request parameter 'id' for method parameter type Long is not present`

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-audio-modal\task.md`
- `rg -n "引用" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mdm\product\index.vue D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mdm`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mdm\product\index.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\mdm\product\index.ts`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mdm\src\main\java\cn\iocoder\yudao\module\mdm\controller\admin\product\MdmProductController.java`
- `apply_patch` -> 创建 `doc/tasks/20260626-mdm-product-reference-id-missing/{task.md,execution-log.md,bug-regression-evidence.md}`，新增 `tests/e2e/mdm-product-reference-id-static.spec.js`
- `node tests/e2e/mdm-product-reference-id-static.spec.js` -> RED FAIL
- `apply_patch` -> 修正 `src/api/mdm/product/index.ts` 中 `/mdm/product/references` 的查询参数为 `id`
- `node tests/e2e/mdm-product-reference-id-static.spec.js` -> GREEN PASS
- `node scripts/mdm-product-master-contract.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 2026-06-26 任务：20260626-mes-remove-preflight-toolbar-button

### 用户需求

- 删除排产工单工具栏中的 `排产前检查` 按钮，保留页面其余功能。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-ChildItem -Path D:\ProjectPackage\Int\IntRuoyi\doc\tasks -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 8 FullName,LastWriteTime`
- `rg -n "排产前检查|排查前检查" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\scheduleorder\index.vue | Select-Object -First 120`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\scheduleorder\index.vue | Select-Object -Skip 680 -First 180`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-usability-static.spec.js`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-replan-scope-static.spec.js`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduling-scope-static.spec.js`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-replan-apply-enabled-static.spec.js`
- `git status --short`
- `git branch --show-current`
- `git diff --unified=0 -- src/views/mes/pro/scheduleorder/index.vue`
- `New-Item -ItemType Directory -Force D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-remove-preflight-toolbar-button`

## 2026-06-23 任务：20260623-schedule-order-sync-button-copy

### 用户需求

- 将截图红框内排产工单工具栏按钮文案由“待同步差异”改为“同步工单”。

### 已执行命令

- `Get-ChildItem -Force`
- `git status --short`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\clear-frontend-copy\SKILL.md`
- `git status --short`（前端子仓库）
- `rg -n "待同步差异|排产前检查|手动重排|排产工单|同步工单" yudao-ui-admin-vue3`
- `Get-ChildItem -Directory .\doc\tasks | Sort-Object LastWriteTime -Descending | Select-Object -First 8 Name,LastWriteTime`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 .\doc\tasks\20260623-unified-electronic-signature-tab\task.md`
- `python C:\Users\BJB110\.codex\skills\clear-frontend-copy\scripts\scan_frontend_copy.py --root D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --format markdown`
- `Get-Content -Encoding utf8 .\src\views\mes\pro\scheduleorder\index.vue | Select-Object -First 90`
- `Get-Content -Encoding utf8 .\tests\e2e\mes-pro-schedule-order-usability-static.spec.js`
- `Get-Content -Encoding utf8 .\tests\e2e\smart-scheduling-smoke-real-flow.e2e.js | Select-Object -Skip 855 -First 30`
- `Get-Content -Encoding utf8 .\package.json`
- `rg -n "待同步差异" src tests -g "!*node_modules*"`
- `New-Item -ItemType Directory -Force .\doc\tasks\20260623-schedule-order-sync-button-copy | Out-Null`
- `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js`
- `node --check tests/e2e/smart-scheduling-smoke-real-flow.e2e.js`
- `rg -n "待同步差异|同步工单" src tests -g "!*node_modules*"`
- `python C:\Users\BJB110\.codex\skills\clear-frontend-copy\scripts\scan_frontend_copy.py --root D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\scheduleorder --format markdown`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\SKILL.md`
- `git diff -- src/views/mes/pro/scheduleorder/index.vue tests/e2e/mes-pro-schedule-order-usability-static.spec.js tests/e2e/smart-scheduling-smoke-real-flow.e2e.js doc/tasks/20260623-schedule-order-sync-button-copy/task.md doc/tasks/20260623-schedule-order-sync-button-copy/execution-log.md docs/request-command-log.md`
- `git status --short`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\references\closeout-rules.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260623-schedule-order-sync-button-copy --mode preview`
- `git add -- src/views/mes/pro/scheduleorder/index.vue tests/e2e/mes-pro-schedule-order-usability-static.spec.js tests/e2e/smart-scheduling-smoke-real-flow.e2e.js doc/tasks/20260623-schedule-order-sync-button-copy/task.md doc/tasks/20260623-schedule-order-sync-button-copy/execution-log.md docs/request-command-log.md`
- `git commit -m "任务: 更新排产同步按钮文案"`
- `git add -- docs/request-command-log.md`
- `git commit --amend --no-edit`

## 2026-06-26 任务：20260626-edhr-batch-template-simulate-fill

### 用户需求

- 在 `eDHR批次执行` 列表的 `复盘` 右侧新增 `模板` 入口，进入左侧模板列表、右侧模板说明的页面。
- 第一个模板出现乱码后，要求直接修复模板说明页的乱码显示问题。
- 在模板说明页每张模板卡片右上角新增 `模拟填写` 按钮，进入新的单模板模拟页。
- 明确模拟页交互不是字段列表表单，而是“左边在原始表单模板内直接模拟填写，右边显示表单显示结果”。
- 调整布局为左右等宽，并曾要求左右显示体同尺寸、内部内容等比例缩放。
- 修复开发期出现的 ESLint 报错：`Disallow self-closing on HTML elements (<slot/>)`。
- 排查并修复开发期出现的动态导入失败：`Failed to fetch dynamically imported module: /src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue`。
- 最终将模拟页显示策略调整为：模板宽度适配容器宽度，高度不限制，按内容自然展开。

### 已执行命令

- `python -X utf8 -c "from pathlib import Path; print(Path(r'C:/Users/BJB110/.codex/skills/frontend-feature-delivery/SKILL.md').read_text(encoding='utf-8'))"`
- `python -X utf8 -c "from pathlib import Path; print(Path(r'C:/Users/BJB110/.codex/skills/frontend-feature-delivery/references/frontend-contract.md').read_text(encoding='utf-8'))"`
- `python -X utf8 -c "from pathlib import Path; print(Path(r'D:/ProjectPackage/Int/IntRuoyi/docs/experience-index.md').read_text(encoding='utf-8'))"`
- `python -X utf8 -c "from pathlib import Path; print(Path(r'D:/ProjectPackage/Int/IntPP/FRONTEND_STYLE.md').read_text(encoding='utf-8'))"`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 status --short`
- `rg -n "20260626-edhr-batch-template-simulate-fill|模板模拟填写|宽度适配容器|高度不限制" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\request-command-log.md D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-simulate-fill -S`
- `rg -n "20260626-edhr-batch-template-simulate-fill|eDHR 模板模拟填写页|左右模板显示体一致|宽度适配容器且高度不受限" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-simulate-fill D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\request-command-log.md -S`
- `python -X utf8 -c "from pathlib import Path; print(Path(r'D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/doc/tasks/20260626-edhr-batch-template-simulate-fill/task.md').read_text(encoding='utf-8'))"`
- `python -X utf8 -c "from pathlib import Path; print(Path(r'D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/doc/tasks/20260626-edhr-batch-template-simulate-fill/execution-log.md').read_text(encoding='utf-8'))"`
- `python -X utf8 -c "from pathlib import Path; print(Path(r'D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/doc/tasks/20260626-edhr-batch-template-simulate-fill/frontend-feature-evidence.md').read_text(encoding='utf-8'))"`
- `apply_patch` -> 新增模板说明页 `模板` 入口、隐藏路由、模板说明页、模拟填写页、可编辑模板组件、宽度适配组件，并持续修正文档与静态契约。
- `node tests/e2e/edhr-batch-template-simulate-static.spec.js`
- `node tests/e2e/edhr-batch-template-preview-static.spec.js`
- `node tests/e2e/edhr-batch-history-static.spec.js`
- `node tests/e2e/edhr-inline-signature-cells-static.spec.js`
- `node tests/e2e/edhr-batch-review-summary-labels-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-simulate-fill\frontend-feature-evidence.md`

## 2026-06-26 任务：20260626-electronic-batch-record-master-detail-layout

### 用户需求

- 将电子批记录页面重新布局为：左侧批记录名称，中间显示所选批记录对应报表名称，右侧显示所选报表的表单模板。
- 支持增加、删除、修改；本次按确认计划复用现有文件导入新增、单报表删除、报表编辑、重命名、签名位和单元格规则维护。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-role-management-toolbar-layout\task.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-role-management-toolbar-layout\execution-log.md`
- `New-Item -ItemType Directory -Force -Path D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-electronic-batch-record-master-detail-layout`
- `apply_patch` -> 创建任务文档、执行日志、前端证据文件和 `tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> RED FAIL，缺少主从三栏容器。
- `apply_patch` -> 修改 `src/views/mes/pro/batchrecordtemplate/index.vue` 为三栏主从布局，新增选中批记录、选中报表和模板预览状态。
- `apply_patch` -> 更新 `scripts/electronic-batch-record-word-import.test.mjs`、`tests/e2e/edhr-word-template-import-real-flow.e2e.js`、`tests/e2e/edhr-template-attachment-rule-real-flow.e2e.js` 以适配三栏结构。
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `node scripts/electronic-batch-record-jimu-list.test.mjs`
- `node scripts/electronic-batch-record-word-import.test.mjs`
- `node scripts/electronic-batch-record-report-page.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260626-electronic-batch-record-master-detail-layout/frontend-feature-evidence.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\SKILL.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-electronic-batch-record-master-detail-layout --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- `node --check tests/e2e/edhr-word-template-import-real-flow.e2e.js`
- `node --check tests/e2e/edhr-template-attachment-rule-real-flow.e2e.js`
## 2026-06-26 任务：20260626-edhr-signature-cell-electronic-signature

### 用户需求

- 批记录表单填写时，签名应进行电子签名，而不是手动输入签名人和签名时间。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\SKILL.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `apply_patch` -> 创建前后端任务文档、执行日志和 evidence 骨架。

## 2026-06-26 任务追加：电子批记录隐藏顶部工具区与表单自适应

### 用户需求

- 红框内不显示，蓝框内的表单宽度自适应，高度等比例缩放。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `apply_patch` -> 更新 `src/components/IFrame/src/IFrame.vue`，新增 `jmreport-viewer-fit-width` 模式和 Jimu 表单等比缩放。
- `apply_patch` -> 移除 `src/views/mes/pro/batchrecordtemplate/index.vue` 残留顶部工具区样式。
- `apply_patch` -> 更新 `scripts/electronic-batch-record-word-import.test.mjs`，按新 UI 契约断言顶部工具区不显示且导入 input 保留。
- `node tests/e2e/batch-record-preview-toolbar.spec.js`
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `node scripts/electronic-batch-record-jimu-list.test.mjs`
- `node scripts/electronic-batch-record-word-import.test.mjs`
- `node scripts/electronic-batch-record-report-page.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 2026-06-28 任务：20260628-electronic-batch-record-preview-cache

### 用户需求

- 红框里的数据做缓存，不要每一次点击进去都加载，除非数据发生了改变。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `rg -n "表单模板|报表名称|批记录名称|签名位|单元格规则|产品信息" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\batchrecordtemplate\index.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\electronic-batch-record-master-detail-layout-static.spec.js`
- `apply_patch` -> 新建任务文档、execution-log、frontend-feature-evidence，并实现电子批记录预览缓存与静态契约更新。

## 2026-06-28 任务：20260628-electronic-batch-record-preview-keepalive

### 用户需求

- 现在几个表单来回切换几次还是会转圈加载。

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\components\IFrame\src\IFrame.vue`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\batchrecordtemplate\index.vue`
- `apply_patch` -> 新建保活切换任务文档，并将右侧预览改为已访问 iframe 保活切换。

## 2026-06-28 任务：20260628-edhr-batch-progress-over-100-fix

### 用户需求

- `edhr批次执行的完成进度怎么会超过100%`
- `继续`

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-edhr-batch-progress-over-100-fix\task.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-edhr-batch-progress-over-100-fix\execution-log.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\edhr-batch\progress.ts`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProEdhrBatchExecutionServiceImpl.java`
- `node scripts/edhr-batch-required-progress.test.mjs` -> RED FAIL，确认模板必填任务分母与后端 `taskApprovedCount` 分子口径不一致。
- `apply_patch` -> 更新 `src/views/mes/pro/edhr-batch/progress.ts`，新增模板必填任务完成数 helper，并统一列表进度与详情文案口径。
- `apply_patch` -> 重写 `scripts/edhr-batch-required-progress.test.mjs` 为行为断言式回归，覆盖特殊节点不应把模板进度抬到 `100%` 以上。
- `node scripts/edhr-batch-required-progress.test.mjs` -> PASS
- `node tests/e2e/edhr-batch-template-preview-static.spec.js` -> PASS
- `node node_modules/eslint/bin/eslint.js src/views/mes/pro/edhr-batch/progress.ts src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue scripts/edhr-batch-required-progress.test.mjs` -> PASS
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\SKILL.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260628-edhr-batch-progress-over-100-fix --mode preview` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-edhr-batch-progress-over-100-fix\frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-edhr-batch-progress-over-100-fix\bug-regression-evidence.md` -> PASS

## 2026-06-29 任务：20260629-feedback-pending-table-wrap

### 用户需求

- 这里的每列的文字很多都显示不全，改成如果显示不完多行显示，不用省略号策略。

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\feedback\index.vue`
- `rg -n "待归属|归属结果|报工时间|缓存池数量" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests`
- `apply_patch` -> 新建 `20260629-feedback-pending-table-wrap` 任务文档、执行日志、前端证据和待归属表多行展示静态契约测试。

## 2026-06-30 任务：20260630-test-server-dcc-browser-cache-write-failure-followup

### 用户需求

- `测试服务器的文件查阅提示DCC 受控浏览本地缓存写入失败，请检查浏览器本地存储权限。`
- `帮我再测试服务器修改,测试服务器还是报错`

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 status --short`
- `apply_patch` -> 将 `20260630-erp-production-order-material-list-bidirectional-link` 与 `20260630-dcc-admin-full-config-package` 显式标记为 `blocked`，并新建 `20260630-test-server-dcc-browser-cache-write-failure-followup` 任务文档与执行日志。
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- `git -C D:\ProjectPackage\Int\release-worktrees\IntRuoyi-frontend-20260630-dcc-cache-rerelease status --short`
- `node D:\ProjectPackage\Int\release-worktrees\IntRuoyi-frontend-20260630-dcc-cache-rerelease\tests\e2e\dcc-browser-cache-write-failure-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\release-worktrees\IntRuoyi-frontend-20260630-dcc-cache-rerelease\tests\e2e\dcc-browser-remember-state-cache-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\release-worktrees\IntRuoyi-frontend-20260630-dcc-cache-rerelease\doc\tasks\20260630-test-server-dcc-browser-cache-write-failure-followup\bug-regression-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\release-worktrees\IntRuoyi-frontend-20260630-dcc-cache-rerelease\doc\tasks\20260630-test-server-dcc-browser-cache-write-failure-followup\frontend-feature-evidence.md` -> PASS
- `apply_patch` -> 在干净前端发布 worktree 中补齐 `20260630-test-server-dcc-browser-cache-write-failure-followup` 的 bug 回归证据、前端证据与补发版门禁记录，并补记本次命令日志。

## 2026-07-01 任务：20260701-edhr-phase6-module-dedup

### 用户需求

- `当前实现的phase1~5与edhr里其他的模块有重复的吗,或者其他有哪些edhr的模块可以删除吗`
- `继续`

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\simplify-codebase\SKILL.md`
- `rg -n "template-simulate|edhr-batch-execution/review|BatchExecutionReviewPage|BatchExecutionTemplatePage|BatchExecutionTemplateSimulatePage" src/router src/views package.json doc/tasks/20260701-edhr-phase6-module-dedup -S`
- `rg -n "review|template|simulate|router\.push|详情|复盘|模板|模拟" src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue -S`
- `rg -n "simulate|router\.push|按钮|Button|模拟|填写|返回|详情" src/views/mes/pro/edhr-batch/BatchExecutionTemplatePage.vue -S`
- `rg -n "template-simulate|模拟填写|router\.push" src/views/mes/pro/batchrecordtemplate/index.vue -S`
- `apply_patch` -> 新建并更新 Phase 6 前端任务台账、去重矩阵、执行日志；从批次列表主操作移除 `复盘`、`模板` 直达入口；在批次详情页管理后台工作区补 `批次模板后台` 入口。
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3 ts:check` -> FAIL，默认 Node 堆大小触发 `JavaScript heap out of memory`。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3 ts:check` -> PASS。
- `node .runtime\edhr-list-click-detail-edge-e2e-v2.mjs` -> PASS，真实测试租户 `测试租户/aoteman` 从 `eDHR批次执行` 列表点击首行 `详情`，`/get` 与 `/workbench` 均 200，详情页显示 `批次总控 / 阶段摘要 / 放行 / 审计 / 管理后台`。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260701-edhr-phase6-module-dedup --mode preview` -> BLOCKED，无删除项；因无法快进合并到 `int_main` 且 cleanup 脚本把本轮生产改动判为 unrelated pending change，未执行 apply。
- `git add -- src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue doc/tasks/20260701-edhr-phase6-module-dedup/task.md doc/tasks/20260701-edhr-phase6-module-dedup/execution-log.md doc/tasks/20260701-edhr-phase6-module-dedup/dedup-matrix.md`
- `git commit -m '任务: 收口eDHR重复入口'` -> PASS，提交 `9fb1db19a`。

## 2026-07-02 任务：20260702-dcc-controlled-preview-transform-controls

### 用户需求

- 在文件浏览里要可以放大缩小、左旋转90度、右旋转90度。

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `rg -n "受控预览|下载当前受控副本|DCC基础条目|文件名称|文件类别|受控目录|禁止截图|外传|识别基础信息|当前预览" yudao-ui-admin-vue3\src yudao-ui-admin-vue3\tests -S`
- `git -C yudao-ui-admin-vue3 status --short`
- `apply_patch` -> 新建本任务文档、执行日志、前端证据，并先补缩放旋转 RED 静态契约测试。
- `node tests/e2e/dcc-common-file-preview-source.spec.js; node tests/e2e/dcc-controlled-file-protection.contract.test.js` -> FAIL，预览组件未实现控制入口。
- `apply_patch` -> 实现 `ProtectedPdfViewer` PDF/图片缩放与左右旋转控制。
- `node tests/e2e/dcc-common-file-preview-source.spec.js; node tests/e2e/dcc-controlled-file-protection.contract.test.js` -> FAIL，既有上传提示断言与旋转静态断言需对齐。
- `apply_patch` -> 调整 DCC 预览静态契约，去除无关上传 accept 断言并校验旋转状态变更。
- `node tests/e2e/dcc-common-file-preview-source.spec.js`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js`
- `pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260702-dcc-controlled-preview-transform-controls\frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-dcc-controlled-preview-transform-controls --mode preview` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-dcc-controlled-preview-transform-controls --mode apply` -> PASS

## 2026-07-02 任务：20260702-dcc-pdf-preview-arraybuffer-detach-fix

### 用户需求

- PDF 预览页报错：`Failed to execute 'postMessage' on 'Worker': ArrayBuffer at index 0 is already detached.`

### 已执行命令

- `rg -n "postMessage|ArrayBuffer|destroyPdfArtifacts|currentPdfBytes|renderPdfPages|getDocument|pdfBytes|previewPayload\\.bytes|data: pdfBytes" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\view\index.vue D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e -S`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- `apply_patch` -> 新建本修复任务文档、执行日志、bug 回归证据，并先补 PDF worker 字节克隆 RED 静态契约。
- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> FAIL，缺少 `clonePdfBytesForWorker`。
- `apply_patch` -> PDF 渲染传入 pdf.js worker 前克隆 `Uint8Array`，避免复用 detached ArrayBuffer。
- `node tests/e2e/dcc-common-file-preview-source.spec.js`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js`
- `pnpm ts:check`

## 2026-07-02 任务：20260702-dcc-preview-sticky-transform-controls

### 用户需求

- 红框内的缩放旋转控制栏在滚动到第二页、第三页、第四页时看不到，采用“预览区内悬浮吸顶控制栏”方案。

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 status --short`
- `rg -n "protected-viewer-transform-controls|protected-viewer-toolbar__actions|protected-viewer-frame|protected-viewer-corner-watermark|canTransformPreview" D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\view\index.vue D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e -S`
- `apply_patch` -> 新建本任务文档、执行日志，并将 PDF/图片缩放旋转控制栏移入预览框内部 sticky 区域。
- `node tests/e2e/dcc-common-file-preview-source.spec.js`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js`
- `pnpm ts:check`

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-dcc-preview-sticky-transform-controls --mode preview` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-dcc-preview-sticky-transform-controls --mode apply` -> PASS
- 用户需求：排产工单不显示排产编码，冻结效果要明显。
- `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js`

## 2026-07-02 任务：20260702-dcc-preview-control-scroll-container-fix

### 用户需求

- 当前控制栏还是会随着滚轮移动；看到第二页时需要返回第一页才能进行放大缩小操作。

### 已执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- `rg -n "protected-viewer-transform-controls|protected-viewer-frame|protected-viewer-content|overflow|sticky|fixed|viewerFrameRef|pageEntries" src/views/dcc/controlled-file/view/index.vue tests/e2e/dcc-common-file-preview-source.spec.js tests/e2e/dcc-controlled-file-protection.contract.test.js`
- `apply_patch` -> 新建滚动容器修复任务文档与 bug 回归证据，先补 PDF / 图片内部滚动视口 RED 契约。
- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> FAIL，缺少 `protected-viewer-frame--transformable`。
- `apply_patch` -> 将 PDF / 图片预览框改为 `protected-viewer-frame--transformable` 内部滚动视口，设置 `max-height: calc(100vh - 180px)` 与 `overscroll-behavior: contain`。
- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> PASS。
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js` -> PASS。
- `git diff --check` scoped to DCC files -> PASS。
- `Invoke-WebRequest http://localhost:8081/src/views/dcc/controlled-file/view/index.vue` 与 Vite style module -> PASS，运行态源码与样式均包含内部滚动视口契约。
- `pnpm ts:check` -> PASS，全量类型检查通过。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-dcc-preview-control-scroll-container-fix --mode preview` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-dcc-preview-control-scroll-container-fix --mode apply` -> PASS

## 2026-07-02 任务：20260702-dcc-preview-transform-controls-grid

### 用户需求

- 将预览控制按钮集合做成 2 行 4 个按钮：放大、缩小、旋转、复原；旋转为右旋。

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `rg -n "protected-viewer-transform-controls|handleZoomIn|handleZoomOut|handleRotateLeft|handleRotateRight|resetViewerTransformState|左旋转90|右旋转90|放大|缩小|复原" src/views/dcc/controlled-file/view/index.vue tests/e2e/dcc-common-file-preview-source.spec.js tests/e2e/dcc-controlled-file-protection.contract.test.js -S`
- `apply_patch` -> 新建任务文档与前端证据，先补 2x2 控制栏 RED 静态契约。
- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> FAIL，缺少 `protected-viewer-transform-controls__grid`。
- `apply_patch` -> PDF / 图片控制栏改为 2x2 按钮网格，按钮为放大、缩小、旋转、复原；旋转仅右旋。

- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> PASS
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js` -> PASS
- `pnpm ts:check` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260702-dcc-preview-transform-controls-grid\frontend-feature-evidence.md` -> PASS
- `Invoke-WebRequest http://localhost:8081/src/views/dcc/controlled-file/view/index.vue` ? Vite style module -> PASS???????? 2x2 ?????????????

## 2026-07-02 ???20260702-dcc-preview-css-zoom

### ????

- PDF ???????????/??????????????????????????????

### ?????

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `rg -n "applyPdfZoomChange|rerenderCurrentPdf|renderPdfPages|getDocument|currentPdfBytes|viewerScale|viewerZoomPercent|canvas-wrap|pageEntries|transformStyle|handleZoomIn|handleZoomOut" src/views/dcc/controlled-file/view/index.vue tests/e2e/dcc-common-file-preview-source.spec.js tests/e2e/dcc-controlled-file-protection.contract.test.js -S`
- `apply_patch` -> ?????????????? PDF ??????????? RED ?????
- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> FAIL???????? `applyPdfZoomChange` / `rerenderCurrentPdf`?
- `apply_patch` -> PDF ????????? canvas???????? CSS transform?????/?????????????
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260702-dcc-preview-css-zoom\frontend-feature-evidence.md` -> PASS
- `Invoke-WebRequest http://localhost:8081/src/views/dcc/controlled-file/view/index.vue` ? Vite style module -> PASS?????????? PDF ???????????? PDF viewport wrapper?

## 2026-07-03 任务：20260703-edhr-process-form-action-columns

### 用户需求

- 红框的位置显示工序，蓝框的位置显示表单，黄框的位置显示针对这个工序的控制按钮。

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\SKILL.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `rg -n "工序复盘|已填写表单|当前工序操作台|基础信息|刷新复盘|当前工序" yudao-ui-admin-vue3`
- `node tests/e2e/edhr-batch-basic-info-dialog-static.spec.js` -> PASS
- `apply_patch` -> 新增 `edhr-process-form-action-columns-static.spec.js` RED 契约。
- `node tests/e2e/edhr-process-form-action-columns-static.spec.js` -> FAIL，缺少三栏语义。
- `apply_patch` -> 将 `BatchExecutionDetailPage.vue` 工序复盘区调整为左工序、中表单、右控制按钮三栏。
- `node tests/e2e/edhr-process-form-action-columns-static.spec.js` -> PASS
- `node tests/e2e/edhr-process-evidence-fusion-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260703-edhr-process-form-action-columns\frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260703-edhr-process-form-action-columns\quality-assurance-evidence.md` -> PASS
- `pnpm ts:check` -> FAIL，Node 默认堆内存 OOM。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-edhr-process-form-action-columns --mode preview` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-edhr-process-form-action-columns --mode apply` -> PASS

## 2026-07-03 任务：20260703-edhr-remove-redundant-review-copy

### 用户需求

- 删除截图红框里的冗余内容。

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `rg -n "工序复盘|当前工序控制按钮|签名、审批、审计、归档、放行和变更都围绕工序|基础信息|刷新复盘" yudao-ui-admin-vue3`
- `apply_patch` -> 新建任务文档、前端证据和 RED 静态契约。
- `node tests/e2e/edhr-remove-redundant-review-copy-static.spec.js` -> FAIL，页面仍显示红框冗余内容。
- `apply_patch` -> 删除 `BatchExecutionDetailPage.vue` 中工序复盘顶部说明、表单区摘要头和右侧控制区摘要头。
- `apply_patch` -> 删除 `BatchExecutionDetailPage.vue` 中已失效的冗余头部样式选择器。
- `node tests/e2e/edhr-remove-redundant-review-copy-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-basic-info-dialog-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> FAIL，非本次修改文件 `src/views/mes/pro/feedback/index.vue` 存在既有类型错误。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-remove-redundant-review-copy/frontend-feature-evidence.md` -> 首次 FAIL，证据文件缺少固定 Acceptance / RED / GREEN / Verification 标记。
- `apply_patch` -> 补齐前端证据固定校验标记并记录 tscheck 阻塞点。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-remove-redundant-review-copy/frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-edhr-remove-redundant-review-copy --mode preview` -> PASS
- `apply_patch` -> 标记任务完成并记录最终验证结果。

## 20260703-dcc-project-code-associated-files-grouping

- 用户需求：DCC 基础条目详情中，识别出来的关联文档需要按分类显示，例如 `技术文档 -> 设计输入 -> XXX.pdf`。
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `rg -n --encoding utf-8 -S "DCC基础条目|关联文档|文件名称|文件编号|基础条目" yudao-ui-admin-vue3\src`
- `rg -n --encoding utf-8 -S "fileTypeLevel1|fileTypeLevel2|fileTypeLevel3|fileTypeLevel4|fileTypeLevel5" ruoyi-vue-pro\yudao-module-dcc\src\main\java yudao-ui-admin-vue3\src yudao-ui-admin-vue3\tests`
- `apply_patch` -> 新建 DCC 任务文档、前端证据骨架，并新增 RED 静态契约。
- `node tests/e2e/dcc-project-code-recognition-static.spec.js` -> FAIL，`ControlledFileVO` 尚未声明 `fileTypeLevel1`。
- `apply_patch` -> 补齐 `ControlledFileVO.fileTypeLevel1~5`，并将 DCC 基础条目关联文档改为按分类层级分组展示。
- `node tests/e2e/dcc-project-code-recognition-static.spec.js` -> PASS。
- `pnpm e2e:dcc:project-code-basic-data:static` -> PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-dcc-project-code-associated-files-grouping/frontend-feature-evidence.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-dcc-project-code-associated-files-grouping --mode preview` -> PASS，预览仅删除本任务额外证据文件。
- `apply_patch` -> 标记任务完成并记录 cleanup preview 结果。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-dcc-project-code-associated-files-grouping --mode apply` -> FAIL，cleanup 脚本未识别原任务状态格式。
- `apply_patch` -> 将任务状态行改为 `Status: completed` 并记录 cleanup apply 阻塞。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-dcc-project-code-associated-files-grouping --mode apply` -> FAIL，cleanup 脚本仍未识别行内状态格式。
- `rg -n --encoding utf-8 "status|completed|Task status|current status|Current Status" C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py` -> 确认脚本读取 `## Current Status` 章节。
- `apply_patch` -> 将任务状态改为 `## Current Status` 章节并记录第二次 cleanup apply 阻塞。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-dcc-project-code-associated-files-grouping --mode apply` -> PASS，删除本任务额外证据文件。

## 2026-07-03 排产工单删除追溯按钮

### 用户需求

- `排产工单删除追溯按钮`

### 已执行命令

- `python -X utf8 -c "from pathlib import Path; print(Path('docs/powershell-memory.md').read_text(encoding='utf-8'))"` -> PASS，读取 PowerShell/编码门禁。
- `rg -n "排产工单|追溯|trace|Trace|追踪|溯源" yudao-ui-admin-vue3 ruoyi-vue-pro docs doc` -> PASS，定位排产工单页面和行操作测试。
- `python -X utf8 -c "...frontend-feature-delivery/SKILL.md..."` -> PASS，读取前端交付技能。
- `python -X utf8 -c "...frontend-contract.md / FRONTEND_STYLE.md..."` -> PASS，读取前端证据契约和统一前端样式。
- `git -C yudao-ui-admin-vue3 status --short` -> PASS，确认前端仓已有历史脏改，本次只处理排产工单追溯按钮相关文件。
- `apply_patch` -> 新建本次任务文档、前端证据，并补充排产工单行操作静态契约。
- `node tests/e2e/mes-schedule-order-row-actions-static.spec.js` -> RED FAIL，确认行操作仍包含 `openOperationLogDialog(row)`。
- `apply_patch` -> 删除排产工单非冻结行操作中的「追溯」按钮。
- `node tests/e2e/mes-schedule-order-row-actions-static.spec.js` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-schedule-order-remove-trace-action/frontend-feature-evidence.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-schedule-order-remove-trace-action --mode preview` -> PASS，首次预览提示会清理 `frontend-feature-evidence.md`。
- `apply_patch` -> 将 `frontend-feature-evidence.md` 加入 Cleanup Keep，准备复验收尾预览。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-schedule-order-remove-trace-action --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
- `apply_patch` -> 标记本次任务完成并记录最终验证结果。
# 2026-07-03 eDHR 复盘摘要移入右侧栏

## 用户需求

- “将红色的区域放在蓝色区域”，截图指向 eDHR 批记录详情页，需要把顶部基础/详情入口与执行摘要移到右侧蓝框空白区域。
- 用户补充：“继续”。

## 执行命令记录

- `Get-Content -LiteralPath D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md -Encoding utf8`
- `Get-ChildItem -LiteralPath doc\tasks -Directory`
- `Get-Content -LiteralPath docs\experience-index.md -Encoding utf8`
- `rg -n "执行编号|字段审计|追溯状态|VERIFIED|审批时间|电子批记录" src`
- `Get-Content -LiteralPath C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md -Encoding utf8`
- `Get-Content -LiteralPath D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md -Encoding utf8`
- `Get-Content -LiteralPath C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md -Encoding utf8`
- `Get-Content -LiteralPath src\views\mes\pro\edhr-batch\BatchExecutionDetailPage.vue -Encoding utf8`
- `git status --short`
- `rg --files tests | rg "edhr.*(batch|detail|review)|batch.*detail|detail-dialog|review-summary"`
- `Get-Content -LiteralPath doc\tasks\20260703-edhr-open-process-form-route\task.md -Encoding utf8`
- `apply_patch` 新增本任务文档、前端证据与 RED 静态契约。
- `node tests/e2e/edhr-review-summary-right-rail-static.spec.js` -> RED 失败，确认现状未满足右侧栏契约。
- `apply_patch` 更新 `BatchExecutionDetailPage.vue`、`edhr-batch-basic-info-dialog-static.spec.js` 和任务执行日志。
- `node tests/e2e/edhr-review-summary-right-rail-static.spec.js; node tests/e2e/edhr-batch-basic-info-dialog-static.spec.js; node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> PASS。
- `node tests/e2e/edhr-remove-redundant-review-copy-static.spec.js` -> PASS。
- `git diff -- src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-review-summary-right-rail-static.spec.js tests/e2e/edhr-batch-basic-info-dialog-static.spec.js doc/tasks/20260703-edhr-review-summary-right-rail/task.md doc/tasks/20260703-edhr-review-summary-right-rail/execution-log.md doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md docs/request-command-log.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md; git diff --check ...` -> evidence 校验缺少 `RED:` / `GREEN:` 标记，随后补齐。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md` -> PASS。
- `git diff --check -- src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-review-summary-right-rail-static.spec.js tests/e2e/edhr-batch-basic-info-dialog-static.spec.js doc/tasks/20260703-edhr-review-summary-right-rail/task.md doc/tasks/20260703-edhr-review-summary-right-rail/execution-log.md doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md docs/request-command-log.md` -> PASS。
- `Get-Content -LiteralPath C:\Users\BJB110\.codex\skills\task-closeout-cleanup\SKILL.md -Encoding utf8`
- `Get-Content -LiteralPath C:\Users\BJB110\.codex\skills\task-closeout-cleanup\references\closeout-rules.md -Encoding utf8`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-edhr-review-summary-right-rail --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
- `git status --short` -> 确认仅暂存本任务文件，跳过既有 `scheduleorder/index.vue`、旧任务文档和旧测试文件。
- `git add -- docs/request-command-log.md src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-batch-basic-info-dialog-static.spec.js tests/e2e/edhr-review-summary-right-rail-static.spec.js doc/tasks/20260703-edhr-review-summary-right-rail/task.md doc/tasks/20260703-edhr-review-summary-right-rail/execution-log.md doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md`
- `git check-ignore -v doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md`
- `git ls-files 'doc/tasks/**/frontend-feature-evidence.md'`
- `git add -f -- doc/tasks/20260703-edhr-review-summary-right-rail/frontend-feature-evidence.md`
- `git diff --cached --check` -> PASS。
- `git commit -m "任务: 调整eDHR复盘摘要右侧栏"` -> PASS。

# 2026-07-06 DCC 基础信息识别无匹配提示

## 用户需求

- 将文控中心“识别基础信息”中“文件可正常识别但未匹配到产品名称”的场景改为识别成功但未识别到产品名，而不是识别失败。

## 执行命令记录

- `Get-Content -LiteralPath D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md -Encoding utf8`
- `Get-Content -LiteralPath D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md -Encoding utf8`
- `Get-Content -LiteralPath C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md -Encoding utf8`
- `Get-Content -LiteralPath C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md -Encoding utf8`
- `rg -n "recognizeProjectCode|recognize-project-code|识别基础信息|识别产品|产品名称" src tests`
- `apply_patch` -> 新增前端任务文档、前端证据和静态契约 RED 断言。
- `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 run e2e:dcc:project-code-recognition:static` -> RED FAIL，脚本入口缺失。
- `apply_patch` -> 扩展 `ControlledFileProjectCodeRecognitionRespVO` 类型、详情页 `NO_MATCH` 提示、静态契约测试和 package 脚本。
- `node tests/e2e/dcc-project-code-recognition-static.spec.js` -> GREEN PASS。
- `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 run e2e:dcc:project-code-recognition:static` -> GREEN PASS。
- `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/detail/index.vue tests/e2e/dcc-project-code-recognition-static.spec.js --format stylish` -> GREEN PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260706-dcc-recognition-no-match-success\frontend-feature-evidence.md` -> 初次 FAIL，缺少 `Acceptance` / `BDD:` 标记，随后补齐。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260706-dcc-recognition-no-match-success --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
# 2026-07-06 排程明细弹框按工单合并

## 用户需求

- 检查当前系统里有多少个图2这种平铺样式弹框，并将生产排程日历任务类明细弹框改成图1这种合并同类样式：同一工单合并展示，点击工单显示该工单对应的其他列。

## 执行命令

- `git status --short` / `git -C yudao-ui-admin-vue3 status --short` -> PASS，确认前端仓本次开始前干净，根目录存在既有无关脏改。
- `python -X utf8` / `rg` 静态排查 -> PASS，确认目标为 `src/views/mes/pro/task/calendar/index.vue` 中任务详情、白班详情、夜班详情、锁定详情四个共享平铺任务弹框。
- `apply_patch` -> 新建任务文档、执行日志、前端证据和 RED 静态契约。
- `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js` -> RED FAIL，旧页面缺少按工单分组布局。
- `apply_patch` -> 将生产排程日历任务类明细弹框改为左侧工单列表、右侧选中工单任务表，并迁移工单产线分析入口。
- `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js` / `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js` / `pnpm.cmd exec eslint ...` / `pnpm.cmd run ts:check:schedule` -> 首轮验证中旧列契约仍锚定右侧任务表内的 `openWorkOrderAnalysis`，需调整为弹框整体保留工单分析入口。
- `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js` -> GREEN PASS。
- `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js` -> GREEN PASS。
- `pnpm.cmd exec eslint src/views/mes/pro/task/calendar/index.vue tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js --format stylish` -> GREEN PASS。
- `pnpm.cmd run ts:check:schedule` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-schedule-calendar-workorder-group-dialog --mode preview` -> PASS，预览只清理本次额外证据文件。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-schedule-calendar-workorder-group-dialog --mode apply` -> BLOCKED，cleanup 脚本只识别 `## Current Status` 英文状态节。
- `apply_patch` -> 补充 `## Current Status` / `completed`，继续收尾。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-schedule-calendar-workorder-group-dialog --mode apply` -> PASS，删除本次额外证据文件，保留任务核心记录。
# 2026-07-06 排程明细任务表产品列拆分

## 用户需求

- 截图红框里的列不显示，绿框里的产品拆开显示编码和名称。

## 执行命令

- `git status --short` -> PASS，确认前端仓仅存在无关既有改动 `scripts/dcc-signature-evidence-export.test.mjs`。
- `python -X utf8` 静态读取 `src/views/mes/pro/task/calendar/index.vue` 和现有排程日历静态测试 -> PASS，确认目标为任务类日汇总弹框右侧 `selectedDaySummaryTaskRows` 表格。
- `apply_patch` -> 新建任务文档、执行日志和 RED 静态契约。
- `node tests/e2e/mes-pro-schedule-calendar-task-product-columns-static.spec.js` -> RED FAIL，右侧任务表仍是合并 `产品` 列。
- `apply_patch` -> 拆分 `产品编码` / `产品名称`，隐藏 `待检` / `执行状态`，同步排程日历静态契约。
- `node tests/e2e/mes-pro-schedule-calendar-task-product-columns-static.spec.js` -> GREEN PASS。
- `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js` -> GREEN PASS。
- `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js` -> GREEN PASS。
- `pnpm.cmd exec eslint src/views/mes/pro/task/calendar/index.vue tests/e2e/mes-pro-schedule-calendar-task-product-columns-static.spec.js tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js --format stylish` -> GREEN PASS。
- `pnpm.cmd run ts:check:schedule` -> GREEN PASS。

# 2026-07-06 eDHR 批次执行角色化权限与操作体验改造

## 用户需求

- 在独立 worktree 中基于 `edhr_batch_improve` 分支完成 eDHR 批次执行角色化权限与操作体验改造，覆盖填写人、审核人、批准人、生产负责人、无关人员，验证通过后融合进 `int_main` 并删除 worktree。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/worktree-memory.md`、worktree/backend/frontend/BDD 技能说明与证据契约 -> PASS。
- `git worktree add -b edhr_batch_improve D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve\yudao-ui-admin-vue3 int_main` -> PASS。
- `Get-NetTCPConnection -LocalPort 8095,48095` -> PASS，目标端口当前未占用。
- 创建 `doc/tasks/20260706-edhr-batch-role-permission-flow/`、`.runtime/runtime.env`，记录 BDD、设计约束、经验门禁和运行态计划 -> PASS。

## 2026-07-06 edhr_batch_improve 验证收尾
- 后端 targeted 回归：mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrWorkTaskFlowContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，156 tests。
- 真实 E2E：
ode tests\e2e\edhr-batch-role-permission-real-flow.e2e.js -> PASS，batch=900000000462，execution=778。
## 2026-07-06 edhr_batch_improve 最终验证收尾
- 前端静态契约：node tests\e2e\edhr-batch-pending-form-entry-static.spec.js -> PASS。
- 前端类型检查：$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。
- 真实 E2E 脚本语法：node --check tests\e2e\edhr-batch-role-permission-real-flow.e2e.js -> PASS。
- 真实 E2E：node tests\e2e\edhr-batch-role-permission-real-flow.e2e.js -> PASS，routeCode=900025，batch=900000000462，execution=778，process=吹球囊成型。
- 五角色复核：填写人、审核人、批准人、生产负责人、无关人员 -> 全部 PASS。

# 2026-07-08 eDHR 待处理节点槽位标签移到右侧栏

## 用户需求

- 蓝框里的内容放在红框的位置，即将 eDHR 批次详情页左侧待处理工序卡片里的槽位状态标签和缺失配置提示移到右侧当前工序摘要栏。

## 执行命令

- `Get-Content -Encoding utf8 docs\powershell-memory.md` -> PASS，完成 PowerShell / UTF-8 门禁。
- `Get-Content -Encoding utf8 docs\experience-index.md` 与 `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` -> PASS，确认前端样式与经验门禁。
- `rg -n "来料检报|待处理详情|跳过节点|当前工序尚未形成" yudao-ui-admin-vue3` -> PASS，定位 `src\views\mes\pro\edhr-batch\BatchExecutionDetailPage.vue`。
- `apply_patch` -> 新增任务文档、执行日志和 `tests\e2e\edhr-batch-pending-slot-tags-right-rail-static.spec.js` 静态契约。
- `node tests\e2e\edhr-batch-pending-slot-tags-right-rail-static.spec.js` -> RED FAIL，左侧仍包含槽位状态标签和缺失配置提示。
- `apply_patch` -> 从左侧待处理卡片移除槽位标签/缺失提示，在右侧待处理详情区域新增同源槽位状态标签和缺失配置提示。
- `node tests\e2e\edhr-batch-pending-slot-tags-right-rail-static.spec.js` -> GREEN PASS。
- `node tests\e2e\edhr-batch-pending-form-entry-static.spec.js` -> BLOCKER，当前工作区既有审批/批准待办动作契约断言失败；本次布局 diff 未触碰审批动作逻辑。
- `node --check tests\e2e\edhr-batch-pending-slot-tags-right-rail-static.spec.js` -> GREEN PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-edhr-pending-slot-tags-right-rail\frontend-feature-evidence.md` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-edhr-pending-slot-tags-right-rail --mode preview` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-edhr-pending-slot-tags-right-rail --mode apply` -> GREEN PASS，仅删除本任务临时证据文件。

# 2026-07-08 eDHR 详情页放行虚拟工序改造

## 用户需求

- 将 eDHR 详情页底部“收尾/放行归档”区域移除，改为左侧工序列表最后一个固定虚拟工序“放行”；选中“放行”后，中间展示放行摘要，右侧当前工序摘要栏展示原红框按钮作为放行工序参数。

## 执行命令

- `Get-Content -Encoding utf8 docs/powershell-memory.md` -> PASS，完成 PowerShell / UTF-8 门禁。
- `Get-Content -Encoding utf8 docs/experience-index.md` 与 `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` -> PASS，确认前端样式与经验门禁。
- `node tests/e2e/edhr-release-virtual-process-static.spec.js` -> RED FAIL，预期失败：详情页尚未定义 `RELEASE_VIRTUAL_PROCESS`、`selectReleaseProcess` 和放行虚拟工序。
- `apply_patch` -> 在 `BatchExecutionDetailPage.vue` 新增“放行”虚拟工序、放行摘要、右侧放行参数栏，并移除底部收尾区。
- `apply_patch` -> 更新收尾按钮、短文案和详情融合静态契约，新增放行虚拟工序静态契约。
- `node tests/e2e/edhr-release-virtual-process-static.spec.js` -> GREEN PASS。
- `node tests/e2e/edhr-closing-action-groups-static.spec.js` -> GREEN PASS。
- `node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> GREEN PASS。
- `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> GREEN PASS。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260708-edhr-release-virtual-process/frontend-feature-evidence.md` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-edhr-release-virtual-process --mode preview` -> GREEN PASS，无删除项、无阻塞。

# 2026-07-08 排产工单工具栏单行优化

## 用户需求

- 图里的 item 改成一行，排布好看些。

## 执行命令

- `Get-Content -Encoding utf8 docs/powershell-memory.md` -> PASS，完成 PowerShell / UTF-8 门禁。
- `Get-Content -Encoding utf8 docs/experience-index.md` 与 `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` -> PASS，确认前端页面 / 表格 / 样式门禁。
- `rg -n "排产编码|同步工单|手动重排|完成筛选|显示字段" yudao-ui-admin-vue3` -> PASS，定位排产工单页 `src/views/mes/pro/scheduleorder/index.vue`。
- `apply_patch` -> 将排产工单顶部查询表单改为单行 flex 布局，并将显示字段入口收进工具栏同排展示。
- `apply_patch` -> 新增 `doc/tasks/20260708-schedule-order-toolbar-single-line/` 任务文档、执行日志、前端证据，并更新工具栏静态契约。
- `node tests\e2e\mes-pro-schedule-order-toolbar-layout-static.spec.js` -> GREEN PASS。
- `node tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-schedule-order-toolbar-single-line\frontend-feature-evidence.md` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-schedule-order-toolbar-single-line --mode preview` -> GREEN PASS，无删除项。
- `git commit` -> BLOCKED，`src/views/mes/pro/scheduleorder/index.vue` 与 `tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` 存在本轮前置未提交重叠改动，无法安全只提交本任务 hunk。
- 用户补充需求：蓝框的 item 靠右排布。
- `apply_patch` -> 给 `完成筛选` 增加右侧起点 class，并将工具栏按钮组改为右对齐。
- `node tests\e2e\mes-pro-schedule-order-toolbar-layout-static.spec.js` -> GREEN PASS。
- `node tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-schedule-order-toolbar-single-line\frontend-feature-evidence.md` -> GREEN PASS。

# 2026-07-09 生产工单列表替换标准列表模板

## 用户需求

- 将生产订单列表替换成标准列表模板。

## 执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md` -> PASS，读取前端交付技能。
- `Get-Content -Encoding utf8 docs/powershell-memory.md` -> PASS，读取 PowerShell / UTF-8 门禁。
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` -> PASS，读取统一前端样式。
- `Get-Content -Encoding utf8 docs/experience-index.md` -> PASS，确认本任务命中 PowerShell 与前端页面 / 表格 / 样式门禁。
- `apply_patch` -> 创建 `doc/tasks/20260709-workorder-unified-list-template/` 任务文档、前端证据和生产工单标准模板静态验收用例。
- `node tests/e2e/mes-pro-workorder-unified-list-template-static.spec.js` -> RED FAIL，生产工单页尚未导入并使用 `UnifiedListTemplate`。
- `apply_patch` -> 将 `src/views/mes/pro/workorder/index.vue` 接入 `UnifiedListTemplate`，保留快速过滤、显示字段、列宽拖拽、分页和原业务动作。
- `apply_patch` -> 更新生产工单相关静态契约，适配通过标准列表模板渲染的工具栏与分页。
- `node tests/e2e/mes-pro-workorder-unified-list-template-static.spec.js`、`node tests/e2e/workorder-key-columns-static.spec.js`、`node tests/e2e/workorder-toolbar-red-box-cleanup-static.spec.js`、`node tests/e2e/unified-list-template-static.spec.js`、`node tests/e2e/user-table-column-config-static.spec.js`、`node tests/e2e/table-quick-filter-static.spec.js` -> GREEN PASS。
- `pnpm ts:check:schedule`、`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-workorder-unified-list-template/frontend-feature-evidence.md` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-workorder-unified-list-template --mode preview` -> GREEN PASS，无阻塞。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-workorder-unified-list-template --mode apply` -> GREEN PASS，删除临时前端证据文件，无阻塞。
- `git commit` -> BLOCKED，当前前端仓存在大量前置未提交改动，且本轮修改的生产工单页与相关静态测试存在文件级重叠；为避免混入非本轮改动，未创建提交。

# 2026-07-09 工序列表展示并筛选所属工艺路线

## 用户需求

- 工序设置列表里增加一列“属于哪些工艺路线”，一个工序可以属于多个工艺路线，并且可以通过工艺路线筛选，例如选择属于“压力泵”的所有工序。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、frontend/backend 交付技能与 closeout 规则 -> PASS。
- 创建 `doc/tasks/20260709-process-route-filter-column/`，记录 BDD、设计约束检查、经验门禁和执行日志 -> PASS。
- RED: `node tests/e2e/mes-pro-process-route-filter-static.spec.js` -> FAIL，前端缺少路线 VO、筛选项和“所属工艺路线”列。
- 实现结果：`src/views/mes/pro/process/index.vue` 新增“所属工艺路线”列和“工艺路线”快速筛选；`src/api/mes/pro/process/index.ts` 新增 `ProProcessRouteVO` 与 `routeList`。
- GREEN: `node tests/e2e/mes-pro-process-route-filter-static.spec.js`、`node tests/e2e/mes-pro-process-unified-list-template-static.spec.js` -> PASS。
- GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests。
- GREEN: 后端/前端 evidence 校验与 closeout preview -> PASS。
- Git commit -> BLOCKED，目标文件存在本轮前置未提交改动且本任务依赖这些改动，无法安全单独提交本任务 hunk。

# 2026-07-09 批次执行列表替换标准列表模板

## 用户需求

- 将批次执行列表替换成标准列表模板。

## 执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md` -> PASS，读取前端交付技能。
- `Get-Content -Encoding utf8 docs/powershell-memory.md` -> PASS，读取 PowerShell / UTF-8 门禁。
- `Get-Content -Encoding utf8 docs/experience-index.md` 与 `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` -> PASS，确认前端页面 / 表格 / 样式门禁。
- `apply_patch` -> 创建 `doc/tasks/20260709-edhr-batch-execution-unified-list-template/` 任务文档、前端证据和批次执行标准模板静态验收用例。
- `node tests/e2e/edhr-batch-execution-unified-list-template-static.spec.js` -> RED FAIL，批次执行列表尚未导入并使用 `UnifiedListTemplate`。
- `apply_patch` -> 将 `src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue` 接入 `UnifiedListTemplate`，保留快速过滤、显示字段、列宽拖拽、分页和原业务动作。
- `node tests/e2e/edhr-batch-execution-unified-list-template-static.spec.js`、`node tests/e2e/user-table-column-config-static.spec.js`、`node tests/e2e/edhr-rehearsal-readiness-panel-static.spec.js`、`node tests/e2e/edhr-readiness-business-action-static.spec.js`、`node tests/e2e/unified-list-template-static.spec.js`、`node tests/e2e/table-quick-filter-static.spec.js` -> GREEN PASS。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-edhr-batch-execution-unified-list-template/frontend-feature-evidence.md` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-edhr-batch-execution-unified-list-template --mode preview` -> GREEN PASS，预览仅删除临时前端证据文件。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-edhr-batch-execution-unified-list-template --mode apply` -> GREEN PASS，已删除临时前端证据文件，无阻塞。
- `git commit` -> BLOCKED，当前前端仓存在大量前置未提交改动；本任务依赖前置任务遗留未跟踪的 `src/components/UnifiedListTemplate/index.vue`，且修改文件与既有脏改存在文件级重叠，无法安全单独提交本任务 hunk。

# 2026-07-09 待同步差异列表替换标准列表模板

## 用户需求

- 将待同步差异列表表替换成标准列表模板。

## 执行命令

- `Get-Content -Encoding utf8 docs/powershell-memory.md` -> PASS，读取 PowerShell / UTF-8 门禁。
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md` -> PASS，读取前端交付技能。
- `Get-Content -Encoding utf8 docs/experience-index.md` 与 `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` -> PASS，确认前端页面 / 表格 / 样式门禁。
- `rg -n "待同步差异|入池状态|阻断原因|UnifiedListTemplate" src tests/e2e` -> PASS，定位待同步差异弹窗在 `src/views/mes/pro/scheduleorder/index.vue`。
- `apply_patch` -> 创建 `doc/tasks/20260709-schedule-order-admission-unified-list-template/` 任务文档、前端证据和待同步差异标准列表模板静态验收用例。

- `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js` -> RED FAIL，待同步差异列表尚未接入标准列表模板独立 table key。
- `apply_patch` -> 将 `src/views/mes/pro/scheduleorder/index.vue` 的“待同步差异”弹窗接入 `UnifiedListTemplate`，新增独立字段配置、列宽拖拽、快速过滤和显示字段配置。
- `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js`、`node tests/e2e/mes-pro-schedule-order-batch-admission-static.spec.js`、`node tests/e2e/mes-pro-schedule-order-pool-static.spec.js`、`node tests/e2e/unified-list-template-static.spec.js`、`node tests/e2e/user-table-column-config-static.spec.js` -> GREEN PASS。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> FAIL，PowerShell 不支持 Bash 风格环境变量前缀。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-admission-unified-list-template/frontend-feature-evidence.md` -> FAIL，证据缺少 RED/GREEN/Verification 固定标记；已补齐后准备重验。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-admission-unified-list-template/frontend-feature-evidence.md` -> GREEN PASS，Frontend feature evidence is valid。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-schedule-order-admission-unified-list-template --mode preview` -> GREEN PASS，预览仅删除临时前端证据文件。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-schedule-order-admission-unified-list-template --mode apply` -> BLOCKED，任务状态未被脚本识别；已补 `Status: completed` 后重跑。
- `apply_patch/Python UTF-8 write` -> 新增 `## Current Status` / `completed`，供 closeout 脚本识别任务完成状态。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-schedule-order-admission-unified-list-template --mode preview` -> GREEN PASS，预览仅删除临时前端证据文件。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-schedule-order-admission-unified-list-template --mode apply` -> GREEN PASS，已删除临时前端证据文件。
- `Git commit -> BLOCKED，当前前端仓存在 93 个脏改路径，且本任务修改的 `src/views/mes/pro/scheduleorder/index.vue` 与 `docs/request-command-log.md` 在本轮开始前已存在未提交改动；为避免混入非本任务 hunk，未创建提交。`
- 用户反馈：标准列表模板应支持列宽可调节，待同步差异列表当前列宽不可调节。
- `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js` -> RED FAIL，待同步差异表格未启用 `border`，且部分业务列未绑定持久化列宽。
- `apply_patch` -> 为待同步差异表格启用 `border`，并补齐工单编码、产品编号、产品名称、规格型号、不可排原因列的 `width` 持久化绑定。
- `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js` -> GREEN PASS。
- `node tests/e2e/mes-pro-schedule-order-batch-admission-static.spec.js`、`node tests/e2e/mes-pro-schedule-order-pool-static.spec.js`、`node tests/e2e/unified-list-template-static.spec.js`、`node tests/e2e/user-table-column-config-static.spec.js`、`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> GREEN PASS。

# 2026-07-09 工序设置红框内容可点击跳转

## 用户需求

- 工序设置列表红框内“批记录表单 / 生产填写人 / 质量填写人 / 设备填写人”均可点击。
- 批记录表单跳转到电子批记录模板查看页并按 `reportId` 精确过滤。
- 填写人按真实来源类型与 ID 跳转到权限角色、部门管理或用户管理，并在目标页过滤。

## 执行命令

- 创建 `doc/tasks/20260709-process-redbox-click-through/`，记录 BDD、经验门禁、设计约束和执行日志 -> PASS。
- RED: `node tests/e2e/mes-pro-process-redbox-click-through-static.spec.js` -> FAIL，缺少结构化字段类型与点击跳转处理。
- 实现结果：工序列表四列改为紧凑 link-style 可点击入口；批记录模板页支持 `reportId` 精确定位；角色/部门/用户页支持 query 初始化过滤和目标行高亮。
- GREEN: `node tests/e2e/mes-pro-process-redbox-click-through-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: frontend feature evidence 校验与 closeout preview -> PASS。
- Git commit -> BLOCKED，当前前端仓存在大量既有脏改，且本任务目标文件与既有改动存在文件级重叠，无法安全单独提交本任务 hunk。
# 2026-07-09 生产订单补齐工艺路线关联产品按钮

## 用户需求

- 在工艺路线详情“关联产品”页签红框位置新增按钮，点击后把生产订单中产品名称等于当前工艺路线名称的产品编号补齐到当前路线关联产品。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/agent-memory/project-error-prevention.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` -> PASS。
- 创建 `doc/tasks/20260709-route-product-bind-from-work-orders/`，记录 BDD、经验门禁、设计约束和前端证据 -> PASS。
- RED: `node tests/e2e/mes-pro-route-product-bind-from-work-orders-static.spec.js` -> FAIL，缺少按钮、API 类型和补齐调用契约。
- 实现结果：`RouteProductList.vue` 表格下方左侧新增 `从生产订单补齐产品` 按钮；`ProRouteProductApi` 新增 `bindFromWorkOrders`。
- GREEN: `node tests/e2e/mes-pro-route-product-bind-from-work-orders-static.spec.js` -> PASS。
- GREEN: `pnpm.cmd exec eslint src/views/mes/pro/route/RouteProductList.vue src/api/mes/pro/route/product/index.ts tests/e2e/mes-pro-route-product-bind-from-work-orders-static.spec.js --format stylish` -> PASS。

# 2026-07-09 标准列表模板空搜索触发重置

## 用户需求

- 标准列表模板里，如果点击搜索的时候输入栏是空的，那么执行重置的命令。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery` 与 `frontend-contract.md` -> PASS。
- 创建 `doc/tasks/20260709-standard-list-empty-search-reset/`，记录 BDD、设计约束、经验门禁和前端证据 -> PASS。
- RED: `node tests/e2e/table-quick-filter-static.spec.js` -> FAIL，旧 `useTableQuickFilter` 未在空输入查询时执行 `resetQuickFilter()`。
- 实现结果：`useTableQuickFilter.applyQuickFilter()` 在快速过滤输入为空时先执行 `resetQuickFilter()`；重置命令统一清理 `quickFilter` 和所有 `queryParamKey` 映射快速过滤参数，并回到第一页重新加载。
- GREEN: `node tests/e2e/table-quick-filter-static.spec.js` -> PASS。
- GREEN: `node node_modules/eslint/bin/eslint.js src/hooks/web/useTableQuickFilter.ts tests/e2e/table-quick-filter-static.spec.js` -> PASS。
- GREEN: 前端 evidence 校验与 task-closeout preview -> PASS。
- BLOCKER: `pnpm.cmd ts:check` -> FAIL，非本任务既有脏改 `src/views/mes/pro/route/RouteFlowGraphDesigner.vue` 存在 `NodeChange.id` 类型错误。
- BLOCKER: `node tests/e2e/unified-list-template-static.spec.js` -> FAIL，当前排产工单页结构已被其他任务改动，旧静态契约不属于本任务范围。
- 提交状态：当前前端仓存在多任务混合脏改且全量验证存在非本任务阻塞，本轮未提交。

# 2026-07-09 排产工单删除选中按钮

## 用户需求

- 删除截图红框选中的按钮，即排产工单页签工具栏中的 `同步工单`、`批量冻结`、`批量解冻`、`批量删除`。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery` 与 `frontend-contract.md` -> PASS。
- 创建 `doc/tasks/20260709-schedule-order-remove-selected-buttons/`，记录 BDD、设计约束、经验门禁和前端证据 -> PASS。
- RED: `node tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js` -> FAIL，排产工单页签工具栏仍渲染 `同步工单`。
- 实现结果：删除排产工单页签工具栏中的 `同步工单`、`批量冻结`、`批量解冻`、`批量删除` 按钮，并删除仅由这些按钮调用的入口函数。
- GREEN: `node tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> PASS。
- GREEN: `pnpm.cmd exec eslint src/views/mes/pro/scheduleorder/index.vue tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js --format stylish` -> PASS。

# 2026-07-09 工艺路线流转图工序可见 Item 持久化

## 用户需求

- 工艺路线“流转关系图”左侧工序详情添加/删除的可见 item，要按当前用户保存；同一用户下次打开、换电脑打开仍一致，不同用户互不影响。
- 任意工序上添加/删除可见 item 后，其他工序也显示同一组 item，但内容按当前工序加载。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery` 与 `frontend-contract.md` -> PASS。
- 创建 `doc/tasks/20260709-route-flow-detail-visible-items/`，记录 BDD、设计约束、经验门禁和前端证据 -> PASS。
- RED: `node tests/e2e/mes-route-flow-detail-visible-items-static.spec.js` -> FAIL，流转关系图尚未接入服务端用户配置。
- 实现结果：`RouteFlowGraphDesigner.vue` 使用 `mes.pro.route.flow.detailFields` 复用 `/system/user-table-column-config/get|save`；添加/删除 item 立即保存，保存失败回滚并禁用继续修改；可见 item 不再写入 URL query 或本地缓存。
- GREEN: `node tests/e2e/mes-route-flow-detail-visible-items-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-selectable-detail-fields-static.spec.js`、`node tests/e2e/mes-route-flow-link-return-state-static.spec.js`、`node tests/e2e/mes-route-flow-default-first-field-static.spec.js`、`node tests/e2e/mes-route-flow-selected-process-detail-static.spec.js`、`node tests/e2e/mes-route-flow-graph-static.spec.js` -> PASS。
- GREEN: `node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/RouteFlowGraphDesigner.vue tests/e2e/mes-route-flow-detail-visible-items-static.spec.js tests/e2e/mes-route-flow-link-return-state-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。
- GREEN: 前端 evidence 校验与 task-closeout apply -> PASS，已清理临时 `frontend-feature-evidence.md`。
- GREEN: 登录前置检查 -> PASS，测试租户 `aoteman` 已通过本机 `http://localhost:8081` 真实登录进入工艺路线页。
- GREEN: `node tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js` -> PASS，真实路径验证添加“批记录表单”、切换工序、刷新页面、新浏览器上下文重新登录后配置仍按服务端用户配置恢复。
- GREEN: `node node_modules\eslint\bin\eslint.js tests\e2e\mes-route-flow-detail-visible-items-real.e2e.js` -> PASS。
- Git commit -> BLOCKED，当前前端仓存在大量既有脏改，且本任务目标文件与前置任务存在文件级重叠；为避免混入非本任务 hunk，未创建提交。

# 2026-07-10 DCC项目代码批量 AI 分类按钮

## 用户需求

- 在 `基础数据 / DCC项目代码` 列表页红框位置增加 `批量AI分类` 按钮。
- 点击后对一百多个项目逐个执行现有 AI 分类，一个项目分类完再进行下一个。
- 按钮点击后显示进度条，让用户看到已分类几个项目。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`docs/agent-memory/project-error-prevention.md`、`frontend-feature-delivery` 与 `frontend-contract.md` -> PASS。
- 创建 `doc/tasks/20260710-dcc-project-code-batch-ai-category/`，记录 BDD、设计约束、经验门禁和前端证据 -> PASS。
- RED: `pnpm.cmd e2e:dcc:project-code-batch-ai-category:static` -> FAIL，缺少工具栏 `批量AI分类` 按钮。
- 实现结果：`ProjectCodeTabPanel.vue` 在导出按钮右侧新增 `批量AI分类`，运行时显示 `el-progress` 项目级进度；按 `pageSize=200` 拉取全部项目代码，逐项目、逐文件串行复用现有 AI 分类 API；空项目计入已处理，失败项目继续后续并汇总项目、文件和后端错误。
- GREEN: `pnpm.cmd e2e:dcc:project-code-batch-ai-category:static` -> PASS。
- GREEN: `pnpm.cmd e2e:dcc:project-code-associated-three-column:static` -> PASS。
- GREEN: `pnpm.cmd e2e:dcc:project-code-ai-category-permission:static` -> PASS。
- RED: `pnpm.cmd ts:check` -> FAIL，Node 默认 4GB 堆内存 OOM。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。
- GREEN: 前端 evidence 校验与 task-closeout preview -> PASS，delete 为空。
- Git commit: `任务: 增加DCC项目代码批量AI分类` -> PASS。

# 2026-07-10 批量 AI 分类按钮不可见回归修复

## 用户反馈

- `批量AI分类 我没看到这个按钮`

## 执行命令

- 核对运行中的 Vite 模块与源码 -> PASS，均包含 `批量AI分类`。
- 官方登录 preflight -> PASS，测试租户 `aoteman` 真实进入 `/mdm/project-code`。
- RED: 真实可见性探针 -> FAIL，缺少 `dcc:project-code:update` 和 `dcc:controlled-file:update`，按钮数量为 0。
- RED: 数据库只读检查 -> FAIL，当前运行库不存在两个权限菜单。
- RED: 两项权限静态契约 -> FAIL，前端任一权限匹配与后端 AND 权限契约不一致。
- 修复：前端新增 `canRunAiCategory` 同时校验两项权限；本机测试租户事务新增两个权限菜单，并给 3 个来源角色新增 6 条授权，其他租户新增记录为 0。
- GREEN: 真实可见性探针 -> PASS，按钮数量为 1 且可见。
- GREEN: 批量 AI 分类、AI 分类权限、关联文档三栏静态契约 -> PASS。
- GREEN: 本任务相关 ESLint、TypeScript 检查、缺陷证据、数据库证据与前端证据校验 -> PASS。
- GREEN: task-closeout preview/apply -> PASS，已清理一次性探针、临时截图和辅助证据文件。
- REGRESSION: `e2e:dcc:project-code-basic-data:static` 仍按旧筛选表单断言，受当前工作区标准列表模板改造影响，与本次修复无关。
- Git commit: `修复: 恢复DCC批量AI分类按钮权限` -> PASS。

# 2026-07-10 工序侧栏字段级刷新

## 用户需求

- 点击工序卡片时不要刷新红框内整个左侧侧栏，只刷新需要更新的字段内容。
- 加载效果采用字段值区域骨架；普通详情和关联设备独立更新；快速切换只显示最后选中工序的数据。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、统一前端样式、前端交付、缺陷修复和 Playwright 门禁 -> PASS。
- 检查 `RouteFlowGraphDesigner.vue`、相关静态测试、真实 E2E 和最近任务状态 -> PASS，确认目标组件当前无未提交改动。
- 创建 `doc/tasks/20260710-route-flow-detail-partial-refresh/`，记录 BDD、设计约束、经验门禁和前端/缺陷证据 -> PASS。
- RED: `node tests/e2e/mes-route-flow-detail-partial-refresh-static.spec.js` -> FAIL，当前侧栏仍使用整体 loading 遮罩。
- 实现字段值骨架、普通详情/设备独立 loading 和请求序号保护 -> PASS。
- GREEN: 新增静态契约及工序详情、字段持久化、返回状态回归测试 -> PASS。
- GREEN: experience-preflight -> PASS，真实 E2E 限定本机、测试租户和真实接口响应。
- GREEN: official-login-preflight -> PASS，测试租户 `aoteman` 真实进入工艺流程页。
- 首次真实 E2E 超时：重复点击当前工序未触发请求；修正为先切回其他工序。
- 第二次真实 E2E 失败：字段选择器文本误判字段已显示；修正为按 `data-flow-detail-field` 键判断。
- GREEN: `node tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js` -> PASS，验证字段级骨架、独立更新和过期响应保护。
- GREEN: 目标 ESLint、TypeScript、前端证据、缺陷证据和视觉复核 -> PASS。
- GREEN: task-closeout preview/apply -> PASS，仅保留任务记录并清理本任务证据与临时截图。
- Git commit: `任务: 工序侧栏字段级刷新` -> PASS。

# 2026-07-10 排产工作台班次产能整数显示

## 用户需求

- 班次产能的数字按整数显示。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、项目防错经验、统一前端样式和缺陷修复门禁 -> PASS。
- 定位截图对应 `src/views/mes/pro/scheduler-workbench/index.vue` 的“班次产能”列 -> PASS，当前使用最多保留 6 位小数的 `formatNumber`。
- 创建 `doc/tasks/20260710-mes-shift-capacity-integer-display/`，记录 BDD、设计约束、经验门禁和缺陷证据 -> PASS。
- RED: `node tests/e2e/mes-scheduler-workbench-process-wip-short-titles-static.spec.js` -> FAIL，班次产能列未使用整数格式化函数。
- GREEN: experience-preflight -> PASS，真实页面验证限定本机、测试租户和只读工序列表。
- 修复：班次产能列复用既有 `formatIntegerNumber`，按 `zh-CN` 四舍五入为带千分位的整数。
- GREEN: 目标静态测试、统一列表模板回归、夜班开排日期回归、目标 ESLint 和 `pnpm.cmd ts:check` -> PASS。
- REGRESSION: `mes-scheduler-workbench-process-wip-controls-static.spec.js` 仍要求已移除的顶部排产说明区存在，与本次整数格式修复无关。
- GREEN: 官方登录预检使用系统 Chrome 后通过；测试租户真实页面检查 20 个班次产能值均为无小数点整数。
- GREEN: task-closeout preview/apply -> PASS，仅保留独立任务记录并清理缺陷证据与截图。

# 2026-07-10 排产工作台白夜班与 X2 展示

## 用户需求

- 选择夜班之后，班次状态变成“白夜班”。
- 班次产能数值后显示绿色 `X2`。

## 执行命令

- 复用排产工作台班次产能整数展示改动，创建并扩展 `doc/tasks/20260710-mes-process-shift-capacity-integer/` 任务记录 -> PASS。
- RED: `node tests/e2e/mes-scheduler-workbench-process-wip-double-shift-static.spec.js` -> FAIL，现有页面缺少“白夜班”映射和绿色 `X2`。
- 修复：以 `nightShiftEnabled/nightShiftMixed` 统一生成班次状态；有效白夜班在班次产能后显示绿色 `X2`，不对产能数值二次乘二；班次状态快速过滤复用同一口径。
- GREEN: 双班静态契约、班次产能整数、夜班开排日期、工作台静态回归、目标 ESLint 和 `pnpm.cmd ts:check` -> PASS。
- GREEN: 官方登录预检使用系统 Chrome 进入本机测试租户排产员工作台 -> PASS。
- GREEN: 测试租户工序 `B050` 真实开启夜班后显示“白夜班”和绿色 `X2`，结束时恢复原夜班状态 -> PASS。
- REGRESSION: `mes-scheduler-workbench-process-wip-controls-static.spec.js` 仍要求已删除的顶部排产说明区存在，与本次改动无关。
# 2026-07-10 eDHR 工序卡片密度与展示序号调整

## 用户需求

- 更改批记录详情左侧工序卡片的显示样式，信息尽量紧凑，每个卡片高度不要太高，让用户一眼看到更多信息。
- 灭菌报告、成品检报告、成品检记录的蓝框序号改为 `90`、`91`、`92`。
- 放行工序的蓝框序号改为 `99`。
- 左侧所有工序序号文字改为黑色。

## 执行命令

- 继续 `doc/tasks/20260710-edhr-batch-process-card-density/`，确认追加需求与当前工序卡片视觉优化属于同一页面范围 -> PASS。
- RED: `node tests/e2e/edhr-batch-process-card-density-static.spec.js` -> FAIL，当前待处理工序卡片仍为 `72px` 最小高度，列表间距仍为 `8px`。
- 修复：左侧列表间距收紧到 `6px`；待处理卡片收紧为 `48px`；已填写卡片改为紧凑双列布局，状态标签右侧对齐，长文本单行省略。
- 定位 `resolvePendingTaskSortText`、`RELEASE_VIRTUAL_PROCESS.sort` 和 `.edhr-batch-detail__process-sort` -> PASS。
- RED: `node tests/e2e/edhr-batch-process-display-sort-static.spec.js` -> FAIL，特殊节点缺少 `90/91/92` 展示序号映射。
- 修复：特殊节点展示序号映射为 `90/91/92`，放行虚拟工序显示 `99`，左侧所有序号文字改为黑色。
- GREEN: 紧凑卡片、展示序号、待处理标题单行、头部上下文、左栏宽度和工序索引回归静态测试 -> PASS。
- GREEN: 目标 ESLint、8GB Node 堆内存 TypeScript、前端证据校验和 `git diff --check` -> PASS。
- GREEN: 官方登录预检使用系统 Chrome 进入本机 `芋道源码/admin` 批次执行页 -> PASS。
- GREEN: 本机 `芋道源码/admin` 只读打开 `881MO090889`，18 个待处理工序卡片高度均为 `48px`，2 个已填写工序卡片高度均为 `54px`，业务写请求为 0。

# 2026-07-10 产品主数据列表列宽拖拽

## 用户需求

- 产品主数据下的列表需要可以调整列宽。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、统一前端样式、前端交付和缺陷修复门禁 -> PASS。
- 核对 `20260710-mdm-product-standard-list` 已完成，并确认当前产品主数据页面保留一处无关未提交的空搜索重置改动 -> PASS。
- 对照已验证的 DCC 产品目录列宽修复，定位产品主数据页面缺少明确表头拖拽热区和末列拖拽配置 -> PASS。
- 创建 `doc/tasks/20260710-mdm-product-column-resize/`，记录 BDD、设计约束、经验门禁和前端/缺陷证据 -> PASS。
- RED: `node tests/e2e/mdm-product-unified-list-template-static.spec.js` -> FAIL，产品主数据表格缺少显式列宽拖拽入口配置。
- 修复：主表增加可识别拖拽热区、`border` 和 `:allow-drag-last-column="true"`，继续复用 `handleProductHeaderDragend` 与 `mdm.product.main` 持久化 -> PASS。
- GREEN: experience-preflight -> PASS，真实页面验证限定本机测试租户，并在结束时恢复列宽配置。
- GREEN: 产品主数据目标静态测试、列配置回归、目标 ESLint 和 `pnpm.cmd ts:check` -> PASS。
- GREEN: 官方登录预检进入本机测试租户 `/mdm/product` -> PASS。
- GREEN: 真实 Playwright 将产品编码列 150px 拖至 230px，刷新后仍为 230px，随后恢复到 150px；保存接口 HTTP 200。
- GREEN: 1280×800 只读验证无整页横向溢出，拖拽热区仍为 `col-resize`。
- GREEN: 前端交付证据校验与缺陷回归证据校验 -> PASS。
- GREEN: task-closeout preview/apply -> PASS，仅保留任务记录并清理一次性证据与真实 E2E 探针。

# 2026-07-10 eDHR 工序辅助表单联动填写

## 用户需求
- 工艺路线工序配置损耗单、过程检验单、参数记录表后，填写该工序批记录时必须同时完成对应辅助表单。

## 执行命令
- 创建 `doc/tasks/20260710-edhr-process-companion-forms/`，记录同工序表单分组、槽位状态、精确打开和返回批次上下文的 BDD/TDD 计划。
- RED：`node tests/e2e/edhr-batch-process-companion-forms-static.spec.js` -> FAIL，批次详情仍逐条渲染任务。
- 实现：批次详情按 `routeProcessId` 分组，主表/损耗单/过程检验单/参数记录表分别显示状态、门禁和打开入口；执行页返回原 `batchExecutionId/batchTaskId`。
- GREEN：目标静态契约、既有卡片密度/显示顺序回归和 `pnpm ts:check` -> PASS。
- GREEN：官方登录预检分别进入测试租户和管理员批次页面；管理员只读结构性 E2E 验证工序分组、任务聚焦、返回上下文，MES 写请求为 0。
- DATA BLOCKER：只读 SQL 确认当前全部租户均无多槽位历史工序任务，严格四槽位真实 E2E 按缺少数据失败，未创建或模拟数据。

# 2026-07-10 eDHR 批记录详情主区域填满

## 用户需求
- 下面不要留空间，让主区域填满。

## 执行命令
- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、统一前端样式和前端交付门禁 -> PASS。
- 检查上一前端任务 `20260710-edhr-process-companion-forms` 仍为进行中；已记录为 blocked，避免两个同页布局任务交叉修改。
- 创建 `doc/tasks/20260710-edhr-batch-main-area-fill/`，记录主区域填满、三列同步拉满、内部滚动和移动端自然布局的 BDD/TDD 计划。

# 2026-07-10 eDHR 工序辅助表单移至右侧详情

## 用户需求
- 主生产表、损耗单、过程检验单、参数记录表应根据当前工序实际配置显示在右侧详情；左侧只负责工序导航，不同工序允许不同组合或没有表单。

## 执行命令
- 创建 `doc/tasks/20260710-edhr-companion-forms-right-panel/`，记录右侧动态表单列表的 BDD/TDD 计划。
- RED：`node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js` -> FAIL，左侧仍展开同工序表单任务。
- 实现：移除左侧表单展开项；右侧按选中任务所属 `routeProcessId` 展示该工序实际表单、状态、门禁和打开入口。
- GREEN：新旧辅助表单契约、卡片密度、显示顺序、主区域填满和 `pnpm ts:check` -> PASS。
- GREEN：管理员只读真实批次 `900000000480` 页面验证通过，左侧仅工序、右侧显示真实主生产表，MES 写请求为 0。
- DATA BLOCKER：当前真实批次不存在多槽位工序，未创建或模拟数据；动态组合由静态契约覆盖。

# 2026-07-10 排产员工作台增加排产逻辑页签

## 用户需求

- 排产员工作台增加一个页签，向不懂代码的用户说明排产算法逻辑。
- 说明尽量通俗、简短，可使用编号、流程图或其他普通用户容易理解的形式。

## 执行命令

- 读取 PowerShell、项目经验、统一前端样式和前端交付门禁，核对前序前端任务已完成 -> PASS。
- 定位排产员工作台现有页签和后端真实排产规则：排产前置、交期与优先级顺序、工艺顺序、产线选择、班次产能、现场任务保护和预览应用 -> PASS。
- RED: `node tests/e2e/mes-scheduler-workbench-algorithm-guide-tab-static.spec.js` -> FAIL，现有页面缺少“排产逻辑”页签。
- 修复：新增“排产逻辑”页签，以 1-7 步短句说明排产主流程，并补充开始时间、数量时长和结果查看三项说明。
- GREEN: 目标静态契约、排产员工作台回归、工序标准列表回归和 `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS。
- REGRESSION BLOCKER: 当前 HEAD 的策略设置静态测试已缺少 `policySettingsForm.defaultNightShiftEnabled` 绑定，属于本任务前已存在的独立失败。
- GREEN: 官方登录预检使用系统 Chrome 进入本机测试租户排产员工作台 -> PASS。
- GREEN: 真实页面点击“排产逻辑”后关键说明全部可见，无横向溢出，MES 写请求为 0。
## 2026-07-10 工艺路线开始/结束节点可点击与可连线

- 用户需求：工序开始和工序结束可以点击并设置连接线；开始允许多条出口，结束只允许一条入口；本次先完成点击与连线，属性后续扩展。
- 执行：创建成对隔离分支 `codex/20260710-route-flow-boundary-links`，前端 worktree 使用端口 `8094`，后端使用 `48094`。
- 门禁：同组件在途任务 `20260710-route-flow-detail-partial-refresh` 已显式阻塞，开始 BDD + 严格 TDD。
- RED：边界静态契约因缺少 `boundaryEdges` 和边界选择状态而失败。
- 实现：START/END 支持点击选中和只读摘要；边界关系进入真实草稿、关系清单、连接线选择、删除、保存、刷新和自动布局；普通工序允许多前置、最多一个后续。
- GREEN：边界/图静态契约、定向 ESLint、8GB TypeScript 检查通过。
- GREEN：官方登录 preflight 通过，测试租户 `aoteman` 进入隔离前端 `8094`。
- GREEN：真实 Playwright 在路线 `RT000017` 完成双 START 分支汇合、唯一 END、非法第二出边/入边拒绝、保存刷新和 API 持久化断言；最终通过页面恢复原拓扑。

# 2026-07-10 eDHR 批次工序顺序与列表显示修复

## 用户需求

- 修复批次详情左侧普通工序显示在“成品检记录（92）”之后的问题。
- 修复工序编码、工序名称和表单名称在窄栏中重叠、遮挡的问题。

## 执行命令

- 读取 PowerShell、缺陷回归、前端交付和统一前端样式门禁，核对当前组件并行改动归属。
- 创建 `doc/tasks/20260710-edhr-batch-process-order-layout/`，记录排序、放行位置和文本布局的 BDD/TDD 计划。
- RED：`node tests/e2e/edhr-batch-process-order-layout-static.spec.js` -> FAIL，页面尚未拆分前置和收尾特殊节点。
- 修复：来料检单独显示在普通工序前；灭菌、成品检报告和成品检记录显示在普通工序后、放行前；工序编码和名称改为单行省略并保留完整提示。
- GREEN：目标静态契约、显示序号回归、卡片密度回归和 `pnpm.cmd ts:check` -> PASS。
- GREEN：官方登录预检使用本机测试租户进入 eDHR 批次执行列表 -> PASS。
- BLOCKER：测试租户批次列表为空，当前环境未配置 `芋道源码/admin` 只读登录凭据，实际有数据页面最终验收及自动提交暂缓。
- INTEGRATION：目标组件修复已随同页并行任务提交 `99be6babb` 进入当前 `int_main`；本任务回归测试和阻塞证据保持未提交。
- RESOLVED：用户提供本机管理员凭据，`芋道源码/admin` 官方登录预检通过。
- GREEN：真实批次 `900000000480` 只读页面确认顺序为来料检、普通工序 1-14、90、91、92、99，工序文本无重叠，MES 写请求为 0。
- COMMIT：回归测试和任务实施记录已提交 `a7e099710`；目标页面修复位于提交 `99be6babb`。
- GREEN：`task-closeout-cleanup` preview/apply -> PASS，临时任务产物已清理，正式任务记录已保留并更新为 `completed`。

# 2026-07-10 eDHR 左侧工序卡片等高与名称显示

## 用户需求

- 左侧工序列表卡片高度被压缩遮挡。
- 所有 item 大小保持一致并完整显示。
- 普通工序显示工序名称，不显示工序编号或编码。

## 执行命令

- 创建 `doc/tasks/20260710-edhr-batch-process-item-uniform-name/`，记录等高、防 flex 压缩和名称显示的 BDD/TDD 计划。
- 新增 `tests/e2e/edhr-batch-process-item-uniform-name-static.spec.js` 作为 RED 回归测试。
- RED：普通工序仍拼接编码和名称，且左侧列表缺少统一高度、防压缩和 240px 完整显示契约。
- 修复：所有节点统一 48px，禁止普通工序与放行节点 flex 收缩；普通工序只显示 `processName`；左栏调整为 240px。
- GREEN：6 个目标静态回归、Node 语法、目标 ESLint、TypeScript 和 `git diff --check` 通过。
- GREEN：本机 `芋道源码/admin` 真实批次 `900000000480` 只读 E2E 验证 19 个节点均为 48px，工序名称完整显示，MES 写请求为 0。
- COMMIT：本任务实现、回归测试与正式任务记录已提交 `615da58ac`。
- GREEN：`task-closeout-cleanup` preview/apply -> PASS，临时证据和 E2E 输出已清理，并行性能诊断脚本已保留。
- GREEN：任务状态更新为 `completed`。

# 2026-07-10 eDHR 批次工序状态背景优化

## 用户需求

- 红框中的“0/1 已完成”等完成计数不显示。
- 已完成工序背景使用淡绿色。
- 正在填写工序背景使用淡黄色。
- 尚未开始工序保持当前白色背景。

## 执行命令

- 读取 PowerShell、缺陷回归、前端交付和统一前端样式门禁，检查上一任务状态与目标组件并行改动。
- 创建 `doc/tasks/20260710-edhr-batch-process-state-background/`，记录 BDD、严格 TDD 和验证范围。
- RED：`node tests/e2e/edhr-batch-process-state-background-static.spec.js` -> FAIL，页面仍显示完成计数且没有状态背景类。
- 修复：移除普通工序完成计数标签，按任务状态统一生成 `is-completed`、`is-in-progress`、`is-not-started` 状态类。
- 样式：已完成背景为淡绿色 `#f0f9eb`，正在填写背景为淡黄色 `#fff8e6`，未开始保持 `#f7f9fc`。
- GREEN：状态背景静态契约、辅助表单、卡片密度、工序顺序、统一名称和 TypeScript 检查 -> PASS。
- GREEN：官方管理员登录预检和真实批次 `900000000480` 只读 E2E -> PASS，页面无完成计数，粗洗工序为淡黄色，其余未开始工序保持原背景，MES 写请求为 0。
- COMMIT：功能代码、回归测试和实施记录已提交 `5c53e06b8`。
- GREEN：`task-closeout-cleanup` preview/apply -> PASS，临时证据已清理，正式任务记录保留并更新为 `completed`。

# 2026-07-10 排产员工作台动态重排说明页签

## 用户需求

- 每次成功重排后更新“排产逻辑”页签的具体数值。
- 不只展示物料需求、库存和短缺，还要展示订单顺序、工序、班次产能、受保护任务、问题和最终任务结果。

## 执行

- 创建独立前端 worktree 与任务文档，按 BDD + 严格 TDD 将静态说明页签升级为权威动态说明。

# 2026-07-10 eDHR 批次详情加载耗时诊断

## 用户需求

- 解释进入批次详情为什么加载很久。

## 执行命令

- 读取批次详情前端加载链路、后端详情/工作台/复盘时间线实现及既有真实页面诊断证据。
- 使用本机真实登录和真实批次只读路径测量首屏请求耗时，不产生 MES 写请求。
- 诊断结论：页面串行等待三个聚合接口；详情接口进入页面即同步整批状态并逐任务计算；复盘时间线对每个已打开执行记录分别查询签名、字段审计、审批快照、主数据追溯和附件，并返回完整表单 JSON。
- 实测阻塞：测试租户当前批次列表为空，旧批次已不存在；未猜测或复用管理员密码，因此未输出虚构耗时数字。

# 2026-07-10 eDHR 收尾节点卡片样式统一

## 用户需求

- 来料检之后的灭菌报告、成品检报告、成品检记录和放行 item 与普通工序 item 风格保持一致。

## 执行命令

- 创建 `doc/tasks/20260710-edhr-post-process-item-style/`，记录卡片样式统一的 BDD/TDD 计划。
- 新增 `tests/e2e/edhr-batch-post-process-item-style-static.spec.js`，覆盖间距、边框、圆角、背景、字重、选中态以及放行节点虚线/渐变清理。
- RED：收尾特殊节点仍有额外分组下边距，放行节点仍保留独立装饰样式。
- 修复：特殊节点与放行节点统一使用普通工序的浅灰蓝紧凑卡片、6px 间距、6px 圆角、48px 高度和蓝色选中外框。
- GREEN：目标静态测试、统一高度回归、状态背景回归、目标 ESLint 和 TypeScript 检查均通过。
- BLOCKER：本机 8081 未运行；尝试启动前端时当前 `node_modules` 缺失 `@babel/helper-validator-identifier`，按锁文件恢复仍未修复，无法完成真实页面只读截图验收；本任务不提交，等待依赖恢复或可用运行态。
- 用户补充截图：红框内灭菌报告、成品检报告和成品检记录仍呈连续分组样式，与普通工序独立卡片不一致。
- 调整方案：移除收尾节点的独立分组结构，直接复用普通工序卡片容器和按钮结构。
- RED：静态契约确认收尾节点仍由 `pending-task-list` 分组渲染。
- 修复：90、91、92 三个节点改为与普通工序同级渲染，并直接复用 `process-task-group` / `process-task-group-head`。
- GREEN：样式统一测试、卡片高度回归、状态背景回归、ESLint 和 TypeScript 均通过。
- BLOCKER：本机 Vite 虽短暂监听 8081，但登录页加载连续超时且进程随后退出，无法补真实页面截图；本任务未提交。
- 用户补充截图：来料检报告仍使用旧卡片结构，与其他已经统一的工序 item 不一致。
- 调整方案：来料检报告也改为与普通工序同级渲染并复用相同卡片容器和按钮结构。
- RED：来料检报告仍由旧的前置节点分组结构渲染。
- 修复：来料检报告改为与普通工序同级，并直接复用同一 `process-task-group` / `process-task-group-head`。
- GREEN：特殊节点样式统一测试、卡片高度回归、状态背景回归、ESLint 和 TypeScript 均通过。

# 2026-07-10 eDHR 主生产表打开填写直达

## 用户需求

- 红框内“打开填写”点击后，先打开或复用当前工序执行记录，再直接进入主生产表填写页。
- 再次进入时继续同一执行记录，不跳转批次详情或通用详情页。

## 执行命令

- 继续 `doc/tasks/20260710-edhr-batch-context-carrier-header/`，补充直达填写页的 BDD、严格 TDD 和前端证据。
- RED：表单卡片仍调用旧入口，未统一使用当前填写载体。
- 修复：表单卡片改为调用 `handleSelectedPendingTaskAction(task)`；普通工序先打开或复用执行记录，再跳转 `/mes/pro/feedback/edhr-execution/form`。
- GREEN：目标静态契约、右侧辅助表单、主区域高度、待填写入口、ESLint 和 TypeScript 检查通过。
- BLOCKER：本机 8081 存在 Vite 监听进程但 HTTP 无响应，官方登录预检与直接 HTTP 探测均超时，无法完成真实页面点击验证；未提交改动。
- 用户复测反馈：访问后仍显示“eDHR 执行详情”。
- 根因定位：详情路由与填写路由共用 `ExecutionPage.vue`，页面标题和详情证据区未按路由模式区分。
- 修复：`ExecutionPage.vue` 增加填写路由模式，表单路由标题改为当前报表名称加“填写”，隐藏执行摘要、技术证据、归档、快照和审计页签。
- GREEN：目标静态测试、详情页回归、工作任务上下文、ESLint 和 TypeScript 检查通过。
- GREEN：重启本机 8081 后根页面和 `main.ts` 返回 HTTP 200，官方登录预检进入测试租户批次执行页。
- BLOCKER：测试租户批次列表为空，无法取得既有执行记录完成只读表单模式 E2E；未创建模拟数据，未提交改动。

# 2026-07-10 批次执行工艺流程跳转

## 用户需求

- 在批次执行详情的批记录预览顶部黄框区域显示当前批次执行对应的工艺流程。
- 工艺流程名称可以点击，并打开该批次关联的工艺流程。

## 执行命令

- 创建 `doc/tasks/20260710-edhr-batch-process-route-link/`，记录经验门禁、BDD、严格 TDD 和前端证据。
- 定位黄框为 `BatchExecutionDetailPage.vue` 的批记录预览顶部中间区域。
- 核对批次详情的 `routeId/routeName/routeCode` 与 `MesProRouteEdit` 的 `flow` 页签跳转契约。

# 2026-07-10 删除 eDHR 执行详情页

## 用户需求

- 删除不再使用的“eDHR 执行详情”页面，避免旧页面和旧路由继续误导后续使用与开发。

## 执行命令

- 继续 `doc/tasks/20260710-edhr-batch-context-carrier-header/`，补充删除详情页的 BDD 与严格 TDD 验证。
- RED：新增详情路由删除契约，确认旧详情路由、遗留入口和隐藏详情内容仍存在。
- 修复：删除详情路由及详情型 UI；执行列表、追踪、审计、追溯和变更入口统一改到执行表单，签名与归档使用独立页面。
- 清理：删除旧详情专属测试，更新仍有效的表单、追踪、证据入口和变更功能契约。
- GREEN：目标静态测试与 ESLint 通过。
- BLOCKER：全量类型检查被既有 DCC `classificationStatus` 类型错误阻塞；测试租户仍无批次数据，未提交。

# 2026-07-10 排产前检查 / 手动重排隐藏浮动列控件

## 用户需求

- “排产前检查 / 手动重排”界面红框中的“显示字段 / 重置”内容不以浮窗显示。

## 执行命令

- 创建隔离前端 worktree `r260710rfc`，按 BDD + 严格 TDD 新增浮动列控件回归测试。
- RED：两张诊断表缺少 `data-user-table-column-explicit`，会被全局增强器挂载浮动控件。
- 修复：预检问题表和重排问题表增加显式排除标记，不修改全局增强器和主列表显示字段功能。
- GREEN：目标静态测试、列配置回归、工具栏回归、ESLint、排产 TypeScript 和真实 Playwright 验证通过。
- 真实验证：测试租户选择 5 条排产工单，预检返回 7 条真实问题；诊断表 `explicit=true`、`globalKey=null`、`managed=false`，未调用应用重排接口。
- 融合：实现提交 `7b8f807e7` 已快进进入前端 `int_main`。
- 收尾：task-closeout preview 因主工作区存在其他任务脏改而阻塞，任务保持 `ready_for_closeout`，worktree 暂不删除。

# 2026-07-10 批次详情标签切换缓存

## 用户需求

- 批次详情页切走后再切回来不应每次重新加载。

## 执行命令

- 创建 `doc/tasks/20260710-edhr-batch-detail-tab-cache/`，记录经验门禁、BDD、严格 TDD 与前端证据。
- RED：批次详情路由配置为 `noCache: true`，标签切换返回时组件被重新创建。
- 二次 RED：仅加入缓存后，路由 query watcher 在切走与返回时仍重复调用加载。
- 修复：批次详情路由加入 `keep-alive` 缓存；watcher 忽略非详情路由和已加载的同一批次，仅在批次标识实际变化时重新加载。
- GREEN：目标静态测试、ESLint、官方登录预检和真实 Playwright 标签切换通过；切走前后详情、工作台与时间线请求计数不变，MES 写请求为 0。
- BLOCKER：全量 TypeScript 检查被 `ExecutionPage.vue` 两处既有错误阻塞，本任务目标文件未报错。

# 2026-07-10 页签缓存同类问题审计

## 用户需求

- 确认其他页签是否也存在切走再切回重复加载的问题。

## 执行命令

- 创建 `doc/tasks/20260710-tab-cache-similar-issue-audit/`，按路由缓存、组件挂载加载和路由 watcher 组合进行静态审计。
- 扫描解析 84 个页面路由，识别 31 个详情/编辑/表单类路由。
- 结果：22 个详情/编辑类页面为 `noCache: true` 加载型页面；eDHR 范围内确认 8 个同类页面。
- 重点：执行表单、主数据追溯详情、字段审计详情除禁用缓存外，还存在未受限路由 watcher，不能只改 `noCache`。
- 本次仅审计，未扩大修改其他页面。

# 2026-07-10 eDHR 页签缓存一致性修复

## 用户需求

- 解决审计发现的 eDHR 页面切走再切回重复加载问题。

## 执行命令

- 创建 `doc/tasks/20260710-edhr-tab-cache-consistency-fix/`，记录经验门禁、BDD、严格 TDD 与真实 E2E。
- RED：新增缓存一致性静态合同，确认目标路由仍为 `noCache: true`。
- 修复：8 个目标路由启用 keep-alive；批次复盘使用独立命名壳组件；执行表单、主数据追溯详情、字段审计详情增加路由与已加载键防重。
- GREEN：目标静态测试、ESLint、TypeScript 检查通过。
- GREEN：本机 `芋道源码/admin` 只读 Playwright 通过；批次复盘返回前后请求计数 `2/2/1 -> 2/2/1`，执行表单 `1 -> 1`，MES 写请求和页面错误均为 0。
- 说明：测试租户登录通过但批次列表无可用记录，未创建模拟数据；最终按项目规则使用 admin 只读复验。

# 2026-07-13 DCC 文件查阅统一管理员样式

## 用户需求

- 让测试服务器普通账号看到的文件查阅页样式与本地管理员截图一致，包括主题。

## 执行命令

- 创建 `doc/tasks/20260713-dcc-browser-admin-style/`，记录经验门禁、BDD、严格 TDD 与前端证据。
- RED：`node tests/e2e/dcc-browser-admin-style-static.spec.js` -> FAIL，确认旧默认主题和文件查阅默认列未匹配管理员截图。
- 修复：前端默认主题改为绿色主色、白色左侧菜单和白色头部；文件查阅默认列改为文件名称、文件编号、操作三列，并使用新的管理员样式列配置 key。
- 补充修复：针对不同账号/不同浏览器截图差异，应用启动清理旧布局、主题、深色模式缓存，并停止持久化主题/布局；DCC项目代码页快速过滤默认字段锁定为文控。
- 二次补充修复：针对 115 浏览器 Ctrl+F5 后仍旧样式，确认 `src/styles/var.css` 首屏默认变量仍是旧深色菜单；已改为绿色主色、白色侧栏、深色菜单文字和浅绿色选中态。
- GREEN：`node tests/e2e/dcc-browser-admin-style-static.spec.js`、`node tests/e2e/dcc-browser-unified-list-template-static.spec.js`、`node tests/e2e/user-table-column-config-static.spec.js` 均通过。
- GREEN：`node --check tests/e2e/dcc-browser-admin-style-static.spec.js` 通过。
- GREEN：缺陷回归证据校验、前端特性证据校验均通过。
- BLOCKER-RETRIED：`pnpm.cmd ts:check` 默认堆内存不足 OOM；一次 120 秒超时；设置 `NODE_OPTIONS=--max-old-space-size=8192` 并加长超时后 `pnpm.cmd ts:check` 通过。
