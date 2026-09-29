# 偏差管理开发文档任务执行日志

## Original Documentation Task Record: piancha_0923

- 用户于 2026-09-24 明确授权按需启用子任务、数据库SQL迁移和服务重启；该授权仅在相应步骤确有需要时使用。
- Worktree: D:\IntRuoyiWorktree\piancha_0923
- Branch: codex/piancha-0923
- Baseline: f46b0bdc20d80ee7fa6e5d6b86898bcae0d0b357
- Runtime reservation: int_main slot 13, frontend 8094/backend 48094; no services started.
- The pre-existing task package was local-exclude-only in the main worktree, so the reviewed docs were copied into this worktree; the main worktree docs were left untouched.

### P1 Read-only Preflight

- MesTeamLeaderActiveOrderCompletionBatchExecutionService.openOrCreate creates a formal batch execution from a validated completion receipt. MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.apply invokes that step before it generates/binds the P3 release application. This provides a formal batch identity before PQC release submission; P1 must add a supported query/selector path for QA.
- Existing MesProEdhrNonconformanceReviewServiceImpl.create accepts PQC_RELEASE, PQC_SUBMISSION, or direct batchExecutionId branches but stores no deviation association; there is no multi-deviation join or deviation source type.
- Existing batch detail route /active-order-detail accepts batchExecutionId; it can back the normal detail view. Permission is currently mes:pro-edhr-batch-execution:query.
- Existing 20260830_mes_edhr_nonconformance_review_mvp.sql only defines the NCR table/menu. 20260618_mes_edhr_oq_pq_execution_deviation.sql defines a separate OQ/PQ case/run deviation and must not be reused.
- No deviation module API, production deviation schema, or P1 regression test was found in the clean baseline f46b0bdc2.
- Phase P1 has not started implementation or testing. The phase is blocked at the workflow gate pending explicit authorization for the required executor and independent tester subagents.
- 用户已明确授权子任务；P1现可按本技能恢复。先处理干净基线的Maven编译失败，再实施偏差P1。
- Baseline test preflight: targeted Maven reactor selection reached MES compilation but failed before Surefire ran. Existing MesTeamLeaderActiveOrderDetailServiceImpl references OperationFact.setReviewMaterialsJson and MesTeamLeaderActiveOrderDetail.SourcePickListDocument, both missing from the clean baseline DTO. No feature test ran; this is a baseline compile blocker for P1 verification.

### P1 Executor Work

- BDD: 活跃订单详情传递正式来源和不合格材料证据 -> Given 服务层已经解析来源领料单和评审材料 JSON, When 读取活跃订单详情响应, Then DTO 与 Controller 必须完整保留结构化来源单据和材料 JSON，且服务/响应模型可以编译。
- RED: `node --test src/test/js/mes-team-leader-active-order-detail-missing-dto-contract.spec.cjs` -> FAIL, assertion found `OperationFact.reviewMaterialsJson` absent from the service DTO; the test also covers typed pick-list document evidence and controller response mapping.
- BDD: P1-AC1 合法创建与租户授权 -> Given 用户具备偏差创建权限且目标批记录属于当前租户, When 以正式batchExecutionId提交偏差, Then 创建和读取只绑定该批记录，跨租户、缺对象或无权请求零业务写入。
- BDD: P1-AC2 上市放行边界 -> Given 批记录已存在正式RELEASED交易, When 查询可选批记录或提交新偏差, Then 已放行批记录不再可选，服务端拒绝新增。
- BDD: P1-AC3 编号并发与月切 -> Given 同租户月序列已创建或首次并发初始化, When 使用上海时区年月分配编号, Then 月内原子递增且唯一，月切重置、超过9999自然扩位，失败不发布号码。
- BDD: P1-AC4 PQC推送前正式批记录候选 -> Given 正式批记录已由完工回执创建但尚无PQC放行交易, When 有权用户按batchExecutionId选择批记录并发起偏差, Then 候选和创建均不依赖订单或PQC申请号。
- BDD: P4-AC3 PQC推送前关键偏差转审 -> Given 正式批记录尚未推送PQC且关联开放关键偏差, When QA从批记录签署发起NCR, Then 无PQC申请ID也可成功转审、自动关闭所选偏差且待审NCR保持冻结。
- BDD: P1-AC5 创建与上市放行串行化及幂等 -> Given 创建请求和上市放行交错或创建响应丢失, When 使用批记录行锁和同键重放, Then 只允许安全顺序成功且重放返回同一偏差、不多写。
- BDD: P1-AC5 上市放行共用批记录锁 -> Given 创建偏差与最终上市放行同时争用同一批记录, When 两条服务路径先锁批记录再读取偏差/放行状态, Then 放行先提交则创建拒绝，创建先提交则放行因开放偏差被拒绝，不出现双成功。
- BDD: P1-AC5 跨批相同幂等键重放 -> Given 两个批记录请求并发使用同一租户/同一幂等键, When 服务先串行锁定幂等请求身份, Then 同载荷重放读取既有偏差，异载荷明确冲突，不因不同批记录锁而落入重复键异常。
- RED: `node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs` -> FAIL, all three P1 contract tests fail because the deviation service and migration do not exist; test scenarios cover tenant/market-release/sequence behavior and formal batch-ID-only source.
- RED: `node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs` -> FAIL, release-serialization scenario found market release acquired the release transaction before the shared batch row lock.
- GREEN: `node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs src/test/js/mes-team-leader-active-order-detail-missing-dto-contract.spec.cjs` -> PASS, 5 tests, 0 failures; covers P1-AC1..P1-AC5 static contracts, migration identities, lock order, and baseline DTO projection.
- GREEN: `mvn -pl yudao-module-mes -am '-DskipTests' compile` -> BUILD SUCCESS; all 25 reactor modules compiled, including MES (3,070 source files) after the clean-baseline DTO repair.
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, deviation Java unit classes passed (7 tests); all 33 existing release-service test methods errored at test-context setup because the isolated fixture does not register JdbcTemplate / signature-service beans required by the newly imported service. No business assertion failed; release gate integration test needs a fixture wiring correction before it is usable.
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL after the initial DTO repair: 7 P1 unit tests passed, while all 33 release-service tests errored during test-context wiring because JdbcTemplate/signature beans were absent. No release assertion ran in that attempt.
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS, 43 tests, 0 failures/errors: release service 33, deviation service 6, number generator 2, and existing release façade 2.
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS, 44 tests, 0 failures/errors: release service 33, deviation service 7, number generator 2, existing release façade 2.
- GREEN: `python script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260608_edhr_batch_execution_schema.sql --sql-file sql/mysql/20260612_mes_edhr_multi_batch_route.sql --sql-file sql/mysql/20260615_mes_edhr_tail_four_goals.sql --sql-file sql/mysql/20260618_mes_edhr_release_precheck_engine.sql --sql-file sql/mysql/20260618_mes_edhr_release_transaction_lifecycle.sql --sql-file sql/mysql/20260924_mes_edhr_deviation_management.sql` -> PASS, migrationCount=6 including P1 migration, batch schema, and full release-transaction dependency closure. Whole-tree gate separately fails on pre-existing `20260921_mes_edhr_nonconformance_review_materials_json.sql` missing release metadata; P1 closure itself passes.
- Database boundary at executor handoff: no migration was applied to any live/shared database. Subsequent main-agent verification applied the migration only to a disposable network-isolated MySQL container, with first/repeat, schema readback and SQL concurrency results recorded below and in execution-log.md.
- Compliance gate found during final review: backend rules require GxP business writes to append a registered GxpAuditService event in the same transaction. P1 create does not yet do so; the repository has no active operation policy for this new action, and owner/approved policy reference/retention classification are not supplied by this task package. No inferred policy seed or fake audit success was added. This must be resolved before claiming regulated production readiness; it is distinct from the P1 functional executor test PASS.

### P1 Executor Handoff

- Status: executor-side P1 code and checks are ready for independent review; main agent owns task-state.json and P1 phase advancement.
- Functional evidence: targeted Maven reactor PASS, 44 tests/0 failures/errors; Node contracts PASS 5/5; focused six-migration policy closure PASS; database/backend skill evidence validators and self-tests PASS; diff whitespace check PASS.
- Not performed: no SQL applied, no shared/live DB accessed, no service started/restarted, no E2E, no commit/push, and no P2 work.
- Release-readiness blockers at executor handoff: MySQL concurrency and migration runtime replay were not verified then; P1 deviation creation also lacks electronic signature and registered GxP audit. Subsequent isolated MySQL results are recorded below. The approved GxP policy owner/reference/signature/retention remain unresolved prerequisites before regulated production use.
- Independent review found the public approval preflight locked the release transaction before the finalizer locked the batch row. The static regression reproduced the batch->release vs release->batch inversion as RED. `finalizeRelease()` now performs a non-locking read for replay/hydration and defers row locks to `finalizeApproval()` in batch->release order.
- BDD: P1-AC5 approval entry lock order -> Given deviation creation locks batch then release transaction, When the public market-release approval path handles replay/hydration and enters finalization, Then every transaction lock must be deferred until after the same batch row lock or replay returns without mutation.
- RED: `node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs` -> FAIL, independent review found `finalizeRelease()` took `requireTransactionForUpdate()` before `finalizeApproval()` acquired the batch row; this can deadlock against create's batch-then-release order.
- GREEN: `node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs` -> PASS, 4 tests; public approval preflight is non-locking, finalizer locks batch before release transaction, and open deviation blocks release.
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS, 44 tests, 0 failures/errors after the review correction.
- BDD: P1-AC1 发起签名和GxP审计原子绑定 -> Given 授权用户持有有效签名凭据且GxP操作已有批准策略, When 发起正式批记录偏差, Then 电子签名绑定规范化偏差内容及正式批记录，签名ID持久化并与GxP ABSENT→PRESENT事件同事务提交；密码不进入幂等摘要，重放不重复签名。
- RED: `node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs` -> FAIL, P1 create lacks required electronic signature, GxP audit append, registered signature action, and credential-free idempotency payload.
- GREEN: `node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs src/test/js/mes-team-leader-active-order-detail-missing-dto-contract.spec.cjs` -> PASS, 7 tests; contracts cover dedicated deviation subject binding, required signature credential, ABSENT->PRESENT audit, policy-seed exclusion, and DTO baseline repair.
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationSignatureIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 4 tests; missing/invalid credential rollback, signature subject deviationId/content-hash binding, same-transaction audit append, idempotent replay no-resign, and missing-policy append rollback.
- Implementation detail: signature subject uses dedicated `recordDeviationInitiationSignature` with `reviewSourceType=EDHR_DEVIATION`, `reviewSourceId=deviationId`, deviation code and canonical persisted content hash. GxP state is ABSENT -> PRESENT; reason is `DEVIATION_INITIATION:偏差编号=...;等级=...`, satisfying category-plus-text shape. No GxP policy registry or migration seed was added.
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrDeviationSignatureIntegrationTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS, 46 tests, 0 failures/errors (2 number, 7 service, 4 signature integration, 33 release).
- Baseline command: `mvn -pl yudao-module-mes -am test` -> FAIL before MES, at yudao-module-infra Surefire (599 tests; 13 failures, 1 error); unrelated existing infra test failures prevented a full baseline test run.
- Compile command: `mvn -pl yudao-module-mes -am '-DskipTests' test` -> FAIL at MES compile with 3 unresolved symbols: `OperationFact.setReviewMaterialsJson(String)` and `MesTeamLeaderActiveOrderDetail.SourcePickListDocument` (declaration and constructor use).
- Scope: task-owned P1 files only; no shared/live DB, services, ports, commit, push, or P2 work.

## Development Execution Resume: piancha_0923

- Worktree: D:\IntRuoyiWorktree\piancha_0923
- Branch: codex/piancha-0923
- Runtime reservation: int_main slot 13, frontend 8094, backend 48094; no services started.
- Baseline revision: f46b0bdc20d80ee7fa6e5d6b86898bcae0d0b357; worktree was clean before this task.
- Development execution state is owned by task-state.json; current phase is P1.

### P1 Read-only Preflight Evidence

- P1 code audit found a formal batch execution is created from the completion receipt in MesTeamLeaderActiveOrderCompletionBatchExecutionService.openOrCreate, before P3 release application binding. This is the likely basis for QA reviewing a batch record before PQC release, but it is not yet wired to deviation behavior.
- Existing nonconformance create supports PQC_RELEASE, PQC_SUBMISSION, or a direct batchExecutionId branch; it has no deviation source or multi-deviation association. The current create path requires source-specific context and records one review without deviation links.
- Existing batch detail query exposes /active-order-detail by batchExecutionId and its controller requires mes:pro-edhr-batch-execution:query; the frontend already routes batch execution rows with batchExecutionId.
- Existing migration 20260830_mes_edhr_nonconformance_review_mvp.sql creates only the NCR table and query/create/dispose permissions; there is no production deviation table, sequence table, migration, API, or test for this feature in the worktree.
- Existing OQ/PQ mes_pro_edhr_validation_deviation is a separate run/case validation entity and cannot be reused as the requested batch-record deviation.
- RED/GREEN implementation and independent testing were not started because development-plan-delivery requires one executor and one independent tester for P1, while root AGENTS.md prohibits subagents without explicit user authorization.

## User Intent

用户要求将对话中已确认的偏差管理需求整理成必要的开发文档。本任务仅制作实施用文档包，不开始功能开发。

## Requirement Decisions

- 偏差只关联正式批记录，不关联具体表单、工序或订单；一个批记录可有多条偏差。
- 等级为普通偏差和重大（关键）偏差两档，重大与关键同义。
- 偏差号采用 PC-YYYYMM-0001；按租户/月、Asia/Shanghai 服务端年月递增，四位补零、自然扩位；正式提交分配、唯一且不回收。
- 每条偏差只有一份处理记录，允许修改该记录，不支持多处理轮次。
- 常规路径验证、QA 关闭、质量负责人批准全部完成才关闭；QA 与质量负责人无顺序关系；关键常规路径增加管理者代表签字。
- QA 仅可对关键偏差手动发起批记录级不合格审批；一个评审可关联同批一条或多条关键偏差。发起成功后自动关闭所选偏差，跳过偏差表自身验证/批准；pending 评审仍冻结。
- 未关闭偏差和待处置不合格评审阻止一线生产提交、一线 PQC 提交、PQC 生产放行和上市放行。
- QA 或持有 PQC 生产放行处置权限的人员可选择让步放行、返工或作废；按处置结果与既存门禁控制后续操作。
- 用户界面操作/展示对象为批记录。上市放行后不能新关联偏差。
- 同账号可同时获得多个权限。

## Evidence Reviewed

- 本机用户提供的偏差处理记录表 DOCX；OfficeCLI 提取了唯一连续大表的字段结构。
- 仓库文件 resource/相关文档/电子批记录系统的审核要求8.25.xlsx：审核表有 40 行，包括偏差直发、未关闭拦截、放行阻断和例外汇总，以及签名/审计/归档要求。
- 当前实现线索：UnifiedListTemplate、批记录详情、ActiveOrderSubmissionDetailPanel、BatchExecutionTraceDrawer、MesProEdhrNonconformanceReviewService、电子签名服务、批次放行检查。
- 旧任务 doc/tasks/nonconformance-review-mvp-implementation：已有统一不合格评审、冻结、三类处置及追溯实现记录。

## BDD Scenarios

- BDD: 普通偏差合法发起 -> Given 用户具备发起权限且批记录同租户未上市放行, When 完成字段和签名后提交, Then 分配唯一 PC 编号并进入未处理列表。
- BDD: 未关闭偏差阻断整批四种动作 -> Given 批记录有未关闭偏差, When 尝试一线生产提交/一线 PQC 提交/PQC 生产放行/上市放行, Then 后端分别拒绝。
- BDD: 关键偏差转评审 -> Given QA 为同一批次选择多条未关闭关键偏差, When 成功发起一个不合格审批, Then 审批关联全部偏差、偏差自动关闭但 pending 审批继续冻结。
- BDD: 待处置评审结束按处置控制 -> Given 批次已有待处置不合格审批, When QA/PQC 放行授权人员选择已签处置结论, Then 后续动作根据真实处置状态和其余门禁判断。
- BDD: 签名修改追溯 -> Given 一份处理记录存在已签字段, When 获授权用户说明原因后修改已签内容, Then 审计记录变化且受影响签名失效。
- BDD: 追溯空态与错误分离 -> Given 偏差读取正常返回空集或服务失败, When 打开批记录偏差 Tab, Then 分别显示“没有偏差”或错误信息。

## RED / GREEN 状态

- 本任务只生成开发文档，未改生产代码，未执行功能 RED/GREEN 测试。
- 后续编码阶段的 RED/GREEN 命令模板和预期行为记录在 test-plan.md；实际命令必须绑定实际实现提交/测试名并保存真实输出，不可将本轮文档校验记作业务 RED/GREEN。

## Workspace Safety

- 读取时分支为 int_main，工作区存在大量与 NCR、活跃表单、批记录、登录策略等相关的未提交源码、测试、规则文件和临时文件。
- 本任务只写入 doc/tasks/20260923-edhr-deviation-management/ 下的任务自有文档；不暂存、不删除、不恢复、不提交、不推送其他改动。
- 在后续实施前重新读取 git status 和相关 diff，不能把当前脏工作区当作干净基线。

## Closeout Evidence（初版历史，不能替代本轮复验）

- task-closeout-cleanup preview -> PASS：13 个正式开发文档全部 keep，没有 delete、blocked 或 warnings。
- task-closeout-cleanup apply -> PASS：保留 13 个任务文档，deleted_paths 为空。
- 未执行 Git commit/push：根目录 AGENTS.md 要求无当轮明确授权时不得提交/推送；task-closeout-rules.md 又将提交和推送列为任务完成门禁。文档暂留 ready_for_closeout，等待该门禁处理。

## Open Implementation Notes（初版记录，以下新复审结论优先）

- 活跃表单主偏差 Tab 只在共享面板 FORMAL_BATCH_SOURCE_DETAIL 范围显示；历史追溯列表 BatchExecutionTraceDrawer 与批记录详情 traceRecordDrawer 是两个独立只读追溯宿主，各使用正式 batchExecutionId。
- 现有 NCR direct batch source 和处置实现必须按当前源代码 diff 复核；特别确认 batch linked rework 是否会产生正式返工路径。
- 本线程确认QA直接转审替代偏差验证和批准；本轮按该分支不附加偏差处理完成门槛，详见修订PRD。
- 一个 pending NCR 存在时后来创建的新关键偏差如何追加尚未确定；首版文档给出拒绝第二评审/不改签名评审集合的建议。

## Follow-up: Frontend Interactions and Consistency Audit

用户要求增加前端交互文档并检查整套文档是否自洽、符合已确认需求；本轮仅调整任务内开发文档。

- 新增 frontend-interaction.md，明确偏差列表、发起、唯一处理记录、签名、不合格评审选择、状态/错误/空态交互和追溯入口。
- 新增 consistency-review.md，按用户确认规则逐项对照 PRD、设计、验收和测试计划，并记录矛盾和修正。
- NCR 分支修正为 QA 可对未关闭关键偏差直接发起，不等待偏差表验证/批准；调查/处理记录未完成也不是门槛；成功创建时自动关闭，NCR 自身签名/处置仍必需。
- 页面范围修正：偏差主 Tab 仅在 FORMAL_BATCH_SOURCE_DETAIL 显示；普通详情、PQC 放行和 NCR 页面不显示；追溯覆盖历史 BatchExecutionTraceDrawer 和批记录详情自身追溯抽屉。读查询只用正式 batchExecutionId。
- 当前没有产品代码变更；本轮运行文档结构/一致性检查，不运行 Maven、pnpm、数据库或 E2E。

## Revision R2: 完整复审与收敛

用户继续要求前端交互及自洽性修复。本轮沿用原任务，不创建重复任务、不启动子Agent。

- 重读全部当前文档及关键代码，纠正18类遗漏/冲突，详细见consistency-review.md。
- 删除原Word“主要→关键”的擅自映射；补全常规关闭所需部门负责人和编制等签名；区分处理内容版本与签名动作，防止同版签名互相失效。
- 明确关键偏差直接转审可以没有完整处理；成功关闭原偏差但待审NCR持续阻断四项动作，不伪造跳过节点为合格。
- 明确未推送PQC也能在批记录入口发起；只有activeOrderId的正常详情必须由后端正式来源补齐批记录身份，不能长期报缺ID替代功能。
- 顶层Tab和只读/内嵌宿主明确隔离；处理/验证/签名/转审/评审处置可用，避免四项门禁自锁。
- 一张NCR创建时选同批多条；首版沿用一批一待审且不事后追加，未选偏差保留开放；原PQC不合格等独立NCR入口不受关键偏差限定。
- 把未获本次范围明确确认的PDF/归档扩展从必交条件移到候选建议；在线追溯仍完整保留。P5-AC2因此修订为在线只读及关闭原因，原未决P4-AC7改为明确待审冲突/不自锁验收。
- 增强为28项AC与28项BDD一一映射，状态JSON明确未来功能not_started；后续必须写实际RED/GREEN，不能复用本轮文档证据。
- 初版user-flows.md曾被生成脚本写成字面量换行，旧结构检查仅查字符串未检出；本轮已恢复真实换行，并在validator检查行数与行首标题。初版PASS仅代表当时有限检查，不能视作当前完整审查通过。
- 按project-experience-consolidation将此通用换行校验经验并入现有docs/powershell-encoding.md，不新建长期经验文件。
- 临时_revise_docs.py、_finish_revision.py、_finalize_records.py仅本任务生成工具；最终由cleanup删除。validate-docs.py为正式交付保留。

### 本轮文档行为检查

- BDD: DOC-R2 签名规则一致 -> Given 用户要求所有签字位电子签名, When 对照PRD、权限、交互、模型和测试, Then 常规关闭含部门负责人，关键管理者例外不冲突。
- BDD: DOC-R2 状态接管一致 -> Given QA直接转审自动关闭偏差, When 对照列表、接口、动作门禁, Then 已处理与批次受限独立展示，NCR继续阻断。
- BDD: DOC-R2 可用页面身份 -> Given 原入口只有activeOrderId, When 审阅交互与后端来源设计, Then 必须补齐正式批记录身份而不是以报错页交付。

业务RED/GREEN、数据库和E2E本轮均未运行。增强文档校验与cleanup结果将在verification-report.md记录。

### R2 文档校验结果

- python -X utf8 doc/tasks/20260923-edhr-deviation-management/validate-docs.py -> PASS：14份Markdown、28AC/28BDD逐项映射、JSON/阶段/保留清单、签名/身份/门禁定向检查及四类技能校验器全部通过。
- 校验首次发现测试用例角色简称不完整，补齐“部门负责人”后复验PASS。
- git diff --check -- docs/powershell-encoding.md -> PASS。
- git check-ignore只读确认任务目录被.git/info/exclude的/doc/tasks/*/排除；文件真实存在，本轮未改规则或提交。
- 未运行功能RED/GREEN、Maven、pnpm、数据库或E2E；未来功能状态保持not_started。

### R2 Cleanup

- task_closeout.py --task-id 20260923-edhr-deviation-management --mode preview -> PASS：16项正式资产keep，3个本任务临时脚本delete，无blocked/warnings。
- 相同taskId --mode apply -> PASS/applied：只删除_finalize_records.py、_finish_revision.py、_revise_docs.py。

## P6 Main-Agent Runtime and Browser Verification

- BDD: 偏差列表和创建入口真实可达 -> Given 用户在真实本机租户登录, When 进入偏差管理并打开“发起偏差”, Then 页面显示全部/未处理/已处理、空态和电子签名表单，目标偏差请求成功返回。
- RED: 首次 Playwright 访问 `/mes/pro/feedback/edhr-deviation` -> FAIL；`DeviationDetail.vue` 返回 HTTP 500，Vite 日志为 `Element is missing end tag`，页面动态模块加载失败。
- RED: `node IntRuoyiFronted/tests/e2e/edhr-deviation-detail-template-static.spec.cjs` -> FAIL，模板标签计数 `8 !== 7`。
- 修正：补齐处理卡片 `template #header` 结束标签；移除偏差列表列上与 `sortColumnAttrs(...)` 重复的 `sortable="custom"`。
- GREEN: `node tests/e2e/mes-edhr-deviation-p3-static.spec.cjs` -> PASS；`node tests/e2e/mes-edhr-deviation-p4-static.spec.cjs` -> PASS；`node tests/e2e/edhr-deviation-detail-template-static.spec.cjs` -> PASS。
- GREEN: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS；偏差相关定向 ESLint -> PASS；`git diff --check` -> PASS。
- GREEN: Maven 定向 reactor `MesProEdhrDeviationHandlingServiceTest, MesProEdhrDeviationNcrIntegrationTest, MesProEdhrDeviationNumberGeneratorTest, MesProEdhrDeviationServiceImplTest, MesProEdhrDeviationSignatureIntegrationTest, MesProEdhrBatchExecutionMapperTest, MesProEdhrReleaseServiceImplTest, MesPqcReleaseOrderDetailServiceTest, MesProBatchRecordExecutionSignatureServiceTest` -> BUILD SUCCESS，92 tests，0 failures/errors/skips。
- GREEN: 真实 Playwright `p6-20260925-r4` -> 登录成功；偏差列表、全部/未处理/已处理、没有偏差、发起偏差、批记录选择请求和电子签名表单均可见；偏差页面目标请求 HTTP 200、无 pageerror。非目标头像资源 `test.yudao.iocoder.cn` 返回 502，已单独记录为环境噪声，未作为偏差功能成功依据。
- GREEN: 真实 Playwright 批记录详情 -> 从批次执行列表打开批记录 `900000001149`，进入正式“偏差”页签，显示“没有偏差”，无业务错误。
- BLOCKED: 写入型 E2E 未提交偏差。任务专属夹具创建入口的前 20 个真实生产工单均由正式页面返回业务码 `1040750424`（产品未绑定启用工艺路线），无法创建新的任务专属批记录；已有批记录属于历史任务，按数据所有权规则不能复用写入。
- GREEN: 按当前用户授权将两份正式偏差 SQL 迁移应用到本机 `int-ruoyi-mysql`；只读核对四张偏差表、NCR扩展列、10个权限菜单和1条GxP策略记录存在，重复执行保持幂等。未写入远程或线上库。
- Runtime: `piancha_0923` 使用已登记 `int_main slot 13`，前端 `8094`、后端 `48094`；后端 health `UP`、前端端口监听。服务未改用随机端口。
- BDD: 前端模板回归 -> Given 缺失slot结束标签会阻断动态路由, When 修复并重新编译, Then SFC HTTP 200、Playwright 可打开列表和创建弹窗；证据详见 `bug-regression-evidence.md`。
- 文档里程碑完成，documentationStatus/cleanupStatus=completed。未执行Git写操作，总体ready_for_closeout保留项目Git交付门禁，非功能实现状态。

## P1 Main-Agent MySQL Verification and SQL Correction

- BDD: 月序列SQL可执行 -> Given 新建MySQL 8.0.39隔离库已应用偏差迁移, When mapper语句插入/递增tenant-month序列, Then返回唯一连接态LAST_INSERT_ID且序列值原子递增。
- RED: 对原 mapper SQL 直接运行 `INSERT INTO mes_pro_edhr_deviation_sequence (tenant_id, year_month, last_value) ...` -> MySQL 8.0.39 `ERROR 1064` near `year_month`; 单独引用该列后再次失败 near `last_value`。`node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs` -> FAIL，4 pass/1 fail，定位到未引用的两个保留标识符。
- 修正：`MesProEdhrDeviationSequenceMapper` 对列名 `year_month` 和 `last_value` 在 INSERT 与重复更新表达式中加 MySQL 标识符引用；Node合同锁住新增语句。
- GREEN: `node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs src/test/js/mes-team-leader-active-order-detail-missing-dto-contract.spec.cjs` -> PASS，6/6。
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，44 tests, 0 failures/errors/skips（编号2、偏差服务7、放行服务33、既有放行门面2）。
- BDD: T04候选真实查询 -> Given 同库内有当前租户未推PQC正式批次、作废批次、已上市批次和其他租户批次, When 由MesProEdhrBatchExecutionMapper查询偏差候选, Then仅返回当前租户未作废且未上市放行的正式批记录。
- RED: 首次 `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrBatchExecutionMapperTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，mapper结果total=0；检查发现BaseDbUnitTest中批次/放行测试fixture的租户列为0，不能满足参数tenantId=1。这是fixture身份未初始化，不是放行或候选过滤结果变更。
- GREEN: fixture按正式租户列建样（含租户1/2），同一mapper test -> BUILD SUCCESS，2 tests, 0 failures；只返回未推送PQC的租户1批次，其他租户/作废/RELEASED各一项被排除。
- GREEN: 最终定向命令 `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrReleaseServiceImplTest,MesProEdhrBatchExecutionMapperTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS, 46 tests, 0 failures/errors/skips（Mapper2、编号2、偏差服务7、放行服务33、既有放行门面2）。
- GREEN: `node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs src/test/js/mes-team-leader-active-order-detail-missing-dto-contract.spec.cjs` -> PASS, 6/6。
- BDD: 迁移首次/重复执行 -> Given 隔离MySQL仅有正式生产菜单5700前置, When 执行 `20260924_mes_edhr_deviation_management.sql` 两次, Then四张偏差表、两条权限按钮存在、临时存储过程清理且既有菜单仍原样。
- GREEN: disposable `mysql:8.0.39`, `--network none`, 无宿主端口、只建 `edhr_test`；迁移首次 -> PASS，重复 -> PASS。只读检查输出 `deviation_tables=4, permission_rows=2, temporary_procedure=0, baseline_menu=1`；唯一键/索引快照及可重复命令已写入`execution-log.md`。容器`a6e9f0d5...`及匿名卷`92f850cf...`已停止并不存在。
- BDD: InnoDB序列并发 -> Given 同租户同月份24个独立MySQL连接, When按修正后的 mapper SQL 同时分配, Then返回值各异且最终序列等于24。
- GREEN: 24 concurrent allocation sessions -> PASS，`calls=24, unique=24, last_value=24`。
- BDD: 幂等键唯一与事务回滚 -> Given 24个连接争用同tenant/key同载荷, When执行生产 reservation SQL, Then数据库仅保留一条同载荷reservation；任一事务随后回滚的序列/request行不残留。
- GREEN: reservation concurrency -> PASS，`calls=24, oneReservation=1, samePayload=true`；显式事务rollback -> PASS，sequence和idempotency行数均为0。
- BDD: 创建/上市放行锁顺序 -> Given 同批次创建与上市放行事务在InnoDB竞争, When都先锁批记录行并在事务内检查偏差/放行状态, Then只能安全单边成功，不留RELEASED且开放偏差。
- GREEN: 与Mapper锁谓词相同的隔离MySQL事务过程，8轮同时启动 -> 8轮均为创建胜出且放行被开放偏差阻止；4轮故意让放行先取得批锁 -> 4笔均放行且最终无开放偏差。结果为安全最终状态，无“已放行+开放偏差”。这是数据库层锁语义模拟，非完整Spring服务/MySQL端到端调用；H2放行集成回归另覆盖服务业务检查。
- `python -X utf8 doc/tasks/20260923-edhr-deviation-management/validate-docs.py --skills-root C:\Users\BJB110\.codex\skills` -> PASS，17份Markdown、28AC/28BDD、状态和保留资产、产品/系统/验收规则全部通过。期间检查发现原状态JSON有30个阶段AC并与规范28AC错位、初版执行日志重复标题；已按规范AC重建阶段归属并修复标题，验证转绿。
- GxP boundary unchanged: create路径仍未发起电子签名或追加经登记的GxP事件；覆盖检查`operations=8, annotations=7`只确认现有政策/注解彼此对应，并不扫描未登记的新写方法，不能作为新偏差动作已登记的证据。获批owner、reference、signaturePolicy及retentionClass缺失，未臆造种子或批准事实。
- 用户澄清“批准就是电子签名”：偏差表内发起人、处理、验证、QA、质量负责人、管理者代表等业务批准节点均用正式电子签名完成，`signaturePolicy=REQUIRED` 属已确认需求。它表示业务签署方式，不代表当前已生成发起签名，也不自动构成 GxP 操作登记/策略版本的批准证据；签名仍需绑定真实偏差正文及正式 signatureId。
- 不变量：本轮未对主机MySQL95或任何共享/线上库写入；未启动应用服务或E2E，未提交/推送。P1还需独立审查最新证据并解决GxP策略/发起签名接入门槛，当前阶段保持未完成。

## P1 Electronic Signature Integration Regression

- BDD: 发起批准即电子签名 -> Given 发起偏差需要业务批准, When 提交偏差正文和签名凭证, Then 服务生成绑定 `EDHR_DEVIATION` 的电子签名、保存正文哈希，并在同一事务追加 GxP 审计；缺少凭证或审计策略不存在时零写入。
- RED: 定向回归首次执行 -> FAIL：新增 `GxpAuditService` 构造依赖后既有放行测试缺少测试替身，且幂等冲突测试存在未使用 stub。
- GREEN: 修正测试依赖与无效 stub 后，`mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrDeviationSignatureIntegrationTest,MesProEdhrReleaseServiceImplTest,MesProEdhrBatchExecutionMapperTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，50 tests，0 failures/errors/skips；`node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs src/test/js/mes-team-leader-active-order-detail-missing-dto-contract.spec.cjs` -> PASS，7/7。
- 电子签名集成已具备代码和回归证据：密码不进入载荷哈希，签名失败、GxP append 失败和缺少凭证均回滚；生产运行仍要求正式登记 `edhr.deviation.create` 的 GxP 策略，未以猜测值写入策略配置或数据库。
- RED: 后续复验发现哈希辅助方法的接口可见性契约错误，编译失败。移除非业务接口暴露及多余 `@Override` 后重跑完整定向命令。
- GREEN: 最新 `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrDeviationSignatureIntegrationTest,MesProEdhrReleaseServiceImplTest,MesProEdhrBatchExecutionMapperTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，50 tests，0 failures/errors/skips。GxP reason 由实际分类码和描述组成；策略注册仍保持 fail closed。

## P1 Signature Credential Redaction

- BDD: 签名凭证不得出现在请求对象日志 -> Given 偏差发起请求包含电子签名密码, When 调用请求对象 `toString()`, Then 输出不包含该密码且请求 JSON/业务字段保持不变。
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationServiceImplTest#signaturePasswordIsExcludedFromRequestToString' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，`MesProEdhrDeviationServiceImplTest.signaturePasswordIsExcludedFromRequestToString` 断言密码未被输出失败（`expected: false but was: true`）。
- 修正：在 `MesProEdhrDeviationCreateReqVO.signaturePassword` 字段增加 Lombok `@ToString.Exclude`，仅改变 `toString()` 脱敏行为，不改变 JSON 序列化或请求字段绑定。
- GREEN: 同一 Maven 定向命令 -> BUILD SUCCESS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。

## Completion Gate Recheck

- `check_plan_completion.py --apply` -> NOT COMPLETE. It reports the active GxP prerequisite, P1 acceptance criteria not completed, P2-P6 not implemented/evidenced, and `test_status` not passed. No phase was falsely advanced.

## GxP Policy Registration and Role-Based Configuration

- BDD: 策略登记可配置且角色化 -> Given 偏差批准人/责任人由权限角色承担且所有批准必须电子签名, When 应用迁移并重复执行, Then `edhr.deviation.create` 以版本 `20260924-edhr-deviation-01`、批准依据标识 `EDHR-DEVIATION-GXP-20260924`、owner `ROLE_QA_QUALITY_OWNER`、`signaturePolicy=REQUIRED` 和 `GXP_BATCH_RECORD` 保存，并保持单条幂等策略操作记录。
- GREEN: `python -X utf8 script/gxp_audit_coverage_gate.py --root . --policy config/gxp-audit-policy.yaml` -> PASS，`operations=9 annotations=8`。
- GREEN: 隔离 `mysql:8.0.39`、`--network none` 容器首次和重复执行 `20260924_mes_edhr_deviation_management.sql` -> PASS；策略查询返回 `edhr.deviation.create / 20260924-edhr-deviation-01 / REQUIRED / ROLE_QA_QUALITY_OWNER / GXP_BATCH_RECORD`，重复执行仍为1条，偏差表为空。容器已删除，未写入共享数据库。
- 配置规则：策略修改通过新的 `policyVersion` 和新操作行进行，历史审计事件引用的旧版本不改写；角色码由权限系统维护，不在偏差迁移中自动分配角色。
- GREEN: 配置策略后定向 Maven 回归 `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrDeviationSignatureIntegrationTest,MesProEdhrReleaseServiceImplTest,MesProEdhrBatchExecutionMapperTest,MesProBatchRecordExecutionSignatureServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，69 tests，0 failures/errors/skips；包含签名服务18项、偏差服务8项、偏差签名集成4项、候选Mapper2项、放行服务35项及编号2项。

## P1 Independent Review and Phase Gate

- Independent PASS: policy review verified `edhr.deviation.create` role-owned registration, coverage gate `operations=9 annotations=8`, static policy tests 6/6, Node contracts 7/7, Maven targeted 69/69, and isolated MySQL core-plus-deviation migration first/repeat with one policy operation row and no shared service/database writes.
- P1 gate: `update_phase_state.py --phase-id P1 --outcome completed --test-status passed` -> P1 AC1–AC5 completed; current phase advanced to P2. Full Spring Java/MySQL concurrency remains an explicit scope limitation, not a hidden PASS claim.

## P2 Unique Handling, Signature Matrix and Normal Closure

- BDD: T07唯一处理与重新验证 -> Given 一条开放偏差至多有一份处理记录, When 首次保存、修改正文或验证不合格后再次保存, Then 处理ID保持不变、contentVersion递增、正文摘要变化且不会插入第二条处理记录。
- BDD: T08普通偏差常规关闭 -> Given 处理内容完整且发起、处理编制、验证、部门负责人、QA和质量负责人均有当前版本电子签名, When QA或质量负责人先后顺序任意地完成签名后关闭, Then 偏差以NORMAL_COMPLETED关闭；缺少任一节点或验证未闭环仍保持OPEN。
- BDD: T09关键偏差管理者代表例外 -> Given 关键偏差执行常规处理, When 缺少管理者代表签名尝试关闭, Then 服务端拒绝；补齐管理者代表电子签名后才允许常规关闭。
- BDD: T10正文修订使旧签名失效 -> Given 旧处理版本存在完整签名, When 以乐观版本号保存新正文, Then 旧版本签名仍可审计但不满足当前 subjectVersion/contentHash，必须重新签署后才能关闭。
- BDD: T11同账号多角色同版签名 -> Given 同一账号具备多个签署角色, When 分别调用QA和质量负责人节点签名, Then 两个独立签名主题均生成，互不使对方失效。
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationHandlingServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，新增测试中的 `verify(handlingMapper).insert(any())` 无法在 MyBatis-Plus 重载方法间解析；这是测试契约编译前置错误，已改为显式 `MesProEdhrDeviationHandlingDO` matcher，未将其记录为业务行为失败。
- RED: 同一命令在统一签名查询改为 `system_electronic_signature` 后首次运行 -> FAIL，测试替身按传入顺序返回QA/质量签名，真实关闭按节点顺序查询时暴露测试夹具顺序错误；修正夹具按节点顺序返回，生产查询逻辑保持按主题、版本和摘要精确匹配。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationHandlingServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，6 tests，0 failures/errors/skips；覆盖唯一处理ID/版本冲突、普通偏差QA/质量任意顺序、关键偏差管理者代表缺签、旧版本签名失效和同账号多节点签名。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-DskipTests' compile` -> BUILD SUCCESS；新增服务、Mapper、VO及统一电子签名主题动作编译通过。
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrDeviationSignatureIntegrationTest,MesProEdhrDeviationHandlingServiceTest,MesProEdhrReleaseServiceImplTest,MesProEdhrBatchExecutionMapperTest,MesProBatchRecordExecutionSignatureServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，75 tests，0 failures/errors/skips（P1原有69 + P2处理6）。
- GREEN: 新增只读处理读取 `get(deviationId)` 后再次运行 `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationHandlingServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，7 tests，0 failures/errors/skips；主模块编译与测试编译均重新执行。
- RED: 直接省略 `-am` 的 MES-only 命令 -> FAIL，测试编译命中并行工作区依赖缓存中的无关旧契约（`FormActionExecutionContext`、签名查询接口和ERP模板字段），不是P2源码错误；按项目要求改用包含依赖模块的标准 reactor 命令。
- GREEN: 标准 reactor 命令 `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationHandlingServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，7 tests，0 failures/errors/skips；新增详情读取当前统一签名节点覆盖测试。
- BDD: T10修订审计 -> Given 处理正文已存在且需要再次修改, When 未填写修订原因或填写原因后保存, Then 前者零更新并返回明确错误，后者在同一事务写入已有操作审计链，记录原因、旧正文/摘要和新正文/摘要。
- GREEN: 加入修订审计后同一标准 reactor 命令 -> BUILD SUCCESS，8 tests，0 failures/errors/skips；新增“修订原因必填”和“MES操作审计写入”断言。P2当前实现不新增SQL，复用已有 `mes_pro_edhr_operation_audit_event` 审计链。
- REGRESSION: 最终 P1+P2 reactor 定向命令（含编号、发起、发起签名集成、处理、放行、候选Mapper和签名服务） -> BUILD SUCCESS，77 tests，0 failures/errors/skips（P1 69 + P2 8）。
- P2实现：`mes_pro_edhr_deviation_handling` 复用既有一对一唯一约束；处理记录以内容版本和SHA-256摘要绑定签名。签名事实从统一 `system_electronic_signature` 按 `EDHR_DEVIATION_HANDLING`主题、动作、处理ID、版本和摘要核验，历史签名保留但旧版本不参与关闭。
- P2边界：本轮未新增P2 SQL（P1迁移已包含处理表）；未添加前端入口/列表/详情页面，归P3；未执行E2E、共享数据库写入、服务重启或Git提交/推送。

## P4 Critical Deviation to Nonconformance Review

- BDD: P4关键偏差直接转不合格评审 -> Given QA选择同一批记录下开放的多条关键偏差且批次尚未推送PQC, When QA使用电子签名发起转审, Then 系统在同一事务创建待审评审、冻结批次、关联并关闭所选偏差，未选偏差保持OPEN。
- BDD: P4转审集合与幂等边界 -> Given 选择集合为空、混入普通/跨批/已关闭偏差或已有待审评审, When QA提交转审, Then 整体拒绝且不关闭偏差；相同幂等键重放返回原评审，载荷冲突拒绝。
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNcrIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，P4转审请求、service方法、关联持久化契约尚未实现。
- GREEN: 同一标准 reactor 命令 -> BUILD SUCCESS，7 tests，0 failures/errors/skips；覆盖QA多选同批开放关键偏差、空集/跨批/已关闭拒绝、待审冲突、签名失败零后续推进、相同幂等键重放和载荷冲突。
- REGRESSION: 同一标准 reactor 命令（`MesProEdhrDeviationNcrIntegrationTest,MesProEdhrNonconformanceReviewApplicationScopeTest`） -> BUILD SUCCESS，34 tests，0 failures/errors/skips；既有PQC_SUBMISSION/PQC_RELEASE不合格评审路径保持通过。
- P4实现边界：新增批记录级 `/create-from-critical-deviations` 入口，使用独立 `deviation-create` 权限；批记录行先锁定，再在事务内验证同批CRITICAL+OPEN集合、签署QA发起、写入NCR、冻结批次/工单、关联并关闭偏差。新增迁移只写入待审评审签名/偏差快照/幂等字段和权限，不执行共享数据库；四项生产/放行门禁及三类处置复用既有 `isBatchFrozen`/处置流程，未放宽。
- Frontend contract: `node tests/e2e/mes-edhr-deviation-p4-static.spec.cjs` -> PASS; frontend API exposes DEVIATION source and multi-selection request, while detail rendering does not mislabel transfer closure as verification success.
- REGRESSION: after P4 NCR changes, standard reactor command covering P1/P2/P4 -> BUILD SUCCESS，84 tests，0 failures/errors/skips；P4 integration 7项、既有NCR 27项及P1/P2全套保持通过。
- P5 read evidence: `MesProEdhrDeviationHandlingService#get` now returns effective signature evidence from `ElectronicSignatureQueryService` for current content version; standard reactor handling test -> BUILD SUCCESS, 8 tests. Historical records remain read-only and version-bound.
- P5 trace evidence: `DeviationTracePane` is wired into both `BatchExecutionDetailPage` trace drawer and `BatchExecutionTraceDrawer`; both hosts use formal batchExecutionId and expose only read-only deviation rows with distinct empty/error states.

## P3 Frontend List, Detail and Trace Slice

- BDD: P3偏差列表与详情 -> Given 当前租户有偏差数据, When 打开偏差管理并切换全部/未处理/已处理, Then 前端调用服务端分页接口并展示独立状态；点击详情读取正式偏差详情。
- BDD: P3批记录只读偏差追溯 -> Given 批记录详情已确定正式 `batchExecutionId`, When 打开追溯抽屉的偏差页签, Then 只读读取该批全部偏差，空集显示“没有偏差”，失败显示错误，不回退到旧批数据。
- RED: `node tests/e2e/mes-edhr-deviation-p3-static.spec.cjs` before P3 files -> FAIL，偏差 API、路由和追溯页签不存在。
- GREEN: same command -> PASS，验证 `/page`、`/get` API、三状态页签、空/错状态、偏差路由及批记录追溯页签。
- GREEN: `mvn -pl yudao-module-mes -am '-DskipTests' compile` -> BUILD SUCCESS，新增偏差分页/详情后端接口编译通过。
- LIMITATION: 前端依赖目录不存在，vue-tsc 未运行；用户未授权真实浏览器 E2E，未启动服务。
- REGRESSION: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrDeviationSignatureIntegrationTest,MesProEdhrDeviationHandlingServiceTest,MesProEdhrReleaseServiceImplTest,MesProEdhrBatchExecutionMapperTest,MesProBatchRecordExecutionSignatureServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，77 tests，0 failures/errors/skips。

## P5 Signature Evidence and Error-State Completion

- BDD: P5签名证据显示真实签名人 -> Given 当前处理版本已有统一电子签名记录, When 读取偏差处理详情, Then 响应返回签名节点、真实签名人显示名、签名含义、服务端时间、验证状态、正文摘要、策略版本和主题版本，前端逐项展示。
- BDD: P5处理读取失败 -> Given 偏差详情已读取但处理记录接口失败, When 前端渲染详情, Then 显示处理记录读取错误和重试入口，不把失败转换为“尚未填写处理内容”或“已处理”。
- RED: 独立P4/P5复核 -> FAIL，签名证据只有actorId且详情页未渲染signatureEvidence；处理读取catch静默转空记录。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes,yudao-module-signature -am '-Dtest=ElectronicSignatureServiceImplTest,MesProEdhrDeviationHandlingServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，22 tests，0 failures/errors/skips；覆盖真实签名人显示名映射和处理详情证据映射。
- GREEN: `node tests/e2e/mes-edhr-deviation-p3-static.spec.cjs` -> PASS；静态合同覆盖signatureEvidence、actorDisplayName、处理读取错误重试及禁止静默空记录。
- P5实现边界：真实姓名由统一签名查询读取系统用户目录的昵称（昵称为空时使用账号显示名）；电子签名事实、正文摘要和验证状态仍来自统一签名记录，不拼造签名成功状态。前端依赖目录缺失，未运行vue-tsc；真实浏览器E2E仍未授权。
- GREEN: 增补 `timeEvidenceId`、`evidenceHash` 后，标准 reactor `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationHandlingServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，9/9。
- GREEN: `node tests/e2e/mes-edhr-deviation-p3-static.spec.cjs; node tests/e2e/mes-edhr-deviation-p4-static.spec.cjs` -> PASS，两个静态合同均通过。
- GREEN: 前端类型检查修复 -> 先暴露 `DeviationTracePane` 批记录ID类型和 `DeviationRespVO` 缺少详情字段，补齐正式类型后用同版本依赖临时junction复验，`pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS；junction已删除。

## P5 Historical Revision and Signature Status Slice

- BDD: P5历史处理与签名 -> Given 唯一处理记录发生过修订且旧版本已有签名, When 读取偏差详情, Then 返回修订原因、前后正文/版本/摘要、操作人、审计链哈希，以及当前有效和旧版本失效签名状态。
- RED: P5审计复核 -> FAIL，原详情只返回当前处理和当前签名，修订历史与失效签名不可见。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationHandlingServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，10/10；P1/P2/P4相关选择回归 113/113。
- GREEN: 前端详情类型和静态合同加入 `revisionHistory`、`signatureHistory`、`SUPERSEDED_INVALID` 后，`pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS，P3/P4静态合同继续通过。
- P5边界：历史签名按统一主题版本/正文摘要读取，旧签名保留并标记失效；未新增SQL，复用既有操作审计表和统一签名事实源。

- GREEN: 追溯偏差行补充关联NCR编号、评审状态和处置结果；后端偏差/处理/NCR定向选择测试 -> BUILD SUCCESS，25/25。
- GREEN: P3管理页接入 `UnifiedListTemplate`、标准分页、关键词过滤、状态页签、权限按钮和电子签名发起表单；静态合同继续 PASS，临时同版本依赖 junction 下 `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS，junction已删除。
- GREEN: P3活跃订单入口增加正式来源解析：仅有activeOrderId时调用 `/batch-options-by-active-order`，唯一正式批记录才继续读取，多批记录/无记录明确报错；后端定向 10/10、前端类型检查和静态合同均PASS。
- GREEN: 追溯偏差读取改为按总数分页汇总，加入请求序列保护和重试按钮；偏差详情补充节点选择、历史签名完整证据字段和修订前后正文展示。前端类型检查与静态合同复验通过。
- GREEN: 偏差处理表单补齐调查日期/成员、整改责任人、CAPA和关联评审字段；批记录主详情页增加独立“偏差”Tab；静态合同与前端类型检查继续通过。
- GREEN: P3详情页接入处理记录保存、节点电子签名和常规关闭入口，后端新增对应受权限保护的写接口；前端类型检查和P3/P4静态合同继续PASS。
- GREEN: 批记录页签跳转保留当前查询参数，正式批记录详情与活跃订单详情均提供独立“偏差”Tab；P3静态合同继续PASS。
- GREEN: 末轮验证：文档校验 PASS（18份Markdown、28项AC/BDD），P3/P4静态合同 PASS，前端 `vue-tsc` PASS，偏差后端定向 27/27 PASS；真实浏览器E2E仍未授权/未执行。
- GREEN: 偏差列表补齐标准快速筛选（关键词、等级、发起时间）与自定义服务端排序字段，保留请求序列保护；前端类型检查和静态合同复验通过。
- GREEN: 电子签名证据增加服务器时区 `Asia/Shanghai`，详情表同时显示签名时间、时区、时间证据和证据摘要；签名/处理定向测试与前端类型检查通过。

## SQL Migration Authorization and Role Permission Gate

- BDD: 角色权限迁移首次/重复执行 -> Given 隔离 MySQL 仅有正式生产菜单 5700 和既有 NCR/GxP 基础表, When 执行偏差管理及关键偏差 NCR 迁移两次, Then 偏差四表、NCR四个扩展字段、查询/发起及七个角色动作权限、GxP操作登记均幂等存在，临时过程清理。
- RED: 既有偏差迁移证据仅有 query/create 两个偏差权限，无法证明 handle、verify、department-confirm、qa-close、quality-approve、critical-management-approve、ncr-create 角色动作已登记。
- GREEN: 授权后在 disposable `mysql:8.0.39 --network none` 容器中执行完整依赖链与两份偏差迁移，首次和重复均返回成功；只读断言为 `deviation_tables=4`、`ncr_columns=4`、`permission_rows=10`、`role_permission_rows=7`、`gxp_operation_rows=1`、`temporary_procedures=0`。未写共享数据库。

## P3/P5 Acceptance Audit and Remediation

- BDD: 管理页标准列表 -> Given 当前租户存在普通/重大偏差, When 使用全部/未处理/已处理、等级、关键字和时间筛选并翻页, Then 使用标准列表模板的服务端排序分页，旧请求不能覆盖新筛选结果，并分别显示空集、无权、身份和网络错误。
- BDD: 正式批记录偏差主Tab -> Given 正式批记录详情或活跃表单详情只有正式批记录身份或只有activeOrderId, When 打开偏差主Tab, Then 先通过正式来源解析batchExecutionId再读取同批偏差，保留导航查询参数，不以订单或批号猜测。
- BDD: P5历史证据 -> Given 处理记录发生修订、旧版本签名存在或偏差已转NCR, When 查看管理详情或两个追溯入口, Then 显示修订前后、修订原因、关闭原因、历史签名及失效状态，以及NCR处置结果。
- RED: 独立验收审计 -> FAIL，P3列表/主Tab/activeOrderId/动作流程和P5历史/NCR处置读模型尚未满足；vue-tsc首次暴露4个类型错误。
- GREEN: 修正 `DeviationRespVO` 字段和追溯组件 `number|string` 正式批记录ID后，`pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` 仍需在后续前端功能补齐后复验；当前工作继续按上述BDD切片推进。

## P5 Backend Revision History Read Slice

- BDD: P5处理修订历史只读查询 -> Given 同一偏差处理记录已发生正文修订, When 读取偏差处理详情, Then 响应返回每次成功修订的修订原因、修订前后正文、操作人、服务器时间、前后摘要及审计链摘要，且查询限定当前租户和该处理记录对象。
- BDD: P5历史签名状态区分 -> Given 当前处理版本和旧处理版本均存在电子签名记录, When 读取偏差处理详情, Then 当前版本签名标记为 `CURRENT_VALID`，旧版本签名保留并标记为 `SUPERSEDED_INVALID`，同时保留统一签名记录的验证状态和证据摘要。
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationHandlingServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，新增 revisionHistory 查询断言所需的操作审计查询字段和响应映射尚未实现。
- RED/COMPILATION: 首次实现后同一命令先被共享 worktree 中偏差分页 Mapper 的四个 `LambdaQueryWrapper.orderBy` 调用阻断（Java 17 将 `(asc, column)` 解析为错误重载）；补齐显式 `condition=true` 后重新执行并得到以下 GREEN，不属于P5业务断言失败。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationHandlingServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，10 tests，0 failures/errors/skips；覆盖修订前后正文、原因、操作人/时间、前后摘要及审计链摘要返回，并区分当前 `CURRENT_VALID` 与旧版本 `SUPERSEDED_INVALID` 签名证据。
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNcrIntegrationTest,MesProEdhrNonconformanceReviewApplicationScopeTest,MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrDeviationSignatureIntegrationTest,MesProEdhrDeviationHandlingServiceTest,MesProEdhrReleaseServiceImplTest,MesProEdhrBatchExecutionMapperTest,MesProBatchRecordExecutionSignatureServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS，113 tests，0 failures/errors/skips。
- P5后端边界：复用既有 `mes_pro_edhr_operation_audit_event` 审计表和租户拦截器，仅新增按对象/UPDATE/SUCCESS读取方法与响应投影；未新增SQL、未写共享数据库、未启动服务。
- Runtime recheck after the prior process interruption: restarted the registered `piancha_0923` pair on `8094/48094`; backend health returned `UP` and frontend root returned HTTP 200.
- Playwright `p6-20260926-r6` after restart again opened the real deviation list and signature create dialog with no page errors; the only console error was the previously recorded non-target external avatar 502. The write-path fixture blocker remains unchanged.
- BDD: 任务专属批记录到普通偏差关闭 -> Given 真实页面创建任务专属批记录并发起普通偏差, When 保存唯一处理、完成五个普通签名节点并常规关闭, Then 偏差进入 CLOSED/NORMAL_COMPLETED，处理结论为 CLOSED_LOOP、验证结果为 PASS。
- GREEN: Playwright batch fixture creation -> PASS，批记录 ID 900000001150 / code EDHRB-1790401592564 / task batch P6-DEV-20260926-CAP4-577008。
- GREEN: Playwright deviation create -> PASS，PC-202609-0001 / signatureId 12576 / HTTP 200 business code 0。
- RED: Playwright handling save with blank JSON textarea -> FAIL，HTTP 200 business code 500，MySQL rejected empty string in JSON column `investigation_members_json`。
- GREEN: frontend JSON normalization plus Playwright handling save -> PASS，handlingId 1 / contentVersion 1 / valid `[]` JSON.
- GREEN: Playwright five-node signature and normal close -> PASS，signature IDs 12577–12581; final close `NORMAL_COMPLETED`.
- GREEN: read-only database verification -> PASS，deviation `PC-202609-0001` is `CLOSED`, handling is one row with `CLOSED_LOOP/PASS`; no production or remote database was touched.
- Cleanup preview with `--worktree-closeout off` -> READY: 19 formal Markdown/task assets kept, three temporary evidence files and `output/playwright/deviation-p6` scheduled for deletion, no blocked paths.
- Cleanup apply with `--worktree-closeout off` -> APPLIED: deleted only temporary evidence files and Playwright output/script; kept task records, formal migrations, production code and `src/test` regression tests.
- Full linked-worktree merge/removal was intentionally not executed: preview identified the main worktree as dirty and unrelated concurrent changes in the current worktree; task remains `ready_for_closeout` pending separate Git/worktree integration handling.

## P6 Critical Deviation Full-Chain Browser E2E

- BDD: 关键偏差转不合格审批与处置 -> Given 任务专属批记录存在一条开放关键偏差, When QA在偏差详情以电子签名发起不合格审批并在评审页完成让步放行, Then 偏差以 `TRANSFERRED_TO_NCR` 关闭，批记录在待审期间冻结，评审处置后显示让步放行结果。
- BDD: 转审入口权限与范围 -> Given 偏差详情为普通或已关闭状态, When 用户查看可执行动作, Then 不显示关键偏差转不合格审批写入口；开放关键偏差才显示该入口并使用独立 `ncr-create` 权限。
- RED: Playwright `p6-critical-20260926-r6` -> FAIL，真实页面创建关键偏差成功，但转审请求 HTTP 200/business code `1040750471`，页面显示“eDHR 不合格评审来源类型无效”；只读运行证据确认该批记录没有 `activeOrderId` 来源，违反“偏差只关联批记录”的需求。
- RED: Playwright `p6-critical-20260926-r8` -> FAIL，补齐批记录级来源后，电子签名内核返回 HTTP 200/business code `1047000002`（`MES:NONCONFORMANCE_REVIEW_CREATE` 未登记）；转审入口已真实执行，业务数据按事务回滚。
- RED: `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-deviation-p1-contract.spec.cjs` -> FAIL，MES 签名适配器未暴露 `ACTION_NONCONFORMANCE_REVIEW_CREATE`。
- GREEN: 注册 `ACTION_NONCONFORMANCE_REVIEW_CREATE` 到 MES 批记录签名适配器，并通过同一 Node 合同测试；后续重新打包运行态后再重跑浏览器链路。
- BDD: 批记录无活跃订单来源也可转审 -> Given 正式批记录存在开放关键偏差但没有活跃订单来源, When QA从偏差详情发起转不合格审批, Then 评审以批记录身份创建、偏差关闭原因写为 `TRANSFERRED_TO_NCR`，不因缺少更深层订单关系而拒绝。
- GREEN: 真实 Playwright `p6-critical-20260926-r9` 创建关键偏差 `PC-202609-0006` 并完成电子签名转审；评审 `EDHR-NCR-20260926235142-900000001150` 创建成功，偏差详情显示“转不合格审批关闭”。
- GREEN: `p6-critical-20260927-r12-resume` 真实页面核验批记录“冻结中”，上传评审材料并完成 QA“让步放行”，处置 POST 返回 HTTP 200/business code 0。
- GREEN: `p6-critical-20260926-r13-trace` 通过正式批记录详情“偏差”只读页签核验 `PC-202609-0006` 和让步放行结果；无 pageerror，唯一控制台错误为非目标头像 502 环境噪声。
- RED/GREEN: 发现并修复批记录级偏差转审不应强制 `activeOrderId` 的约束；新增 `qaCanTransferCriticalDeviationWhenBatchHasNoActiveOrderOrigin`，标准 reactor 通过 8/8。
- RED/GREEN: 发现并修复 `MES:NONCONFORMANCE_REVIEW_CREATE` 未在统一签名适配器登记的问题；P1 Node 合同通过 6/6，MES 偏差/NCR/处理/签名回归通过 36/36。
- E2E范围边界：任务专属手工批记录没有活跃订单来源，因此未把生产报工、PQC提交、PQC放行三条需要更深层订单/工序上下文的冻结动作伪装为通过；已真实核验批记录冻结状态、QA处置和批记录级追溯。
- Closeout preview/apply with `--worktree-closeout off` -> APPLIED; task records, production code, migrations and formal tests were retained, no blocked paths. Linked worktree merge/removal was intentionally skipped because the main worktree contains unrelated dirty changes and no Git commit/merge authorization was given in this run.
