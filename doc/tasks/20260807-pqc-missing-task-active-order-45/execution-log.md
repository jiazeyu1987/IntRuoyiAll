# 执行日志

## User Intent

用户报告：当前工序缺少待执行 PQC 检验任务，`activeOrderId=45`，`routeProcessId=null`，`processId=null`。

## BDD

- BDD: 当前活跃订单待执行 PQC 任务 -> Given 已加入的活跃订单存在正式路线工序和有效 PQC 规程，When 打开/查询当前工序待检任务，Then 系统返回或生成可追溯到正式 `routeProcessId/processId` 的 `PENDING` PQC 检验任务。
- BDD: 已提交任务不进入待检列表 -> Given 活跃订单已有 `SUBMITTED` PQC 检验记录，When 查询待执行 PQC 检验任务，Then `SUBMITTED` 记录不作为待执行任务返回，且不会用空工序提示替代缺失的 `PENDING` 任务。

## Execution Evidence

- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md` 和 `references\bug-contract.md`。
- 已读取 `docs/backend-development.md`、`docs/database-rules.md`、`docs\engineering\technology-stack-routing.md`。
- 已读取 `docs\experience-index.md`，命中 PQC 待检工单任务链路门禁。
- 根因定位：`MesFrontlinePqcContextServiceImpl.groupTasksByProcess()` 对 `routeProcessId/processId` 为空的 PQC 任务执行静默过滤，导致真实脏数据被后续通用 `PRO_FRONTLINE_PQC_TASK_REQUIRED(activeOrderId, null, null)` 掩盖。
- 实施修复：非 `CANCELLED` 的 PQC 任务缺少正式 `routeProcessId/processId` 时直接抛出 `PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH`，并输出 `taskId/activeOrderId/routeProcessId/processId`。
- 数据闭环：新增 `sql/mysql/20260807_mes_pqc_task_identity_closure.sql`，按正式 PQC 规程和活跃订单工序快照回填缺失身份，校验未解析、重复和快照不匹配后再收紧 `route_process_id/process_id NOT NULL`。
- 防复发：更新 `20260802_role_requirement_matrix_m6_migration_preflight.sql`，PQC 任务权限预检同时拦截 `route_process_id` 或 `process_id` 为空。
- Bug evidence validator：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260807-pqc-missing-task-active-order-45\bug-regression-evidence.md` -> PASS，`Bug regression evidence is valid.`。
- Cleanup preview：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260807-pqc-missing-task-active-order-45 --mode preview` -> READY，保留 `task.md/execution-log.md/verification-report.md`，删除临时 `bug-regression-evidence.md` 与 `migration-policy-gate.json`，无 blocked/warnings。
- Cleanup apply：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260807-pqc-missing-task-active-order-45 --mode apply` -> APPLIED，已删除临时证据和迁移策略输出文件，主工作区 `linked=False` 未触发 worktree merge/remove。
- Experience consolidation：已合并长期经验到 `docs/backend-development.md#mes-pqc-项目级检验快照门禁` 和 `docs/experience-index.md`，补充非取消 PQC 任务缺正式工序身份时必须 fail fast、预检/迁移同时拦截 `route_process_id/process_id` 为空。

## RED / GREEN

- BDD: 当前活跃订单待执行 PQC 任务 -> Given 活跃订单存在正式路线工序和有效 PQC 规程，When 查询当前工序待检任务，Then 返回或明确阻断缺少正式工序身份的 `PENDING` PQC 检验任务。
- RED: `python -X utf8 -m pytest script\tests\test_mes_pqc_task_identity_closure_sql.py -q` -> FAIL，迁移文件缺失且 M6 预检未覆盖 `process_id IS NULL`。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldFailFastWhenPendingPqcTaskMissingFormalProcessIdentity" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED，初次 RED 运行与并发 MES Maven 编译争用同一模块 `target`，未作为产品失败结论。
- GREEN: `python -X utf8 -m pytest script\tests\test_mes_pqc_task_identity_closure_sql.py -q` -> PASS，`3 passed in 4.42s`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldFailFastWhenPendingPqcTaskMissingFormalProcessIdentity" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260807-pqc-missing-task-active-order-45\migration-policy-gate.json` -> PASS，`status=passed`，`migrationCount=447`。

## Blockers

- 暂无。未执行远程或生产数据库 SQL；本任务仅提交代码、静态 SQL 合约和本地回归验证。

## Reopen 2026-08-07 22:xx

- 用户反馈：仍然出现同一错误。
- 重新打开排查：先核对 48081 运行 Jar 是否包含 PQC fail-fast 修复，再核对本地库 activeOrderId=45 的 PQC 任务身份数据是否已迁移；不直接改库、不重启未知进程。
- 运行态根因：旧 48081 Jar `backend-latest-20260807-2158-process-config-responsible-routes.jar` 内的 `MesFrontlinePqcContextServiceImpl` 仍包含 `PRO_FRONTLINE_PQC_ACTIVE_ORDER_EMPTY`，且缺少 `CANCELLED/pqcTaskIdentityText/selectActiveOrderIdsByTaskStatus` 标记，说明用户复现时本地运行态未加载本任务修复。
- 数据核对：本地库 `activeOrderId=45` 的 PQC 任务为 `SUBMITTED` 1 条、`PENDING` 0 条，且任务身份已是 `route_process_id=980675/process_id=922985`；因此该活跃订单不应继续出现在一线 PQC 待检工单列表。
- 本地运行态修复：以当前运行 Jar 为底包，仅替换 `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar` 为已验证 target 模块，生成 `output/runtime/int_main/backend-latest-20260807-2215-pqc-missing-task-active-order-45.jar`；nested jar 校验 `compress_type=0`，新 Jar 内 class 包含 `CANCELLED/pqcTaskIdentityText/selectActiveOrderIdsByTaskStatus`。
- Runtime restart：已停止明确归属当前 `int_main` 的旧 48081 PID `53868`，启动新 PID `6360`，`http://127.0.0.1:48081/actuator/health` 返回 `UP`，进程命令行指向 `backend-latest-20260807-2215-pqc-missing-task-active-order-45.jar`。
- 登录态只读复核：默认本机租户/账号调用 `/admin-api/mes/pro/feedback/frontline/device-account/pqc/active-orders` 返回 `PQC_ACTIVE_ORDER_COUNT=6` 且 `CONTAINS_ACTIVE_ORDER_45=False`，确认 `activeOrderId=45` 已不再作为待执行 PQC 工单暴露。
- Experience consolidation：本次新增教训已被现有 `docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁` 覆盖，未新增长期经验文档。
- 二次 cleanup：`task-closeout-cleanup --mode preview/apply` 已删除 `runtime-jar-check*` 与 `runtime-jar-stage` 临时目录文件；任务目录最终仅保留 `task.md/execution-log.md/verification-report.md`。
