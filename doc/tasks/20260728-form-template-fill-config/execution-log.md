# Execution Log

## 2026-07-28

USER INTENT: 在表单中心模板右侧预览红框区域增加“填写配置”按钮，功能与批记录表单页签下的“填写配置”一致；已确认数据保存到模板自身 `jimuSchemaJson`，不绑定批记录链路。

RULES READ: `frontend-feature-delivery` skill、`references/frontend-contract.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`、`docs/experience-index.md`。

DIRTY BASELINE: 任务开始前 `git status --short --branch --untracked-files=all` 显示既有脏改动。已按项目门禁提交独立基线 `7ee56ab4 chore: baseline existing dirty worktree changes`，文件清单：

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImplTest.java`
- `doc/tasks/20260727-edhr-notify-all-valid-candidates/backend-api-evidence.md`
- `doc/tasks/20260727-edhr-notify-all-valid-candidates/bug-regression-evidence.md`
- `doc/tasks/20260727-edhr-notify-all-valid-candidates/execution-log.md`
- `doc/tasks/20260727-edhr-notify-all-valid-candidates/failure-inventory.md`
- `doc/tasks/20260727-edhr-notify-all-valid-candidates/test-plan.md`
- `doc/tasks/20260727-edhr-notify-all-valid-candidates/test-report.md`
- `doc/tasks/20260727-edhr-notify-all-valid-candidates/verification-report.md`

BDD: 模板自身填写配置 -> Given 用户在表单中心模板列表选中一个非作废且无审批锁定的模板；When 点击右侧预览工具栏“填写配置”；Then 打开与批记录填写配置一致的单元格/辅助行配置弹窗，并使用当前模板 `jimuSchemaJson` 作为读取和保存来源。

BDD: 草稿保存约束 -> Given 当前模板版本状态为 `DRAFT`；When 用户保存填写配置；Then 页面通过表单中心 `saveTemplateJimuSchema` 保存合并后的 `jimuSchemaJson`，保留既有 `sheetLayoutJson`、`cellRules`、`signatureCellMarkers`、`assistRows` 和 `fillAssignments`，且不调用 MES 批记录 `cell-rules` 或 `save-by-report` 接口。

BDD: 非草稿只读约束 -> Given 当前模板版本不是 `DRAFT`；When 用户打开“填写配置”；Then 弹窗可查看当前配置但保存按钮禁用，并明确提示“只有草稿版本可以保存填写配置”。

## Evidence

RED: `node tests/e2e/form-template-fill-config-static.spec.js` -> FAIL, expected reason: missing `src/views/form-center/template/components/FormTemplateFillConfigDialog.vue` and preview toolbar entry.
