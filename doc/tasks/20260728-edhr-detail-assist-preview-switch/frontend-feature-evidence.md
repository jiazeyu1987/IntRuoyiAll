# Frontend Feature Evidence: eDHR 详情页辅助模式 Switch

## Feature Goal

在 eDHR 批次详情页右侧表单列表上方增加“原表模式/辅助模式”Switch，只控制中间预览区；无辅助配置时 Switch 保留但禁用。

## Non-Goals

- 不改变右侧卡片“查看表单/打开填写”按钮逻辑。
- 不改变 `ExecutionPage.vue` 填写页既有辅助模式行为。
- 不新增前端接口或写入动作。

## Acceptance

- 右侧非放行工序显示“原表模式/辅助模式”Switch。
- 有辅助行时切换中间只读预览；无辅助行时保留 Switch 但禁用。
- Switch 不改变右侧卡片打开载体，也不增加写请求入口。

## Entry Points And Owned Files

- 页面入口：eDHR 批次详情页 `BatchExecutionDetailPage.vue`。
- 任务文件：`IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`。
- 静态合同：`IntRuoyiFronted/tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js`。

## API And Data States

- 继续消费 `EdhrBatchExecutionReviewFormViewModel.executionSnapshotJson` 可选字段。
- `assistRows.length > 0` 时启用辅助模式；无辅助行时 Switch 禁用并显示“未配置辅助模式”。
- 中间辅助预览只读取 `fields`、`assistRows` 和 `cellValuesJson`，不调用保存、提交、签名、上传或打开表单入口。

## BDD Scenarios

- BDD: 详情页原表/辅助预览切换 -> Given 当前选中右侧表单且配置辅助行, When 打开 Switch, Then 中间区域显示辅助字段只读列表。
- BDD: 无辅助配置禁用 Switch -> Given 当前表单无 `assistRows`, When 查看右侧栏顶部 Switch, Then Switch 禁用并提示“未配置辅助模式”。
- BDD: 禁用提示完整可见 -> Given 当前表单无辅助配置, When 右侧栏宽度较窄, Then “未配置辅助模式”独占第二行且不换行，不被蓝框裁切。
- BDD: Switch 不改变打开载体 -> Given 用户切换详情页辅助模式, When 点击右侧卡片操作, Then 仍按原卡片逻辑查看或打开表单。

## RED / GREEN

- RED: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> FAIL，缺少右侧 `el-switch` 和辅助只读预览。
- GREEN: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- RED: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> FAIL，新增可见性合同后现有 Switch 单行 flex 布局会挤压“未配置辅助模式”。
- GREEN: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS，Switch 三列 grid、禁用提示第二行不换行合同通过。

## Verification

- 静态合同覆盖 Switch 绑定、禁用条件、只读辅助字段和右侧动作隔离。
- 后端 preview 回归为前端提供正式 `executionSnapshotJson.assistRows`。

## Responsive / Accessibility / States

- Switch 使用 `aria-label="详情页辅助模式切换"`，只在非放行工序右侧栏展示。
- Switch 主行使用三列 grid 固定“原表模式 / Switch / 辅助模式”，禁用提示独占第二行，避免窄右侧栏裁切。
- 辅助只读列表使用字段卡片布局，长字段名和值使用 `overflow-wrap`，避免遮挡。
- 空辅助配置显示禁用提示，不隐藏 Switch，不触发降级写入。

## Regression / Blockers

## Blockers

- PASS: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js`
- PASS: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js`
- PASS: `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- PASS: `node tests/e2e/edhr-loss-form-open-action-static.spec.js`
- PASS: `pnpm ts:check`
- PASS: `pnpm build:local`
- PASS: `node src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`
- Blockers: 当前无前端类型、构建或 eDHR 静态合同错误；工作区存在并行改动，提交前需隔离。
