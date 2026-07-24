# Execution Log: DCC 批量识别 5 个 Codex 并发

BDD: 同一批量识别任务最多 5 个 Codex 并发 -> Given 受控浏览全域存在多个待识别文件 / When 管理员启动批量识别且 worker-count=5 / Then 后端在同一任务内最多并发处理 5 个文件，并持续更新成功、失败、跳过和剩余数量。

BDD: 同一文件不会被多个 Codex 重复识别 -> Given 多个 worker 同时处理候选文件 / When 某个文件已被一个 worker 取得识别 claim / Then 其他 worker 不能取得同一文件同一识别范围的 claim，必须失败暴露或跳过既有成功记录。

BDD: 已成功识别账本默认跳过 -> Given 文件在同一 recognition version 下已有成功识别记录 / When 批量识别未勾选覆盖已有值 / Then 该文件计入 skippedExistingCount，不重新调用 Codex。

GREEN: experience-preflight -> PASS，已读取 PowerShell、服务器、发布、后端与数据库交付门禁；本阶段只做本机源码与测试改动，暂不执行测试服发布/重启。

RED: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，旧实现为单任务串行 for 循环，新增并发回归测试会因 5 个候选无法同时进入 worker 而失败。
GREEN: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，9 tests，覆盖 worker-count=5 的任务内并发与既有跳过/claim/失败统计回归。
GREEN: python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q -k "publish_runtime_preserves_dcc_project_code_codex_configuration or publish_compose_uses_isolated_runtime_names_ports_and_dcc_config" -> PASS，2 tests，确认业务仓 release 脚本保持原有合同；测试服 worker-count 发布透传由 IntRuoyiMaintance 维护仓落地。
GREEN: backend-api-evidence -> PASS，`python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260701-dcc-batch-recognition-five-workers\backend-api-evidence.md` 通过；随后按 closeout preview 清理该临时 evidence 文件，仅保留 task.md 与 execution-log.md。
GREEN: task-closeout-preview -> PASS，`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260701-dcc-batch-recognition-five-workers --mode preview` 返回 ready，blocked `<none>`。
GREEN: release-script-boundary -> PASS，按“发布链路统一在 IntRuoyiMaintance 修改”门禁撤回业务仓 `script/deploy` 与 `script/tests` 的发布配置改动；业务仓最终只保留后端并发实现、配置绑定与任务记录。
BLOCKER: test-server-five-worker-runtime -> 当前测试服务器 IMAGE_TAG=release-20260701-1720-message-text-fix，远端 .env 缺少 DCC_PROJECT_CODE_RECOGNITION_WORKER_COUNT=5，backend 启动参数缺少 project-code-recognition.worker-count，运行 jar 不含新增并发 worker 代码；已按 fail-fast 阻止启动产品识别，需先发布已提交的后端与维护仓发布链路改动到测试服。

BDD: 进度弹窗持续识别到文件夹完成 -> Given 管理员对当前父文件夹启动批量识别 / When 任务仍在 WAITING 或 RUNNING / Then 页面只允许隐藏进度，不提供手动停止按钮，后端 worker 持续处理直到候选文件结束。

BDD: 成功失败卡片显示对应记录 -> Given 批量识别已有成功或失败记录 / When 管理员点击进度弹窗中的成功或失败数量 / Then 受控浏览列表按 recognitionStatus 与 batchRecognitionTaskId 过滤展示对应记录。

BDD: 已有账本计入成功失败总数 -> Given 文件已有同一识别范围和识别版本的 SUCCESS 或 FAILED 记录 / When 未勾选覆盖重新识别 / Then 后端不重复调用 Codex，但将已有 SUCCESS 计入 successCount、已有 FAILED 计入 failedCount，并保持成功 + 失败 = 总数。

RED: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，旧实现把已有 SUCCESS/FAILED 账本统一计入 skippedExistingCount，导致成功 + 失败 != 总数。

RED: pnpm.cmd e2e:dcc:browser-batch-recognition:static -> FAIL，旧前端仍存在停止识别按钮，且成功/失败卡片没有记录钻取筛选入口。

GREEN: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，14 tests，覆盖已有 SUCCESS/FAILED 账本按成功/失败计数且不重复调用 Codex。

GREEN: pnpm.cmd e2e:dcc:browser-batch-recognition:static -> PASS，确认进度弹窗移除手动停止、成功/失败可点击筛选、前端请求携带 recognitionStatus 与 batchRecognitionTaskId。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check -> PASS，前端 relaxed 类型检查通过；首次未设置堆内存时 Node OOM exit=134，按现有前端大项目构建方式提高本地类型检查堆内存后通过。

GREEN: task-closeout-preview -> PASS，`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260701-dcc-batch-recognition-five-workers --mode preview` 返回 ready，blocked `<none>`。
