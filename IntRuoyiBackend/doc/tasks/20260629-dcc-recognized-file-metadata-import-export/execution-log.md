# Execution Log：DCC 已识别文件名/文件编号导入导出

BDD: 已识别文件可按当前浏览筛选导出 -> Given 受控浏览页存在已识别并回写 `fileName`/`fileNumber` 的受控文件且用户带着当前目录/关键字/状态筛选 / When 用户执行导出 / Then 后端导出结果只包含命中当前筛选且文件名称、文件编号均可用的记录。

BDD: 导入模板明确文件 ID 与目标字段 -> Given 文控角色准备批量修正已识别文件名称与文件编号 / When 用户下载导入模板 / Then 模板包含受控文件 ID、文件名称、文件编号等必要列，便于后续预览校验。

BDD: 导入预览先暴露失败不直接覆盖 -> Given 用户上传包含文件名/文件编号修正的 Excel / When 执行导入预览 / Then 后端逐行校验文件存在性、角色权限、必填字段与编号冲突，并返回新增失败原因而不直接落库。

BDD: 导入确认复用正式元数据校验链路 -> Given 某批导入预览无失败行 / When 用户确认导入 / Then 后端逐行复用受控文件元数据更新校验逻辑写回 `fileName`、`title`、`fileNumber`，并返回批次结果。

BDD: 未识别或业务字段为空的记录不得伪装为可导出成功 -> Given 某受控文件没有完成文件名称/文件编号回写 / When 用户执行导出 / Then 导出结果不得把空文件名称/编号伪装为成功识别结果。

- 2026-06-29：GREEN: task-bootstrap -> PASS，已创建 `task.md` 与 `execution-log.md`，并完成经验门禁摘录。
- 2026-06-29：RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileMetadataImportExportControllerTest,DccControlledFileMetadataImportExportServiceTest,DccControlledFileMetadataUpdateServiceTest" test` -> FAIL，预期原因：导入导出控制器映射、服务接口、Excel VO 与预览确认实现尚不存在。
- 2026-06-29：GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccControlledFileMetadataImportExportControllerTest,DccControlledFileMetadataImportExportServiceTest,DccControlledFileMetadataUpdateServiceTest" test` -> PASS，15 项后端控制器/服务测试通过。
- 2026-06-29：GREEN: `npx eslint D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\dcc\controlledFile\workflow.ts D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\browser\index.vue` -> PASS，前端导入导出 API 与浏览页交互无定向 lint 错误。
- 2026-06-29：BLOCKER: `npm run ts:check` -> 仓库级失败，命中既有非本次范围错误：`src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue` 中 `recordCategory: "TEMPLATE"` 与 `EdhrRecordCategory` 不兼容。
