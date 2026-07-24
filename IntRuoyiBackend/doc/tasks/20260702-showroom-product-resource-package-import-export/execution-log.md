# 执行日志：展厅产品导入导出资源包化

- BDD: 资源包导出携带音频文件 -> Given 展厅产品和奖项存在已发布讲解音频 / When 导出产品资源包 / Then 包内包含 manifest/json 和每条讲解对应音频文件，且 manifest 引用包内相对路径。
- BDD: 资源包跨环境导入重建音频 -> Given 在另一环境导入资源包 / When 导入逻辑读取 manifest/json / Then 音频从包内二进制创建新文件 ID，不依赖源环境音频文件ID。
- BDD: 缺失包内音频快速失败 -> Given manifest/json 引用了音频资源但包内文件缺失 / When 导入资源包 / Then 导入失败并明确指出缺失资源路径。
- GREEN: experience-preflight -> PASS，已读取 docs/experience-index.md、docs/powershell-memory.md、backend-api-delivery 与 bug-regression-fix-loop 技能契约。- RED: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldIncludeKeywordSheetAndBilingualNarrationAudioSheet" test -> FAIL, 导出仍按单 Excel 合同校验且未打包音频二进制。
- GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldIncludeKeywordSheetAndBilingualNarrationAudioSheet" test -> PASS, 产品导出改为 zip 资源包并包含 product-data.xlsx、manifest.json 与 assets/narration 音频。- GREEN: pnpm -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check with NODE_OPTIONS=--max-old-space-size=8192 -> PASS
- GREEN: backend-api-evidence validation -> PASS
- GREEN: frontend-feature-evidence validation -> PASS
RED: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldIncludeKeywordSheetAndBilingualNarrationAudioSheet" test -> FAIL, 导出仍按单 Excel 合同校验且未打包音频二进制。
GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldIncludeKeywordSheetAndBilingualNarrationAudioSheet" test -> PASS
- GREEN: Playwright E2E showroom product resource package -> PASS, browser at http://localhost:8081 logged in, exported zip, imported zip through /showroom/product/import-excel, and verified manifest/product-data/audio assets.
- RED: API zip import resource package with existing product missing narration assets -> FAIL, row failed with SHOWROOM_AUDIO_GENERATION_FAILED when package did not include that product narration.
- GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductResourcePackageShouldAllowProductWithoutNarrationAssets" test -> PASS
- GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test -> PASS- RED: API zip import resource package with existing product missing narration assets -> FAIL, row failed with SHOWROOM_AUDIO_GENERATION_FAILED when package did not include that product narration.
- GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductResourcePackageShouldAllowProductWithoutNarrationAssets" test -> PASS
- GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test -> PASS
- GREEN: API zip import existing exported package -> PASS, failureCount=0 and awardFailureCount=0.- GREEN: API zip import existing exported package on rebuilt current runtime -> PASS, failureCount=0 and awardFailureCount=0.

- BDD: ???????????? -> Given ??????????????????? / When ???? current??????????????? Website ?????? / Then ???????????MinIO ?? URL????? URL??????? URL ???????
- RED: `/showroom/display/website-config` -> FAIL??? `code=500` ????????? `ShowroomApiRuntime.displayWebsiteConfig` ? `legacyWebsiteConfigProjector` ? null?
- GREEN: ??????????? -> PASS????? `/admin-api/infra/file/28/get/20260521/...jpg` ?? `image/jpeg` ???? `ffd8ffe0 JFIF`??????? `/showroom/sites/yingtai-showroom/stages/TEST/assets/company-home-image/...` ?? `image/jpeg`?
- GREEN: ?????? -> PASS?MinIO ?? `http://127.0.0.1:9000/yudao/20260521/...jpg` ?? 403 AccessDenied???????????????????????
- GREEN: `mvn.cmd -pl yudao-module-showroom -am "-Dtest=ShowroomReleaseAdminPublishIntegrationTest#publishReleaseEndpointShouldSwitchCurrentPointerAndExposeReadableContracts,ShowroomLegacyWebsiteConfigProjectionApiTest,ShowroomLegacyWebsiteConfigConditionalRequestTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS?3 tests?
- GREEN: `mvn.cmd -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS???? `yudao-server-exec.jar`?
- GREEN: ????? `/showroom/display/website-config?siteKey=yingtai-showroom&stage=TEST` ? `/showroom/display/website-config/response?siteKey=yingtai-showroom&stage=TEST` -> PASS??? `code=0`??? `homeImageUrl` ? `/showroom/sites/.../assets/...`??????? `image/jpeg`?