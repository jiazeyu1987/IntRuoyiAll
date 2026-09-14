# 执行日志：一线生产取消工单匹配上下文

BDD: 一线生产运行态不匹配工单 -> Given 一线生产员工进入已授权路线工序 / When 后端加载 runtime-config 且当前组长没有 activeOrder/workOrder/task / Then 接口仍返回员工、设备、损耗和生产提交基础上下文，不报 `productionSubmitContext.activeOrder routeId=...`。

BDD: 一线生产仍校验真实必填身份 -> Given 一线生产正式提交 / When 缺少实际填写员工、签名密码或授权工序身份 / Then 后端继续 fail fast，不使用默认员工、默认工单或吞异常。

- INFO: user-scope-change -> 用户明确说明“一线生产不需要匹配任何工单”。
- INFO: rules -> 已读取 bug-regression-fix-loop、task closeout、PowerShell/编码、backend-development。
- BDD: 一线生产正式提交不匹配工单 -> Given 一线生产员工选择路线/工序/工位/员工并输入所选员工电子签名密码 / When 正式提交没有 workOrderId、taskId、itemId、recordbookId、recordbookPayload / Then 后端创建反馈和工序池生产提交事件，记录签名员工为实际填写员工，不生成默认工单或默认记录本。
- BDD: PQC 仍保持工单上下文 -> Given 一线 PQC 检验提交 / When 缺少 activeOrder/workOrder/task 或记录本来源 / Then PQC 链路继续 fail fast，不受一线生产无工单口径影响。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolEventServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before fix，旧运行态/提交链路要求 activeOrder/workOrder/task/recordbook，无法覆盖无工单一线生产提交。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolEventServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，22 tests，0 failures，0 errors。
- GREEN: `node tests\e2e\frontline-formal-submit-static.spec.cjs` -> PASS，正式提交静态合同不再要求 workOrderId/taskId/itemId/recordbookId，recordbookPayload 可选。
- GREEN: `node tests\e2e\frontline-formal-submit-selected-employee-static.spec.cjs` -> PASS，所选员工电子签名合同通过。
- GREEN: `node tests\e2e\role-matrix-ac-m10-sop-production-static.spec.cjs` -> PASS，SOP 生产角色矩阵静态合同通过。
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\fix-frontline-production-no-work-order-context\migration-policy-gate.json` -> PASS，status=passed，migrationCount=449，包含 `20260808_mes_process_pool_frontline_no_work_order`。
- GREEN: scoped `git diff --check -- <task-owned files>` -> PASS，仅输出 LF/CRLF 工作区提示，无 whitespace error。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\fix-frontline-production-no-work-order-context\bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\fix-frontline-production-no-work-order-context\database-schema-evidence.md` -> PASS。
- BLOCKED: `mvn -pl yudao-module-mes "-Dtest=MesP0FrontlineSubmitIdempotencyTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED，同模块共享 `target` 被并发 Maven 编译/测试占用；一次隔离目录尝试发现 Maven 日志仍写入共享 `target\classes` 后已立即停止本任务启动的命令，未清理、未强杀其它进程。
- BLOCKED: 并发确认 -> 3 分钟等待窗口内仍有 6 个 MES Maven 相关进程运行，包括 `mvn -pl yudao-module-mes -DskipTests compile` 与 `mvn -pl yudao-module-mes -am -DskipTests compile`；继续补跑相邻 JUnit 会扩大产物污染风险。
- INFO: no-fallback-check -> 未引入默认工单、默认任务、默认物料、默认记录本或吞异常；缺少路线/工序/工位/设备账号/实际员工/签名密码仍 fail fast。
- INFO: experience-consolidation -> 已按 `project-experience-consolidation` 规则查找长期经验归宿；本次经验已合并在既有 `docs/backend-development.md` 一线生产正式提交门禁与 `docs/experience-index.md`，未新建长期经验文档。
