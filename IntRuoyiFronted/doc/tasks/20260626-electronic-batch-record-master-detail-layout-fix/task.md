# 任务：电子批记录主从布局可见回归修复

## 任务目标

- 左侧批记录名称列表只显示批记录名称，单列展示，不显示数量或其他元信息。
- 中间报表列表只显示报表名称，单列展示；选中报表后在右侧集中提供打开、编辑、签名位、单元格规则、重命名、删除操作。
- 右侧表单模板缺少布局时，说明真实原因是所选 Jimu 报表未返回 `rows` 模板布局数据，并给出正式处理路径，不展示默认模板或 mock 成功。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-electronic-batch-record-master-detail-layout\task.md`
- 状态：`COMPLETED`
- 处理说明：本任务是该三栏布局的可见回归修复，继续沿用已有 API 和任务证据，不回退工作区其他脏改。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 前端页面必须沿用 IntPP 运维台样式：白底、轻边框、紧凑表格、明确操作区、稳定尺寸。
  - 本次默认只做本机前端代码与静态验证；如进入真实 Playwright E2E 或登录后验证，必须先运行官方 `login-preflight.mjs` 最小登录路径并在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少模板布局时明确暴露缺失前置条件，不生成默认表格、不静默切换到预览 iframe。
- `是否从根因和长期维护角度解决`：是。按现有接口约束说明缺少 `sheetLayoutJson/rows` 的真实原因，并把报表级操作集中到右侧选中上下文。
- `是否存在临时补丁或绕过`：否。不新增后端接口、不新增 mock 数据、不隐藏接口错误。

## BDD 场景

- `BDD: 左侧仅显示批记录名称 -> Given 存在批记录名称列表 / When 页面展示左侧列表 / Then 每项只显示批记录名称文本，不显示数量标签或其他元信息。`
- `BDD: 中间仅显示报表名称 -> Given 已选择批记录名称 / When 中间展示该批记录的报表 / Then 列表只显示报表名称，报表级操作显示在右侧选中报表操作区。`
- `BDD: 中间报表列表与左侧展示一致 -> Given 左侧批记录名称使用按钮列表 / When 中间展示报表名称列表 / Then 中间使用同样的按钮列表视觉样式，且列宽与左侧一致。`
- `BDD: 左侧批记录名称可单独删除 -> Given 左侧存在多个批记录名称 / When 用户点击某个批记录名称行内删除按钮 / Then 前端调用按批记录名称删除接口并刷新列表，其他批记录不受影响。`
- `BDD: 缺少模板布局显示真实原因 -> Given 所选报表的模板接口未返回布局 JSON / When 右侧加载表单模板 / Then 页面提示该报表缺少 Jimu rows 模板布局并提示编辑或重新导入。`
- `BDD: 顶部工具区不显示 -> Given 用户进入电子批记录三栏页面 / When 页面加载完成 / Then 批记录区域上方不再显示旧搜索、刷新、导入等顶部工具区。`
- `BDD: Jimu 表单按右侧宽度缩放 -> Given 选中报表已有 Jimu 表单 / When 右侧 iframe 加载同源 JMReport viewer / Then 页面隐藏 viewer 工具条，并按预览容器可用宽度等比缩放表单，高度使用缩放后的表单高度。`

## 里程碑

1. M1：创建回归修复任务文档与 RED 静态测试。`COMPLETED`
2. M2：实现左/中名称单列和右侧操作区、模板缺失说明。`COMPLETED`
3. M3：运行静态回归、既有脚本和类型检查。`COMPLETED`
4. M4：回写证据、收尾预览并按验证结果处理提交。`COMPLETED`

## 预期验证

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `node scripts/electronic-batch-record-jimu-list.test.mjs`
- `node scripts/electronic-batch-record-word-import.test.mjs`
- `node scripts/electronic-batch-record-report-page.test.mjs`
- `node tests/e2e/batch-record-preview-toolbar.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260626-electronic-batch-record-master-detail-layout-fix/frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS
- `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS
- `node scripts/electronic-batch-record-word-import.test.mjs` -> PASS
- `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，中间报表列表已改为与左侧一致的按钮列表且同宽。
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，中间报表列表已移除分页器并使用滚动容器展示全部报表。
- `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS，脚本已同步断言报表列表不再使用表格。
- `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS，脚本已同步断言固定大页查询和无分页控件。
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，已修复 `pageSize=1000` 超过后端上限问题，前端内部按 `pageSize=200` 循环加载。
- `node scripts/electronic-batch-record-word-import.test.mjs` -> PASS，批记录过滤断言已同步内部分页加载函数。
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，右侧表单模板已改为真实 Jimu iframe 预览，不再因缺少单元格 `rows` 布局误报。
- `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS，脚本已同步断言右侧预览使用 `getDesignerPath` 与 `IFrame`。
- `pnpm ts:check` -> 首次 OOM；`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> 本轮复跑 FAIL，当前工作区无关 DCC 文件 `src/views/dcc/controlled-file/access-rules/index.vue(16,9)` 存在 `Cannot find name 'router'`，阻塞全量类型检查。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，右侧真实 Jimu 预览改动后类型检查通过。
- `node tests/e2e/batch-record-preview-toolbar.spec.js` -> PASS，共享 `IFrame` 已支持隐藏 JMReport 工具条后按宽度等比缩放。
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，顶部工具区不再显示，右侧预览启用 `jmreport-viewer-fit-width`。
- `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS，右侧 Jimu 预览契约保持有效。
- `node scripts/electronic-batch-record-word-import.test.mjs` -> PASS，旧顶部搜索断言已改为隐藏工具区与导入 input 保留契约。
- `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260626-electronic-batch-record-master-detail-layout-fix/frontend-feature-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-electronic-batch-record-master-detail-layout-fix --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` -> PASS

## Cleanup Keep

- `doc/tasks/20260626-electronic-batch-record-master-detail-layout-fix/frontend-feature-evidence.md`
