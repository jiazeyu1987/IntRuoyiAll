# 任务：DCC 已识别文件名/文件编号导入导出

- Task ID: `20260629-dcc-recognized-file-metadata-import-export`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

为 DCC 受控浏览中已经识别并回写到受控文件业务字段的 `fileName` 与 `fileNumber` 提供正式导入导出能力，支持按当前浏览筛选导出，支持通过 Excel 导入预览与确认更新，并保持现有受控文件元数据校验、权限与失败即暴露的行为。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-browser-recognition-ledger-version\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成，可继续当前任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：本次命中 PowerShell/中文编码门禁；任务文档、执行日志与命令记录需显式 UTF-8 读写。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：PowerShell 5.1 下不得用默认重定向写中文文件；命令输出含中文前需显式设置 UTF-8 编码。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。导入文件缺失、表头错误、目标文件不存在、权限不足或元数据校验失败时直接报错，不做默认忽略、静默跳过或兜底成功。
- 是否从根因和长期维护角度解决：是。沿用现有 DCC 导入预览/确认模式与元数据更新服务，避免新增一次性脚本入口或绕开正式校验链路。
- 是否存在临时补丁或绕过：否。不做纯前端本地导入、不做接口旁路直改数据库、不做无预览确认的盲覆盖。

## Milestones

- M1: 建立任务文档、执行日志与请求命令记录，确认范围与 BDD/TDD 方案。状态：completed。
- M2: 补后端 RED 测试，覆盖控制器导入导出映射、服务导出筛选、导入预览与确认更新。状态：completed。
- M3: 实现后端受控文件文件名/文件编号导入导出能力与所需 VO/服务。状态：completed。
- M4: 实现前端浏览页导入导出入口、API 对接与预览确认弹窗。状态：completed。
- M5: 运行定向验证、补充 evidence、更新台账并准备提交。状态：completed。

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileMetadataImportExportControllerTest,DccControlledFileMetadataImportExportServiceTest,DccControlledFileMetadataUpdateServiceTest" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-recognized-file-metadata-import-export\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-recognized-file-metadata-import-export\frontend-feature-evidence.md`

## Current Blockers

- 仓库级前端 `npm run ts:check` 存在既有非本次范围错误：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\edhr-batch\BatchExecutionTemplateSimulatePage.vue` 的 `EdhrRecordCategory` 类型不兼容。本次 DCC 定向 ESLint 已通过。

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileMetadataImportExportControllerTest,DccControlledFileMetadataImportExportServiceTest,DccControlledFileMetadataUpdateServiceTest" test` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-recognized-file-metadata-import-export\backend-api-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-recognized-file-metadata-import-export\frontend-feature-evidence.md` -> PASS
