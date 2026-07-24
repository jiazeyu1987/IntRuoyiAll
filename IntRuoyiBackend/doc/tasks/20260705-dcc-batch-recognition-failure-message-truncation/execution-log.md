# 执行日志：DCC 批量识别失败原因截断修复

- BDD: 批量识别失败原因过长时账本仍可保存 -> Given 批量识别中的单文件项目码识别抛出超过 failure_message 列宽的真实异常 / When 服务保存失败识别账本 / Then 失败账本保留可读原因且长度不超过 512，不再触发 Data truncation。
- INFO: powershell-preflight -> PASS，已读取 `docs/powershell-memory.md`，后续命令使用 UTF-8 显式读取或 `apply_patch`。
- INFO: bug-contract -> PASS，已读取 `bug-regression-fix-loop` 缺陷证据契约。
- INFO: root-cause -> 当前 `DccControlledFileBatchRecognitionServiceImpl#normalizeLastFailureMessage` 只保护任务进度 `last_failure_message` 和批量补写账本路径；`DccControlledFileProjectCodeRecognitionServiceImpl#resolveFailureMessage` 仍可能把超长异常文本直接写入 `dcc_controlled_file_recognition_record.failure_message`。
- RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_truncatesLongFailureMessageBeforePersistingRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因：新增回归用例断言失败账本 `failureMessage.length() <= 512`，旧实现保存 600 字符异常文本。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_truncatesLongFailureMessageBeforePersistingRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，43 tests。
- INFO: completed -> 已将项目码识别失败账本入口统一按 `failure_message` 列宽 512 截断，原异常仍继续抛出给批量任务计失败，不引入 fallback。
- GREEN: bug-regression-evidence-validation -> PASS。
- GREEN: task-closeout-preview -> PASS，keep `task.md`、`execution-log.md`、`bug-regression-evidence.md`，delete `<none>`，blocked `<none>`。
