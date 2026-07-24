# Word 导入弹窗界面精简

## Task Goal

按用户截图要求调整批记录表单列表的“导入 Word”弹窗：Word 文件行只保留“选择文件”按钮，不再显示已选文件名和格式提示；重建产线候选不默认勾选，避免用户误以为必须重建产线。

## Milestones

1. 补充静态回归测试，先证明当前弹窗仍显示文件名/格式提示且预检后默认勾选产线候选。
2. 修改前端导入弹窗模板和预检状态同步逻辑，保持导入参数与后端契约不变。
3. 运行定向前端静态测试，并记录 RED/GREEN 证据。

## Expected Verification

- 定向静态测试覆盖：批记录表单列表导入弹窗不再包含文件名显示和格式提示 DOM；预检成功后 `selectedRouteProductOptionKeys` 保持空数组。
- 回归静态测试覆盖：原 Word 导入预检契约仍通过，未破坏导入动作、历史引用、确认弹窗和运行中提示。

## 经验门禁

- PowerShell：中文文件读写使用 UTF-8；不得用默认 `Get-Content`/`Set-Content` 污染中文。
- 前端页面/表格/样式：遵循 IntPP 运营台风格，保持控件紧凑、信息不过度堆叠。
- 批记录 Word 表单识别：本次仅调整弹窗 UI 与默认选择状态，不改 Word 解析、结构化、视觉网格或导入业务算法。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接移除误导性展示并取消默认产线勾选，保留用户显式选择产线的路径。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## Verification Result

- RED: `node tests/e2e/batch-record-word-import-dialog-ui-static.spec.js` -> FAIL，现有弹窗仍显示文件名/“未选择 Word 文件”。
- GREEN: `node tests/e2e/batch-record-word-import-dialog-ui-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/batch-record-word-import-preflight-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/batch-record-form-import-prereq-static.spec.js` -> PASS。
- GREEN: `task-closeout-cleanup preview/apply` -> PASS，保留 task/execution-log/verification-report，清理临时 evidence 文档。
