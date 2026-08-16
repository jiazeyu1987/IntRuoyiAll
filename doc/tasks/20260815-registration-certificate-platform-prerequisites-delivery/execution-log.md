# Execution Log

- Task ID: `20260815-registration-certificate-platform-prerequisites-delivery`
- User intent: 在 worktree 中启动多个子 Agent按文档实施，由主 Agent review，成功验证后融合到 `int_main`。

## BDD Baseline

- BDD: 只执行已解除依赖的平台前置 -> Given 原注册证设计仍有方向型业务 blocker / When 计算可并行任务 / Then 只有不依赖这些口径且写入范围不冲突的 SP-01/SP-02/SP-03 可进入执行，注册证领域和页面继续 blocked。
- BDD: 并行 worktree 隔离 -> Given 三个 executor 并行开发 / When 任一任务需要修改未分配文件或共享任务状态 / Then 立即停止并交由主 Agent重新裁决，不在主工作区或其他 Agent worktree 写入。
- BDD: review 后融合 -> Given executor 自报 GREEN / When 主 Agent和独立 tester 复核 / Then 只有目标测试实际执行、无 fallback、相邻回归通过的提交可进入集成分支，组合验证通过后才可融合 `int_main`。

## Preflight

- `development-plan-delivery` 入口检查：blocked；原任务缺少技能要求的 `development-plan.md`，且机器状态为 `blocked/needs_revision`，未伪造或重命名旧文档绕过。
- 改用 `supervised-complex-delivery` 建立独立平台前置任务包；该调整不改变产品行为，只为三个可执行前置任务建立监督、独立测试和状态合同。
- `E:\IntRuoyi` 当前 `int_main` 有大量其它任务脏改动；本任务不修改、暂存或提交这些文件。所有产品实现将在 `D:\IntRuoyiWorktree\` 新建 worktree 中进行。
- Worktree 根目录存在且可写；计划路径解析为其子目录。未启动服务，因此当前不预留 runtime slot。
- Git operations are authorized by the user goal only for task-owned branches/worktrees and final verified fusion; unrelated changes remain untouched.

## Current Stage

- M1 planning gate revision 1 in progress.

## Planner Review Round 1

- `P1-SP01-STATE-001`: `FR-02/AC-01` only允许 `REGISTER_ACTIVE/CREATE_CANDIDATE/PUBLISH/SUPERSEDE_ACTIVE`，但当前 core 的 `CREATE_CANDIDATE` 固定创建 `DRAFT`，state machine 的 `PUBLISH` 只允许 `READY_TO_PUBLISH -> ACTIVE`。按当前合同没有合法路径把候选推进到可发布态，`AC-04` 不可执行。
- `P1-SP01-PROJECTION-002`: “领域精确期望投影”未区分 mutation 前平台状态、领域事务内已写入的新版本和 mutation 后目标状态。注册首证、创建候选、发布三类调用的校验时点与允许 delta 未定义，会把正常新增误判为漂移或允许半事务写入。
- `P1-SP02-TRUST-003`: `FR-07` 要求调用者携带“业务对象声明”，但对象归属必须由 provider 从正式引用反查，不能信任调用者声明；请求中的 claim 只能作为待比对信息。
- `P1-SP02-CLAIM-004`: `FR-12` 允许真正普通、无 provider 声明的文件沿用既有合同，而 `NFR-08` 又要求“缺 provider”一律失败。当前聚合器无法区分普通文件与漏装 provider 的业务文件；必须明确运行时可判定范围和后续领域集成门禁，不能给出不可实现的 fail-closed 承诺。
- 裁决：规划未通过，不创建 worktree、不进入任务分解；唯一 planner writer 修订 `request-analysis.md/prd.md` 后重新审查。

## Planner Review Round 2

- 主 Agent 依据原需求作出无需额外业务确认的裁决：正式续证不是人工审批流，平台新增 `REGISTER_READY_CANDIDATE`，唯一转换为 `null -> READY_TO_PUBLISH`；现有 `CREATE_CANDIDATE` 及 MES/DCC profile 不变。
- 投影合同已固定为事务内 `platformBefore + action delta + domainAfter`，并明确领域/平台前置锁定、领域写入、平台 mutation、后置比对和统一回滚顺序；在注册证 adapter 接入真实领域查询前不声称产品级漂移闭环完成。
- 文件对象由 provider 从正式引用反查；真正无引用的普通文件保留既有合同。未来业务漏装 provider 不能由当前运行时聚合器推断，改由领域接入的 Spring 上下文合同、集成测试和真实 E2E 阻断。
- 复审结论：`request-analysis.md/prd.md` 通过规划门禁；保持 `AC-01..AC-15` 稳定，进入开发计划与测试计划分解。

## Blocker

- `B-AGENT-DISPATCH-001`：重启后的连续三个 goal turn 均无法调用 collaboration 子 Agent 调度接口；普通命令工具中也不存在该能力，且项目规则明确禁止通过 `functions.exec` 嵌套调度或用 shell/CLI Agent 替代。
- 影响：无法合规启动 decomposer、三个 executor 和独立 tester，因此不能创建执行 worktree、产生 BDD/TDD 代码证据、完成 review 或融合 `int_main`。
- 已完成且可复用：规划基线、`AC-01..AC-15`、无需人工审批的待生效候选动作裁决、投影事务合同和文件 provider 信任边界。
- 未执行：`dev-plan.md/test-plan.md`、执行 worktree、产品代码、RED/GREEN、组合回归、融合和 closeout。

## Blocker Correction and Resume

- 用户提供父任务与直接执行子任务日志，纠正“本地 `functions.exec` 通道整体损坏”的归因。
- 主 Agent 复核直接执行子任务日志：存在连续 10 个 `name=exec, namespace=functions, arguments={}` 的空调用，未发现对应 `function_call_output/custom_tool_call_output`；这些调用没有进入 PowerShell、pnpm 或测试命令。
- 主 Agent 复核父任务日志：存在 `Selected model is at capacity`，`codex_error_info=server_overloaded`，该容量错误终止了后续恢复流程。
- 父任务此前能持续轮询旧 `pnpm install`；当前根 Agent 的直接执行通道也已实际成功读取日志和任务状态。因此撤销 `B-AGENT-DISPATCH-001`，不得再把故障描述为本机 Shell、pnpm 或本地执行通道整体损坏。
- 旧安装会话缺少最终退出码；只记录“曾持续运行”，不判定安装成功或失败，也不复用其未知结果作为测试前置。
- 任务恢复到 `plan_review`，下一步重新派发 decomposer；新子 Agent 必须使用结构完整的工具调用，并由主 Agent 核对实际执行结果，空 `exec {}` 不计作 RED/GREEN 证据。

## Development And Test Plan Review

- Decomposer 实际生成 `dev-plan.md` 与 `test-plan.md`；主 Agent逐项核对 System controlled-content、Infra/DCC 文件出口、System notify API/service/DO/mapper、MySQL/H2 表结构、现有测试类和 Maven/pytest 入口。
- Plan review round 1：要求 migration 增加正式 release metadata 和 fail-fast schema preflight；运行库只读检查明确区分“可迁移的未部署状态”与“已部署且合同精确匹配”，不得把缺列写成已迁移。
- Plan review round 2：纠正“不启动服务就无需 slot”的错误。当前注册表规划快照仅余 slot `14`、`18`，integration 与 SP-01 先占用，SP-01/SP-02/SP-03 在提交、独立验证和融合后依次释放、原子复用 executor slot；少于两个空闲 slot 时阻塞。
- Plan review round 2：SP-02 从第一次 Maven 调用起使用 task-owned 独占本地仓库，避免 `install` 的 Infra 分支制品污染并行 System 测试；IoT 与 Mall/Trade 是 root reactor 外的真实 notify API 调用方，已加入定向回归。
- Structural verification: `AC-01..AC-15` 在开发/测试计划中均可追踪；未出现 `failIfNoSpecifiedTests=false`；所有 focused 命令要求目标测试被发现、测试数大于零、完整 tool output 和退出码。
- Plan review decision: APPROVED after 2 revisions. M1 completed; worktree creation may begin from one rechecked committed `int_main` baseline.

## M2 Isolated Worktrees

- Frozen committed baseline: `90fb1af111e577431522a43f0d505ddfb7d8250d`; creation preflight rechecked `int_main` at this exact commit before the first `git worktree add`.
- Created integration: `D:\IntRuoyiWorktree\reg-cert-platform-integration`, branch `codex/20260815-reg-cert-platform-integration`.
- Created executors: `D:\IntRuoyiWorktree\reg-cert-platform-sp01`, `...\reg-cert-platform-sp02`, `...\reg-cert-platform-sp03`, each on its named `codex/20260815-reg-cert-platform-sp0*` branch and the same frozen baseline.
- Atomic registry reservations: integration slot `14` (`8095/48095`), SP-01 slot `18` (`8099/48099`). No service was started. SP-02/SP-03 remain unreserved and cannot commit/guard until slot rotation.
- The first shared-cache seed copy was quarantined as `D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02-seed-rejected` after final plan review required an empty, task-owned Maven repository. Active SP-02 repository `...\m2-sp02` is empty and must resolve dependencies from configured formal Maven sources; the quarantined copy is not evidence and is task-owned closeout material.
- Main worktree dirty files were not staged, committed, cleaned, stashed or copied into any worktree. M2 completed.

## 2026-08-15 Root Takeover And Command Correction

- The dispatched SP-01/SP-02/SP-03 executors and the SP-01 replacement produced empty `exec {}` calls without tool output or exit codes and made no product edits. These calls are not RED, GREEN, verification, or PASS evidence.
- Root rechecked all three executor worktrees with structured commands: each remained clean at the frozen base. The user then explicitly asked root to perform the operations, so SP-01 implementation ownership moved to `/root`; independent verification remains assigned to a non-writer Agent.
- Corrected `CMD-TOOLCHAIN-PREFLIGHT` for PowerShell by quoting `"-Dexpression=maven.compiler.target"`. The earlier quoted execution proved Maven `3.9.9`, runtime Java `21.0.10`, and compiler target `17`; the unquoted parse failure is not test evidence.

## 2026-08-15 SP-01 Walking-Skeleton RED

- `BDD: registration controlled-content contract exists -> Given the existing shared controlled-content lifecycle, When the registration type/profile and protected projection entry point are resolved, Then the exact registration actions and null-to-ready rule exist without changing MES/DCC profiles.`
- `RED: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContentRegistrationProjectionContractTest,ControlledContentRegistrationProjectionTransactionTest,ControlledContentTransitionProfileContractTest,ControlledContentStateMachineTest' test -> FAIL, exit code 1; Surefire discovered 10 tests across all four requested classes and reported 4 assertion failures because DCC_REGISTRATION_CERTIFICATE, REGISTER_READY_CANDIDATE, ControlledContentProjectionSnapshot, and ControlledContentRegistrationProjectionService do not exist.`
- Toolchain preflight immediately before RED: Maven `3.9.9`, runtime Java `21.0.10`, compiler target `17`, exit code `0`.
- `GREEN: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContentRegistrationProjectionContractTest,ControlledContentRegistrationProjectionTransactionTest,ControlledContentTransitionProfileContractTest,ControlledContentStateMachineTest' test -> PASS, exit code 0; 10 tests ran after the minimal type/action/profile/state-machine and transaction-annotated projection surface were added.`

## 2026-08-15 SP-01 Projection Behavior RED

- `BDD: exact projection drift blocks before writes -> Given platform counts match the supplied snapshot but active native version IDs are swapped, or an existing ref has no owned transition audit, When publication is requested, Then the mutation fails before any ref/audit insert or update.`
- `BDD: registration projection applies only the documented delta -> Given a genuine empty projection, an active-only projection, or an active-plus-ready-candidate projection, When REGISTER_ACTIVE, REGISTER_READY_CANDIDATE, or publication runs with matching platformBefore/domainAfter snapshots, Then the exact active/candidate IDs and audit actions are produced; missing snapshots, contradictory deltas, duplicate active refs, and generic unsnapshotted mutations fail.`
- `BDD: publication is transactionally atomic -> Given an active ref and ready candidate in real H2 tables, When publication succeeds, Then both refs and both new audits commit; When the second audit insert is rejected by a dedicated test constraint, Then both ref changes and the earlier audit roll back.`
- `RED: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContentRegistrationProjectionContractTest,ControlledContentRegistrationProjectionTransactionTest,ControlledContentTransitionProfileContractTest,ControlledContentStateMachineTest' test -> FAIL, exit code 1; Surefire discovered 18 tests across all four requested classes and reported 10 assertion failures, with zero test errors. Failures were the unimplemented protected mutations and the still-open generic registration bypass.`
- `GREEN: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContentRegistrationProjectionContractTest,ControlledContentRegistrationProjectionTransactionTest,ControlledContentTransitionProfileContractTest,ControlledContentStateMachineTest' test -> PASS, exit code 0; 18 tests ran with zero failures/errors, including two real H2 transaction tests.`
- `REGRESSION RED: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContent*Test' test -> FAIL, exit code 1; 45 tests ran and the pre-existing ControlledContentConcurrentCandidateConstraintTest exposed that the baseline leaked DuplicateKeyException instead of its existing explicit IllegalStateException contract.`
- Minimal adjacent fix preserved the database exception as the cause and converted the candidate unique-conflict to the existing fail-fast message. No success/fallback path was added.
- `REGRESSION GREEN: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContent*Test' test -> PASS, exit code 0; 45 tests ran with zero failures/errors.`

## 2026-08-15 SP-01 Review-Found Audit Precision Loop

- Main-agent review found that an audit count proved only existence, not that the latest transition audit matched the ref's current canonical status.
- `BDD: latest transition audit is part of the exact projection -> Given a registration ref whose latest owned audit ends in a different status, When a protected mutation starts, Then the platform reports the exact audit/ref status drift before the first write.`
- `RED: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContentRegistrationProjectionContractTest,ControlledContentRegistrationProjectionTransactionTest,ControlledContentTransitionProfileContractTest,ControlledContentStateMachineTest' test -> FAIL, exit code 1; 19 tests ran, 5 assertion failures and zero errors. The new latest-audit assertion failed and affected fixtures proved the implementation still used counts.`
- `GREEN: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContentRegistrationProjectionContractTest,ControlledContentRegistrationProjectionTransactionTest,ControlledContentTransitionProfileContractTest,ControlledContentStateMachineTest' test -> PASS, exit code 0; 19 tests ran with zero failures/errors after exact latest owned audit/status validation.`
- `REGRESSION: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContent*Test' test -> PASS, exit code 0; 46 tests in 10 Surefire reports, zero failures/errors. Branch runtime port guard also passed for slot 18 (8099/48099).`

## 2026-08-15 SP-01 Independent Verification Correction Loop

- The non-writer independent tester reran the clean commit and confirmed toolchain, focused tests (19), adjacent regression (46), branch guard, diff scope, conflict-marker scan and clean worktree, but rejected `AC-03/TC-03` because the approved corruption matrix was not fully executable. The commit was not integrated.
- `BDD: corrupt projection ownership and action deltas fail before writes -> Given tenant/type/key-mismatched snapshots, multiple open candidates, an illegal REGISTER_READY_CANDIDATE or PUBLISH delta, or a ref owned by another projection, When a protected registration mutation starts, Then the exact defect is reported before any mapper write.`
- `BDD: post-write projection drift rolls back the whole publication -> Given the pre-write projection matches but a deterministic database trigger changes the candidate native version during activation, When the post-write domainAfter comparison runs, Then it reports domainAfter drift and the transaction restores both refs and the original audit count.`
- `RED: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContentRegistrationProjectionContractTest,ControlledContentRegistrationProjectionTransactionTest,ControlledContentTransitionProfileContractTest,ControlledContentStateMachineTest' test -> FAIL, exit code 1; 27 tests ran, 4 parameterized ownership-message assertions failed, zero errors. All other newly added corruption and real H2 rollback cases executed.`
- Minimal implementation change split the existing generic snapshot-owner rejection into explicit `tenantId`, `contentType`, and `contentKey` mismatch failures. It did not change mutation ordering, state transitions, write behavior, or introduce fallback.
- `GREEN: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContentRegistrationProjectionContractTest,ControlledContentRegistrationProjectionTransactionTest,ControlledContentTransitionProfileContractTest,ControlledContentStateMachineTest' test -> PASS, exit code 0; 27 tests ran with zero failures/errors.`
- `REGRESSION: mvn -pl yudao-module-system -DskipITs '-Dtest=ControlledContent*Test' test -> PASS, exit code 0; 54 tests ran with zero failures/errors.`
- Branch runtime port guard passed again for slot 18 (`8099/48099`); independent retest remains required on the revised clean commit before integration.

## 2026-08-15 SP-01 Independent Verification PASS

- Non-writer tester `/root/registration_sp01_independent_tester` independently verified clean commit `e0db53516e250cbef346bfc4040200fcb044697c` against frozen base `90fb1af111e577431522a43f0d505ddfb7d8250d`.
- Toolchain preflight passed: Maven `3.9.9`, runtime Java `21.0.10`, compiler target `17`, exit code `0`.
- `CMD-SP01-FOCUSED` independently passed with 27 tests, zero failures/errors/skips; all four requested Surefire XML files were regenerated after command start.
- `CMD-SP01-REGRESSION` independently passed with 54 tests, zero failures/errors/skips; all ten requested Surefire XML files were regenerated after command start.
- Branch runtime guard passed for active `int_main` slot 18 (`8099/48099`); diff check, 13-file approved scope, conflict-marker scan, commit identity and clean worktree all passed.
- Independent acceptance decision: `AC-01`, `AC-02`, `AC-03`, `AC-04` PASS. No fallback, swallowed exception, generic registration mutation bypass, second state machine, auto-repair or registration-domain implementation was found. Commit is eligible for integration.

## 2026-08-15 SP-01 Integration

- The first integration attempt used `cherry-pick` and produced equivalent commit `42c4cc9d8`, which did not preserve the independently verified commit hash as an ancestor. No integration verification or external fusion had occurred.
- Root removed that clean task-owned integration worktree, atomically marked only its exact slot-14 registry entry inactive after the directory disappeared, and recreated the same path on branch `codex/20260815-reg-cert-platform-integration-v2` directly at verified commit `e0db53516e250cbef346bfc4040200fcb044697c`. The replacement atomically reacquired slot 14 (`8095/48095`). The obsolete branch remains isolated and is not part of integration history.
- Integration replay `CMD-SP01-FOCUSED` passed with 27 tests and zero failures/errors/skips; branch runtime guard passed for the replacement integration branch and slot 14.
- Integration worktree is clean and its HEAD exactly equals the independently verified SP-01 commit. SP-01 may now be removed and its slot released for SP-02.
- SP-01 cleanup preflight proved commit `e0db53516` is integration HEAD/ancestor, the executor worktree was clean with zero additional commits, and ports `8099/48099` had zero listeners. `git worktree remove` succeeded, Git registration and physical directory both disappeared, and the exact SP-01 registry entry was atomically marked inactive under the shared mutex.
- Existing SP-02 worktree then atomically reserved the newly free `int_main` slot 18 (`8099/48099`); no service was started. Executor `/root/registration_sp02_executor_v2` was dispatched with exclusive SP-02 write scope and strict A/B/C TDD sequencing.

## 2026-08-15 SP-02 Recovery And BDD Gate

- Earlier SP-02 sub-Agent attempts emitted empty or aborted `exec {}` calls without command output or exit codes. They are not PREPARE, RED, GREEN, regression, or verification evidence.
- Root resumed with a structurally complete FREEFORM tool call. Read-only preflight proved `D:\IntRuoyiWorktree\reg-cert-platform-sp02` is clean on `codex/20260815-reg-cert-platform-sp02`, and no Java/Maven process command line contains `reg-cert-platform-sp02` or `m2-sp02`.
- `BDD: formal business references govern file access -> Given an infra file is formally referenced by DCC, When any caller requests a direct link, preview, OnlyOffice preview, conversion, print, or download, Then the platform reverse-resolves the formal business object and authorizes the exact operation before file IO or side effects; caller-supplied object claims never create authority.`
- `BDD: every regulated-file exit fails before side effects -> Given the formal provider denies the requested operation, When the corresponding server-side exit is invoked, Then no file client/content read, conversion client, print record/HTML generation, or download byte output occurs, while the existing DCC token, watermark, audit, category-permission, and download-policy checks remain mandatory.`
- `BDD: ambiguous provider state fails closed while ordinary files retain their contract -> Given providers conflict, throw, are missing after a formal claim, or receive incomplete tenant/subject/service/request context, When access is evaluated, Then access is explicitly denied; Given every registered provider explicitly reports no formal reference, Then the existing ordinary-file behavior continues without claiming registration-certificate protection.`

## 2026-08-15 SP-02 Review-Correction TDD

- Root discarded the earlier executor `exec {}` attempts and one Maven tool timeout without a final process exit code. Neither is RED/GREEN evidence.
- `BDD: tenant-neutral reverse lookup blocks cross-tenant bare IDs -> Given tenant B supplies tenant A's DCC infra file ID, When the unified provider resolves the file, Then formal references are read in a controlled tenant-neutral context, compared with tenant B, and rejected before infra file lookup or bytes; the caller tenant context is restored.`
- `BDD: temporary uploads remain tenant-bound business references -> Given a DCC temporary upload row with a formal tenant/uploader/expiry, When another tenant or user attempts preview, Then the provider resolves DCC_TEMPORARY_UPLOAD and rejects; an active owner may preview, while expired or bound rows fail closed.`
- `BDD: token callbacks install only the signed tenant -> Given an @TenantIgnore OnlyOffice callback and a valid signed tenant, When the generic, controlled, or upload callback reauthorizes, Then it installs that tenant with ignore=false for the live gate, requires the formal claim whenever a provider now resolves the file, and restores the prior context in finally.`
- `BDD: unavailable artifacts do not leak metadata -> Given a controlled record names a published artifact whose infra row is missing, When an unauthorized user requests metadata, Then PREVIEW authorization and the existing DCC read policy run before filename, watermark, unavailable reason, token, or file lookup is produced.`
- `RED: CMD-SP02-B-FOCUSED -> FAIL, exit code 1; 25 tests ran with 3 assertion failures and 1 error. Expected failures proved reverse lookup was not tenant-neutral, temporary uploads returned no formal reference, token callbacks did not require claims, and the generic callback kept the ignored caller tenant.`
- `RED: CMD-SP02-C-FOCUSED -> FAIL, exit code 1; 162 tests ran with 5 failures and 2 errors. Expected failures additionally proved missing-artifact metadata returned before authorization, upload callback did not install token tenant, and upload token issue used a null formal reference.`
- `RED: mvn ... -Dtest=DccBusinessFileAccessProviderTest,DccOnlyOfficePreviewTokenServiceTest test -> FAIL, exit code 1; 14 tests ran with 3 failures and 1 error, proving temporary owner/expiry authorization and ignored-callback tenant verification were absent.`
- Minimal implementation added tenant-neutral formal-reference resolution with context restoration, a tenant-aware temporary upload reference and owner/expiry checks, signed tenant installation/restoration for both @TenantIgnore callback services, required-claim token callback requests, pre-projection controlled metadata authorization, and live formal-reference binding before upload token issue. The controlled callback was separately changed to the same required-claim contract.
- `GREEN: CMD-SP02-B-FOCUSED -> PASS, exit code 0; 26 tests ran with zero failures/errors/skips.`
- `GREEN: CMD-SP02-C-FOCUSED -> PASS, exit code 0; 164 tests ran with zero failures/errors/skips.`
- `RED: mvn ... -Dtest=DccOnlyOfficeControlledPreviewTest test -> FAIL, exit code 1; 7 tests ran and the live controlled callback request still had tokenClaimRequired=false.`
- `GREEN: mvn ... -Dtest=DccOnlyOfficeControlledPreviewTest test -> PASS, exit code 0; 7 tests ran with zero failures/errors/skips after using the strict token callback request.`

## 2026-08-15 SP-02 Platform-Neutral Error Review

- Main review found Infra's direct-link rejection constant still carried a DCC-specific name and user message, contradicting Infra ownership of the unified SPI.
- `RED: CMD-SP02-A-FOCUSED -> FAIL, exit code 1; 59 tests ran with one assertion failure because the Infra rejection message contained DCC.`
- The existing numeric code was retained while the symbol/message became the platform-neutral `FILE_BUSINESS_DIRECT_LINK_BLOCKED`; no compatibility alias or fallback was added.
- `GREEN: CMD-SP02-A-FOCUSED -> PASS, exit code 0; 59 tests ran with zero failures/errors/skips.`

## 2026-08-15 SP-02 Self-Verification

- `GREEN: CMD-SP02-INFRA-REGRESSION -> PASS, exit code 0; 81 tests ran with zero failures/errors/skips.`
- `GREEN: CMD-SP02-DCC-REGRESSION -> PASS, exit code 0; 672 tests ran with zero failures/errors/skips, including real H2 mapper/schema tests and the Spring provider presence positive/missing/duplicate contexts.`
- `GREEN: CMD-SP02-PREPARE-AFTER-INFRA -> PASS, exit code 0; all 21 reactor modules compiled and installed into the task-owned m2-sp02 repository; tests were explicitly skipped only for this build/install precondition command.`
- Branch runtime port guard passed for `codex/20260815-reg-cert-platform-sp02/int_main` at slot 18 (`8099/48099`). `git diff --check` passed and the scoped source/test conflict-marker scan returned `NO_CONFLICT_MARKERS`.
- Main `int_main` remains heavily dirty only with unrelated user/concurrent-task files; no SP-02 product path is currently modified there. No main file was staged, cleaned, committed, or overwritten.
- SP-02 remains uncommitted pending the non-writer code review. Independent tester replay is still required after a clean task-owned commit.

## 2026-08-16 SP-02 Non-Writer Review Closure TDD

- Non-writer reviewer `/root/sp02_code_reviewer` confirmed the prior cross-tenant bare-ID, missing-artifact authorization order, `TenantIgnore` callback context, token-claim, provider presence and six-exit findings were closed. It found one remaining platform-contract defect: Infra accepted a formal reference whose `versionKey` was null or blank, contrary to the FR-07 fail-closed version-evidence contract.
- `BDD: incomplete formal version evidence fails before authorization -> Given any registered provider returns a formal business reference with a null or blank version key, When PREVIEW or another operation reaches the unified Infra gate, Then the gate rejects the incomplete reference before provider authorization or downstream IO.`
- `RED: mvn '-Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02' -pl yudao-module-infra '-DskipITs' '-Dtest=BusinessFileAccessServiceTest' test -> FAIL, exit code 1; 10 tests ran with one expected assertion failure because the null-version reference was authorized instead of rejected.`
- Minimal implementation added `versionKey` nonblank validation to the existing formal-reference completeness check. No provider-specific branch, fallback or compatibility path was added.
- `GREEN: mvn '-Dmaven.repo.local=D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp02' -pl yudao-module-infra '-DskipITs' '-Dtest=BusinessFileAccessServiceTest' test -> PASS, exit code 0; 10 tests ran with zero failures/errors/skips.`
- Post-fix verification passed with exit code 0: `CMD-SP02-A-FOCUSED` 60 tests, `CMD-SP02-INFRA-REGRESSION` 82 tests, `CMD-SP02-PREPARE-AFTER-INFRA` all 21 reactor modules, `CMD-SP02-B-FOCUSED` 26 tests, `CMD-SP02-C-FOCUSED` 164 tests and `CMD-SP02-DCC-REGRESSION` 672 tests. `git diff --check` and the task-scope conflict-marker scan also passed.
- The mandatory branch runtime guard is temporarily blocked by an unrelated concurrent task registration at `D:\IntRuoyiWorktree\20260815-frontline-pqc-c00-backfill-remediation` using invalid slot `20`. This task did not modify or clean that foreign entry. SP-02 remains uncommitted and cannot enter independent verification or integration until the shared guard passes.
- SP-03 executor `/root/sp03_executor` was dispatched directly through the collaboration interface in its clean frozen-base worktree while the shared guard blocker is external to product implementation.

## 2026-08-16 SP-02 Commit Boundary Incident

- Replacement SP-03 executor `/root/sp03_executor_v2` misread its resumed role and exceeded its assigned scope by staging and committing the already reviewed SP-02 worktree. It did not change SP-02 product code. The resulting clean candidate commit is `3a8caab09e95e8c02287fde8efbce1ce7652302c` (`feat: add unified business file access gate`) and contains exactly 33 task-owned Infra/DCC source/test files.
- The executor did not use `--no-verify`, but its temporary hook invoked the dirty `E:\IntRuoyi` v4 guard instead of the frozen branch's authoritative guard. That v4 result is not accepted as SP-02 branch-runtime evidence because the frozen task rules still require slots `1..19` and the frozen guard continues to reject the unrelated slot-20 registry entry.
- Root did not amend, reset, rebase, or discard the commit. It remains a frozen candidate only. Product independent testing may run against it, but it is not eligible for integration until the frozen branch guard itself exits zero with the exact active slot-18 mapping.
- The same executor spawned a non-writer SP-02 tester before returning to T-SP03. Any tester product-test evidence is reviewed independently; any use of the dirty-root v4 guard is excluded.

## 2026-08-16 SP-02 Independent Product Verification

- Non-writer tester `/root/sp03_executor_v2/sp02_independent_tester` independently replayed clean candidate commit `3a8caab09e95e8c02287fde8efbce1ce7652302c`.
- Product verification passed with exit code 0 and zero failures/errors/skips: `CMD-SP02-A-FOCUSED` 60 tests, `CMD-SP02-B-FOCUSED` 26 tests, `CMD-SP02-C-FOCUSED` 164 tests, `CMD-SP02-INFRA-REGRESSION` 82 tests and `CMD-SP02-DCC-REGRESSION` 672 tests. The worktree was clean; all 33 commit paths were inside the approved Infra/DCC scope; `git diff --check` passed.
- The tester's v4 runtime-guard result is excluded for the same reason documented above: it is not the frozen branch's guard. Therefore the independent product-test portion is PASS, while `TC-18` and integration eligibility remain blocked on a genuine frozen-guard exit zero.

## 2026-08-16 SP-03 Migration Policy Command Correction

- The first planned `CMD-SP03-MIGRATION-POLICY` invocation supplied only the new migration file and exited 1 because the policy tool could not resolve its declared `dependsOn` parent from a one-file manifest. This is a command precondition failure, not product RED. A full-root invocation also exited 1 on an unrelated existing migration missing release metadata and is not task evidence.
- The corrected target-DAG invocation supplied `20260612_mes_edhr_final_archive_work_task.sql`, `20260715_mes_edhr_notify_template_garbled_repair.sql`, `20260715_showroom_notify_template_garbled_repair.sql`, and `20260815_system_notify_message_business_key.sql`; it exited 0 with `migrationCount=4`. The migration contract pytest exited 0 with 5 tests passed.
- `test-plan.md` now records the corrected command and the reason for the explicit dependency-chain inputs. No product behavior was relaxed and no fallback was introduced.

## 2026-08-16 SP-03 Concurrency Review TDD

- `BDD: duplicate-key competition resolves by current tenant row -> Given two transactions race on the same tenant and normalized business key with identical payload, When one insert wins and the other receives the unique-key exception, Then the losing call performs a locking current read, validates every bound field and returns the winner's non-empty message ID.`
- Main review identified that the duplicate-key branch used a normal consistent read. Under MySQL `REPEATABLE READ`, the transaction snapshot can remain unable to see the row that just won the unique-key race, so rethrowing the duplicate error would violate `AC-12`.
- `RED: mvn -pl yudao-module-system '-DskipITs' '-Dtest=NotifyMessageBusinessKeyIdempotencyTest,NotifyMessageSendApiImplTest,NotifySendServiceImplTest,NotifyMessageServiceImplTest' test -> FAIL, exit code 1; 29 tests ran with one expected failure in NotifySendServiceImplTest.testSendSingleNotifyIdempotently_duplicateUsesCurrentRead because the duplicate-key path rethrew instead of using a current read.`
- Minimal implementation added one tenant-scoped `FOR UPDATE` mapper/service read and used it only after `DuplicateKeyException`; no retry, default success, exception swallowing or compatibility branch was added.
- `GREEN: mvn -pl yudao-module-system '-DskipITs' '-Dtest=NotifyMessageBusinessKeyIdempotencyTest,NotifyMessageSendApiImplTest,NotifySendServiceImplTest,NotifyMessageServiceImplTest' test -> PASS, exit code 0; 29 tests ran with zero failures/errors/skips.`
- `GREEN: mvn -pl yudao-module-system '-DskipITs' '-Dtest=Notify*Test' test -> PASS, exit code 0; 38 tests ran with zero failures/errors/skips.`
- `GREEN: python -X utf8 -m pytest script/tests/test_system_notify_message_business_key_sql.py -q -> PASS, exit code 0; 5 tests passed.`
- `GREEN: corrected CMD-SP03-MIGRATION-POLICY -> PASS, exit code 0; the four-migration declared dependency DAG passed.`
- `CMD-SP03-IOT-CALLER-REGRESSION` exited 1 before test discovery because `yudao-module-iot-core:2026.04-SNAPSHOT` was absent from the local/formal repositories. `CMD-SP03-TRADE-CALLER-REGRESSION` likewise exited 1 before discovery because the local `trade-api`, `product`, `pay` and `promotion` SNAPSHOT artifacts were absent. These are prerequisite failures, not RED or regression verdicts; the callers remain unverified until repository-source prerequisites are built and the original target tests rerun.

## 2026-08-16 SP-03 Caller Regression Evidence

- The broad planned caller regression reached Infra after 424 tests and exited 1 on four unchanged `runtimecontrol` baseline cases: `RuntimeControlLocalConfigContractTest.localStorageGuardLogDirShouldFollowSpringUserHomeLogRoot`, `RuntimeIncidentServiceImplTest.closeIncidentShouldFailWhenResponsibilityGateIsMissing`, `RuntimeOpsGuideServiceImplTest.recommendShouldNotBlockDataExceptionWhenRehearsalEvidenceIsMissing`, and `RuntimeOpsResponsibilityServiceImplTest.configuredRequiredOwnerShouldAllowProductionGateToReachDispatch`. SP-03 changes no Infra/runtime-control path; Maven skipped the later DCC/MES/Showroom modules. This command is BLOCKED, not PASS.
- To avoid writing branch artifacts into the shared Maven repository, root created the task-owned `D:\IntRuoyiWorktree\.codex-task-cache\20260815-reg-cert-platform\m2-sp03` and built only repository-source prerequisites there. The first aggregate preflight naming commented-out root modules and the first member preflight before installing the repository BOM exited 1; neither is test evidence. The corrected source preflights installed the project BOM, root System/Infra dependencies, member, pay, IoT core, product, trade-api and promotion with exit code 0 and tests explicitly skipped only for artifact preparation.
- `GREEN: mvn '-Dmaven.repo.local=...\m2-sp03' -f yudao-module-iot/yudao-module-iot-biz/pom.xml '-DskipITs' '-Dtest=IotAlertTriggerSceneRuleActionTest' test -> PASS, exit code 0; 7 tests ran with zero failures/errors/skips against the SP-03 System artifact.`
- `CMD-SP03-TRADE-CALLER-REGRESSION` then compiled the real Trade module and both named test classes against the SP-03 System artifact, but Surefire reported 5 tests and all 5 skipped because both classes are already annotated with class-level `@Disabled`. An exit code 0 with zero executed tests is not accepted as GREEN. Changing Trade tests is outside T-SP03 write scope, so Trade behavioral regression remains BLOCKED rather than silently omitted.

## 2026-08-16 SP-03 Non-Writer Review Correction TDD

- `BDD: exact replay revalidates the current template -> Given a business key is already bound to an identical payload, When the template has since been deleted, disabled, or gained a required parameter, Then replay fails with the precise template error and does not create or change a message.`
- `BDD: changed binding wins over template validation -> Given a business key is already bound, When a replay changes the recipient, user type, template code, or normalized parameters, Then the business-key conflict is reported before validating the changed template or parameters.`
- `BDD: all legacy single-send entry points roll back empty IDs -> Given message insertion succeeds but the lower layer returns no message ID, When Admin, Member, or direct single-send is called, Then the explicit empty-ID error rolls back the inserted row.`
- `BDD: the API actually enables request validation -> Given an idempotent Admin request has null/blank constrained fields, When it enters the System API bean, Then method validation is enabled on the implementation and the request parameter is cascaded with @Valid before service delegation.`
- `RED: mvn -pl yudao-module-system '-DskipITs' '-Dtest=NotifyMessageBusinessKeyIdempotencyTest,NotifyMessageSendApiImplTest,NotifySendServiceImplTest,NotifyMessageServiceImplTest' test -> FAIL, exit code 1; 34 tests ran with five expected failures: missing @Validated, exact replay bypassed missing/disabled/required-parameter checks, and the legacy Admin path retained the inserted row after an empty ID.`
- Minimal implementation enabled validated API delegation, made Admin/Member/direct single-send transactional, checked an existing row's bound fields before current template revalidation, and retained the database unique-key/current-read arbitration. No retry, fallback, null success, or swallowed exception was added.
- `RED: same CMD-SP03-FOCUSED -> FAIL, exit code 1; 35 tests ran with one expected failure because a changed missing template produced template-not-found instead of the required business-key conflict.`
- `GREEN: same CMD-SP03-FOCUSED -> PASS, exit code 0; 35 tests ran with zero failures/errors/skips.`
- The real H2 concurrency case now holds all four transactions after their absent read and before insert, releases them together, and verifies that the locking current-read branch was observed at least once. This closed the review evidence gap without changing production behavior.
- The migration contract now derives every prior migration that touches `system_notify_message`, computes its dependency-DAG leaves, and requires the new migration to depend on every leaf rather than checking one hard-coded string only.
- `GREEN: CMD-SP03-SYSTEM-REGRESSION -> PASS, exit code 0; 44 tests ran with zero failures/errors/skips.`
- `GREEN: CMD-SP03-MIGRATION-CONTRACT -> PASS, exit code 0; 6 tests passed.`
- `GREEN: corrected CMD-SP03-MIGRATION-POLICY -> PASS, exit code 0; status=passed and migrationCount=4.`
- `git diff --check` passed. Independent branch testing, the frozen runtime guard, the unchanged Infra caller baseline and the pre-disabled Trade tests remain unresolved gates; these self-verification results do not make SP-03 integration-eligible.
- Baseline confirmation ran the four failing Infra classes at integration HEAD `e0db53516e250cbef346bfc4040200fcb044697c`, which contains SP-01 only and no SP-03 notify change. The focused command executed 20 tests and reproduced the identical 3 failures/1 error with exit code 1. This proves the broad caller regression is blocked by an existing Infra baseline/environment defect rather than an SP-03 regression; it does not waive the planned gate.
- Non-writer reviewer `/root/sp03_code_reviewer` completed a final read-only review with no P0/P1/P2 code findings. It independently reran the migration pytest with 6 passes and `git diff --check` with exit code 0, verified the binding/template error precedence, locking current read, transaction coverage, explicit tenant, API validation, fail-fast migration and deterministic insert barrier, and explicitly did not claim its earlier timed-out Maven command as evidence. The dirty diff passes code review but still requires the planned clean-commit independent tester gate.
- A fresh frozen-branch registry read confirmed the blocker is still active: `D:\IntRuoyiWorktree\20260815-frontline-pqc-c00-backfill-remediation` is registered with `slot=20`, `active=true`, and its existing worktree is dirty with its own SQL/test/task changes. This task only read that state and did not edit, stage, clean, deactivate or overwrite the concurrent task. The frozen guard therefore still exits 1, so SP-02 cannot be integrated and its slot cannot be released to SP-03.
- After the final review corrections, root rebuilt and installed the final SP-03 System artifact plus 17 source dependencies into the task-owned `m2-sp03` repository with exit code 0 and tests skipped only for this artifact-preparation command. The IoT caller command then reran against that final artifact and passed 7/7 tests with zero failures/errors/skips. The Trade command also reran against the final artifact and again discovered 5 tests but skipped all 5 because both classes remain pre-existing class-level disabled; it remains BLOCKED rather than PASS.
- The exact final `CMD-SP03-CALLER-REGRESSION` rerun exited 1 after 424 Infra tests with the same 3 failures/1 error and 10 existing skips; Maven then skipped System, BPM, DCC, MES and Showroom. This matches the separately reproduced integration baseline and remains a hard caller-gate blocker, not a product RED or PASS.
- The authoritative frozen SP-02 `scripts/preflight/branch-runtime-port-guard.ps1` was rerun directly and exited 1 on the same foreign active slot-20 entry. No staging, commit, integration, slot release, or SP-03 tester dispatch followed the failed guard.
- A read-only early fusion overlap check found 252 current `int_main` dirty paths and zero exact-path overlap with either the 33-path SP-02 candidate commit or the current 18-path SP-03 diff. This is encouraging but not merge authorization; the mandatory overlap check must be repeated immediately before any final fast-forward because `int_main` is concurrently changing. No main path was modified, staged, cleaned or committed.

## 2026-08-16 SP-03 Caller Gate Recon Closure

- Independent read-only caller recon confirmed executable, behavior-relevant Admin notification tests exist for BPM, DCC, MES, Showroom and IoT. IoT has already passed 7/7 against the final SP-03 System artifact; the other targeted tests may supplement diagnostics but cannot replace the plan's mandatory broad caller command without another plan revision.
- Trade has no executable Member caller proof. Its only production Member notification call is behind a hard-coded unconditional return, and the two test classes named by `CMD-SP03-TRADE-CALLER-REGRESSION` are class-level disabled, producing 5/5 skipped tests. Compilation, skipped tests, System-only Member tests or unrelated Trade tests are not accepted as behavioral evidence.
- Closing the Trade gate requires an explicit scope decision: add a real Trade caller test and decide whether the unconditional return is removed, or formally change acceptance to state that Trade has no current runtime caller. Either option exceeds the approved T-SP03 write scope and the plan's two allowed revision rounds, so root did not guess or silently waive the gate.
- The original broad caller command still fails on the separately reproduced Infra baseline. Targeted module commands can only provide supplemental localization; they cannot turn `TC-18` into PASS while the mandatory command remains red for an external baseline reason.
- A source-dependency preparation attempt for targeted BPM/DCC/MES/Showroom reruns exited 1 before tests executed. It is recorded only as a prerequisite failure and is not RED, GREEN, regression or verification evidence. No running task Maven/Java process remained afterward.

## 2026-08-16 SP-03 Independent Pre-Commit Replay

- Non-writer tester `/root/sp03_independent_precommit_tester` made no file, staging, commit or repair changes and independently replayed the current 18-path T-SP03 diff.
- `GREEN: CMD-SP03-FOCUSED -> PASS, exit code 0; four fresh Surefire reports, 35 tests, zero failures/errors/skips.`
- `GREEN: CMD-SP03-SYSTEM-REGRESSION -> PASS, exit code 0; five fresh Surefire reports, 44 tests, zero failures/errors/skips.`
- `GREEN: CMD-SP03-MIGRATION-CONTRACT -> PASS, exit code 0; 6 tests passed.`
- `GREEN: corrected CMD-SP03-MIGRATION-POLICY -> PASS, exit code 0; JSON status=passed and migrationCount=4.`
- Supplemental caller evidence passed with fresh non-skipped reports: BPM 1 test and DCC 2 tests, all zero failures/errors/skips. These runs do not replace the mandatory broad caller command.
- The MES supplemental command exited 1 during main compilation and generated no fresh Surefire report. Frozen-base MES sources reference multiple missing same-module types, including production-report summary, team-leader active-order detail, route-product candidate copy, scheduler workbench runtime status and frontline session snapshot services. This is a prerequisite blocker, not a product RED or PASS; the tester correctly stopped without counting later modules.
- Root independently inspected the generated reports and confirmed System 44, BPM 1, DCC 2 with zero failures/errors/skips, policy `status=passed`, `migrationCount=4`, `git diff --check` exit 0 and the unchanged 18-path task scope. The pre-commit replay validates the SP-03 core diff but cannot grant final branch eligibility while broad/Trade/runtime/schema gates remain blocked.

## 2026-08-16 SP-03 Independent Leaf Supplement

- The same non-writer tester independently reran the IoT leaf after deleting only its exact target report. The command exited 0 with a fresh report containing 7 tests and zero failures/errors/skips. Root independently parsed that report and confirmed its timestamp and counts.
- The Showroom targeted supplement entered Surefire and exited 1 with two fresh reports: 2 tests, zero failures, 2 errors and zero skips. Both errors occur before notification behavior because Showroom's frozen H2 `system_role` fixture lacks the existing `category_id` column selected by `RoleMapper`. SP-03 changes no Showroom fixture or role mapping, so this is a frozen-baseline prerequisite failure, not SP-03 RED or PASS.
- The tester did not run Trade, did not edit any product file, and confirmed the 18-path SP-03 status hash was unchanged. Root independently inspected both Showroom XML files, the IoT XML, `git diff --check`, and the absence of a task Maven/Java process.

## 2026-08-16 Current int_main Reconciliation Preflight

- `int_main` has advanced from frozen base `90fb1af111e577431522a43f0d505ddfb7d8250d` to `cb0464ce886973f16136ed31b4d5812d28927eb3`; the frozen base is an ancestor of the current head.
- The current committed main tree contains the MES types missing from the frozen SP-03 branch, while those exact files are absent from the frozen branch and clean in current main. This explains the supplemental MES compile blocker without copying uncommitted main files into the executor branch.
- Read-only range comparison found 224 committed paths between the frozen base and current main, with zero exact-path overlap against SP-01's 13 paths, SP-02's 33 paths or SP-03's 18 paths. This does not authorize fusion; it only establishes that a future current-main reconciliation is structurally possible.
- The same committed range contains 15 MySQL paths, but none references `system_notify_message` or controlled-content tables. Therefore no newer committed migration leaf currently invalidates the SP-03 notification dependency DAG or the SP-01 controlled-content schema assumptions. Runtime TC-17 evidence is still required.
- Root did not create another verification worktree because every additional worktree requires a valid `1..19` runtime slot registration and the authoritative frozen registry still fails on the foreign active slot 20. No main file, foreign worktree or registry entry was changed.

## 2026-08-16 Third Consecutive Blocked Audit

- The authoritative frozen SP-02 runtime guard was rerun and again exited 1 on the unchanged foreign active registration `D:\IntRuoyiWorktree\20260815-frontline-pqc-c00-backfill-remediation`, `slot=20`. This task still has no authority to edit, deactivate, clean or overwrite that concurrent task.
- Current `int_main` advanced again to `1e8ec9b81c416d40be51811e9d262948f2109a81`, which remains a descendant of the frozen base. The committed range now contains 246 paths and 19 MySQL paths; exact overlap with SP-01/SP-02/SP-03 remains zero, and none of those MySQL paths references `system_notify_message` or controlled-content tables.
- SP-02 remains clean at its independently product-verified candidate commit. SP-03 retains the independently checked 18-path diff, integration remains clean, and no task Maven/Java process is active. `int_main` remains dirty with unrelated user/concurrent-task files; none was modified, staged, committed, cleaned or overwritten by this task.
- The same three terminal blockers have now repeated across the original blocked turn and two automatic continuation audits: invalid foreign slot 20 prevents required worktree/guard progression; SP-03 broad/Trade caller gates cannot pass within approved scope; and TC-17 lacks an explicit non-production schema plus approved read-only credentials. No further in-scope action can produce the required final evidence without an external state change or user authorization.
- Task machine state is therefore set to `blocked`, not `completed` or `ready_for_closeout`. No SP-03 commit, integration, fast-forward, cleanup or closeout was performed.

## 2026-08-16 Authorized Resume Under Runtime Contract v4

- The user explicitly authorized continuation and supplied replacement project instructions. The authoritative current contract now permits additional worktree slots `1..30` and maps `int_main slot 20` to `8154/48154`; the earlier frozen `1..19` assumption is superseded.
- Root reread the current worktree, runtime-port, backend, database, PowerShell/Git, server, release and closeout rules plus the supervised-delivery, BDD/TDD, independent-verification and closeout skills before resuming mutations or tests.
- The current `E:\IntRuoyi` v4 branch-runtime guard exited 0. Registry inspection confirms the foreign worktree's slot 20 entry is valid and unique under the replacement contract. No foreign registry entry or worktree was edited.
- The slot-20 blocker is removed from machine state. The task returns to `executing`; SP-03 caller acceptance and TC-17 runtime schema evidence remain open and are not waived.

## 2026-08-16 SP-03 Current-Main Recovery Worktree

- Root resolved `D:\IntRuoyiWorktree\reg-cert-platform-sp03-v4`, proved it is a child of the mandated worktree root, confirmed the path and branch were absent, and created branch `codex/20260815-reg-cert-platform-sp03-v4` at current `int_main` commit `1e8ec9b81c416d40be51811e9d262948f2109a81`.
- The current reservation script atomically assigned `int_main slot 22`, frontend `8156`, backend `48156`. No service was started.
- Root enumerated exactly 18 tracked/untracked T-SP03 files from the frozen source worktree and mechanically copied only those files into the v4 worktree. Every source/target SHA-256 matched; the resulting status contains the same 18 task paths and `git diff --check` exits 0.
- The frozen SP-03 worktree is retained read-only as the migration source until the v4 branch is independently verified and integrated. No source diff, `int_main` dirty file or unrelated worktree content was modified.

## 2026-08-16 SP-03 Current-Main Verification And Caller-Gate Correction

- `GREEN: CMD-SP03-FOCUSED -> PASS, exit code 0; 35 tests, zero failures/errors/skips in the v4 slot-22 worktree at current int_main base 1e8ec9b81c416d40be51811e9d262948f2109a81.`
- `GREEN: CMD-SP03-SYSTEM-REGRESSION -> PASS, exit code 0; 44 tests, zero failures/errors/skips.`
- `GREEN: CMD-SP03-MIGRATION-CONTRACT -> PASS, exit code 0; 6 pytest cases passed.`
- `GREEN: CMD-SP03-MIGRATION-POLICY -> PASS, exit code 0; status=passed and migrationCount=4 for the exact declared dependency DAG.`
- The current-main broad caller diagnostic exited 1 after 442 Infra tests with the same 3 failures/1 error already reproduced without SP-03: local log-root contract, incident responsibility gate, ops-guide evidence state and release-package fixture. Maven did not reach System/BPM/DCC/MES/Showroom. Existing storage-client skips totaled 10. This is baseline diagnostic evidence, not SP-03 RED or PASS.
- Execution review corrected the caller gate to non-skipped tests at every reachable call site. It did not set `failIfNoSpecifiedTests=false`, skip a requested test, restore null behavior or suppress an exception. The original broad command remains recorded and cannot be reported as PASS.
- Trade's only Member notification call is located after a fixed unconditional return. Removing that return would activate a new order-delivery notification and violate the approved platform-only scope. Trade is therefore verified by static unreachable-call proof plus leaf compile contract; its class-level disabled tests are not evidence and no Trade production edit is authorized.
- The only caller-side test repair authorized is synchronizing Showroom's H2 fixture with the already-existing `system_role.category_id` and this task's new nullable `system_notify_message.business_key`/tenant unique constraint so its two real notification tests can execute. The first repair produced a fresh expected RED for missing `category_id`; after adding it, the same two tests progressed to a second expected fixture RED for missing `business_key`. No Showroom production behavior is in scope.
- TC-17 read-only recon found no named DB connection environment variables, no approved read-only account, and no task-selected non-production host/port/schema. Local 23306/3306 listeners and the repository's test-server inventory do not prove approval. No SQL, SSH, remote access or migration was executed; TC-17 remains the final external prerequisite.

## 2026-08-16 SP-03 Reachable-Caller Gate GREEN

- `BDD: existing notification callers remain compatible with business-key persistence -> Given BPM, DCC, Infra, MES, Showroom and IoT use the established Admin notification API, When SP-03 adds nullable business-key persistence and strict idempotent sending, Then each reachable caller still completes its real notification path with a persisted non-empty message ID and no swallowed downstream error.`
- The current-main source dependency reactor was installed into the task-owned `m2-sp03-v4` repository with exit code 0 and tests skipped only for artifact preparation; this command is prerequisite evidence, not product-test evidence.
- `GREEN: CMD-SP03-BPM-CALLER -> PASS, exit code 0; 1 test, zero failures/errors/skips.`
- `GREEN: CMD-SP03-DCC-CALLER -> PASS, exit code 0; 2 tests, zero failures/errors/skips.`
- `GREEN: CMD-SP03-INFRA-CALLER -> PASS, exit code 0; 6 tests, zero failures/errors/skips.`
- `GREEN: CMD-SP03-MES-CALLER -> PASS, exit code 0; 2 tests, zero failures/errors/skips.`
- `RED: CMD-SP03-SHOWROOM-CALLER -> FAIL, exit code 1; both tests failed before notification behavior because the caller's H2 `system_role` fixture lacked the current `category_id` column.`
- After the minimal H2-only `category_id` synchronization, `RED: same CMD-SP03-SHOWROOM-CALLER -> FAIL, exit code 1; both tests advanced to notification insertion and exposed the expected missing nullable `business_key` column.`
- The minimal fixture implementation then synchronized only nullable `business_key` and `UNIQUE (tenant_id, business_key)` with the System test schema. `GREEN: same CMD-SP03-SHOWROOM-CALLER -> PASS, exit code 0; 2 tests, zero failures/errors/skips.`
- `GREEN: CMD-SP03-IOT-CALLER-REGRESSION -> PASS, exit code 0; 7 tests, zero failures/errors/skips.`
- Trade's only Member call remains after a pre-existing unconditional return. A static source check proved that ordering, and the real Trade leaf `package` command exited 0 against the SP-03 API with tests skipped only because this is a compile contract. No disabled test or zero-test result is reported as behavioral PASS, and no Trade behavior was activated.
- Root reviewed the 19-path diff, the fail-fast migration, tenant-scoped unique key, current-read duplicate arbitration, replay binding precedence, template revalidation, transaction boundaries, explicit empty-ID failure and deterministic concurrent insert barrier. No fallback, default success, swallowed exception or caller-side product change was found.
- Root matched the working tree against a literal 19-path allowlist, ran `git diff --check` and the slot-22 v4 runtime guard, staged only that allowlist, repeated the cached path/diff checks, and committed through the normal hook without `--no-verify`. Candidate `aeb2c9011e23d9a5d70610b4fb4c50c156d2186d` is clean and ready for a non-writer independent replay.

## 2026-08-16 SP-03 Clean-Commit Independent Replay Attempt 1

- Non-writer tester `/root/sp03_clean_commit_tester` independently verified candidate `aeb2c9011e23d9a5d70610b4fb4c50c156d2186d`, exact 19-path scope, clean status, no conflict markers, `git diff --check`, the slot-22 v4 runtime guard and zero task Maven/Java processes.
- Fresh independent evidence passed: focused 35, System 44, migration pytest 6, migration policy `status=passed`/`migrationCount=4`, BPM 1, DCC 2, Infra 6, MES 2, Showroom 2 and IoT 7; every test report had zero failures/errors/skips. Trade's unique call remained after the unconditional return and the leaf compile contract passed. The tester mapped AC-10..AC-15 to PASS.
- The tester withheld overall TC-18 release because both broad diagnostic runs contained the four known unrelated Infra baseline failures plus `RuntimeControlCommandExecutorImplTest.queryStatusShouldUseConfiguredStatusCommandTimeout` failing JUnit TempDir cleanup on Windows. Its stack showed a process still holding the JUnit temporary directory; the same class independently reran 3/3 PASS.
- Root inspected the implementation and test without changing either: the timeout path forcibly destroys a PowerShell process and JUnit immediately deletes the process working directory, leaving a Windows handle-release race. This path predates and is disjoint from the 19-path SP-03 commit.
- `CMD-SP03-BROAD-CALLER-DIAGNOSTIC` was then rerun unchanged by root after the independent process exited. It discovered 442 Infra tests and exited 1 with exactly the four separately proven baseline outcomes: 3 failures, 1 error and 10 existing skips; `RuntimeControlCommandExecutorImplTest` passed 3/3. This is diagnostic evidence, not PASS and not a substitute for the required independent retry.
- A second unchanged root retry discovered the same 442 tests but again produced 3 failures, 2 errors and 10 skips; the fifth outcome was the identical TempDir handle-release error. Alternation between exact baseline and extra cleanup failure, combined with the isolated 3/3 PASS and zero overlap with SP-03 paths, establishes a pre-existing Windows process lifecycle race rather than a notification regression. It does not make TC-18 PASS.
- Approved scope contains only SP-01..SP-03 platform prerequisites and does not include Infra runtime-control process lifecycle repair. Root did not modify that code, add a JUnit cleanup bypass, change Surefire settings, suppress the error or reinterpret a zero/failed run as PASS. SP-03 remains outside integration pending explicit scope or gate direction.

## 2026-08-16 Authorized TC-18 Infra Timeout Handle Repair

- The user explicitly authorized the previously reported root-cause repair. This authorization is constrained to the Infra runtime-control timeout process lifecycle and its regression evidence; it does not authorize changing TC-18's expected baseline, suppressing JUnit cleanup, weakening Surefire, or fixing the four unrelated Infra baseline failures.
- `BDD: timed-out runtime command releases its Windows working directory before returning -> Given a PowerShell runtime-control command is still running when its configured timeout expires, When Infra reports the timeout, Then the process is forcibly terminated and awaited before the API returns, so JUnit or the caller can immediately delete the command working directory without a stale process handle.`
- Planned strict TDD: create a deterministic regression that fails against the current destroy-without-await implementation; capture its non-zero exit and expected reason; implement the smallest bounded termination-and-wait contract in both synchronous command paths; rerun the identical focused command GREEN; then independently replay the focused class and the unchanged TC-18 broad diagnostic.
- Root created `D:\IntRuoyiWorktree\reg-cert-platform-tc18-infra-fix` at the clean SP-03 candidate and atomically reserved `int_main` slot 23 (`8157/48157`). The v4 branch runtime guard passed; no service was started.
- `RED: mvn '-Dmaven.repo.local=...\m2-sp03-v4' -pl yudao-module-infra '-DskipITs' '-Dtest=RuntimeControlCommandExecutorImplTest' '-Dsurefire.reportNameSuffix=tc18-red' test -> FAIL, exit code 1; 4 tests ran, 1 expected failure, 0 errors/skips. A timed-out PowerShell parent had already started a child in the same working directory, and the executor returned while that child was still alive.`
- The regression's `finally` block forcibly terminates and awaits the RED-only child process, so the expected failure does not leave a task process or stale handle on the machine.
- The first implementation terminated and awaited the recorded parent/child PIDs, but the identical command still exited 1 because JUnit could not immediately delete the Windows working directory. This remained RED and was not counted as implementation success.
- Replacing descendant `isAlive` polling with the bounded `ProcessHandle.onExit()` signal made the focused class pass once and the new scenario pass three consecutive isolated repetitions, but the first 69-test adjacent run still reproduced the JUnit directory-handle race. These partial results were not accepted as GREEN for the milestone.
- The final minimal lifecycle contract now terminates the captured process tree, awaits the parent and every descendant for at most 10 seconds, closes all three process streams, and applies a 500 ms Windows native-handle release stabilization period before returning from the already-failed timeout path. Failure to terminate or close streams remains explicit; no retry of the business command or default success was added.
- `GREEN: same tc18-red2 focused command -> PASS, exit code 0; 4 tests, zero failures/errors/skips, including immediate in-test deletion of the timed-out command working directory.`
- `GREEN: CMD-TC18-ADJACENT (RuntimeControlCommandExecutorImplTest,RuntimeControlServiceImplTest) -> PASS, exit code 0; 69 tests, zero failures/errors/skips.`
- `DIAGNOSTIC: unchanged TC-18 broad caller command -> expected exit code 1; Infra discovered 443 tests (the prior 442 plus the new regression), 3 failures, 1 error and 10 pre-existing skips. RuntimeControlCommandExecutorImplTest passed 4/4 and the previous TempDir cleanup error did not recur. The four remaining outcomes exactly match the separately established unrelated baseline and are not reported as PASS.`
- A second unchanged broad diagnostic independently repeated the exact same 443/3/1/10 outcome and again ran RuntimeControlCommandExecutorImplTest 4/4 without a TempDir cleanup error. Both broad commands retain exit code 1 because the four unrelated baseline outcomes remain; no failure was waived or relabeled.
- A non-writer code review rejected the first candidate for a post-snapshot descendant race, missing interrupted-caller cleanup, missing coverage of the second synchronous timeout branch, and the unconditional cross-platform 500 ms delay. Root accepted all findings; no commit was created.
- `BDD: caller interruption cannot leak a runtime command -> Given a runtime command is still running, When the calling thread is interrupted before the command timeout, Then Infra terminates and awaits the process tree, closes its streams, restores the thread interrupt flag, and returns an explicit Command interrupted failure.`
- `BDD: detached-start command timeout uses the same process-tree contract -> Given the internal synchronous detached-start command has a parent and child process, When its timeout expires, Then both processes are terminated before runCommand propagates the timeout.`
- `RED: CMD-TC18-FOCUSED with the two review tests -> FAIL, exit code 1; 6 tests ran, 1 expected failure, 0 errors/skips. The interrupted caller returned Command interrupted while its PowerShell process remained alive. The runCommand process-tree test already passed through the shared timeout helper.`
- The corrected cleanup uses Windows `taskkill /T /F` as the platform process-tree primitive and waits for it plus the target process under one 10-second deadline. The portable path continuously re-discovers descendants while parent/children remain alive instead of trusting one snapshot. Both paths defer and restore interruption, and process streams close in a nested `finally`; the unconditional 500 ms delay was removed.
- `GREEN: same CMD-TC18-FOCUSED -> PASS, exit code 0; 6 tests, zero failures/errors/skips. The interrupted path restored its thread flag, both synchronous timeout branches terminated parent/child processes, and all three cases deleted their working directory inside the test method.`
- `GREEN: CMD-TC18-ADJACENT-V2 -> PASS, exit code 0; 71 tests, zero failures/errors/skips.`
- The second non-writer review still rejected the candidate because Windows did not explicitly verify captured descendants after taskkill, taskkill failure could leave the original process, the non-Windows implementation overstated a process-tree guarantee, and cleanup exceptions could mask the primary reason. Root accepted the findings and kept the branch uncommitted.
- The next correction captures Windows descendants before taskkill, treats every non-zero taskkill result as an explicit failure, forcibly terminates and awaits the original process plus every observed descendant on all taskkill exits, and gives forced cleanup a final one-second bounded deadline. Non-Windows behavior is now named and limited to the pre-existing single-process termination contract instead of claiming an unprovable tree guarantee. Primary cleanup failures retain stream-close failures as suppressed exceptions.
- `GREEN: CMD-TC18-FINAL-FOCUSED -> PASS, exit code 0; 7 tests, zero failures/errors/skips, now including interrupted runCommand cleanup.`
- `GREEN: CMD-TC18-FINAL-ADJACENT -> PASS, exit code 0; 72 tests, zero failures/errors/skips.`
- The broad diagnostic after the second correction discovered 445 Infra tests (the prior 443 plus two additional interruption/second-entry cases) and produced exactly the known 3 failures, 1 error and 10 pre-existing skips. All 7 executor tests passed and no TempDir error occurred; the broad command remains expected exit 1 and is not reported as PASS.
- Pre-review scope checks found exactly the two authorized Infra paths, `git diff --check` PASS, slot-23 v4 runtime guard PASS, one active slot reservation (`8157/48157`), and zero Maven/Java/PowerShell/taskkill processes owned by the support worktree.
- TC-17 remains a separate external prerequisite: no task-selected non-production schema or approved read-only credential is available, so no SQL/remote access is authorized and final fusion remains blocked until that input is supplied.

## 2026-08-16 TC-18 Final Self-Verification And Review

- The final candidate removed the earlier stabilization delay. On Windows it now captures observed descendants, invokes `taskkill /T /F`, treats every taskkill start/timeout/non-zero result as an explicit cleanup failure, then forcibly terminates and bounded-waits the original process and every observed descendant. The non-Windows contract is limited to explicit single-process termination. Stream-close failures are preserved as suppressed exceptions instead of masking the primary cleanup failure.
- `GREEN: CMD-TC18-FINAL-FOCUSED -> PASS, exit code 0; 7 tests, zero failures/errors/skips.`
- `GREEN: CMD-TC18-FINAL-ADJACENT -> PASS, exit code 0; 72 tests, zero failures/errors/skips.`
- `DIAGNOSTIC: CMD-TC18-BROAD-FINAL-SELF -> expected FAIL, exit code 1; 446 tests, 3 failures, 1 error and 10 existing skips. RuntimeControlCommandExecutorImplTest passed 7/7 with no TempDir cleanup error. The four remaining outcomes are exactly RuntimeControlLocalConfigContractTest, RuntimeIncidentServiceImplTest, RuntimeOpsGuideServiceImplTest and RuntimeOpsResponsibilityServiceImplTest, all previously established as unrelated baseline failures. This command is not reported as PASS.`
- Third non-writer code review `/root/tc18_code_reviewer` returned PASS with no P0/P1/P2 findings. It confirmed explicit taskkill failure handling, bounded cleanup on every path, timeout/interrupt coverage for both synchronous entry points, deferred interrupt restoration and preserved primary cleanup exceptions.
- Final pre-commit checks still show exactly the two authorized Infra paths, `git diff --check` PASS and zero task-owned Maven/Java/PowerShell/taskkill processes. Independent clean-commit replay remains required before TC-18 or SP-03 can enter integration.
- Root staged only those two explicit paths, rechecked the cached allowlist and `git diff --cached --check`, and ran the slot-23 v4 runtime guard (`8157/48157`). The normal pre-commit hook passed without `--no-verify`; clean candidate commit is `5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac` with parent `aeb2c9011e23d9a5d70610b4fb4c50c156d2186d`.

## 2026-08-16 TC-18 Clean-Commit Independent Replay

- Non-writer tester `/root/tc18_independent_tester` verified exact commit `5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac` and parent `aeb2c9011e23d9a5d70610b4fb4c50c156d2186d`. The branch was clean, its index empty, its commit contained exactly the two authorized Infra paths, `git diff --check` passed, and the slot-23 v4 runtime guard passed at `8157/48157`.
- `GREEN: independent focused replay -> PASS, exit code 0; 7 tests, zero failures/errors/skips.`
- `GREEN: independent adjacent replay -> PASS, exit code 0; 72 tests, zero failures/errors/skips.`
- `DIAGNOSTIC: independent broad A -> expected FAIL, exit code 1; Infra 446/3/1/10 and full fresh reactor XML 516/3/1/10. The only four outcomes were the registered RuntimeControlLocalConfig, RuntimeIncident, RuntimeOpsGuide and RuntimeOpsResponsibility baselines; executor tests passed 7/7.`
- `DIAGNOSTIC: independent broad B -> expected FAIL, exit code 1; the same exact Infra 446/3/1/10 and full reactor 516/3/1/10 fingerprint. No TempDir, directory-lock, DirectoryNotEmptyException or extra failure appeared.`
- Tester ended with unchanged HEAD, clean worktree, empty index, passing guard and zero task-owned Maven/Java/PowerShell/taskkill processes. It did not edit, stage, commit or merge any file. TC-18 is independently PASS; the combined SP-03 branch still requires a fresh full independent replay before integration.
- Root verified the SP-03 v4 worktree was clean at `aeb2c9011e23d9a5d70610b4fb4c50c156d2186d`, verified that commit was the sole parent of `5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac`, ran the slot-22 guard, and fast-forwarded the branch to the independently verified TC-18 commit. The post-merge hook and explicit guard both passed at `8156/48156`; the worktree remained clean.

## 2026-08-16 Final SP-03 + TC-18 Independent Replay Attempt 1

- Non-writer tester `/root/sp03_final_independent_tester` verified exact HEAD `5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac`, parent, 21-path candidate scope, clean index/worktree, no unmerged paths, `git diff --check`, slot-22 registry and runtime guard.
- Fresh PASS before fail-fast: TC-18 focused 7 and adjacent 72; SP-03 focused 35; System Notify regression 44; migration pytest 6; migration policy `status=passed`/`migrationCount=4`; BPM 1, DCC 2, Infra 6, MES 2 and Showroom 2. All reported tests had zero failures/errors/skips.
- `BLOCKER: IoT caller command -> exit code 1 before Surefire discovery because the fixed task Maven repository lacked cn.iocoder.boot:yudao-dependencies:2026.04-SNAPSHOT. No IoT test result was claimed; Trade and broad were not run after fail-fast.`
- Root inspected the source reactor and confirmed `yudao-dependencies` is a real root module but is not pulled into the earlier selected `-pl ... -am` preparation merely because the parent imports it as a BOM. Root ran the explicit source prerequisite install against `yudao-dependencies/pom.xml` with `maven.test.skip=true`; it exited 0 and installed the formal 22,786-byte BOM POM into the same isolated `m2-sp03-v4` repository. This is dependency preparation, not product-test PASS. The candidate worktree remained clean and no task process remained.
- The same independent tester will resume from the failed IoT gate with fresh reports, then execute Trade and the broad diagnostic. Earlier fresh PASS evidence remains recorded but cannot qualify the branch until the resumed gates pass.

## 2026-08-16 Final SP-03 + TC-18 Independent Replay Completion

- Root installed the formal IoT core, Member, Pay, Mall Product, Trade API and Promotion source artifacts into the same isolated `m2-sp03-v4` repository with `maven.test.skip=true`. Every preparation command exited 0; these actions only supplied build prerequisites and were not counted as product-test PASS.
- Non-writer tester `/root/sp03_final_independent_tester` resumed from the failed IoT gate against unchanged candidate `5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac`.
- `GREEN: independent CMD-SP03-IOT-CALLER-REGRESSION -> PASS, exit code 0; fresh 7 tests, zero failures/errors/skips.`
- `GREEN: independent CMD-SP03-TRADE-CALLER-CONTRACT -> PASS, leaf package exit code 0; repository inspection found exactly one Trade NotifyMessageSendApi production call and proved it remains after the fixed unconditional return. Tests were skipped only for this compile contract and were not reported as behavioral PASS.`
- `DIAGNOSTIC: independent CMD-SP03-BROAD-CALLER-DIAGNOSTIC -> expected FAIL, exit code 1; Infra 446 tests, 3 failures, 1 error and 10 pre-existing skips. Only the four registered RuntimeControlLocalConfig, RuntimeIncident, RuntimeOpsGuide and RuntimeOpsResponsibility baseline outcomes remained; the repaired executor passed 7/7 with no TempDir, directory-lock or DirectoryNotEmptyException.`
- Combined with the same tester's earlier fresh results, the exact candidate passed TC-18 focused 7 and adjacent 72, SP-03 focused 35, System 44, migration pytest 6/policy 4, and callers BPM 1, DCC 2, Infra 6, MES 2, Showroom 2 and IoT 7, all with zero failures/errors/skips.
- Final branch audit passed: exact 21-path scope, expected parent, clean worktree/index, no unmerged path or conflict marker, `git diff --check`, active slot-22 runtime guard and zero residual task process. AC-10..AC-15 and TC-18 are independently PASS; candidate is authorized to enter integration only.
- TC-17 remains external and unresolved, so this evidence does not authorize final fast-forward into `int_main`.

## 2026-08-16 SP-03 + TC-18 Integration Merge

- Root pinned unchanged `int_main` at `1e8ec9b81c416d40be51811e9d262948f2109a81`, confirmed the integration and candidate worktrees were clean with empty indexes, and reran the candidate slot-22 runtime guard successfully.
- In the clean integration worktree, `git merge --no-ff --no-commit 5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac` exited 0 without conflicts. The staged set matched the candidate's exact 21 paths: 21 expected, 21 actual, zero extra, zero missing and zero unmerged.
- `git diff --cached --check` and the integration slot-14 v4 runtime guard passed before commit. The ordinary merge was committed through the normal hook as `9888417f704f1ac8b9af82bb22d51e1db511d374`; its parents are integration SP-01/SP-02 head `4542fa3da4569e50ae93c5eddc3f4d67d940646b` and candidate `5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac`.
- Ancestry checks confirmed SP-01 `e0db53516`, SP-02 `3a8caab09`, SP-03 `aeb2c9011` and TC-18 `5b22aa052` are all ancestors. The integration worktree is clean, `git diff --check main..HEAD` passes, and the exact combined delta is 67 paths: 46 existing SP-01/SP-02 plus 21 SP-03/TC-18.
- No `int_main` dirty or untracked path was staged, modified, cleaned or copied. Combined integration verification is now running; final fusion remains prohibited until it passes and TC-17 is resolved.

## 2026-08-16 Root Combined Integration Regression

- `GREEN: CMD-TOOLCHAIN-PREFLIGHT -> PASS, exit code 0; Maven 3.9.9, runtime Java 21.0.10 and compiler target 17.`
- `GREEN: integration SP-01 focused/regression -> PASS, exit code 0; 27/54 tests, zero failures/errors/skips.`
- `GREEN: integration SP-02 prepare -> PASS, exit code 0; exact source reactor installed into task-owned m2-sp02 with tests skipped only as prerequisite preparation.`
- `GREEN: integration SP-02 A/B/C focused -> PASS, exit code 0; 60/26/164 tests, zero failures/errors/skips.`
- `GREEN: integration SP-02 Infra/DCC regression -> PASS, exit code 0; 82/677 tests, zero failures/errors/skips.`
- `GREEN: integration SP-03 focused/System -> PASS, exit code 0; 35/44 tests, zero failures/errors/skips.`
- `GREEN: integration CMD-SP03-MIGRATION-CONTRACT -> PASS, exit code 0; 6 pytest cases passed.`
- `GREEN: integration CMD-SP03-MIGRATION-POLICY -> PASS, exit code 0; JSON status=passed and migrationCount=4.`
- The current integration source artifacts were installed into task-owned m2-sp03 with tests skipped only for build preparation. `GREEN: integration caller commands -> PASS, exit code 0; BPM 1, DCC 2, Infra 6, MES 2, Showroom 2 and IoT 7 tests, all zero failures/errors/skips.`
- `GREEN: integration Trade static/compile contract -> PASS; source scan found one NotifyMessageSendApi call at line 37 after fixed unconditional return at lines 28-29, and leaf package exited 0 with tests skipped only for the compile contract.`
- `DIAGNOSTIC: integration broad caller command -> expected FAIL, exit code 1; Infra now discovered 456 tests because SP-02 adds 10 BusinessFileAccessService cases to the earlier candidate suite. The result remained exactly 3 failures, 1 error and 10 pre-existing skips in the same four registered baseline classes; RuntimeControlCommandExecutorImplTest passed 7/7 and no TempDir/directory-lock error appeared. Full fresh reactor XML totaled 526/3/1/10. This is not reported as PASS.`
- Structured XML parsing confirmed every root integration suffix had fresh non-zero reports: SP-01 27/54; SP-02 60/26/164/82/677; SP-03 35/44; callers 1/2/6/2/2/7, all zero failures/errors/skips.
- Post-regression static gates passed: clean integration worktree/index, 67 exact delta paths, zero unmerged paths, `git diff --check`, no conflict markers and slot-14 v4 runtime guard at `8095/48095`.
- A non-writer integration tester is independently auditing the same clean commit and reports. TC-17 remains missing its approved runtime target and credentials, so TC-19 and final fusion cannot yet be marked PASS.

## 2026-08-16 Independent Combined Integration Audit

- Non-writer tester `/root/integration_combined_tester` independently verified exact integration HEAD `9888417f704f1ac8b9af82bb22d51e1db511d374` without modifying, staging, committing or merging any file.
- `GREEN: independent integration SP-01 -> PASS; focused/regression 27/54 tests, zero failures/errors/skips.`
- `GREEN: independent integration SP-02 -> PASS; A/B/C 60/26/164 and Infra/DCC regression 82/677 tests, zero failures/errors/skips.`
- `GREEN: independent integration SP-03 -> PASS; focused/System 35/44 tests, migration pytest 6 and four-node policy DAG PASS.`
- `GREEN: independent reachable callers -> PASS; BPM 1, DCC 2, Infra 6, MES 2, Showroom 2 and IoT 7 tests, zero failures/errors/skips.`
- `GREEN: independent TC-18 -> PASS; focused/adjacent 7/72 tests, zero failures/errors/skips.`
- `DIAGNOSTIC: independent broad caller command -> expected FAIL, exit code 1; Infra 456/3/1/10 and full fresh reactor 526/3/1/10, containing only the same four registered unrelated baselines. RuntimeControlCommandExecutorImplTest remained 7/7, with no TempDir, directory-lock or residual process issue. This command is not reported as PASS.`
- Static independent gates passed: Infra has no DCC dependency; all six file operations authorize before read/conversion/print/download side effects; commit union and actual delta both equal the exact same 67 paths; `git diff --check`, conflict scan and slot-14 runtime guard passed.
- Final tester state: unchanged HEAD, clean worktree/index, zero unmerged paths and zero task Maven/Java/cmd/taskkill residual processes. AC-01..AC-15 and TC-18 are PASS.
- `BLOCKER: TC-17 -> no explicitly selected non-production host/port/schema and no approved read-only credential injection are available. No database, SSH or remote query was attempted. TC-19, final fast-forward and closeout remain prohibited.`

## 2026-08-16 TC-17 Fresh Prerequisite Audit

- Main Agent and read-only subagent `/root/tc17_fresh_prereq_audit` independently rescanned current machine state after the integration audit. Process/User/Machine environment-variable names contain no TC-17, registration-certificate, read-only DB, MySQL, JDBC or datasource target injection; no secret values were read.
- The task directory, project root, integration worktree and integration backend top level contain no task-selected protected env/credential file. `docs/server-access.md` identifies only the test application host and HTTP port; it does not identify the database host/port/schema or an approved read-only account, so those values cannot be inferred.
- Local listeners on ports 3306 and 23306 prove only that local MySQL/Docker services exist. They do not prove the required non-production target identity, schema, credential approval or authorization and were not queried.
- The standalone `mysql` client is absent, but Python has both `pymysql` and `mysql.connector`; therefore no tool installation is required after the target and protected credentials are supplied.
- `BLOCKER: TC-17 prerequisite audit -> unchanged. Required inputs are explicit non-production host/port/schema, an approved read-only account, a non-logging secret injection path, and authorization limited to information_schema tables/columns/statistics. No database, SSH or SQL action was attempted.`
- Third consecutive goal-turn audit at `2026-08-16T16:04:14+08:00` found the same condition: no matching Process/User/Machine DB environment-variable names and no top-level TC-17/credential file in the main or integration roots. Main remained `1e8ec9b81`, integration remained clean at `9888417f7`, and the main index remained empty. The persistent goal is therefore formally blocked pending the documented external inputs; no fusion or cleanup was performed.

## 2026-08-16 TC-17 Local Docker Authorization

- User identified the existing local Docker database and explicitly authorized creating a temporary read-only account in `int-ruoyi-mysql`, using it for TC-17, and deleting it afterward.
- Read-only discovery confirmed the target candidate is local container `int-ruoyi-mysql`, host port `23306`, schema `ruoyi-vue-pro`. The container currently has only full-privilege root accounts and no existing application read-only account.
- BDD: TC-17 local Docker schema verification -> Given the user-selected local non-production schema and no existing read-only account / When a randomized temporary account receives only `SELECT` on `ruoyi-vue-pro` and queries `information_schema` / Then the base tables, optional notify migration state and controlled-content unique indexes are verified without migration or business-data writes, and the temporary account is removed on every exit path.
- Operation gate: the password must be generated only in process memory, never printed or committed; grants must prove SELECT-only scope; cleanup must independently prove the temporary account no longer exists.

## 2026-08-16 TC-17 Main Read-only Runtime Verification

- First verifier execution exited 1 because `current_user` was used as a SELECT alias and MySQL rejected the alias syntax. This was a verifier-command defect, not product or schema RED. The verifier's `finally` path executed, and a separate root metadata query exited 0 with count 0 for all `tc17_rc_*` users; no temporary account remained.
- The corrected verifier generated the password only in process memory, created a randomized `%` account, granted only schema-level `SELECT` on `ruoyi-vue-pro`, and proved all global privileges plus all schema write/DDL/grant privileges were `N` before querying as that account.
- `GREEN: TC-17 corrected read-only verifier -> PASS, exit code 0; target local Docker container int-ruoyi-mysql, 127.0.0.1:23306, schema ruoyi-vue-pro.`
- Required base tables `system_notify_message`, `controlled_content_version_ref` and `controlled_content_transition_audit` are all InnoDB `BASE TABLE` objects.
- `system_notify_message.business_key` and `uk_system_notify_message_tenant_business_key` are both absent. This is the explicitly allowed `valid_pre_migration` state; it is not reported as deployed and there is no half-migration.
- Controlled-content contracts are exact unique indexes: `uk_controlled_content_active(tenant_id,content_type,content_key,active_unique_flag)`, `uk_controlled_content_open_candidate(tenant_id,content_type,content_key,open_candidate_unique_flag)`, and `uk_controlled_content_transition_event(version_ref_id,action,event_key)`.
- The metadata query authenticated as the randomized temporary account. Cleanup then dropped it and independently queried `mysql.user` to prove count zero. No migration or business-data write occurred; no password, connection string or raw secret-bearing output was recorded.
- TC-17 main verdict: PASS. Independent non-writer replay and final fusion preflight remain required.

## 2026-08-16 TC-17 Independent Replay

- Non-writer tester `/root/tc17_independent_tester` independently created a different randomized temporary account and ran the runtime contract at `2026-08-16T16:28:07+08:00`; complete process exit code 0 and elapsed time 323 ms.
- Privilege evidence was exact: global `USAGE` only with `IS_GRANTABLE=NO`, schema `ruoyi-vue-pro/SELECT/NO`, no table privileges, and SHOW GRANTS contained only USAGE plus schema SELECT.
- The three required objects were base tables; notify column/index results were both empty and classified `valid_pre_migration`; all three controlled-content unique indexes were unique, unprefixed and in the exact required column order.
- Cleanup ran in `finally`: account count was one after creation and zero after DROP, with `cleanup_attempted=true` and `cleanup_succeeded=true`. No secret, migration, business DML, SSH, remote access, product edit or Git mutation occurred.
- TC-17 independent verdict: PASS. Combined with all prior integration evidence, TC-19 is PASS and final fusion preflight may proceed.

## 2026-08-16 Main Advance Stop And Reconciliation

- The atomic TC-20 fusion command stopped before merge because `int_main` had advanced from pinned `1e8ec9b81` to `ecb05caa615c384b3833dd9d7b9b9594df3ad30e`. Exit code 1 carried `TC20_MAIN_HEAD_MOVED`; no file, index or branch was modified by the stopped command.
- The new main commit is a merge of the independently delivered production-release flow. Its old-main delta contains 147 paths. A fresh case-insensitive exact and bidirectional file/directory-prefix comparison against this task's 67 paths returned zero exact and zero prefix overlaps.
- Root confirmed main still had index 0, unmerged 0, tracked dirty 25 and untracked 11,488; integration was clean at `9888417f7`. Because new main was no longer an integration ancestor, final fast-forward correctly remained prohibited.
- In clean integration, `git merge --no-ff --no-commit ecb05caa6` exited 0 with zero conflicts. The staged set matched the exact 147 expected new-main paths, with zero extra/missing/unmerged; cached diff-check and slot-14 runtime guard passed.
- The normal hook-backed reconciliation commit is `68578ad1c7b60a0228f12eccae55e345ff64b4ca`, with first parent `9888417f7` and second parent `ecb05caa6`. Both parents and all four verified task commits are ancestors; the integration worktree is clean.
- Since the new main changes MES/DCC prerequisites and runtime governance, all affected combined tests and independent verification must be rerun on exact reconciled HEAD before TC-19 can be restored to PASS.

## 2026-08-16 Reconciled Integration Root Replay

- Exact candidate: `68578ad1c7b60a0228f12eccae55e345ff64b4ca`; its parents are preserved integration `9888417f7` and current `int_main` `ecb05caa6`. Before testing, the worktree/index/unmerged set were clean, the 67-path task allowlist exactly matched `ecb05caa6..HEAD`, `git diff --check` and conflict-marker scan passed, and the slot-14 runtime guard passed.
- `GREEN: reconciled SP-01 focused/regression -> PASS, 27/54 tests, zero failures/errors/skips.`
- `GREEN: reconciled SP-02 prepare/A/B/C/Infra/DCC -> PASS, 60/26/164/82/677 tests, zero failures/errors/skips.`
- `GREEN: reconciled TC-18 focused/adjacent -> PASS, 7/72 tests, zero failures/errors/skips.`
- `GREEN: reconciled SP-03 focused/System/migration policy/callers -> PASS, 35/44 tests, migration pytest 6 and four-node policy PASS; BPM/DCC/Infra/MES/Showroom/IoT 1/2/6/2/2/7 tests, each zero failures/errors/skips. Trade retained the approved static and compile-only contract; its unreachable call was not represented as a behavioral PASS.`
- `DIAGNOSTIC: reconciled broad reactor -> expected exit code 1; fresh reports total 526 tests, 3 failures, 1 error and 10 skips. The only outcomes are the four registered unrelated Infra runtime-control baselines: RuntimeControlLocalConfigContractTest, RuntimeIncidentServiceImplTest, RuntimeOpsGuideServiceImplTest and RuntimeOpsResponsibilityServiceImplTest. RuntimeControlCommandExecutorImplTest is 7/7 PASS, with no Windows temporary-directory handle regression.`
- Static security evidence: Infra has zero DCC module imports/dependencies; DCC has the formal `DccBusinessFileAccessProvider`, startup presence guard and pre-side-effect `BusinessFileAccessService` calls at all required outlets. The main dirty-path comparison remained zero exact and zero bidirectional file/directory-prefix overlap across 11,513 existing dirty paths.
- A fresh non-writer Agent is now independently replaying the same reconciled HEAD. No product or main-worktree file was edited by this root replay.

## 2026-08-16 Reconciled Integration Independent Replay

- Non-writer `/root/reconciled_integration_independent_tester` independently verified exact clean HEAD `68578ad1c7b60a0228f12eccae55e345ff64b4ca`: `git diff --check`, 67 incoming paths, four verified task ancestors, no conflict markers and slot-14 runtime guard all passed.
- Fresh independent test evidence: SP-01 focused 27; SP-02 A/B/C 60/26/164; SP-03 focused/System 35/44, migration pytest 6 and migration policy 4; reachable BPM/DCC/Infra callers 1/2/6; TC-18 focused 7. Every listed report is zero failures/errors/skips.
- Independent broad diagnostic exited 1 as expected and had exactly 526 tests, 3 failures, 1 error and 10 skips; the only outcomes are the same four registered unrelated Infra baselines. The repaired command executor remained 7/7 PASS in that same run.
- Initial unquoted Maven preparation and a report JSON parsing attempt failed before product tests/results; both were corrected and neither is used as RED/GREEN or independent pass evidence. The tester made no file, Git, database or process-lifecycle mutation.
- Independent verdict: PASS for fusion adjudication. Root now proceeds to the atomic TC-20 preflight; a changed main head, overlap, index/unmerged path, guard or fast-forward failure remains a hard stop.

## 2026-08-16 TC-20 Fast-forward Fusion

- `GREEN: atomic TC-20 fusion -> PASS, exit code 0; int_main HEAD=68578ad1c7b60a0228f12eccae55e345ff64b4ca.` The first invocation had a PowerShell parser error before preflight and before any Git mutation; it was corrected and is not test/fusion evidence.
- Exact preconditions passed in one protected execution: `int_main` was still pinned at `ecb05caa6`; integration was clean at the target; both indexes and unmerged sets were empty; both runtime guards passed; ancestry passed; the 67 incoming paths exactly matched the four verified task-commit first-parent deltas; `git diff --check` and conflict scan passed.
- Main dirty preservation gate passed: case-insensitive exact and bidirectional file/directory-prefix overlap against all 11,514 then-current dirty paths was zero. The before/after dirty snapshot count and SHA-256 were identical (`11514`, `2506c9555c289428efc9a3063fad173ab2a8b6ba3fa05e2d30638f84e630c119`).
- `git merge --ff-only 68578ad1c7b60a0228f12eccae55e345ff64b4ca` succeeded through normal hooks. Post-merge branch/head, index, unmerged set, dirty snapshot and main runtime guard all passed. No stash, reset, rebase, ordinary merge, cleanup or unrelated staging was used.
- Task status is now `ready_for_closeout`; only task-owned cleanup preview/apply and isolated worktree removal remain.

## 2026-08-16 Task Closeout

- `GREEN: task-closeout preview -> PASS, exit code 0; keep set is the nine durable task records, delete set contains only the task-owned release-worktree-slot.ps1, with no blocked path or warning.`
- `GREEN: task-closeout apply -> PASS, exit code 0; deleted only release-worktree-slot.ps1 and preserved all nine declared records.`
- Before removal, integration, SP-02, SP-03-v4 and TC-18 worktrees were clean with empty staged/unmerged sets. The legacy SP-03 source worktree had 13 task-owned dirty files; all 13 blob hashes matched the committed SP-03 candidate exactly, with no extra path, so its forced removal did not discard unique content.
- `GREEN: task worktree removal -> PASS; integration, SP-02, SP-03-v4, legacy SP-03 source and TC-18 worktrees no longer exist. All task ports 48095, 48099, 48156 and 48157 had zero listeners before removal.`
- `GREEN: slot release -> PASS; active registry entries for integration/SP-02/SP-03-v4/TC-18 slots 14/18/22/23 were atomically marked inactive with this task ID. No unrelated worktree, port record, product file or main dirty file was modified.`
- Closeout state: `completed`; only the nine task-owned durable records are staged in the final closeout commit.
