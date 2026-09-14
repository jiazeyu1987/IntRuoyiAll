# Test Plan

## Scope And Evidence Rules

本计划只验证 `SP-01`、`SP-02`、`SP-03` 的平台前置能力，覆盖 `AC-01..AC-15`。不验证或实现注册证主档、页面、菜单、审批、grant、提醒任务、历史导入、注册证文件 provider，也不把 SP-02 的通过解释为注册证文件权限 P0 已关闭。

所有产品测试均在对应隔离 worktree 中执行，Maven 命令的工作目录固定为该 worktree 的 `IntRuoyiBackend`。规划阶段不运行这些命令。有效证据必须同时包含：完整命令、tool output、退出码、Surefire/pytest 对应目标测试名、测试数大于零和失败/通过摘要。以下情况一律不是 RED、GREEN 或 PASS：测试未发现、目标类未执行、只有编译失败或环境失败、`exec {}`、没有 tool output、没有退出码、手写 PASS、mock 成功或关闭“指定测试未匹配即失败”的 Surefire 保护。

严格 TDD 顺序固定为：先写可观察的 BDD 测试；执行本计划指定 focused 命令，测试必须被发现且因缺少预期行为或错误行为发生断言失败；只写最小正式实现；执行完全相同的 focused 命令得到 GREEN；再执行相邻回归。依赖、编译、数据库或环境缺失属于 blocker，不能记为 RED。executor 只向主 Agent 回传结构化证据，中央 `execution-log.md` 和 `test-report.md` 均由主 Agent 独占写入。

规划复核的真实工具链是 Maven `3.9.9`、runtime Java `21.0.10`，项目编译合同是 source/target `17`。每个 executor 和独立 tester 首条构建证据均执行 `CMD-TOOLCHAIN-PREFLIGHT`；Java 21 不是失败，Maven 编译 target 不是 17 才是合同失败。不得静默切到 JDK 17；若构建插件或源码硬性要求 JDK 17，停止并报告精确错误。

## Command Catalog

以下命令中的类名是计划合同。executor 新增测试后才执行 RED；不得用“类不存在”制造 RED。

- `CMD-TOOLCHAIN-PREFLIGHT`:
  `mvn -version; java -version; mvn help:evaluate "-Dexpression=maven.compiler.target" -q -DforceStdout`
  证据必须显示 Maven `3.9.9`、runtime Java `21.0.10` 和 compiler target `17`；若执行时机器版本已变化，先报告并重审，不伪造既有版本。

### SP-01

- `CMD-SP01-FOCUSED`:
  `mvn -pl yudao-module-system -DskipITs -Dtest=ControlledContentRegistrationProjectionContractTest,ControlledContentRegistrationProjectionTransactionTest,ControlledContentTransitionProfileContractTest,ControlledContentStateMachineTest test`
- `CMD-SP01-REGRESSION`:
  `mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContent*Test' test`

### SP-02

SP-02 是并行任务中唯一需要 `install` 分支制品的 executor，必须从第一次 Maven 调用开始使用任务独占本地仓库 `D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02`，不能先污染共享 Maven 仓库再补隔离，也不能复制未知来源的共享缓存作为种子。该空目录仅由本任务 Maven 从项目配置的正式仓库解析依赖；解析失败即前置阻塞。executor/tester 的 prepare、focused、regression 每条 Maven 命令都必须显式使用同一 `maven.repo.local`。任务 cache 保留到 integration 复验结束，再按 closeout 只清理本任务目录。

- `CMD-SP02-PREPARE-BASELINE`:
  `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02 -pl yudao-module-dcc -am -Dmaven.test.skip=true -DskipITs install`
- `CMD-SP02-PREPARE-AFTER-INFRA`:
  `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02 -pl yudao-module-dcc -am -Dmaven.test.skip=true -DskipITs install`

- `CMD-SP02-A-FOCUSED`:
  `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02 -pl yudao-module-infra -DskipITs -Dtest=BusinessFileAccessServiceTest,FileServiceImplTest,FileControllerTest test`
- `CMD-SP02-B-FOCUSED`:
  `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02 -pl yudao-module-dcc -DskipITs -Dtest=DccBusinessFileAccessProviderTest,DccBusinessFileAccessProviderContextTest,DccFileDirectLinkAccessGuardTest,DccOnlineFilePreviewServiceTest,DccOnlineFilePreviewControllerTest test`
- `CMD-SP02-C-FOCUSED`:
  `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02 -pl yudao-module-dcc -DskipITs -Dtest=DccBusinessFileAccessProviderTest,DccOnlyOfficePreviewTokenServiceTest,DccOnlyOfficeControlledPreviewTest,DccOnlineFilePreviewServiceTest,DccOnlyOfficeDocumentPdfConversionServiceTest,DccControlledFileUploadApiTest,DccControlledFilePrintServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFilePreviewDownloadApiTest test`
- `CMD-SP02-INFRA-REGRESSION`:
  `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02 -pl yudao-module-infra -DskipITs '-Dtest=File*Test,BusinessFile*Test' test`
- `CMD-SP02-DCC-REGRESSION`:
  `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02 -pl yudao-module-dcc -DskipITs '-Dtest=Dcc*File*Test,DccOnlyOffice*Test' test`

SP-02 必须分三次完成 test-first：先只新增/修改 A 测试并让 `CMD-SP02-A-FOCUSED` 产生已发现测试的断言 RED，完成 A 最小实现和同命令 GREEN；再新增 B 测试并产生 B RED，完成 B 最小实现/GREEN；最后新增 C 测试并产生 C RED，完成 C 最小实现/GREEN。B 前运行 `CMD-SP02-PREPARE-AFTER-INFRA` 使 DCC 使用已 GREEN 的 Infra 制品。任何 A/B/C RED 出现前不得先写该切片生产实现，也不得一次写完全部生产代码再倒填三个 RED。

### SP-03

- `CMD-SP03-FOCUSED`:
  `mvn -pl yudao-module-system -DskipITs -Dtest=NotifyMessageBusinessKeyIdempotencyTest,NotifyMessageSendApiImplTest,NotifySendServiceImplTest,NotifyMessageServiceImplTest test`
- `CMD-SP03-SYSTEM-REGRESSION`:
  `mvn -pl yudao-module-system -DskipITs '-Dtest=Notify*Test' test`
- `CMD-SP03-MIGRATION-CONTRACT`:
  `python -X utf8 -m pytest script/tests/test_system_notify_message_business_key_sql.py -q`
- `CMD-SP03-MIGRATION-POLICY`:
  `New-Item -ItemType Directory -Force -Path target\registration-certificate-platform | Out-Null; python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260612_mes_edhr_final_archive_work_task.sql --sql-file sql/mysql/20260715_mes_edhr_notify_template_garbled_repair.sql --sql-file sql/mysql/20260715_showroom_notify_template_garbled_repair.sql --sql-file sql/mysql/20260815_system_notify_message_business_key.sql --output target/registration-certificate-platform/migration-policy-gate.json`
  The policy gate input must include the declared dependency chain through the latest migration that touches `system_notify_message`; the previous single-file invocation is a tool precondition failure, not product RED.
- `CMD-SP03-BROAD-CALLER-DIAGNOSTIC`:
  `mvn -pl yudao-module-bpm,yudao-module-dcc,yudao-module-infra,yudao-module-mes,yudao-module-showroom -am -DskipITs test`
  This is retained as a baseline diagnostic. Four unrelated Infra runtime-control cases fail identically on current `int_main` without SP-03 and stop the reactor before the actual callers, so this command is not allowed to substitute for or invalidate the targeted caller gate below.
- `CMD-SP03-CALLER-REGRESSION` consists of all of these non-skipped targeted commands against the task-owned current SP-03 System artifact:
  - `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp03 -pl yudao-module-bpm -DskipITs -Dtest=BpmMessageServiceImplTest#sendMessageWhenTaskAssigned_dccProcess_usesNotifyInbox test`
  - `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp03 -pl yudao-module-dcc -DskipITs -Dtest=DccControlledFileMessageReplayServiceTest#replayMessageJobs_distributionPendingJob_sendsAndMarksSent+replayMessageJobs_downstreamFailure_marksFailedAndRethrows test`
  - `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp03 -pl yudao-module-infra -DskipITs -Dtest=RuntimeOpsAlertServiceImplTest test`
  - `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp03 -pl yudao-module-mes -DskipITs -Dtest=MesProEdhrWorkTaskOwnershipTransferTest#reconcileProcessFormFillTaskOwnership_transfersSameSourceActiveTaskAndSendsReassignmentNotify+reconcileProcessFormFillTaskOwnership_failsFastWhenReassignmentNotifyFails test`
  - `mvn -Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp03 -pl yudao-module-showroom -DskipITs -Dtest=ShowroomAssignmentWorkflowTest#assignmentShouldRequirePersistedNotifyMessageId,ShowroomHttpApiIntegrationTest#productWorkflowShouldCreateNotifyMessagesForReviewersAndSubmitter test`
- `CMD-SP03-IOT-CALLER-REGRESSION`:
  `mvn -f yudao-module-iot/yudao-module-iot-biz/pom.xml -DskipITs -Dtest=IotAlertTriggerSceneRuleActionTest test`
- `CMD-SP03-TRADE-CALLER-CONTRACT`:
  prove with repository search that the only Trade `NotifyMessageSendApi` call remains after a fixed unconditional return, then compile the Trade leaf against the current SP-03 System artifact with tests skipped only for this compile contract. This is static evidence that Trade has no current runtime caller, not test PASS; removing the return or counting class-level `@Disabled` tests is forbidden in this task.

Root `IntRuoyiBackend/pom.xml` 第 23、26 行实际注释了 Mall 和 IoT reactor，因此 IoT 测试和 Trade compile contract 必须用各自 leaf pom 独立执行，不能把 root reactor PASS 当作已覆盖它们。每条测试命令必须发现对应现有测试；依赖制品缺失是 blocker，不得静默遗漏调用方。

### Integration

- `CMD-INTEGRATION-REGRESSION` consists of the SP-01 focused/regression commands, all SP-02 A/B/C and Infra/DCC regression commands, the SP-03 focused/System commands, every targeted SP-03 caller command, IoT caller regression, and the Trade static/compile contract. The unrelated failing broad Infra diagnostic is recorded separately and is never reported as PASS.
- `CMD-INTEGRATION-MIGRATION` consists of both `CMD-SP03-MIGRATION-CONTRACT` and `CMD-SP03-MIGRATION-POLICY`, executed from the integration worktree.
- `CMD-BRANCH-RUNTIME-GUARD` from repository root:
  `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1`
- Diff checks from repository root:
  `git diff --check <frozen-base>..HEAD`, `git diff --name-status <frozen-base>..HEAD`, and `rg -n "^(<<<<<<<|=======|>>>>>>>)" IntRuoyiBackend`.

For every Maven command that executes tests, the executor/tester must inspect the newly generated XML files under the invoked module's `target/surefire-reports`, prove at least one requested target report exists, and sum `tests` to a value greater than zero. Stale reports are not evidence: only the target module's task-owned report directory may be removed before the run. The Maven dependency preparation commands use `maven.test.skip=true` and are prerequisite evidence only, never RED/GREEN/PASS. For each pytest command, the terminal summary must show a non-zero collected count. A zero-test exit, even if the process exit code is zero, is FAIL.

## SP-01 BDD And TDD Cases

### TC-01 Registration profile and transition

- `test_case_id`: `TC-01`
- `mapped_task_ids`: `T-SP01`
- `mapped_acceptance_ids`: `AC-01`
- `environment_setup`: SP-01 worktree on the frozen committed baseline; `CMD-TOOLCHAIN-PREFLIGHT` proves Maven 3.9.9, runtime Java 21.0.10 and compile target 17; tests in `ControlledContentTransitionProfileContractTest`, `ControlledContentStateMachineTest`, and new `ControlledContentRegistrationProjectionContractTest`.
- `steps`: Given existing MES/DCC profiles and a registration content type, When all actions and transition rules are enumerated, Then registration actions equal exactly `REGISTER_ACTIVE`, `REGISTER_READY_CANDIDATE`, `PUBLISH`, `SUPERSEDE_ACTIVE`; the new action allows only `null -> READY_TO_PUBLISH`; MES/DCC actions are unchanged and every unlisted action is rejected. Write assertions first, run `CMD-SP01-FOCUSED` for a discovered assertion-failure RED, implement the enum/profile/rule, then run the identical command GREEN.
- `expected_result`: Exact set equality and exact transition rule pass; no second state machine and no change to MES/DCC behavior.
- `evidence`: RED and GREEN command output/exit code, Surefire XML with target class names and tests greater than zero, plus `CMD-SP01-REGRESSION` PASS.

### TC-02 ID-level projection drift before writes

- `test_case_id`: `TC-02`
- `mapped_task_ids`: `T-SP01`
- `mapped_acceptance_ids`: `AC-02`
- `environment_setup`: new projection contract test with mocked ref/audit mappers and deterministic domain/platform snapshots having equal counts but swapped native version IDs.
- `steps`: Given counts match but active/candidate IDs differ, When a protected registration transition is requested, Then a drift exception names the mismatched IDs and is thrown before either mapper insert/update. Establish RED/GREEN with `CMD-SP01-FOCUSED`.
- `expected_result`: Failure occurs before the first write; Mockito verifies no write calls and no auto-repair.
- `evidence`: failing assertion RED, same-command GREEN, ordered/no-interaction verification in test source, Surefire report and regression PASS.

### TC-03 Missing, contradictory, or corrupt projection inputs

- `test_case_id`: `TC-03`
- `mapped_task_ids`: `T-SP01`
- `mapped_acceptance_ids`: `AC-03`
- `environment_setup`: parameterized contract cases for missing `platformBefore`/`domainAfter`, tenant/type/key mismatch, illegal delta, multiple active/open candidates and existing ref without transition audit; a separate genuinely empty projection fixture.
- `steps`: Given each invalid case, When a protected mutation runs, Then it fails before writes without supplying defaults; Given a genuinely empty projection with zero ref/audit, Then valid initial registration is not rejected. Run strict RED then same-command GREEN through `CMD-SP01-FOCUSED`.
- `expected_result`: Every corrupt non-empty case is rejected; only the explicitly empty initial case is accepted.
- `evidence`: parameterized case names in Surefire XML/output, mapper no-write assertions, RED/GREEN exits and adjacent regression.

### TC-04 Atomic publication and rollback

- `test_case_id`: `TC-04`
- `mapped_task_ids`: `T-SP01`
- `mapped_acceptance_ids`: `AC-04`
- `environment_setup`: dedicated SP-01 test DDL/resource and a real Spring transaction test named `ControlledContentRegistrationProjectionTransactionTest`; no use or edit of the System global H2 fixture owned by SP-03.
- `steps`: Given matching snapshots and a ready candidate, When publication succeeds, Then old active becomes superseded, candidate becomes active and two audits exist. Inject a failure into a later ref/audit write, rerun publication, and query after rollback. Use `CMD-SP01-FOCUSED` for RED and the same command for GREEN.
- `expected_result`: Successful state is complete; injected failure leaves prior rows unchanged, no second active, and no partial audit.
- `evidence`: database assertions before/after rollback, non-zero Surefire report, RED/GREEN output and `CMD-SP01-REGRESSION` PASS. An annotation-only assertion is insufficient.

## SP-02 BDD And TDD Cases

### TC-05 Public direct link is denied before bytes

- `test_case_id`: `TC-05`
- `mapped_task_ids`: `T-SP02`
- `mapped_acceptance_ids`: `AC-05`
- `environment_setup`: Infra aggregator test plus existing `FileServiceImplTest`/`FileControllerTest` and DCC formal-reference provider fixture for a controlled DCC file.
- `steps`: Given DCC formally claims an infra file, When the public direct link is requested, Then `DIRECT_LINK` authorization denies before file bytes/client access and records sanitized rejection evidence. First execute `CMD-SP02-A-FOCUSED` with discovered failing assertions, implement the minimum Infra gate, then execute the identical command GREEN; separately prove DCC provider mapping with `CMD-SP02-B-FOCUSED`.
- `expected_result`: Business file cannot pass `@PermitAll/@TenantIgnore` by URL knowledge; byte access is never invoked.
- `evidence`: both focused outputs and exit codes, non-zero target reports, ordered mock no-IO assertion, sanitized audit assertion and both module regressions.

### TC-06 Generic preview resolves the formal object

- `test_case_id`: `TC-06`
- `mapped_task_ids`: `T-SP02`
- `mapped_acceptance_ids`: `AC-06`
- `environment_setup`: `DccBusinessFileAccessProviderTest`, generic preview service/controller tests, formal DCC artifact references for same-tenant allowed, cross-tenant, wrong object/company scope, missing grant and `controlled=false` samples.
- `steps`: Given a caller has only coarse controller permission and submits a bare file ID, When metadata or binary preview runs, Then the provider reverse-resolves the formal object and `PREVIEW` is checked before data access; all invalid samples deny, including `controlled=false`. Run `CMD-SP02-B-FOCUSED` RED then identical GREEN.
- `expected_result`: Caller claims never create identity or authority; metadata and binary entry points share the same operation-aware gate.
- `evidence`: target Surefire cases, formal-reference query assertion, denial-before-read ordering, RED/GREEN and DCC regression.

### TC-07 OnlyOffice token binding and real-time recheck

- `test_case_id`: `TC-07`
- `mapped_task_ids`: `T-SP02`
- `mapped_acceptance_ids`: `AC-07`
- `environment_setup`: existing token, controlled preview, generic preview and upload callback tests extended with tenant, user/service identity, formal object, infra file ID, operation, expiry and authorization-revocation fixtures.
- `steps`: Given a valid business-file token, When callback/readback occurs, Then signature/claims and real-time `ONLYOFFICE_PREVIEW` authorization both pass before read. Change tenant, user, file, operation, expiry or revoke access one at a time and assert denial/no-read. Run `CMD-SP02-C-FOCUSED` RED then the identical command GREEN.
- `expected_result`: Token possession alone never grants access and no cross-operation token replay succeeds.
- `evidence`: six negative case names, read-client no-interaction assertions, RED/GREEN output, non-zero reports and DCC regression.

### TC-08 Convert, print, and download enforce the common gate

- `test_case_id`: `TC-08`
- `mapped_task_ids`: `T-SP02`
- `mapped_acceptance_ids`: `AC-08`
- `environment_setup`: conversion, print, query and preview-download tests with provider-deny and provider-allow fixtures; existing DCC conversion validity, print-category permission and download-policy fixtures retained.
- `steps`: Given provider denies each operation, When conversion, print or download is invoked, Then respectively no external conversion client, print record/HTML builder, or byte reader runs. Given provider allows, Then existing DCC checks still run and can independently deny. Execute `CMD-SP02-C-FOCUSED` RED then identical GREEN.
- `expected_result`: Unified gate is necessary but does not bypass DCC controls; every side effect follows authorization.
- `evidence`: operation-specific no-interaction/order assertions, existing-check denial assertions, target report count, RED/GREEN and DCC regression.

### TC-09 Ambiguous provider state fails closed; ordinary files remain ordinary

- `test_case_id`: `TC-09`
- `mapped_task_ids`: `T-SP02`
- `mapped_acceptance_ids`: `AC-09`
- `environment_setup`: `BusinessFileAccessServiceTest` cases for duplicate claims, provider exception and incomplete context; `DccBusinessFileAccessProviderContextTest` boots the relevant Spring slice and asserts exactly the required DCC provider is present; ordinary infra file has no formal business reference.
- `steps`: Given duplicate claims, provider error, missing authorization provider, tenant/subject/service identity/request ID, When any exit evaluates access, Then it explicitly denies. Remove the DCC provider from the Spring test context and prove the context contract fails. Given every provider explicitly reports no formal reference for an ordinary file, Then the pre-existing ordinary-file contract continues without labeling it registration-authorized. Use `CMD-SP02-A-FOCUSED` and `CMD-SP02-B-FOCUSED` in strict RED/same-command GREEN cycles.
- `expected_result`: No ambiguity defaults to allow; provider absence is detectable; ordinary-file behavior cannot serve as registration-file proof.
- `evidence`: each negative branch's error assertion, context bean/count assertion, ordinary-file positive case, both focused command pairs, non-zero reports and regressions.

## SP-03 BDD And TDD Cases

### TC-10 First idempotent send and persistence contract

- `test_case_id`: `TC-10`
- `mapped_task_ids`: `T-SP03`
- `mapped_acceptance_ids`: `AC-10`
- `environment_setup`: System notify H2 fixture updated by SP-03; new `NotifyMessageBusinessKeyIdempotencyTest` and API test; enabled template, complete parameters, explicit tenant and new stable business key.
- `steps`: Given valid input, When idempotent Admin send is called, Then it returns a non-empty message ID and persists the trimmed key. Write the test first, run `CMD-SP03-FOCUSED` for a discovered assertion-failure RED, implement minimum DTO/API/service/DO/mapper behavior, and rerun the identical command GREEN. Separately run both migration commands.
- `expected_result`: one row, exact normalized key, non-empty ID, nullable `varchar(255)` column and unique `(tenant_id,business_key)` contract.
- `evidence`: RED/GREEN output and exit codes, non-zero Surefire reports, H2 row assertion, pytest collected count, migration policy JSON and System regression.

### TC-11 Serial replay returns the same row

- `test_case_id`: `TC-11`
- `mapped_task_ids`: `T-SP03`
- `mapped_acceptance_ids`: `AC-11`
- `environment_setup`: successful TC-10 row and the exact same tenant, recipient type/ID, template, normalized parameters and key.
- `steps`: Given the first request committed but its acknowledgement was lost, When the identical request is replayed, Then return the original ID and retain exactly one row. Run strict RED and same-command GREEN with `CMD-SP03-FOCUSED`.
- `expected_result`: deterministic same ID and row count one; no second render/insert presented as success.
- `evidence`: ID equality and row-count assertions, RED/GREEN command evidence and System regression.

### TC-12 Concurrent replay uses the database constraint

- `test_case_id`: `TC-12`
- `mapped_task_ids`: `T-SP03`
- `mapped_acceptance_ids`: `AC-12`
- `environment_setup`: real transactional H2/MySQL-compatible test with independently synchronized worker transactions and the formal unique constraint; no single-thread mock of concurrency.
- `steps`: Given several workers use the same tenant/key/payload, When they cross an insert barrier and finish, Then every successful call returns one identical non-empty ID and the database has at most one row. Run `CMD-SP03-FOCUSED` RED then identical GREEN.
- `expected_result`: duplicate-key is re-read and strictly compared; it is never swallowed into null/new ID.
- `evidence`: worker IDs, row-count assertion, constraint-related trace without secrets, non-zero report, RED/GREEN and regression.

### TC-13 Same key with changed payload is a conflict

- `test_case_id`: `TC-13`
- `mapped_task_ids`: `T-SP03`
- `mapped_acceptance_ids`: `AC-13`
- `environment_setup`: parameterized replays changing recipient type, recipient ID, template code or one normalized template parameter independently.
- `steps`: Given a key is already bound, When one bound field changes, Then throw the explicit business-key conflict and preserve the original row. Run strict RED/GREEN through `CMD-SP03-FOCUSED`.
- `expected_result`: every changed-field case fails; row count and stored payload remain unchanged.
- `evidence`: parameterized case names/error code, before/after row assertions, RED/GREEN and System regression.

### TC-14 Tenant isolation for identical keys

- `test_case_id`: `TC-14`
- `mapped_task_ids`: `T-SP03`
- `mapped_acceptance_ids`: `AC-14`
- `environment_setup`: two explicit tenant contexts with the same business key and same payload; cleanup is task-owned test data only.
- `steps`: Given two tenants, When each sends and replays, Then each has a distinct message ID and each tenant can only retrieve its own row. Run `CMD-SP03-FOCUSED` RED then identical GREEN.
- `expected_result`: two rows across tenants, one per tenant/key; no cross-tenant read or replay.
- `evidence`: IDs/tenant row assertions, explicit tenant context setup, RED/GREEN and regression.

### TC-15 Invalid sends fail explicitly

- `test_case_id`: `TC-15`
- `mapped_task_ids`: `T-SP03`
- `mapped_acceptance_ids`: `AC-15`
- `environment_setup`: blank/overlength business key, missing/disabled template, missing template parameter and mocked lower-level empty message ID; both new idempotent API and corresponding existing non-idempotent path are covered.
- `steps`: Given each invalid condition, When send is invoked, Then it throws a precise error, returns no null success and persists no successful message. First change the existing disabled-template null expectation to the required exception and run `CMD-SP03-FOCUSED` RED; implement the fail-fast behavior and run the identical command GREEN.
- `expected_result`: every invalid case is observable failure; no swallowed exception, placeholder success or compatibility branch remains.
- `evidence`: error-code/type assertions, no-row assertions, RED/GREEN output, non-zero reports and System/caller regressions.

## Database And Runtime Schema Gates

### TC-16 Migration file and full-schema contract

- `test_case_id`: `TC-16`
- `mapped_task_ids`: `T-SP03`, `T-INTEGRATE`
- `mapped_acceptance_ids`: `AC-10`, `AC-12`, `AC-14`
- `environment_setup`: committed migration `sql/mysql/20260815_system_notify_message_business_key.sql`, System H2 fixture, full schema `sql/mysql/ruoyi-vue-pro.sql`, and migration contract test; UTF-8 Python runtime.
- `steps`: Before SQL implementation, add contract assertions and run `CMD-SP03-MIGRATION-CONTRACT` for collected failing tests. Add the minimal additive nullable column and unique index, release metadata and full-schema/H2 synchronization, then run the identical pytest command GREEN and run `CMD-SP03-MIGRATION-POLICY`.
- `expected_result`: column is nullable `varchar(255)`, no historic-row backfill/default business key exists, unique key order is `(tenant_id,business_key)`, release metadata 精确包含 `allowedEnvironments=test,backup,prod; dependsOn=20260715_showroom_notify_template_garbled_repair; type=schema; riskLevel=medium`，contract test 同时断言该依赖是实际触及 `system_notify_message` 的最新 DAG 叶子；mismatched existing schema fails through preflight, and all four artifacts agree.
- `evidence`: pytest RED/GREEN with non-zero count, policy gate exit zero/JSON, exact SQL assertions and diff.

### TC-17 Read-only runtime schema evidence

- `test_case_id`: `TC-17`
- `mapped_task_ids`: `T-INTEGRATE`
- `mapped_acceptance_ids`: `AC-04`, `AC-10`, `AC-12`, `AC-14`
- `environment_setup`: 必须取得明确的非生产目标 schema 和只读凭据，并通过批准的本地秘密处理使用。连接前 tester/main Agent 必须读取 `docs/server-access.md`, `docs/database-rules.md`, `docs/release-backup-restore.md`；凭据和原始连接串不得进入日志。本 gate 只读且绝不授权执行 migration。
- `steps`: Query `information_schema.tables`, `information_schema.columns` and `information_schema.statistics` read-only. Prove the `system_notify_message` base table exists and record whether `business_key`/its unique index are absent (valid pre-migration state) or already present; if present, the column must be nullable `varchar(255)` and the unique index columns exactly tenant then business key, otherwise fail. Also verify the deployed controlled-content ref/audit tables and their active/open-candidate uniqueness contracts from `20260718_controlled_content_lifecycle.sql`. Do not apply or repair migrations in this gate.
- `expected_result`: runtime preflight 与 additive migration 兼容，任何已部署的新列/索引必须精确匹配；基表缺失、半迁移、冲突列/索引、controlled-content 合同不匹配或目标/凭据缺失都会阻断最终 fast-forward。新 notify 列/索引完全缺失只记为“尚未部署”，不阻断代码融合且绝不报告为已迁移，但前置基表与既有 controlled-content 合同必须有真实只读证据。
- `evidence`: redacted query, result rows, target environment identifier, timestamp and exit code recorded by main Agent; no secrets or writes.

## Independent Verification And Integration

### TC-18 Independent branch verification

- `test_case_id`: `TC-18`
- `mapped_task_ids`: `T-VERIFY`
- `mapped_acceptance_ids`: `AC-01..AC-15`
- `environment_setup`: executor 开发可并行，但 tester 只在槽位轮转到该分支、active registry entry/branch runtime guard 均通过、executor 分支已提交且 clean 后进入；assigned tester 未编写该分支产品代码，只接收需求/命令而不接收 executor 的 PASS 结论。integration/SP-01 先占当前两个槽，SP-01 进入 integration 并移除 worktree/释放槽后 SP-02 才提交和复验，SP-02 同样清理后 SP-03 才提交和复验。
- `steps`: 每个分支先运行 `CMD-TOOLCHAIN-PREFLIGHT`。For SP-01 rerun both SP-01 commands. For SP-02 run `CMD-SP02-PREPARE-AFTER-INFRA` then all five SP-02 focused/regression commands，且全部使用固定 task-owned Maven repo。For SP-03 rerun focused, System regression, both migration commands, every targeted `CMD-SP03-CALLER-REGRESSION` command, `CMD-SP03-IOT-CALLER-REGRESSION` and `CMD-SP03-TRADE-CALLER-CONTRACT`; also confirm the broad diagnostic reproduces only the separately proven Infra baseline failures and is never recorded as PASS. Inspect actual tests, production diff, test reports, output, exit codes and AC mapping. Tester must not modify product code or tests; failures return to the original executor.
- `expected_result`: all target tests are discovered and pass, every AC has direct evidence, Git diff stays inside approved branch scope, and no uncommitted product diff remains.
- `evidence`: tester identity/task, branch/commit SHA, command outputs/exits, XML/pytest counts, diff list and explicit PASS/FAIL for every mapped AC returned to main Agent.

### TC-19 Integrated combined regression and security ordering

- `test_case_id`: `TC-19`
- `mapped_task_ids`: `T-INTEGRATE`
- `mapped_acceptance_ids`: `AC-01..AC-15`
- `environment_setup`: integration worktree contains only independently verified SP-01, SP-02 and SP-03 task commits, applied in order; worktree is clean before verification.
- `steps`: After each commit, rerun its focused command. After all commits, run `CMD-TOOLCHAIN-PREFLIGHT`, `CMD-INTEGRATION-REGRESSION`, `CMD-INTEGRATION-MIGRATION`, conditional TC-17, Spring provider presence, `CMD-BRANCH-RUNTIME-GUARD`, diff checks and conflict-marker scan. Review call order so the SP-02 gate precedes file IO, conversion, printing and download; confirm Infra has no DCC dependency and DCC supplies the runtime provider.
- `expected_result`: combined regression has non-zero tests and passes; static/H2 migration contract, runtime schema preflight and provider context pass; no whitespace/conflict markers/out-of-scope files or authorization-after-side-effect path exists.
- `evidence`: integration HEAD, every command output/exit and test count, runtime schema evidence, provider context report, `git diff --check`, name-status list, marker scan and security call-order review.

### TC-20 Safe fast-forward and dirty-worktree preservation

- `test_case_id`: `TC-20`
- `mapped_task_ids`: `T-FUSE`
- `mapped_acceptance_ids`: `AC-01..AC-15`
- `environment_setup`: TC-19 is PASS; main Agent records current `int_main` HEAD and full staged/unstaged/untracked path set without changing it; integration HEAD and frozen base are known.
- `steps`: Compute incoming changed paths and intersect them with current dirty paths. If non-empty, stop. If empty, prove `int_main` is an ancestor and use only fast-forward merge. Recheck commit ancestry and the exact dirty path/content baseline after merge. Run closeout preview/apply only for task-owned artifacts.
- `expected_result`: no user/concurrent changes are staged, committed, stashed, cleaned or overwritten; only verified task commits enter `int_main`; collision or non-fast-forward state blocks fusion.
- `evidence`: before/after HEAD, ancestry exit codes, incoming/dirty/intersection lists, fast-forward output, preserved dirty baseline and closeout records.

## Acceptance Traceability Matrix

| Acceptance | Primary case | Focused command | Adjacent/integration evidence |
| --- | --- | --- | --- |
| `AC-01` | `TC-01` | `CMD-SP01-FOCUSED` | `CMD-SP01-REGRESSION`, `TC-19` |
| `AC-02` | `TC-02` | `CMD-SP01-FOCUSED` | no-write ordering, `TC-19` |
| `AC-03` | `TC-03` | `CMD-SP01-FOCUSED` | parameterized corruption cases, `TC-19` |
| `AC-04` | `TC-04` | `CMD-SP01-FOCUSED` | transaction rollback, `TC-17`, `TC-19` |
| `AC-05` | `TC-05` | `CMD-SP02-A/B-FOCUSED` | both SP-02 regressions, `TC-19` |
| `AC-06` | `TC-06` | `CMD-SP02-B-FOCUSED` | DCC regression, `TC-19` |
| `AC-07` | `TC-07` | `CMD-SP02-C-FOCUSED` | DCC regression, `TC-19` |
| `AC-08` | `TC-08` | `CMD-SP02-C-FOCUSED` | DCC regression, `TC-19` |
| `AC-09` | `TC-09` | `CMD-SP02-A/B-FOCUSED` | context contract, both regressions, `TC-19` |
| `AC-10` | `TC-10`, `TC-16` | `CMD-SP03-FOCUSED` | migration/runtime schema, `TC-19` |
| `AC-11` | `TC-11` | `CMD-SP03-FOCUSED` | System regression, `TC-19` |
| `AC-12` | `TC-12`, `TC-16` | `CMD-SP03-FOCUSED` | unique constraint/runtime schema, `TC-19` |
| `AC-13` | `TC-13` | `CMD-SP03-FOCUSED` | System regression, `TC-19` |
| `AC-14` | `TC-14`, `TC-16` | `CMD-SP03-FOCUSED` | tenant/runtime schema, `TC-19` |
| `AC-15` | `TC-15` | `CMD-SP03-FOCUSED` | System and caller regressions, `TC-19` |

## E2E Decision And Stop Conditions

本任务没有前端交付，也没有获批的注册证真实用户入口，因此不运行 Playwright E2E。这是 PRD 范围判定，不是用单测替代已要求的 E2E；后续注册证领域/provider/页面存在后，其真实权限路径仍须单独以 Playwright 验证。本任务不启动本地前后端服务。

- 目标测试数为零、报告缺目标类或命令无明确退出码：阻塞证据，不接受 PASS。
- Maven/JDK/依赖、H2/事务 fixture 或 pytest 前置缺失：报告准确 prerequisite，不能记作 RED。
- SP-02 需要修改当前主工作区已脏的 DCC controller、DCC error constants 或 DCC H2 fixture：停止并请求主 Agent 重审写入范围。
- 运行态目标数据库/只读凭据缺失：`TC-17` FAIL，组合验收和最终 fast-forward 停止；若实际只读查询证明 schema 半迁移或不兼容，同样停止融合。
- 注册证文件 provider 缺失：不影响平台 SP-02 合同测试，但注册证文件权限 P0 必须继续标记 blocked。
- 独立 tester 修改产品代码/测试、分支有未提交产品 diff、集成引入未验证修复：该验证无效并退回主 Agent。
- incoming 路径与融合时 `int_main` dirty 路径相交，或无法 fast-forward：停止，不 stash、不覆盖、不自动冲突解决。

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；验证现有三个平台核心的正式扩展、数据库约束和所有服务端出口。
- 是否存在临时补丁或绕过：否。
