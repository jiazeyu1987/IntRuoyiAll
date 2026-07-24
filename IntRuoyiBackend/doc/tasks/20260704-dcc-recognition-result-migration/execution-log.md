# 执行日志：DCC 识别结果迁移包后端

## 2026-07-04

- BDD: export migration package keeps success and failure rows -> Given 测试服当前目录存在成功与失败识别记录 / When 用户导出识别迁移包 / Then Excel 包含成功行、失败行、失败原因和稳定匹配键。
- BDD: preview does not match by test server id -> Given 测试服 ID 与正式服 ID 不一致但目录路径和文件编号唯一匹配 / When 正式服导入预览 / Then 系统匹配正式服文件并显示可应用。
- BDD: preview blocks unsafe rows with reasons -> Given 文件找不到、重复匹配、识别失败、产品或项目缺失 / When 导入预览 / Then 逐行标记不可应用原因且不写入正式服。
- BDD: confirm applies only applicable successful rows -> Given 预览批次中存在可应用和不可应用行 / When 确认导入 / Then 只更新可应用成功行。
- TASK: backend-task-docs -> DONE, 已创建后端服务仓任务文档。
- RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason：缺少迁移包 VO 与服务方法。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=*Recognition*Migration*,DccControlledFileMetadataImportExportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests。
- TASK: backend-implementation -> DONE, 已实现识别结果迁移包导出、预览和确认应用。
- BUG: migration package exported recognition short code as product code -> 本地导出包可加载但预览被阻塞，原因是迁移包产品编码列导出 `PTCABC`，导入端按 MDM 14 位 `dcc_product_code` 校验失败。
- RED: local migration export/import-preview -> FAIL，本机导出 `recognition-migration-20260704-230109.xlsx` 后回传预览，total=1、applicable=0、blocked=1，失败原因 `MDM_PRODUCT_DCC_CODE_INVALID`。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests；回归断言迁移包产品编码列导出 MDM 14 位 DCC 产品编号。
- GREEN: local migration export/import-preview after fix -> PASS，本机后端重启后重新导出 `outputs/dcc-recognition-migration-local-check/recognition-migration-after-fix-20260704-231853.xlsx` 并回传预览，total=1、applicable=1、blocked=0、failedRecognition=0。
