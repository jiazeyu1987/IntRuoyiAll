# CSV 正式送审分册包改造

## Task Goal

将 `docs/csv-validation` 现有 CSV 材料改造成可面向药监现场核查老师审阅的中文 Word 分册式交付包，避免全部内容集中在单一文件中，并明确哪些内容可提交、哪些内容必须由企业补齐真实签字和执行证据后才能作为正式已完成验证结论。

## Milestones

- [x] 读取项目 CSV 材料、OfficeCLI Word 规则和收尾规则。
- [x] 设计正式送审分册结构和签字责任矩阵。
- [x] 使用 OfficeCLI 生成或更新 Word 分册、目录导读和 README。
- [x] 验证 Word 文件结构、正文、占位风险、页码字段和 OpenXML 有效性。
- [x] 记录验证结果、剩余阻塞和交付边界。

## Expected Verification

- `officecli validate` 对本次生成或更新的 `.docx` 文件全部通过。
- `officecli view ... outline/text/issues` 抽查关键分册结构清晰，无误导性“已完成”表述。
- 提交包必须保留真实缺口，不伪造 QA 批准、企业签章、IQ/OQ/PQ 执行结果、培训记录、供应商证据或生产运行证据。
- 文件必须为中文 Word 分册，目录 README 清楚说明交付顺序和需确认签字角色。

## Current Status

ready_for_closeout

中文 Word 分册式送审包已完成并通过 OfficeCLI 结构与有效性验证。当前未获当轮 Git 提交/推送授权，因此不执行提交或推送；若严格执行项目 Git 完成门禁，本任务不能标记为 completed。

## Design Constraint Checks

- 遵守严格无 fallback：不使用 Python Office 库伪造 Office 输出，不把未执行验证写成已通过。
- Office 文档使用 `officecli` 生成和验证。
- 不执行 Git commit/push，除非用户在当前轮明确授权。
- 中文文档使用 UTF-8 文本源或 OfficeCLI 写入，避免编码降级。

## Cleanup Candidates

- doc/tasks/csv-official-submission-package-20260909/create_frontmatter_doc.ps1
- doc/tasks/csv-official-submission-package-20260909/00-preview.png
- doc/tasks/csv-official-submission-package-20260909/00A-preview.png

## Cleanup Keep

- doc/tasks/csv-official-submission-package-20260909/task.md
- doc/tasks/csv-official-submission-package-20260909/execution-log.md
- doc/tasks/csv-official-submission-package-20260909/verification-report.md
- docs/csv-validation/official-submission-package/
