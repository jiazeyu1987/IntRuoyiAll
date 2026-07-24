# Execution Log

- BDD: eDHR BPM 任务分配通知不应再依赖短信手机号 -> Given 流程定义为 `mes-edhr-approval-v1` / When BPM 分配审批任务给审核人 / Then 系统必须发送正式站内信而不是短信。
- BDD: eDHR BPM 审批通过通知不应再依赖短信手机号 -> Given 流程定义为 `mes-edhr-approval-v1` / When 审批通过后通知发起人 / Then 系统必须发送正式站内信而不是短信。
- BDD: 非 eDHR BPM 流程不受影响 -> Given 流程定义不是 `mes-edhr-approval-v1` 且不是 `dcc-controlled-file-approval` / When BPM 发送任务或审批通知 / Then 系统仍保持原有短信路径。

## Phase: edhr-bpm-notify-to-inbox

- changed paths:
  - `yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/message/BpmMessageServiceImpl.java`
  - `yudao-module-bpm/src/test/java/cn/iocoder/yudao/module/bpm/service/message/BpmMessageServiceImplTest.java`
  - `sql/mysql/20260622_mes_edhr_bpm_notify_to_inbox.sql`
  - `script/tests/test_edhr_bpm_notify_to_inbox_sql.py`
  - `doc/tasks/20260622-edhr-bpm-notify-to-inbox/task.md`
  - `doc/tasks/20260622-edhr-bpm-notify-to-inbox/execution-log.md`
- implemented behavior:
  - 为 `mes-edhr-approval-v1` 增加独立站内信模板路由，覆盖任务分配、审批通过、审批驳回、任务超时四类 BPM 通知。
  - 保持 DCC 既有站内信逻辑不变，保持其他普通 BPM 流程短信逻辑不变。
  - 新增 eDHR BPM 站内信模板 SQL 和对应 SQL 契约测试。
RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_bpm_notify_to_inbox_sql.py -q` -> FAIL，修复前缺少 eDHR BPM 站内信模板 SQL 种子，SQL 契约无法通过。
RED: `mvn -pl yudao-module-bpm -Dtest=BpmMessageServiceImplTest test` -> FAIL，修复前 `mes-edhr-approval-v1` 仍走短信路径，没有独立站内信模板路由。
- validation commands:
  - `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_bpm_notify_to_inbox_sql.py -q`
  - `mvn -pl yudao-module-bpm -Dtest=BpmMessageServiceImplTest test`
- validation results:
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_bpm_notify_to_inbox_sql.py -q` -> PASS，2 passed
GREEN: `mvn -pl yudao-module-bpm -Dtest=BpmMessageServiceImplTest test` -> PASS，9 tests passed
- BLOCKER: `python -X utf8 D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-preview-int-main-clean\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-preview-int-main-clean\sql\mysql` -> FAIL，`20260622_mes_edhr_bpm_notify_to_inbox.sql` 的 `dependsOn` 误写为带 `.sql` 后缀的文件名，迁移策略门禁只识别 migrationId，导致主线合并后误报缺失依赖。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-preview-int-main-clean\script\tests\test_edhr_bpm_notify_to_inbox_sql.py -q` -> FAIL，修正前 `release-migration` 头使用 `.sql` 后缀依赖名，新补的 SQL 契约断言会识别为非法 migrationId。
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-preview-int-main-clean\script\tests\test_edhr_bpm_notify_to_inbox_sql.py -q` -> PASS，3 passed，SQL 契约已覆盖 `dependsOn` 只能写 migrationId、不能带 `.sql` 后缀。
- GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-preview-int-main-clean\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-preview-int-main-clean\sql\mysql` -> PASS，已将 `dependsOn` 改为 `20260611_mes_edhr_work_task_flow,20260612_mes_edhr_final_archive_work_task`，主线发布级迁移门禁恢复通过。
- known risks or blockers:
  - 本任务只修复通知机制，不替代主线程对 eDHR 全链路演练结果的最终汇总
