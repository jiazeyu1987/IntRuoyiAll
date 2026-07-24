# 执行日志：电子批记录主从布局可见回归修复

## 2026-06-26

- 初始化任务：创建跟进任务目录，记录用户反馈、经验门禁和设计约束。
- BDD: 左侧仅显示批记录名称 -> Given 存在批记录名称列表 / When 页面展示左侧列表 / Then 每项只显示批记录名称文本，不显示数量标签或其他元信息。
- BDD: 中间仅显示报表名称 -> Given 已选择批记录名称 / When 中间展示该批记录的报表 / Then 列表只显示报表名称，报表级操作显示在右侧选中报表操作区。
- BDD: 缺少模板布局显示真实原因 -> Given 所选报表的模板接口未返回布局 JSON / When 右侧加载表单模板 / Then 页面提示该报表缺少 Jimu rows 模板布局并提示编辑或重新导入。
- RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，当前页面仍把报表操作放在中间列表，且左侧/中间/右侧提示未满足单列与缺少布局说明合同。
- GREEN: `apply_patch` -> PASS，左侧批记录名称项移除数量标签，中间报表列表只保留报表名称列，报表操作迁移到右侧选中报表操作区。
- GREEN: `apply_patch` -> PASS，缺少模板布局时提示所选 Jimu 报表 JSON 未返回 `rows` 模板布局，并提示编辑保存或重新导入。
- GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS。
- GREEN: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-word-import.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS。

## 2026-06-26 追加：中间报表列表滚动显示全部

- BDD: 中间报表列表滚动显示全部 -> Given 选中批记录下存在超过 10 份报表 / When 页面展示中间报表名称列表 / Then 不显示分页器或每页条数选择，列表区域用滚动条显示全部报表名称。
- RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，当前页面仍缺少 `BATCH_RECORD_REPORT_LIST_PAGE_SIZE` 固定大页查询合同，且仍显示分页器。
- GREEN: `apply_patch` -> PASS，移除中间报表列表下方 `Pagination`，报表查询固定 `pageNo: 1` 和 `pageSize: BATCH_RECORD_REPORT_LIST_PAGE_SIZE`，列表容器继续通过 `overflow: auto` 滚动展示。
- GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS。
- GREEN: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-word-import.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS。
- BLOCKER: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> FAIL，当前工作区无关 DCC 文件 `src/views/dcc/controlled-file/access-rules/index.vue(16,9)` 存在 `Cannot find name 'router'`，阻塞全量类型检查；本次电子批记录相关静态与脚本回归已通过。
- RED: `pnpm ts:check` -> FAIL，Node 默认 4GB 堆内存不足，`vue-tsc` 报 JavaScript heap out of memory。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260626-electronic-batch-record-master-detail-layout-fix/frontend-feature-evidence.md` -> PASS。

## 2026-06-26 追加：右侧显示真实 Jimu 表单预览

- BDD: 选择报表显示真实表单 -> Given 选中报表已有 Jimu 表单 / When 右侧加载表单模板 / Then 页面嵌入该报表真实 Jimu 预览，不因单元格 `rows` 布局缺失显示“缺少电子批记录模板布局”。
- BDD: 表单模板区域自适应缩放 -> Given 右侧区域宽度小于 Jimu 表单原始宽度 / When 表单预览加载完成 / Then 表单按可用宽度等比例缩放，高度按缩放比例展开，顶部工具栏不显示。
- RED: 用户截图反馈 -> FAIL，右侧仍用单元格布局说明组件，已有 Jimu 表单的报表在缺少 `sheetLayoutJson.rows` 时误报“缺少电子批记录模板布局”。
- RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，页面缺少右侧 `IFrame` 和 `getDesignerPath` 真实预览合同。
- GREEN: `apply_patch` -> PASS，右侧改为通过 `BatchRecordReportApi.getDesignerPath` 获取真实 Jimu 预览路径并嵌入 `IFrame`，不再使用 `EdhrExecutionTemplateGuide` 阻断主预览。
- GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS。
- GREEN: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-word-import.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260626-electronic-batch-record-master-detail-layout-fix/frontend-feature-evidence.md` -> PASS。

## 2026-06-26 追加：隐藏顶部工具区并让右侧 Jimu 表单按宽度等比缩放

- BDD: 顶部工具区不显示 -> Given 用户进入电子批记录三栏页面 / When 页面加载完成 / Then 批记录区域上方不再显示旧搜索、刷新、导入等顶部工具区。
- BDD: Jimu 表单按右侧宽度缩放 -> Given 选中报表已有 Jimu 表单 / When 右侧 iframe 加载同源 JMReport viewer / Then 页面隐藏 viewer 工具条，并按预览容器可用宽度等比缩放表单，高度使用缩放后的表单高度。
- RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，页面仍包含 `batch-record-toolbar-shell`。
- RED: `node tests/e2e/batch-record-preview-toolbar.spec.js` -> FAIL，共享 `IFrame` 仅支持 `jmreport-viewer`，缺少 `jmreport-viewer-fit-width`、`fitWidthMinHeight` 与表单缩放逻辑。
- GREEN: `apply_patch` -> PASS，移除页面顶部工具区残留样式，右侧预览传入 `sameOriginChromeMode="jmreport-viewer-fit-width"` 和 `fitWidthMinHeight="520"`。
- GREEN: `apply_patch` -> PASS，共享 `IFrame` 新增 `jmreport-viewer-fit-width` 模式，隐藏 JMReport viewer 工具条后定位 `.jm-sheet`，按 iframe 可用宽度计算 scale，并设置 iframe 高度为缩放后的表单高度。
- GREEN: `node tests/e2e/batch-record-preview-toolbar.spec.js` -> PASS。
- GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS。
- GREEN: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-word-import.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-electronic-batch-record-master-detail-layout-fix --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` -> PASS，保留任务文档、执行日志和 frontend evidence，无删除项、无阻塞项。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-electronic-batch-record-master-detail-layout-fix --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` -> PASS，保留 `task.md`、`execution-log.md`、`frontend-feature-evidence.md`，无删除项、无阻塞项。

## 2026-06-26 追加：中间报表列表对齐左侧样式

- BDD: 中间报表列表与左侧展示一致 -> Given 左侧批记录名称使用按钮列表 / When 中间展示报表名称列表 / Then 中间使用同样的按钮列表视觉样式，且列宽与左侧一致。
- BDD: 中间报表列表滚动显示全部 -> Given 选中批记录下存在超过 10 份报表 / When 页面展示中间报表名称列表 / Then 不显示分页器或每页条数选择，列表区域用滚动条显示全部报表名称。
- RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，当前中间报表列表仍使用 `el-table`，缺少 `batch-record-report-list__item`。
- GREEN: `apply_patch` -> PASS，将中间报表列表从 `el-table` 改为按钮列表，主从布局改为 `240px 240px minmax(0, 1fr)`。
- RED: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> FAIL，旧脚本仍要求 `batch-record-report-name-table`。
- GREEN: `apply_patch` -> PASS，同步脚本断言报表列表使用 `batch-record-report-list__item/name` 且不再使用表格。
- GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS。
- GREEN: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-word-import.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS。

## 2026-06-26 追加：批记录名称行级删除

- BDD: 左侧批记录名称可单独删除 -> Given 左侧存在多个批记录名称 / When 用户点击某个批记录名称行内删除按钮 / Then 前端调用按批记录名称删除接口并刷新列表，其他批记录不受影响。
- RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，页面缺少 `handleDeleteBatchRecordName` 和左侧行内删除按钮。
- GREEN: `apply_patch` -> PASS，左侧批记录名称行新增删除图标按钮，点击后调用 `deleteGeneratedReportsByBatchRecordName` 并刷新主从状态。
- GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS。
- GREEN: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

## 2026-06-26 追加：报表全量滚动加载遵守后端 pageSize 上限

- BDD: 滚动列表遵守后端分页上限 -> Given 后端 `pageSize` 最大值为 200 / When 中间报表列表需要展示全部报表 / Then 前端不显示分页器，但内部按 `pageSize=200` 循环请求并合并全部页。
- RED: 用户真实反馈 -> FAIL，`pageSize=1000` 被后端 `@Max(200)` 拒绝，错误为 `每页条数最大值为 200`。
- RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，当前页面仍使用 `BATCH_RECORD_REPORT_LIST_PAGE_SIZE = 1000`。
- GREEN: `apply_patch` -> PASS，`BATCH_RECORD_REPORT_LIST_PAGE_SIZE` 改为 200，并新增 `getAllReportsForSelectedBatchRecord()` 按页循环请求、合并全部报表。
- GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS。
- GREEN: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-word-import.test.mjs` -> PASS。
- GREEN: `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260626-electronic-batch-record-master-detail-layout-fix/frontend-feature-evidence.md` -> PASS。
