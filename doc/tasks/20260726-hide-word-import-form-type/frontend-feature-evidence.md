# Frontend Feature Evidence

## Feature Goal

隐藏“导入 Word”弹窗中的“表单类型”表单项。

## Non-goals

- 不修改后端 API。
- 不修改 DCC 产品名称候选来源。
- 不修改 Word 预检、升版确认和导入提交逻辑。
- 不新增 fallback、mock 或兼容分支。

## Requirements

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

- Given 用户打开“导入 Word”弹窗
- When 弹窗渲染
- Then “表单类型”整行不可见，“产品名称”和“Word 文件”仍可见，内部类型为 `MAIN`

## Verification

- RED: 待记录。
- GREEN: 待记录。
- Responsive/accessibility: 删除首行后沿用现有 Element Plus 表单布局；标签和值控件顺序保持清晰。
- Loading/empty/error/permission: 本次不改变对应状态和权限。
- E2E path: 真实页面只读打开弹窗并检查可见文本，不提交或写入数据。

## Blockers

- 无。

