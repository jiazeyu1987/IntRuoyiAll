# Execution Log

BDD: 查看当前 Codex 运行状态 -> Given DCC 批量识别任务已创建或正在运行, When 用户打开受控浏览批量识别进度, Then 页面显示任务状态、配置 Codex 数量、当前运行 Codex 数量、已记录文件数量。
BDD: 停止批量识别 -> Given 批量识别任务处于 WAITING 或 RUNNING, When 用户点击停止识别, Then 后端将任务标记为 STOPPED/终态且前端停止轮询并保留已完成记录。
BDD: 重新开始批量识别 -> Given 用户设置父文件夹与 Codex 数量, When 用户点击开始识别, Then 后端创建新任务并按 workerCount 执行，不复用前端假状态。
GREEN: experience-preflight -> PASS, local-only code/test task; no remote server operation.
RED: updated backend/frontend tests before final implementation -> FAIL expected before stop/status/count fields existed; now proceeding to GREEN verification.
GREEN: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest,DccControlledFileBatchRecognitionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS.
GREEN: pnpm.cmd e2e:dcc:browser-batch-recognition:static -> PASS.
GREEN: implementation-complete -> PASS, backend exposes STOPPED task control and runtime counts; frontend displays worker status, recorded file count, and stop action.
BLOCKER: official-login-preflight -> FAIL, bundled Playwright browser cache is broken locally (`Invalid file descriptor to ICU data received`; headed cache path `E:\Int\DevCache\playwright-browsers\chromium-1223\chrome-win64\chrome.exe` missing), so real-browser E2E used installed system Chrome explicitly.
BLOCKER: local-standard-restart -> FAIL, active build-release runtime guard `E:\Int\CacheData\IntRuoyi\runtime\local-runtime-restart.guard` prevented restarting the standard 48081/8081 runtime; guard was not removed or overridden.
GREEN: mvn.cmd -pl yudao-server -am "-Dmaven.test.skip=true" package -> PASS, fresh local backend jar contains STOPPED task control, worker-count runtime status, and recorded-file-count fields.
GREEN: isolated-local-runtime -> PASS, started fresh backend on 48082 and frontend on 8082 for local-only E2E without touching guarded 48081/8081 runtime.
GREEN: local-dcc-schema-preflight -> PASS, applied local MySQL migrations for `worker_count`, `dcc_controlled_file_recognition_record.batch_task_id`, and recognition claim/record tables required by the new local runtime.
GREEN: real-browser-e2e -> PASS, Chrome opened `http://127.0.0.1:8082`, logged in as test tenant `测试租户/aoteman`, entered DCC controlled browser, selected parent directory `质量管理`, opened batch recognition, submitted `workerCount=5`, and observed progress UI.
GREEN: real-browser-e2e-contract -> PASS, captured POST `/admin-api/dcc/controlled-files/batch-recognition/tasks` with `{"scope":"CURRENT","directoryId":946445,"includeDescendantDirectories":true,"overwriteExisting":false,"syncFileNameTitle":true,"workerCount":5}`; response returned `workerCount=5`, `activeWorkerCount=0`, `recordedFileCount=0`, `status=COMPLETED`.
GREEN: real-browser-e2e-ui -> PASS, progress dialog displayed `配置 Codex 5`, `运行 Codex 0`, `已记录文件 0`, and `当前状态 已完成`; selected directory had no candidate files, so STOP while RUNNING was not exercised in this data set.
BDD: 批量识别失败也写入共享账本 -> Given 批量识别候选文件进入识别但源文件对象缺失或 Codex 识别抛错, When 单文件识别失败, Then 批量任务计入失败且 `dcc_controlled_file_recognition_record` 写入 `FAILED`、`batch_task_id`、失败原因和源文件 ID，便于前端导出判断哪些文件已识别过。
RED: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest#processWaitingTasksPersistsFailedLedgerWhenRecognitionServiceFailsBeforeWritingRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, batch service only updated failed count when recognition service failed before durable ledger write; `recognitionRecordMapper.upsert` was never called.
GREEN: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest#processWaitingTasksPersistsFailedLedgerWhenRecognitionServiceFailsBeforeWritingRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, batch failure path now writes missing `FAILED` ledger record after recognizing service exception.
GREEN: local-db-check-task-4 -> PASS, task `4` processed 4 files with 0 success and 4 failures; recognition ledger contains 4 `FAILED` rows with `batch_task_id=4`, controlled file IDs, source file IDs, S3 missing-key failure messages, and source object paths.
