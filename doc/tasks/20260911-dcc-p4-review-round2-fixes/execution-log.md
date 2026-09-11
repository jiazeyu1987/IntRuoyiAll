# Execution Log

BDD: Project OWNER uses authoritative access rule -> Given a user only has an active project-code correction assignment but no active OWNER access rule, When the user creates or links a major revision, Then authorization is denied; Given an active unexpired USER OWNER rule exists, Then authorization succeeds.

BDD: Expired or non-OWNER access does not grant revision rights -> Given a project access rule is EDIT, VIEW, inactive, not yet valid, or expired, When OWNER authorization is checked, Then access is denied.

BDD: Legacy GxP subject index upgrades safely -> Given the old `subject_id` column has a full utf8mb4 index, When the forward migration runs, Then it drops the old subject index before widening the column and recreates the prefix index afterward.

BDD: Approval reason is authentic and mandatory -> Given an approval request or signature command contains a null, empty, or whitespace-only reason, When validation/signing runs, Then the request fails before any signature projection is persisted and no default reason is invented.

## RED/GREEN Evidence

- RED: `mvn.cmd -q -pl yudao-module-dcc -am "-Dtest=DccProjectAccessServiceImplTest,DccApprovalReasonValidationTest,DccApprovalTaskAdapterTest,DccControlledFileSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`DccSignatureVerificationServiceImpl` 漏导入 `CONTROLLED_FILE_SIGNATURE_REASON_REQUIRED`，DCC 主源码无法编译。
- RED: 同一命令在补齐导入后 -> FAIL，`reviewApproveRejectsBlankReasonBeforeWorkflow` 证明审批适配器直接构造 VO 时不会自动触发 Bean Validation，空白意见仍进入工作流。
- GREEN: `mvn.cmd -q -pl yudao-module-dcc "-Dtest=DccProjectAccessServiceImplTest,DccApprovalReasonValidationTest,DccApprovalTaskAdapterTest,DccControlledFileSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，35 tests，0 failures，0 errors。
- GREEN: `python -X utf8 -m pytest script\tests\test_gxp_audit_core_contract.py -q` -> PASS，6 tests。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260513_dcc_base_schema.sql --sql-file sql\mysql\20260908_gxp_audit_trail_core.sql --sql-file sql\mysql\20260911_dcc_project_access_rule.sql` -> PASS，3 migrations。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，620 migrations（同时补齐已提交的 DCC DIRECT 发布策略元数据后）。
- GREEN: `git diff-files --check -- IntRuoyiBackend IntRuoyiFronted docs doc` -> PASS，仅换行提示。
- GREEN: database schema evidence validator -> PASS。
- GREEN: backend API evidence validator -> PASS。
- GREEN: bug regression evidence validator -> PASS。
- RED: `python -m pytest script/tests/test_gxp_audit_core_contract.py -q` -> FAIL，新增顺序断言证明旧脚本在删除 `idx_gxp_audit_event_subject` 之前先扩容 `subject_id`。
- RED: 定向 Java 复现命令 -> FAIL，6 个断言分别证明审批 VO 接受空白、签名服务走默认原因路径、OWNER 实现未使用权威规则 Mapper。
- RED: `DccApprovalTaskAdapterTest#reviewApproveRejectsBlankReasonBeforeWorkflow` -> FAIL，空白批准意见未被适配器拒绝。
- GREEN: DCC OWNER/审批/签名/Workflow/影响任务相邻回归 -> PASS，188 tests，0 failures，0 errors。
- GREEN: GxP 顺序与项目访问规则合同 -> PASS，8 tests。

## Implementation Notes

- 新增 `dcc_project_access_rule` 的正式 DO、Mapper 和 migration；OWNER 服务不再引用项目代码修正任务。
- OWNER 仅接受有效期内的显式 OWNER 规则，主体支持 USER、DEPT、ROLE、POSITION；EDIT/VIEW 不放行升版。
- GxP 迁移改为先删除旧完整索引，再扩容字段，最后建立 191 字符前缀索引。
- 审批通过 VO、审批中心适配器和签名服务三层均拒绝空白意见；签名服务在授权、证据和落库动作前先校验原因。
- SKILL VALIDATION: bug-regression/backend-api/database-schema evidence validators -> PASS。
- EXPERIENCE: 按 `project-experience-consolidation` 合并到现有 `docs/backend-development.md` 与 `docs/database-rules.md`，未新建长期经验文档。

## Blockers

- 未执行真实数据库迁移、运行态重启或 Playwright E2E；本轮提交代码不声明运行态合规通过。
- CLOSEOUT: `task_closeout.py --task-id 20260911-dcc-p4-review-round2-fixes --mode preview` -> PASS；`--mode apply` -> PASS。临时技能 evidence 已删除，`task.md`、`execution-log.md`、`verification-report.md` 保留。因本轮未授权 Git 提交/推送，状态保持 `ready_for_closeout`。
