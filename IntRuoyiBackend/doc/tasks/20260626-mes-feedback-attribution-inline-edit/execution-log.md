# 执行日志：MES 报工归属草稿保存与再次归属后端改造

## 2026-06-26

- 初始化任务：创建后端任务包，记录门禁、设计约束、BDD 与测试目标。
- RED: `mvn --% -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest,MesProFeedbackServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，H2 测试表缺少 `mes_pro_feedback.source_import_record_id`。
- CHANGE: 新增 `source_import_record_id` 到 `mes_pro_feedback` 的测试建表、基础 schema 与增量迁移，并回填历史单条链路。
- CHANGE: `MesProFeedbackImportRecordServiceImpl` 的首次归属与再次归属统一走同一套 `applyAttribution`；再次归属先校验链路完整性、正式报工草稿态与缓存池消费安全，再执行回滚/重建。
- CHANGE: `MesProFeedbackServiceImpl.submitFeedback` 在草稿提交到 `APPROVING` 后触发排产进度同步；归属阶段不再自动提交、不再自动同步进度。
- CHANGE: 新增 `createFeedbackWithScheduleSnapshot` 内部入口，导入归属显式保留排产快照，避免当前订单超产归属在真实链路被普通建单剩余量校验误拦。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest,MesProFeedbackServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，33 个定向后端回归全部通过。
- GREEN: `experience-preflight` -> PASS，本轮仅对本机 Docker MySQL 执行已存在的幂等 MES schema 迁移与本地重启脚本保底修复，不涉及远端环境。
- RED: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT source_import_record_id FROM mes_pro_feedback LIMIT 1;"` -> FAIL，真实本机库报 `Unknown column 'source_import_record_id' in 'field list'`。
- INVESTIGATION: `DESCRIBE mes_pro_feedback; SHOW CREATE TABLE mes_pro_feedback; SHOW TABLES LIKE 'mes_pro_feedback_surplus_%';` -> PASS，确认本机 `mes_pro_feedback` 缺少 `source_import_record_id` 与索引，但 `mes_pro_feedback_surplus_pool` / `mes_pro_feedback_surplus_allocation` 已存在，根因收敛为本地 runtime schema 漂移而非 Mapper 查询错误。
- CHANGE: `script/deploy/restart-int-ruoyi-local.ps1` 新增 `20260624_mes_feedback_surplus_pool.sql` 与 `20260626_mes_feedback_import_reattribute_link.sql` 的本地 schema 保底探针与执行入口，避免本机历史库缺少正式报工来源列时继续启动旧结构。
- RED: `git show HEAD:script/deploy/restart-int-ruoyi-local.ps1 | rg "20260624_mes_feedback_surplus_pool.sql|20260626_mes_feedback_import_reattribute_link.sql"` -> FAIL，HEAD 旧版本未把这两条 MES 迁移纳入本机标准重启保底流程。
- GREEN: `python -X utf8 -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q` -> PASS，`11 passed in 0.13s`。
- GREEN: `DESCRIBE mes_pro_feedback; SELECT source_import_record_id FROM mes_pro_feedback LIMIT 5; SHOW INDEX FROM mes_pro_feedback WHERE Key_name = 'idx_mes_pro_feedback_source_import_record_id';` -> PASS，真实本机库已存在 `source_import_record_id` 与 `idx_mes_pro_feedback_source_import_record_id`。
- GREEN: `SELECT COUNT(*) AS total_feedback_rows, SUM(CASE WHEN source_import_record_id IS NOT NULL THEN 1 ELSE 0 END) AS linked_feedback_rows FROM mes_pro_feedback; SELECT COUNT(*) AS import_rows_with_feedback FROM mes_pro_feedback_import_record WHERE deleted = b'0' AND feedback_id IS NOT NULL AND feedback_id > 0;` -> PASS，回填结果为 `258 / 257 / 257`，历史正式报工与导入记录关联数量一致。
- CHANGE: 新增 `bug-regression-evidence.md` 与 `database-schema-evidence.md`，记录本机 runtime schema 漂移根因、保底迁移入口、数据安全分析与真实库验证结果。
- BLOCKER: scope-switch -> 当前线程切换到“角色管理三分改名与导航重组”独立菜单任务；为避免与 MES 归属链路混提，先将本任务文档显式标记为 BLOCKED，待后续独立上下文继续收尾与提交。
- RESUME: 当前线程按用户新需求恢复本任务，目标补齐“有剩余报工依然可以重排”的后端合同与回归验证；本次继续只处理 MES 报工归属 / 自动重排相关文件。
- BDD: 已有受保护任务但仍有剩余报工量时允许重排续排 -> Given 排产工序已有已完成/已报工等受保护任务且 remainingQuantity 仍大于 0 / When 用户预览并应用重排 / Then 系统保留原受保护任务，同时为剩余量继续生成新的活动任务，而不是直接阻断整次重排。
- RED: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldKeepFinishedTaskAndGenerateNewActiveTaskForRemainingQuantity+replanApply_shouldKeepFinishedTaskAndCreateNewActiveTaskForRemainingQuantity -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，旧逻辑把“工序存在受保护旧任务”直接视为整工序已承接，未按 remainingQuantity 续排，预览出现 `ACTIVE_TASK` 阻断，应用抛 `1040250017`。
- CHANGE: `MesProAutoScheduleServiceImpl` 改为按 `scheduleOrderProcess.remainingQuantity` 计算该工序重排量；当工序已有受保护旧任务且仍有剩余量时，保留旧任务时间占位，但继续为剩余量生成新的 AUTO 活动任务，不再整工序跳过。
- CHANGE: 分段 / 无限产能任务的 `quantity` 改为基于当前工序待续排量分摊，而不是继续按整张工单数量写入，避免“剩余 0.5 仍重排出整单 1.0”。
- CHANGE: `buildLinkPlans` 过滤同工序相邻步骤的伪依赖，避免“旧受保护任务 + 新续排任务”并存时生成同工序自依赖。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldKeepFinishedTaskAndGenerateNewActiveTaskForRemainingQuantity+replanApply_shouldKeepFinishedTaskAndCreateNewActiveTaskForRemainingQuantity -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，33 个自动排产 / 重排后端回归全部通过。
- RESUME: 用户反馈“仍然报错，怀疑后端未更新”；本轮先做本机 runtime 诊断，不新增业务代码。
- INVESTIGATION: `Get-CimInstance Win32_Process ... ; netstat -ano | Select-String '48081'` -> PASS，确认本机实际服务端口 `48081` 由 PID `59992` 提供，启动时间为 `2026-06-26 15:47:51`。
- INVESTIGATION: `git show -s --format='%H%n%ci%n%s' 08b8156677` -> PASS，确认“剩余报工量仍可重排续排”修复提交时间为 `2026-06-26 15:50:18 +0800`，晚于当时在线后端进程启动时间，根因确定为本地后端尚未加载本次修复。
- GREEN: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi-backend.bat` -> PASS，按项目标准脚本完成本机后端重启并重新打包最新 jar。
- GREEN: `Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*48081*' -and $_.Name -eq 'java.exe' }` -> PASS，重启后在线后端切换为 PID `57188`，启动时间 `2026-06-26 16:04:25`，运行 jar 为 `E:\Int\CacheData\IntRuoyi\runtime\backend-20260626-160303.jar`。
- GREEN: `Invoke-WebRequest -UseBasicParsing http://localhost:48081/actuator/health` -> PASS，返回 `{\"status\":\"UP\"}`，确认本机后端已加载最新构建并恢复健康。
