# 任务：审阅矩阵页签改名并隐藏三列

## 任务目标

将 `src/views/dcc/controlled-file/categories/index.vue` 中页签文案 `DCC审阅矩阵` 改为 `审阅矩阵`，并在矩阵总览页中做前端精简：
- `CategoryReviewMatrixTable.vue` 隐藏筛选区 `启用状态`、`是否已配置`，以及总览表 `启用状态`、`可查阅主体`、`待审预览主体`、`下载规则`、`当前状态/风险`、`当前版本`、`生效时间`、`备注` 列。
- `CategoryViewMatrixTable.vue` 隐藏筛选区 `启用状态`、`是否已配置`，以及总览表 `启用状态` 列。
保持现有类别编码/名称查询、矩阵编辑/预览/删除操作和后端接口合同不变。

## 里程碑

- [x] M1：创建任务文档，记录经验门禁、设计约束检查与 BDD 场景。
- [x] M2：先修改静态契约，锁定页签改名与三列隐藏的 RED 失败。
- [x] M3：最小修改审阅矩阵页签与总览表列定义。
- [x] M4：运行定向静态验证并补齐执行证据。

## 预期验证

- `node tests/e2e/dcc-review-matrix-tab-static.spec.js`
- `node tests/e2e/dcc-category-governance-summary-static.spec.js`

## 当前状态

已完成。审阅矩阵页签已改名为 `审阅矩阵`，筛选区已隐藏 `启用状态`、`是否已配置`，审阅矩阵总览表已隐藏 `启用状态`、`可查阅主体`、`待审预览主体`、`下载规则`、`当前状态/风险`、`当前版本`、`生效时间`、`备注` 列；查看矩阵筛选区已隐藏 `启用状态`、`是否已配置`，总览表已隐藏 `启用状态` 列，相关定向静态验证已通过。

## 前一任务检查

- 前端最近任务 `20260625-mes-feedback-attribution-row-fill-fix` 已标记完成，允许继续本任务。
- 本任务只修改 DCC 类别页签、审阅矩阵总览列、定向测试与本任务文档，不覆盖其他未归属改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：保持现有密集操作台表格布局，不新增装饰性结构，只做页签文案和列显示裁剪。
- `docs/experience-index.md`：本任务仅做本机源码与静态验证，不执行真实 E2E、服务器写入或高风险动作，因此不触发 `experience-preflight` 门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅调整正式展示字段，不增加兜底分支。
- `是否从根因和长期维护角度解决`：是。直接收敛审阅矩阵列表信息密度，避免继续在总览表暴露不需要的冗余列。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 审阅矩阵页签改名 -> Given 用户进入类别页 When 查看顶部页签 Then 原 DCC审阅矩阵 页签显示为 审阅矩阵。`
- `BDD: 审阅矩阵总览隐藏冗余列 -> Given 用户进入审阅矩阵列表 When 查看表头 Then 不再显示 可查阅主体、待审预览主体、下载规则、当前状态/风险、当前版本、生效时间、备注 列。`
- `BDD: 审阅矩阵仅保留精简查询 -> Given 用户进入审阅矩阵列表 When 查看查询区 Then 只保留 类别编码、类别名称、查询、重置、刷新列表、按人反查。`
- `BDD: 审阅矩阵核心操作保留 -> Given 用户进入审阅矩阵列表 When 查看操作区 Then 编辑、删除、预览能力保持不变。`
- `BDD: 查看矩阵仅保留精简查询 -> Given 用户进入查看矩阵列表 When 查看查询区 Then 只保留 类别编码、类别名称、查询、重置、按人反查。`
- `BDD: 查看矩阵隐藏启用状态列 -> Given 用户进入查看矩阵列表 When 查看表头 Then 不再显示 启用状态 列。`

## Cleanup Keep

- `doc/tasks/20260625-dcc-review-matrix-hide-columns/task.md`
- `doc/tasks/20260625-dcc-review-matrix-hide-columns/execution-log.md`
- `doc/tasks/20260625-dcc-review-matrix-hide-columns/frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/dcc-review-matrix-hide-columns-static.spec.js`：PASS
- `node tests/e2e/dcc-review-matrix-tab-static.spec.js`：PASS
- `node tests/e2e/dcc-category-governance-summary-static.spec.js`：PASS
