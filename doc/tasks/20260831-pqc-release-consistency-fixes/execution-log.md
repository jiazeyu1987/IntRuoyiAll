# Execution Log

## User Intent

- 在 `int_main` 静态分析后继续修复发现的生产放行链路缺口。

## BDD

- BDD: 申请级不合格审查冻结工单 -> Given 待 PQC 放行申请关联有效生产工单; When PQC 发起不合格审查; Then 系统记录工单原冻结状态并将工单冻结，后续正式生产提交和领料出库均被拒绝。
- BDD: QA 让步恢复并保留签字 -> Given 申请级评审冻结了原本未冻结的工单; When QA 处置为让步放行; Then 系统恢复工单原冻结状态，申请保持待 PQC 放行且待办保持可签字。
- BDD: QA 返工或作废终结申请 -> Given 申请级评审处于待处理; When QA 处置为返工或作废; Then 系统原子关闭评审、终结放行申请并完成 PQC 待办；返工恢复原冻结状态，作废保持冻结。
- BDD: 资料未就绪禁止点击放行 -> Given 待放行申请的批记录、过程检验或物料平衡预检存在 blocker; When PQC 查看待放行列表; Then 后端返回未就绪及原因，前端放行按钮不可点击。
- BDD: PQC 签名响应不确定恢复 -> Given 用户已提交签名请求但客户端未收到明确结果; When 前端查询申请权威回执; Then 已放行时按成功处理，仍待放行时保留原幂等键允许重试，回执查询失败时进入不确定锁定。

## Evidence

- M0 静态审查：申请级评审未写工单 `temporaryFrozen`；生产提交和领料出库未检查冻结工单；返工/作废只关闭评审；放行按钮仅按 `underReview` 禁用；签名每次生成新幂等键且没有权威回执恢复。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest test` -> FAIL，服务缺少工单冻结状态所有者依赖。
- RED: `python -X utf8 -m pytest script\tests\test_mes_pqc_release_review_work_order_freeze_sql.py` -> FAIL，缺少工单原冻结状态正式迁移。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest,MesWmProductIssueFreezeGateTest,MesProcessPoolEventFreezeGateTest" test` -> PASS，8 个冻结/终态测试通过。
- GREEN: `python -X utf8 -m pytest script\tests\test_mes_pqc_release_review_work_order_freeze_sql.py` -> PASS。
- M1：申请级评审锁定工单，保存 `previousWorkOrderTemporaryFrozen` 后冻结；新生产报工及领料出库保存、提交、拣货、完成均执行正式工单冻结门禁。
- M2：让步恢复原冻结状态并保留待办；返工/作废用 CAS 终结申请并完成 PQC 待办，返工恢复、作废保持冻结。
- 已知非任务阻断：原 `MesProcessPoolEventServiceTest` 的 H2 fixture 缺少既有 `simulation_stage` 列，导致该历史 DB 测试无法作为本次门禁证据；本次新增独立单测不依赖漂移 fixture，完整构建仍将在 M4 验证。
- RED: `node tests\e2e\pqc-production-release-mvp-completion-static.spec.js` -> FAIL，API 缺少放行就绪字段，页面缺少稳定幂等键与权威回执恢复。
- GREEN: `node tests\e2e\pqc-production-release-mvp-completion-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesPqcReleaseBatchExecutionServiceTest,MesProEdhrNonconformanceReviewApplicationScopeTest,MesWmProductIssueFreezeGateTest,MesProcessPoolEventFreezeGateTest" test` -> PASS，21 tests。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，30 modules。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ...` -> PASS，552 migrations。
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，worktree slot 61，前端 8356，后端 48356。
- GREEN: `git diff --check` -> PASS。
- GREEN: backend API、frontend feature、database schema evidence validators -> PASS；关键结果已归档到 `verification-report.md`，临时 evidence 文件待 cleanup 删除。
- M3：待放行分页仅对当前页执行正式 dossier 只读预检；页面消费 `approvalReady`，并用稳定幂等键、明确业务失败分流、权威回执恢复和不确定锁定完成签名交互。
- M4：完成聚焦回归、类型检查、完整后端打包、迁移策略和端口治理门禁。按用户授权未执行映射运行态验证及写入 E2E。

## Current Milestone

M5 - implementation commit, cleanup, fast-forward integration and worktree removal.

## Final Closeout

- Final implementation commit: `51550d308` (`fix: pqc production release consistency`).
- Closeout record commit: `2d45eb452` (`chore: complete pqc release consistency task`).
- Project experience commit: `a4f78dfda` (`docs: record pqc release consistency gate`).
- `int_main` was advanced through the verified implementation, closeout record, and project experience commits after rebasing onto the latest mainline tip.
- Task-only temporary evidence files and the one-off registry helper script were deleted from the task directory.
