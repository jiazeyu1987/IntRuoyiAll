# DCC 识别记录导出空数据后端修复

## 任务目标

修复识别记录导出服务在批量任务账本存在时仍可能导出空 Excel 的问题。

## 里程碑

1. 补充后端服务回归测试，复现候选列表为空但任务账本存在的导出场景。
2. 修复导出取数逻辑，支持按 `batchRecognitionTaskId` 直接导出任务账本。
3. 运行后端目标测试并记录证据。

## 预期验证

- `DccControlledFileMetadataImportExportServiceTest` 新增用例 RED 后 GREEN。
- 导出 Excel 至少包含表头和任务账本记录行，不再只输出表头。

## 当前状态

已完成：识别记录导出已支持按 `batchRecognitionTaskId` 直接导出任务账本，并补回受控文件信息生成 Excel。

## 经验门禁

- `docs/powershell-memory.md`：PowerShell 命令和中文输出显式 UTF-8，不使用 `&&`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。
- 是否存在临时补丁或绕过：否。

## 验证结论

- RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest#recognitionRecordExport_batchTaskRowsDoNotDisappearWhenBrowserCandidatesAreEmpty" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`selectListByBatchTaskId` 尚不存在。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest#recognitionRecordExport_batchTaskRowsDoNotDisappearWhenBrowserCandidatesAreEmpty" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，7 tests。
