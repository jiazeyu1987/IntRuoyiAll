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
- RED: 前端产品资源包导出使用默认 30000ms timeout -> FAIL, 大资源包导出超过 30 秒时页面提示接口请求超时。
- GREEN: pnpm -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check with NODE_OPTIONS=--max-old-space-size=8192 -> PASS, 产品资源包导出请求 timeout 已提升到 5 分钟。- GREEN: Playwright click E2E showroom product export -> PASS, 芋道源码/admin 真实登录后进入 /showroom/product，点击产品管理导出按钮，下载 zip，并用 zipfile 校验 manifest/product-data/audio assets。
- GREEN: frontend import wording check -> PASS, 标准导入弹窗明确展示 zip 资源包优先导入且兼容历史 Excel。
- GREEN: Playwright frontend zip import E2E -> PASS, 芋道源码/admin 真实登录后进入 /showroom/product，点击导入，选择 showroom-product-resource-package.zip，确认导入成功，failureCount=0 and awardFailureCount=0。
