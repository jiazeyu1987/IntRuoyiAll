# 任务：展厅产品导入导出资源包化

- Task ID: $taskId
- Created: 2026-07-02
- Current Status: completed

## Task Goal

将展厅产品管理导出从单一 Excel/JSON 元数据形态升级为可跨环境导入的资源包：资源包内包含结构化元数据文件与音频等二进制资源，导入时不依赖源环境文件 ID。

## Milestones

1. 梳理当前产品 Excel 导入导出与展厅资源包能力边界。in_progress
2. 设计资源包合同：manifest/json、音频文件目录、Excel/元数据映射与失败条件。pending
3. 补 BDD/TDD 回归，复现跨环境只带文件 ID 无法可靠导入音频。pending
4. 实现资源包导出与导入解析。pending
5. 运行后端回归与资源包导入导出验证。pending

## Expected Verification

- mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test
- 资源包导出后解包检查：必须包含 manifest/json 与音频文件。
- 资源包导入：必须用包内音频重建文件记录，不依赖源环境 音频文件ID。

## 经验门禁

- 已读取 docs/experience-index.md。
- 已读取 docs/powershell-memory.md，PowerShell 中文与文件写入使用 UTF-8。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是用资源包携带二进制音频，消除跨环境文件 ID 依赖。
- 是否存在临时补丁或绕过：否。

## Current Blockers

- 暂无。
## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldIncludeKeywordSheetAndBilingualNarrationAudioSheet" test` -> `PASS`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test` -> `PASS`，42 个用例通过。
- `pnpm -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check` with `NODE_OPTIONS=--max-old-space-size=8192` -> `PASS`
- `validate_backend_api.py` -> `PASS`
- `validate_frontend_feature.py` -> `PASS`

## Current Status

- completed
## Follow-up?????????????

- ???????????????????????????
- ???MinIO ?? URL ?? 403??????????? `/showroom/display/website-config` ???????? release projector????????
- ???????????? `ShowroomLegacyWebsiteConfigProjector`????? `siteKey/stage`?????????? `/showroom/sites/.../assets/...`?
- ???Showroom release/display ?? 3 ??????????????? website-config ???? `code=0`????????? `image/jpeg`?
