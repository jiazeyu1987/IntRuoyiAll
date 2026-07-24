# 任务：展厅产品资料 Excel 导入导出补齐音频与关键词中英对照（后端）

- Task ID: `20260701-showroom-product-excel-audio-keyword-roundtrip`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

升级 `yudao-module-showroom` 的产品资料 Excel 导入导出合同，在现有 `产品列表 + 奖项` 工作簿基础上补齐：

- 产品与奖项的双语讲解/音频相关字段与回导逻辑；
- 关键词中英对照页签的导出与导入；
- 现有产品、奖项、封面与展柜映射回导链路不回退。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-nas-locator-blacklist-pattern-search\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成，不阻塞本次产品资料 Excel 合同升级。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Java、测试、Markdown 与日志统一按 UTF-8 处理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接升级 Excel 工作簿合同与回导发布链路，不保留“导出没有音频、导入静默沿用旧值”的旧规则。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 产品资料导出工作簿携带讲解与音频合同 -> Given 当前租户产品和奖项存在双语讲解稿与音频 / When 调用 /showroom/product/export-excel / Then 工作簿必须导出产品、奖项可回导的讲解稿、音频地址、时长和音色字段。`
- `BDD: 产品资料导出工作簿携带关键词中英对照页签 -> Given 当前租户存在关键词中英对照数据 / When 调用 /showroom/product/export-excel / Then 工作簿包含 关键词中英对照 Sheet，且按当前租户完整导出。`
- `BDD: 回导导出工作簿可重建讲解与关键词 -> Given 用户回导系统导出的完整工作簿 / When 调用 /showroom/product/import-excel / Then 产品、奖项讲解与音频以及关键词中英对照按工作簿内容生效，缺少必要字段时显式失败。`

## Milestones

1. M1：建立任务文档并确认现有产品/奖项/关键词 Excel 合同边界。`completed`
2. M2：补集成测试 RED，锁定旧合同缺少音频和关键词页签。`completed`
3. M3：实现 exporter、import extras、runtime 回导和关键词替换逻辑。`completed`
4. M4：跑 GREEN 验证并补 evidence。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-showroom-product-excel-audio-keyword-roundtrip\backend-api-evidence.md`

## Current Blockers

- 暂无。

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test` -> `PASS`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-showroom-product-excel-audio-keyword-roundtrip\backend-api-evidence.md` -> `PASS`
- `RED: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldIncludeKeywordSheetAndBilingualNarrationAudioSheet" test` -> `FAIL`，复现导出取 `latest` 草稿导致音频字段为空。
- `GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldIncludeKeywordSheetAndBilingualNarrationAudioSheet" test` -> `PASS`，导出改为读取同源 `latestPublished` 讲解音频。
- `GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test` -> `PASS`，42 个产品 Excel 导入导出回归用例通过。
- `GREEN: E2E export inspect` -> `PASS`，`D:\ProjectPackage\Int\IntRuoyi\output\playwright\showroom-product-export-e2e\fixed-audio-export-1782911491344.xlsx` 的 `讲解音频` Sheet 共 92 行，92 行均包含音频文件ID、音频地址和音频时长。
