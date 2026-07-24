# 执行日志

- BDD: 批量任务账本记录不因候选列表为空而丢失 -> Given 批量任务已经产生成功或失败账本记录 / When 导出带 batchRecognitionTaskId 的识别记录 / Then 后端必须从任务账本导出记录，不得只输出空表头。
- RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest#recognitionRecordExport_batchTaskRowsDoNotDisappearWhenBrowserCandidatesAreEmpty" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`selectListByBatchTaskId` 尚不存在。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest#recognitionRecordExport_batchTaskRowsDoNotDisappearWhenBrowserCandidatesAreEmpty" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，7 tests。
