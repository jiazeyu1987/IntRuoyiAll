# 执行日志：DCC 批量识别锁等待超时修复

BDD: 并发 worker 不争抢任务进度行 -> Given 批量识别任务配置 5 个 worker 且候选文件为多个不同文件 / When 各 worker 并发完成单文件识别 / Then 单文件识别和识别账本可并发执行，但任务进度行只由协调线程按快照持久化，不因 worker 同时更新同一行产生锁等待超时。
BDD: 文件识别失败仍记录失败账本 -> Given 某个文件识别服务在写入成功账本前失败 / When 批量任务继续处理后续文件 / Then 系统记录该文件失败账本和最后错误，并完成任务统计，不吞异常也不返回默认成功。

- INFO: 截图现象为批量识别进度中 `总数=32`、`配置 Codex=5`、`运行 Codex=5`、`已处理=3`、`失败=3`、最后错误 `Lock wait timeout exceeded; try restarting transaction`。
- ROOT-CAUSE: `DccControlledFileBatchRecognitionServiceImpl.processCandidates` 在同一任务内启 5 个 worker；旧实现每个 worker 的 `processOneCandidate -> updateProgressAfterCandidate -> updateTaskProgress` 都会写同一条 `dcc_controlled_file_batch_recognition_task` 任务进度行，形成高频热点行更新。
- RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest#processWaitingTasksDoesNotLetWorkersConcurrentlyUpdateTaskProgressRow" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现 worker 并发写任务进度行，测试抛出 `task progress row updated concurrently by worker threads`，任务从 RUNNING 变为 FAILED。
- FIX: `DccControlledFileBatchRecognitionServiceImpl` 多 worker 模式改用 `ExecutorCompletionService<CandidateOutcome>`；worker 返回单文件结果，协调线程串行调用 `updateProgressAfterCandidate` 写任务进度。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am clean "-Dtest=DccControlledFileBatchRecognitionServiceTest#processWaitingTasksDoesNotLetWorkersConcurrentlyUpdateTaskProgressRow" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，15 tests。- GREEN: bug regression evidence validation -> PASS，`Bug regression evidence is valid.`
- GREEN: backend API evidence validation -> PASS，`Backend API evidence is valid.`
- GREEN: task-closeout preview -> PASS，keep task/core evidence only, delete none, blocked none, warnings none.