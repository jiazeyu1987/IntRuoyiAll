# Execution Log

## Intent

- USER: 截图红框中的原表模式规则类型图例不显示。
- SCOPE: 仅关闭 eDHR 执行填写页的图例；保留表格、单元格类型角标和其他页面默认图例。

## BDD

- BDD: 隐藏原表模式规则图例 -> Given 用户进入 eDHR 执行填写页并切换到原表模式 / When 原始批记录表单渲染 / Then 顶部“文本、数字、日期、日期时间、勾选、签名、附件、自动待确认、已确认”图例不显示，表格和单元格规则角标继续显示。

## Milestones

- completed: 已定位红框 DOM 为 `EdhrExecutionTemplateEditableForm.vue` 的 `edhr-template-editable-form__rule-legend`。
- completed: 已确认组件已有 `showRuleLegend` 显式开关，其他页面默认值为 `true`。
- RED: `node tests/e2e/edhr-fill-workspace-original-rule-legend-hidden-static.spec.js` -> FAIL，预期原因：执行填写页尚未传入 `:show-rule-legend="false"`。
- completed: `ExecutionPage.vue` 原表模式调用显式传入 `:show-rule-legend="false"`，共享组件默认值保持 `true`。
- GREEN: `node tests/e2e/edhr-fill-workspace-original-rule-legend-hidden-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-fill-workspace-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js` -> PASS，模拟页既有图例关闭行为未受影响。
- REGRESSION: `pnpm ts:check` -> PASS。
- CHECK: `git diff --check` -> PASS，仅输出 Windows LF/CRLF 提示，无 whitespace error。
- CONCURRENCY: 验证后发现 `ExecutionPage.vue` 同时包含其它任务的辅助卡片元信息隐藏和网格压缩改动；本任务只拥有 `:show-rule-legend="false"` 单行，提交时必须选择性暂存。
- EXPERIENCE: 本次使用共享组件已有显式展示开关，经验已由前端静态契约隔离和同文件并行改动选择性暂存门禁覆盖，无需更新长期经验文档。
- completed: 实现与必需验证完成，状态更新为 `ready_for_closeout`。
- CHECK: frontend feature evidence 首次校验失败，缺少精确 `BDD:` 与 `Verification` 标记；已按校验器契约补齐，等待复验。
- GREEN: `validate_frontend_feature.py --evidence doc/tasks/20260729-edhr-original-form-legend-hide/frontend-feature-evidence.md` -> PASS。
- CLEANUP: `task_closeout.py --task-id 20260729-edhr-original-form-legend-hide --mode preview` -> ready；keep 四个任务证据文件，delete/blocked/warnings 均为 none。
