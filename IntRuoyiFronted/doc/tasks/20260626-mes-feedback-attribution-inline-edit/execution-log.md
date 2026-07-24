# 执行日志：MES 报工待归属混合展示与修改归属前端改造

## 2026-06-26

- 初始化任务：创建前端任务包，记录门禁、设计约束、BDD 与静态验证范围。
- RED: `node tests/e2e/mes-feedback-attribution-continuation-static.spec.js` -> FAIL，归属保存后仍强制切回 `PENDING`。
- RED: `node tests/e2e/mes-feedback-import-current-batch-static.spec.js` -> FAIL，当前批待归属列表默认仍强制 `PENDING`。
- RED: `node tests/e2e/mes-feedback-attribution-process-picker-static.spec.js` -> FAIL，归属弹窗未回显 `selectedQuantity`，也未调用 `reattributeImportRecord`。
- RED: `node tests/e2e/mes-feedback-tracking-static.spec.js` -> FAIL，仍保留顶部归属结果卡与“归属时已回写进度”旧文案。
- CHANGE: 待归属列表去掉顶部归属结果卡，改为混合展示待归属和已归属；保存成功后保留当前批过滤，不再强制切回 `PENDING`。
- CHANGE: 行内状态改为橙色“选择归属”、绿色“已归属”；操作列支持 `修改归属`、`查看正式报工` 与不可修改原因展示。
- CHANGE: 归属弹窗支持修改模式，打开时根据候选 `selectedQuantity` 回显旧分配，提交时按模式调用 `attributeImportRecord` 或 `reattributeImportRecord`。
- CHANGE: 页面与弹窗所有进度提示统一改为“提交正式报工后回写排产进度”。
- GREEN: `node tests/e2e/mes-feedback-attribution-continuation-static.spec.js` -> PASS
- GREEN: `node tests/e2e/mes-feedback-import-current-batch-static.spec.js` -> PASS
- GREEN: `node tests/e2e/mes-feedback-attribution-process-picker-static.spec.js` -> PASS
- GREEN: `node tests/e2e/mes-feedback-tracking-static.spec.js` -> PASS
- GREEN: finalize-task-doc -> PASS，已将 `task.md` 当前状态更新为“已完成”，并补齐最终验证结果区块。
