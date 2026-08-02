# 20260802 DCC 项目代码文件类型归属 E2E 验证

## Task Goal

验证已有受控文件通过真实页面修改“文件分类/文件类型”和归属 DCC 项目代码后，DCC 项目代码 item 详情的关联文档三栏中间“文件类型”列会同步体现变更；覆盖 5 个不同文件类型。

## Milestones

- [x] 建立任务目录与 BDD/验证门禁
- [x] 定位可用本机运行态、测试租户账号、项目代码、受控文件和 5 个文件类型
- [x] 通过真实前端路径修改已有文件元数据
- [x] 在 DCC 项目代码 item 详情中验证文件类型同步
- [x] 记录验证报告、阻塞或残留风险

## Expected Verification

- 使用 Playwright 操作本机前端真实页面，不能用 API-only 或 SQL 直接改文件元数据。
- 覆盖 5 个不同文件类型；每次保存后在目标 DCC 项目代码详情的关联文档中看到文件出现在对应文件类型下。
- 最终只读 API/DB 仅用于辅助核验和恢复确认。
- 记录任务自有测试数据、文件 ID、项目代码 ID、5 个文件类型和最终状态。

## 经验门禁

- `docs/e2e-rules.md#e2e-脚本入口存在性门禁`：真实 E2E 只有 Playwright 操作真实页面并完成目标断言后才能记为 PASS。
- `docs/e2e-rules.md#element-plus-下拉选择门禁`：Element Plus 选择框必须按可见业务文本选择目标项，不得用隐藏 value 或数组下标冒充真实选择。
- `docs/frontend-development.md#dcc-基础条目关联文档分类树门禁`：DCC 项目代码关联文档必须按正式文件分类树和 `fileTypeTaxonomyId` 展示，不能由文件名、空值或硬编码推断。
- `docs/database-rules.md#dcc-文件类别规则种子门禁`：不得直接 SQL 修改 `dcc_controlled_file` 分类字段来绕过正式页面链路。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：本任务只做验证，不改生产代码；若发现同步失败，记录真实失败点。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

doc/tasks/20260802-dcc-project-code-filetype-assignment-e2e/dcc-project-code-filetype-assignment-e2e.cjs

## Current Status

ready_for_closeout
