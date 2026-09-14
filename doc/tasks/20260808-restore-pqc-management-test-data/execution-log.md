# Execution Log

## User Intent

- 用户反馈 `PQC组长 > PQC管理` 页面显示 `No Data`，要求恢复里面的测试数据。

## BDD

- BDD: PQC 管理测试数据恢复 -> Given 指定 PQC 组长账号具有唯一启用人员范围且范围内检验员存在已提交 PQC 记录，When 打开 `PQC管理` 页签，Then 列表展示提交时间、PQC 检验员、工序、生产工单、检验数量、损耗数量、损耗明细、产品和操作列。

## Evidence

- RULE: 已读取 `docs/task-closeout-rules.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`。
- RULE: 已读取 `bug-regression-fix-loop` 技能和 `bug-contract.md`。
- RULE: 已读取 `docs/backend-development.md` 中 `MES PQC组长人员范围与管理数据可见性门禁`。
- RULE: 已读取 `docs/local-runtime.md`、`docs/login-access.md`、`docs/e2e-rules.md`，并读取 `playwright`、`project-experience-consolidation`、`task-closeout-cleanup` 技能。

## Milestone Updates

- M1: completed。只读核对确认历史任务目标数据仍存在：PQC task `223..227`、PQC event `181..185`、PQC record `104..108` 均未删除，tenant `1`，检验员 `659`，admin 的启用 PQC 人员范围 `980046` 和 `pqc_permission` 角色均正常。
- RED: `GET /mes/pro/process-pool/team-leader/submission/page?leaderType=PQC&submitDate=2026-08-08` -> FAIL as expected，业务码 `0` 但 `total=0`；同一接口查 `2026-08-07` 返回 `total=17` 且包含 5 条 `CODX-PQC-20260807-SP-WO-*` 目标工单，证明数据未被删除而是被当前默认提交日期筛掉。
- ROOT CAUSE: 目标 5 条 PQC 事件和对应 PQC 记录的 `server_submit_time` 仍为 `2026-08-07 13:36:49..13:36:59`；页面默认按当前提交日期 `2026-08-08` 查询，PQC 管理没有自动回看历史日期。
- M2: completed。执行受控事务，仅更新目标 5 条 `mes_pro_process_pool_event` 和对应 5 条 `mes_pro_process_pool_pqc_record` 的 `server_submit_time` 到 `2026-08-08`，保留原时间分秒；前置 event/record 计数均为 `5`，DML 影响行数为 `5/5`。
- GREEN: 只读数据库复核 -> PASS，目标 event/record 今日计数均为 `5`，人员范围 `scope_active=1`、角色 `role_active=1`、目标事件 `target_event_count=5`。
- GREEN: 只读 API 复核 -> PASS，`submitDate=2026-08-08` 返回 `total=5`、`targetCount=5`，工单为 `CODX-PQC-20260807-SP-WO-01..05`。
- GREEN: Playwright 真实页面复核 -> PASS，使用本机 `芋道源码/admin` 通过真实登录页进入 `/mes/pro/process-pool/pqc-leader`，点击 `PQC管理` 页签，页面可见 5 条目标工单；接口 `apiTotal=5`，`consoleErrorCount=0`，`pageErrorCount=0`。
- EXPERIENCE: 已将“PQC 管理 No Data 先核对默认提交日期与事件 `server_submit_time`，测试 fixture 恢复只允许精确标识更新事件/记录提交时间”的经验合并到 `docs/backend-development.md` 和 `docs/experience-index.md`。
- CLOSEOUT: `task-closeout-cleanup --mode preview` -> PASS，keep 三份核心任务记录，delete/blocked/warnings 均为 `<none>`。
- CLOSEOUT: `task-closeout-cleanup --mode apply` -> PASS，无删除项；任务状态已更新为 `completed`。
