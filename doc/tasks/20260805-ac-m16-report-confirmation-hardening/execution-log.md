# Execution Log

## Intent

用户要求修复 AC-M16 生产班组长确认员工报工不符合项。目标是让生产报工确认必须走分配链路、退回后继续分配被拒绝、重复终态一致阻断，并补齐测试和 schema 证据。

## Skill Usage

- 使用 `bug-regression-fix-loop`：先复现失败行为，再写 RED 回归并修复。
- 使用 `backend-api-delivery`：加固后端服务入口、权限/角色/校验和错误行为。
- 使用 `database-schema-delivery`：补齐数据库唯一约束/测试 fixture。
- 使用 `task-closeout-cleanup`：收尾时按 preview/apply 清理任务临时证据。

## BDD Scenarios

- BDD: 生产通过必须进入分配 -> Given 一个 `PRODUCTION_SUBMIT` 报工事件 When 调用通用复核接口尝试 `APPROVED` Then 后端必须拒绝并要求使用生产分配确认链路。
- BDD: 退回后禁止继续分配 -> Given 一个生产报工已存在 `REJECTED` 终态复核 When 生产组长再调用分配确认 Then 后端必须拒绝且不插入 review/allocation/completion。
- BDD: 已有终态复核禁止二次分配 -> Given 一个生产报工已存在任意终态复核但无分配行 When 调用分配确认 Then 后端必须按终态重复拒绝。
- BDD: 生产分配只能由生产组长执行 -> Given 非 `PRODUCTION` leaderType 调用分配确认 When 员工范围满足或伪造 leaderType Then 后端仍必须拒绝。

## Baseline Evidence

- 基线：`fdf1b49d8 Baseline: preserve residual AC-M18 and AC-M19 task docs`
- 基线：`c7a713c03 Baseline: preserve residual AC-M21 task docs`
- 基线：`5702e9d59 Baseline: preserve residual process pool task updates`
- 基线：`b01682b49 Baseline: preserve residual MES test updates`
- 基线：`e7c27613e Baseline: preserve residual schedule progress test update`
- 基线：`f8c1a38f7 Baseline: preserve residual QA and release owner updates`
- 基线：`ca181206a Baseline: preserve residual QA regulation updates`
- 工作区说明：并行任务仍在持续写入部分 QA/矩阵相关文件；AC-M16 目标修复将保持选择性修改和提交。

## Progress

- completed: M1 任务文档、BDD/TDD 计划和并行基线证据已建立。
- completed: M2 新增/更新 AC-M16 回归，旧服务实现下 RED 失败：`java @doc\tasks\20260805-ac-m16-report-confirmation-hardening\junit-console-red.args` -> FAIL，3 个新用例失败。
- completed: M3 服务修复已完成：通用复核拒绝生产报工通过，报工确认入口限定生产组长并锁定终态 review。
- completed: M4 schema 修复已完成：恢复损坏的 `20260730_mes_process_pool_team_leader.sql`，新增 `20260805_mes_process_pool_ac_m16_terminal_constraints.sql` 唯一终态约束。
- in_progress: M5 定向 javac + JUnit Console GREEN 通过；标准 Maven 仍因并行 Maven 构建多次超时。

## Verification Evidence

- RED: `java @doc\tasks\20260805-ac-m16-report-confirmation-hardening\junit-console-red.args` -> FAIL，13 个服务测试中 3 个 AC-M16 新用例失败。
- GREEN: `javac @doc\tasks\20260805-ac-m16-report-confirmation-hardening\javac-main-check2.args` -> PASS。
- GREEN: `javac @doc\tasks\20260805-ac-m16-report-confirmation-hardening\javac-test-check2.args` -> PASS。
- GREEN: `java @doc\tasks\20260805-ac-m16-report-confirmation-hardening\junit-console-green.args` -> PASS，13/13 服务测试通过。
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\20260805-ac-m16-report-confirmation-hardening\migration-policy-gate.json` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260805-ac-m16-report-confirmation-hardening\bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260805-ac-m16-report-confirmation-hardening\backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260805-ac-m16-report-confirmation-hardening\database-schema-evidence.md` -> PASS。
- BLOCKED: 标准 Maven `-am` 与不带 `-am` 两个目标命令均超时，未生成新的 AC-M16 surefire 报告；当前存在非本任务 Maven 进程，未终止。
- EXPERIENCE: 已按 `project-experience-consolidation` 合并经验到 `docs/powershell-memory.md`：Maven 阻塞时 JUnit Console + 显式 javac 只能作为补充证据，标准 Maven 仍需记录 blocked。
