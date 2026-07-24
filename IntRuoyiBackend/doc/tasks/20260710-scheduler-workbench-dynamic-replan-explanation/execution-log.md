# Execution Log

## BDD

BDD: 成功应用人工重排后生成说明 -> Given 用户完成一次可应用的人工重排 / When 重排事务提交 / Then 保存一条包含本次完整计算数据且来源为人工的说明快照。

BDD: 夜间重排生成相同口径说明 -> Given 夜间任务成功应用重排 / When 重排事务提交 / Then 保存相同结构且来源为夜间自动的说明快照。

BDD: 物料计算包含充足与短缺物料 -> Given 重排订单需要多种物料 / When 系统计算库存 / Then 快照记录每种物料的需求量、可用量、短缺量及订单贡献。

BDD: 工序产能保留实际计算值 -> Given 重排包含多道工序 / When 系统完成产能计算 / Then 快照记录班次、工作站、设备、人员、每小时产能、预计时长和瓶颈标记。

BDD: 失败重排不覆盖说明 -> Given 已存在成功重排快照 / When 新重排预览或应用失败 / Then 最近成功快照保持不变。

BDD: 快照写入失败回滚重排 -> Given 排产任务已准备写入但快照持久化失败 / When 应用重排 / Then 整个事务失败且不留下部分排产结果。

BDD: 查询仅返回当前租户最近记录 -> Given 多个租户存在多次成功重排 / When 当前租户查询重排说明 / Then 只返回本租户最新一条记录。

## TDD

- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProReplanExplanationSchemaContractTest,MesProAutoScheduleControllerContractTest,MesProAutoScheduleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 `MesProReplanExplanationSnapshotDO` 和 `MesProReplanExplanationSnapshotMapper`，证明快照持久化尚未实现。
- RED: schema 契约已要求 migration、测试 schema、租户字段、请求唯一索引和最新记录索引，当前生产代码尚未满足。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProReplanExplanationSchemaContractTest,MesProAutoScheduleControllerContractTest,MesProAutoScheduleServiceImplTest,MesProNightlyReplanServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，63 个测试通过。
- GREEN: `python -X utf8 script/tests/test_mes_replan_explanation_snapshot_sql.py` -> PASS，迁移脚本和初始化脚本契约通过。
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，后端完整依赖构建通过。
- GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/login-access.md`、`docs/server-access.md` 和 Playwright 执行规范；真实写入验证限定本机测试租户 `tenant_id=122`、账号 `aoteman`，禁止访问远端环境和芋道源码租户写入。
- GREEN: real-e2e-first-replan -> PASS，测试租户通过真实前端对工单 `TESTERP62AF41D87EFA` 应用重排，保存快照 `id=1`、请求编号 `98954ea1-3124-4873-8859-2b424b811b11`、应用时间 `2026-07-10 12:54:36`。
- GREEN: real-e2e-second-replan -> PASS，同一真实用户路径再次应用重排，保存快照 `id=2`、请求编号 `26086c5c-da7c-47a2-b0a9-3cde37028d0f`、应用时间 `2026-07-10 12:59:53`；最新记录包含 51 个新增任务、51 个删除任务、0 个保留任务、27 种物料、1 个工单和 27 条问题。
- GREEN: task-closeout-cleanup-preview -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，清理 `backend-api-evidence.md`、`database-schema-evidence.md` 和 `output/e2e-runtime/`。
- GREEN: task-closeout-cleanup-apply -> PASS，任务分支快进融合到后端 `int_main`；Windows 删除 worktree 目录时报权限占用，确认目标为空且位于本任务 worktree 后手动删除。
- GREEN: post-merge-backend-verification -> PASS，后端主工作区运行 `python -X utf8 script/tests/test_mes_replan_explanation_snapshot_sql.py`、63 个定向 Maven 测试和 `git diff --check` 均通过。

## Current Status

completed
