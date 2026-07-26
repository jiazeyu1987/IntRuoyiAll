# Frontend Feature Evidence

## Feature Goal

隐藏“导入 Word”弹窗中的“表单类型”表单项。

## Non-goals

- 不修改后端 API。
- 不修改 DCC 产品名称候选来源。
- 不修改 Word 预检、升版确认和导入提交逻辑。
- 不新增 fallback、mock 或兼容分支。

## Acceptance

- `AC-1`: 弹窗不渲染标签为“表单类型”的表单项。
- `AC-2`: 弹窗继续渲染“产品名称”和“Word 文件”表单项。
- `AC-3`: `selectedFormSlotType` 初始化、打开和重置时继续使用 `MAIN`。
- `AC-4`: 相关静态合同和真实 E2E 脚本不再尝试操作已隐藏的下拉框。

## UI Entry

- Route/page: MES 批记录表单列表。
- Component: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`。
- Entry action: “导入 Word”按钮。

## API And State Contract

- 保留 `wordImportDialog.selectedFormSlotType`。
- 保留 `DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE = 'MAIN'`。
- API 调用和异常处理不变。

## BDD

- BDD: 隐藏导入 Word 表单类型
- Given 用户打开“导入 Word”弹窗
- When 弹窗渲染
- Then “表单类型”整行不可见，“产品名称”和“Word 文件”仍可见，内部类型为 `MAIN`

## Verification

- RED: `node tests/e2e/batch-record-word-import-form-type-hidden-static.spec.js` -> FAIL，旧模板仍渲染“表单类型”表单项。
- GREEN: `node tests/e2e/batch-record-word-import-form-type-hidden-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-batch-record-word-import-default-main-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/batch-record-form-import-prereq-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/batch-record-word-dcc-project-select-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: Playwright real readonly path -> PASS, `http://127.0.0.1:8081/mes/pro/batch-record-form-list`, dialog text had `hasFormType=false`, `hasProductName=true`, `hasWordFile=true`.
- Responsive/accessibility: 删除首行后沿用现有 Element Plus 表单布局；标签和值控件顺序保持清晰。
- Loading/empty/error/permission: 本次不改变对应状态和权限。
- E2E path: 使用 Playwright CLI 登录 `芋道源码/admin`，通过真实页面点击“导入”打开弹窗并检查可见文本；未提交或写入数据。

## Known Regression Blockers

- `node tests/e2e/edhr-batch-record-form-list-static.spec.js` -> FAIL 于既有“批量删除按钮必须调用批量删除处理函数”断言，与本次导入弹窗改动无关。
- `node tests/e2e/edhr-form-slot-frontend-static.spec.js` -> FAIL，因为既有脚本引用不存在的 `src/views/mes/pro/route/RouteFlowConfigPanel.vue`，与本次改动无关。
- `edhr-word-form-cell-rule-recognition-real.e2e.js` 仍覆盖非 `MAIN` 附加表单导入，依赖已按本需求隐藏的表单类型下拉；本任务未运行该写入路径，若仍需附加表单导入，需要另行批准并提供新的正式入口。
