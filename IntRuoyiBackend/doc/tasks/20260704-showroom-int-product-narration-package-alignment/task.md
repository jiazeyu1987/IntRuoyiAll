# 任务：展厅 INT 产品资源包语音对齐

- Task ID: 20260704-showroom-int-product-narration-package-alignment
- Created: 2026-07-04
- Current Status: completed

## Task Goal

修复展厅产品 zip 资源包导入导出中 `INT-*` 产品数据与产品语音不一致的问题：zip 中每个 `INT-*` 产品必须同时具备中文、英文产品语音，且语音目标编码必须与产品编码精确一致；缺失或混入 `product_*` / `e2e*` 等非产品清单编码时必须显式失败。

## Milestones

1. 建立任务文档、经验门禁和设计约束检查。completed
2. 补充 RED 回归测试，覆盖 `INT-*` 缺语音与错配语音。completed
3. 实现 zip 资源包导出/导入严格校验。completed
4. 运行目标测试和展厅导入导出回归。completed
5. 更新证据并提交本任务改动。completed

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldFailWhenIntProductNarrationIncomplete+importProductResourcePackageShouldRejectIntProductWithoutNarrationAssets+importProductResourcePackageShouldRejectProductNarrationCodeOutsideProductList+importProductExcelShouldImportNarrationSheetAndKeywordSheet" test`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260704-showroom-int-product-narration-package-alignment\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260704-showroom-int-product-narration-package-alignment\bug-regression-evidence.md`

## 经验门禁

- 已读取 `docs/experience-index.md`：涉及 PowerShell、真实 E2E、服务器或导入长链路前需记录对应门禁；当前先做本地 TDD。
- 已读取 `docs/powershell-memory.md`：PowerShell 输出、中文文件和多行命令使用 UTF-8 明确路径，不使用 `&&`。
- 已读取缺陷修复与后端 API 技能合同：先复现/RED，再最小修复，记录 RED/GREEN、合同与风险。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，收紧 zip 资源包合同，按产品编码精确校验产品语音，而非沿用旧语音或跳过缺失。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: INT 产品 zip 导入缺少产品语音必须失败 -> Given zip 产品列表存在 INT-12 / When 讲解音频和 manifest 没有 INT-12 的中英文 PRODUCT 语音 / Then 导入失败并返回 INT-12 缺中文和英文语音。`
- `BDD: INT 产品 zip 导入混入旧产品语音必须失败 -> Given zip 产品列表只存在 INT-12 / When 讲解音频或 manifest 出现 product_049 的 PRODUCT 语音 / Then 导入失败并提示语音目标编码不在产品列表中。`
- `BDD: INT 产品 zip 导出缺少产品语音必须失败 -> Given 当前筛选导出的产品包含 INT-12 / When INT-12 缺中文或英文已发布语音 / Then 导出 zip 失败并列出缺失语言，不产出不完整资源包。`

## Current Blockers

- 暂无。

## Final Verification Result

- RED：`mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldFailWhenIntProductNarrationIncomplete+importProductResourcePackageShouldRejectIntProductWithoutNarrationAssets+importProductResourcePackageShouldRejectProductNarrationCodeOutsideProductList" test` -> FAIL，旧行为允许 `INT-12` 缺产品语音导入、允许 `product_049` 语音混入，导出缺语音错误未命中合同码。
- GREEN：同一目标测试 -> PASS，3 tests。
- GREEN：`mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldImportNarrationSheetAndKeywordSheet+exportProductExcelShouldIncludeKeywordSheetAndBilingualNarrationAudioSheet+exportProductExcelShouldFailWhenIntProductNarrationIncomplete+importProductResourcePackageShouldRejectIntProductWithoutNarrationAssets+importProductResourcePackageShouldRejectProductNarrationCodeOutsideProductList" test` -> PASS，5 tests。
- GREEN：`mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test` -> PASS，46 tests。

## Cleanup Keep

- doc/tasks/20260704-showroom-int-product-narration-package-alignment/backend-api-evidence.md
- doc/tasks/20260704-showroom-int-product-narration-package-alignment/bug-regression-evidence.md
