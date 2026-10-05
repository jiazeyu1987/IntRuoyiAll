# DCC Public Backend Completion

## Root business decisions received 2026-10-02

用户已明确确认：驳回后的升版重提仍申请原目标号（A/1→A/2失败后仍A/2），保留每次失败申请、真实BPM、正文来源、属性与签名历史；项目/产品审核人由后台配置，提交时带出。Root共同合同shared-contract.md、implementation-contract.md及需求HTML v1.4已更新，原“未知政策”门禁关闭。

本Agent后端范围扩展：在当前公共API依赖交付后，接精确返工前驱/真实BPM终结与同目标号候选身份（先schema/锁/历史保护设计、BDD/RED/GREEN，前向迁移只准备不执行）；随后接正式后台审核人员配置与提交时审核身份快照，不借admin/申请人默认代替。配置缺失/停用/跨tenant明确拒绝，不改批准节点的独立规则。与detail前端Owner协调配置读取/维护合同。

公共项目文件浏览另需browserScope=GLOBAL|PROJECT_FOLDER（与selectorScope分开）或等效明确只读合同，列表含当前用户有权查看的WORKING/在途/受控及版本事实；继承实际Query权限/分页total、精确projectFolderId，不能用latest-controlled选择器冒充完整浏览或让用户退回NAS才能找草稿。浏览Agent已交coordination.md，优先给出稳定响应供其接线。

## Goal
Complete the existing shared upload/draft baseline and the delegated closure batch: full logical-project browsing, same-target rejected revision retries, and configured project/product reviewers frozen per application.

## Milestones
1. Repair strict HTTP fixture and prove existing placement boundary.
2. Prove real A creation plus B placement, payload replay and rollback cleanup.
3. Wire explicit registered placement inheritance with project -> folder -> Master/File locking.
4. Deliver coordinated detail read APIs, targeted regression, review evidence.
5. P05 full browser, P06 exact failed-attempt reuse and P07 configured reviewer contract with backend/HTTP/H2/Flowable verification.

## Expected Verification
Isolated H2 and in-memory Flowable only; Maven reactor targeted tests, main application compile, additive migration dependency policy checks. Root retains server, real DB, E2E, final local integration and Git ownership; this subtask performs none of those actions.

## Current Status

ready_for_closeout — G64 rejection120/4 and current control/remediation final16/1 independently all0, source/XML seals retained; Maven/source/target frozen, Root owns actual UI/runtime/Git/goal closeout.

Preserved G53 latestdefinition finite receipt9/3 PASS and G49 projectapplication broad102/7 plus later43/2 PASS are independent prior deliveries, not additive totals or replacements for RootactualUI.

G57 local restoration separate newRunner1/test1 is ready_for_closeout final R2: actual oldstartup missingRAMJob RED1failure0error -> 17GREEN -> minimal24/2 all0 CLI0 at18:52:39. Defaultdisabled, only dcc-local-development/exactexisting5625/localDBUUID/Shanghai/RAM/globalSyncfalse, no pooledconnection state setter or DBCRUD/sharedsync. TwooriginalXML archived-r2; final manifest1e1fe7d3...ccb4. Maven FREE Root, noactualDB/UI/runtime/Git/package by this owner; do not sum24 with284 or claim fullHTML.

Previous finite delivery remains ready_for_closeout: LD03/G48 mapping17production/6tests/4schema, actual219/6 PASS and six XML permanently archived. The new G49 implementation does not rewrite that receipt or claim the overall goal completed.


## G02 Root review repair

Root resumes this existing record for a bounded obsolete readiness repair. Current goal authorizes local integration Git, isolated services and real UI E2E; Root retains final Git/runtime ownership. Prior no-authorization statements are historical, not blockers for implementation.

- Given a live independent OBSOLETE MATRIX_REVIEW or MATRIX_APPROVAL task for the exact tenant/file and authorized participant, When the public detail requests action readiness, Then the same formal review guard as the signed action applies and readiness succeeds without requiring the file's original UPLOAD/REVISION process.
- Given wrong tenant/file/task/actor, unsigned assignment or another application round, When readiness is requested, Then it rejects before signatures, workflow writes, or obsolete effects.
- Given a native UPLOAD/REVISION task, When readiness is requested, Then existing doc-control artifact and date checks remain unchanged.
- RED/GREEN: focused service tests exercising real Workflow and Signoff services plus existing native Workflow/Signoff regression. Runtime E2E remains pending shared dependencies.

## BDD
- G13-REVIEW Given native UPLOAD/REVISION MATRIX_APPROVAL, When actual public approve is dispatched with an enabled same-tenant selected owner, Then real signature reason/HMAC signs that exact canonical Selection and formal File owner projection joins true task/BPM/signature in the same transaction. Tampered Selection/reason mismatch rejects before owner write; another node/tenant/task/actor account fails without advancement. Each new actual approval action may project its own selected owner, while every original signature/owner signed fact stays immutable; an exact same-task replay cannot change Selection. Same-file/BPM rework reapproval uses actual BpmTaskServiceImpl.returnTask and true Flowable state/history; no first-person forever guard.
- G13-REVIEW Given authorized completed approval or real signing/BPM/audit failure/concurrent approvals, When public API executes, Then owner and actual DCC/unified signature rows/Flowable task/status commit or roll back coherently; history reads saved names and new checkin/formal candidate clears owner approval facts. HTTP Long identities are exact strings. Native approval-center item exposes PROCESS_IN_MODULE for owner-required approval and opens exact existing task/BPM detail, never offers a quick request missing owner.
- UI05-IMPLEMENT Given one source Master referenced by two folders in one destination project and a second destination project with distinct fixed versions, When usage-page is read by a source-name-visible actor, Then exact tenant/source/Master and global/visible distinct-project counts, restricted flag and authorized SQL total/page are coherent; labels require destination listReadableProjectIds/hard scope and no detail/body grant. Stored selected versions never follow latest. Invalid tenant/Master/folder/reference identity rejects without empty success. Concurrent cancellation cannot mix count/page snapshots; HTTP Long values remain exact strings.
- G08-B1 Given Master.latestControlledFileId still points to an OBSOLETE file or a file missing controlledTime, When browser latestVersionOnly=true runs, Then shared SQL candidates/count/page exclude that row without changing pointers or falling back to currentActive. Given latestVersionOnly=false/history filter and authorized users, Then actual obsolete history remains readable. Database page totals and rows use the identical state predicate and string identities.
- G08-UI05 Readonly scope: inspect existing project references and HTML flow-11, propose explicit fixed-version usage-detail fields, tenant/file/Master/project/folder authorization and pagination/count semantics for Root Review; no new API implementation or data/schema change in this batch.
- G07-P16 Given A/1 executes and A/2/A/3 are controlled with effective dates earlier/equal/later than each other, When a due version activates, Then every strictly lower currently controlled version (ACTIVE and pending effective) atomically becomes OBSOLETE with the new successor, actual obsolete time, SYSTEM_ACTOR audit and 20-year name/number retention; higher future and uncontrolled/failed candidates remain untouched. Original effective/control/approval dates, artifacts, bodies, application snapshots and signatures stay immutable. Latest/currentActive pointers remain authoritative and no lower pending row can reverse execution or poison later jobs. Concurrent jobs/replays produce one effect; late audit failure rolls back all rows/pointers/retention/ledger/events.
- G07-P17 Given an approved explicit schedule/runtime configuration, When the forward activation-job registration package is applied, Then the official handler is registered once in paused status and later enabled only through official Quartz-aware controls; missing approved frequency or mismatched existing handler data fails. The user has now approved Asia/Shanghai, seven-day workbench reminder and every-minute Quartz cron 0 * * * * ?. This task only prepares registration/configuration; Root owns services/real database execution.
- P14 Given a NEW_UPLOAD SOURCE MultipartFile whose actual full name is already actively occupied anywhere in the tenant (including 20-year obsolete retention), When upload-preview runs, Then the official NameClaim readonly preflight rejects before storage/ticket writes without disclosing the other project. Case/extension variants stay distinct; unresolved legacy claims fail explicitly; other contexts/purposes skip new-name checks. Given an exact same actor/session/body/name already bound to a legal own NEW working draft, When replayed, Then the original bound ticket is returned without new bytes/claims; different identity/content rejects. Final transaction unique claim and concurrency controls remain mandatory. Later upload failures roll back ticket records and remove only newly allocated bytes.
- P15 Given relation name visibility without detail/body/edit access, When relation-permissions is queried for current source or historical target, Then exact tenant/file/Master/project and formally registered folder/name metadata plus independent canPreview are available without a detail/project management read. Missing or mismatched parent/placement fails explicitly and name-only reads never grant body/edit/history secrets or omit relations.
- G04-P13 Given a formal project OWNER selecting another requester's legal WORKING body for explicit REPLACEMENT, When selected attributes are read in formal replacement context and submitted, Then a new OWNER-owned formal candidate/BPM inherits exact selected saved provenance and actual while the source draft stays unchanged; ordinary save/restore/INITIAL/PARTIAL remain requester-only. Wrong baseline, non-OWNER, hard scope or selected identity reject before writes. Given same-number historical A/2 attempts, When detail history is read, Then each authorized row exposes its own true processInstanceId; name-only rows do not expose private process identity.
- G04-P10 Given a legal older requester-owned WORKING iteration with original saved defaultSource/actual and a newer working iteration/current project default, When exact selected draft attributes are read, Then the original source and actual remain unchanged and read never depends on submission readiness; hard scope, tenant/Master/project/type/category/requester failures reject with zero writes. When the same body is formally submitted, its frozen defaultSource/actual match what was read.
- G04-P11 Given an explicitly replaced department set for NEW/REVISION/OBSOLETE, When real preview/submit resolves the route, Then removed default leaders are never parsed and only chosen enabled department leaders are frozen; strict Long string inputs bind and later approval/doc-control nodes/history remain unchanged. Invalid department/type/user combinations reject before writes.
- G04-P12 Given failed and later obsolete applications bound to actual distinct Flowable BPM history for one file, When rounds/evidence are read, Then all exact recorded rounds remain available independently of the original upload/current defaults/signature text; invalid BPM identity or foreign snapshots fail explicitly.
- G04-P09 Given persisted UPLOAD/REVISION/OBSOLETE BPM mappings for the selected file, When history choices are read, Then only that tenant/project/file's bound non-draft rounds are returned with exact identities; missing history is not invented from current defaults or signature text. Given name-visible files, When relation permissions are read, Then actual mutation and binary guards produce independent edit/preview capabilities, expected access denial is read-only and infrastructure errors propagate.
- G04-P09 Given a current relation set points at a file from another Master, When read, Then it fails identity validation before presenting a mixed source. Root owns this bounded backend read wiring after the completed child handoff.
- G04-P08 Given a default department has been explicitly removed and a valid replacement selected, When route readiness or formal submission resolves participants, Then only the selected departments are resolved; missing leaders from removed defaults do not block the edited request. Null retains defaults, empty/duplicate/invalid department sets reject, later approval/doc-control nodes remain unchanged.
- G04-P08 HTTP Given exact Long department IDs on route-preview, When the public controller dispatches, Then the selected set reaches the formal service without being discarded or treated as user IDs; legacy manual user rejection remains. Root currently implements this bounded repair after backend_closure completed; no other backend writer is running.
- P05 Given authorized WORKING, in-flight and controlled versions with formal project placements, When browserScope GLOBAL/PROJECT_FOLDER is queried, Then the real Query authorization filters all version rows before identical SQL count/page; logical folders never fall back to NAS, Long values remain exact strings, and name visibility does not grant body permissions. selectorScope remains latest-controlled only and cannot mix with browserScope.
- P06 Given an A/2 rejected formal attempt whose exact legal rework chain is selected, When the same target is submitted again, Then a new immutable attempt/BPM/body/default/actual/signature history is retained at A/2; only the exact terminal predecessor is excluded from candidate uniqueness, under Master and predecessor locks and stable request idempotency. Concurrent unrelated candidates and non-terminal processes reject.
- P07 Given a tenant's configured enabled project/product reviewer, When a new or rejected creation request is submitted, Then the current account identity is frozen in that request; later configuration edits do not change review authorization. Missing/disabled/foreign-tenant configuration rejects before writes. Approval retains its independent existing rule.
- P01 Given HTTP submit/working with exact Long folder identity, When dispatched, Then actor and exact request reach outer orchestration before actual creation.
- P02 Given actual A create and B binding, When placement or later audit fails, Then all DB records and newly allocated source copies roll back; successful same payload replay is stable and changed folder/reason/actual rejects.
- P03 Given a registered source location, When checkin or formal INITIAL/revision candidate is created, Then it explicitly inherits that exact logical folder with its actual NAS identity; legacy unregistered sources remain accurately unregistered without guessing.
- P04 Given formal working draft and authorized requester, When draft is read/saved/restored, Then original defaultSource and actual values are preserved; no fabricated BPM or current-default overwrite.

## Design Constraints / 设计约束检查
- Only integration tree and shared backend scope delegated by Root; frontend and original ABCD trees untouched.
- One authoritative B placement system. Project -> folder -> Master/File lock order, no NAS/name inference.
- Confirmed same-target revision reuse uses exact recorded failed predecessor and a new immutable attempt. Reviewer configuration is tenant-owned and freezes the enabled account at submission. No guessed admin, applicant or BPM identity.
- No child agents, services, real DB, E2E or Git writes by this subtask. Root retains final authorized integration/closeout; historical no-authorization wording does not block this implementation.

## Cleanup Keep

- doc/tasks/20261002-dcc-public-backend-completion/g66-current-reviewer-junit/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectReviewerConfigurationTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g66-current-attributes-junit/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectAttributesServiceTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g66-current-attributes-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkingAttributesHttpContractTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g66-current-attributes-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowAttributesIntegrationTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g66-current-attributes-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccSelectedWorkingAttributesDatabaseTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g66-current-reviewer-verification-receipt.json

- doc/tasks/20261002-dcc-public-backend-completion/g66-current-attributes-verification-receipt.json

- doc/tasks/20261002-dcc-public-backend-completion/g66-current-proof-fingerprints.json

- doc/tasks/20261002-dcc-public-backend-completion/g66-backend-completion-proof-audit.md

- doc/tasks/20261002-dcc-public-backend-completion/g66-config-authority-completion-proof-audit.md

- doc/tasks/20261002-dcc-public-backend-completion/g64-current-control-r3-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationControlledEventIntegrationTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-current-control-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationControlledEventIntegrationTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-current-control-r3-verification-receipt.json

- doc/tasks/20261002-dcc-public-backend-completion/g64-current-control-r3-fingerprints.json

- doc/tasks/20261002-dcc-public-backend-completion/g64-current-control-verification-receipt.json

- doc/tasks/20261002-dcc-public-backend-completion/g64-current-control-fingerprints.json

- doc/tasks/20261002-dcc-public-backend-completion/g64-current-control-green-attempt1.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-current-control-fixture-red.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-current-control-remediation-bdd.md

- doc/tasks/20261002-dcc-public-backend-completion/g64-name-ref-current-delivery-fingerprints.json

- doc/tasks/20261002-dcc-public-backend-completion/g64-name-ref-current-runtime-tests.md
- doc/tasks/20261002-dcc-public-backend-completion/g64-name-ref-current-preflight.json
- doc/tasks/20261002-dcc-public-backend-completion/g64-name-ref-current-r1-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g64-name-ref-current-final-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g64-name-ref-current-r1-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccUploadNamePreflightDatabaseTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g64-name-ref-current-r1-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccLegacySourceNameOccupancyTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g64-name-ref-current-r1-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccProjectReferenceFormalAuthorityIntegrationTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g64-name-ref-current-reference-r2-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccProjectReferenceFormalAuthorityIntegrationTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g64-name-ref-current-reference-r3-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccProjectReferenceFormalAuthorityIntegrationTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowNativeCompletionEvidenceTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRejectedRevisionRetryDatabaseTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileFinalizationServiceImplTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccNativeRejectionTransactionTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-backend-verification-receipt.json

- doc/tasks/20261002-dcc-public-backend-completion/g64-backend-fingerprints.json

- doc/tasks/20261002-dcc-public-backend-completion/g64-native-reject-effective-red-r2.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-native-reject-effective-red.xml

- doc/tasks/20261002-dcc-public-backend-completion/g64-native-reject-key-bdd.md

- doc/tasks/20261002-dcc-public-backend-completion/g64-name-reference-config-proof-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g64-be-rejection-remediation-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g63-native-obsolete-current-round-readonly-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g62-obsolete-obligation-wiring-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g62-obsolete-obligation-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g62-backend-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g62-backend-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g62-final-junit/TEST-cn.iocoder.yudao.module.bpm.formcenter.runtime.DccWorkflowFormCenterObligationTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g62-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileObsoleteFormEffectExecutorTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g62-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileObsoleteServiceTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g62-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowObsoleteTransactionTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g61-pending-obsolete-entry-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g61-pending-obsolete-entry-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g61-backend-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g61-backend-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g61-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g61-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationNameMetadataDatabaseTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g60-pending-obsolete-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g60-pending-obsolete-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g60-backend-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g60-backend-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g60-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledContentAdapterTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g60-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileObsoleteFormEffectExecutorTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g60-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileObsoleteServiceTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g60-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccPendingObsoletePlatformTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g60-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowObsoleteTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g60-final-junit/TEST-cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentLifecycleCoreServiceTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g60-final-junit/TEST-cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentStateMachineTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g60-final-html-code-direction-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g59-jdbc-event-time-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g59-jdbc-event-time-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-backend-fingerprints-r3.json
- doc/tasks/20261002-dcc-public-backend-completion/g59-backend-verification-receipt-r3.json
- doc/tasks/20261002-dcc-public-backend-completion/g59-jdbc-event-time-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccLifecycleProjectionRepairDatabaseTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g59-native-platform-lifecycle-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g59-native-lifecycle-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-repair-roster-tenant-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-repair-event-identity-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-gxp-policy-candidate.yaml
- doc/tasks/20261002-dcc-public-backend-completion/g59-repair-audit-rule.review.sql
- doc/tasks/20261002-dcc-public-backend-completion/g59-repair-impact.json
- doc/tasks/20261002-dcc-public-backend-completion/g59-backend-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g59-backend-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g59-backend-fingerprints-r2.json
- doc/tasks/20261002-dcc-public-backend-completion/g59-backend-verification-receipt-r2.json
- doc/tasks/20261002-dcc-public-backend-completion/g59-final-r2-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledContentAdapterTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-final-r2-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileFinalizationServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-final-r2-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccLifecycleProjectionRepairDatabaseTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-final-r2-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccNativePlatformLifecycleTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-final-r2-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccThreeControlledVersionActivationTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-final-r2-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowLifecycleTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-final-r2-junit/TEST-cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentLifecycleCoreServiceTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-final-r2-junit/TEST-cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionContractTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-final-r2-junit/TEST-cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentStateMachineTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-repair-final-r3-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccLifecycleProjectionRepairDatabaseTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g59-repair-final-r4-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccLifecycleProjectionRepairDatabaseTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g59-native-lifecycle-contract-plan.md

- doc/tasks/20261002-dcc-public-backend-completion/g59-native-platform-lifecycle-readonly-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g58-training-record-duty-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g58-training-duty-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g58-backend-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g58-backend-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g58-final-junit/TEST-cn.iocoder.yudao.module.dcc.approval.DccApprovalTaskAdapterTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g58-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.category.DccCategoryPermissionAdminServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g58-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileUploadApiTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g58-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g58-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccOfflineTrainingEntryDatabaseTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g58-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccTrainingRecordDutyApprovalTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g58-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccTrainingRecordDutyDatabaseTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g57-document-control-category-access-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g57-activation-ram-recovery-readonly-plan.md
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-junit-archive.json
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-junit-archive-r2.json
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-verification-receipt-r2.json
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-fingerprints-r2.json
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccLocalActivationStartupSyncRunnerTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-junit/TEST-cn.iocoder.yudao.module.infra.service.job.JobStartupSyncRunnerTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-junit-r2/TEST-cn.iocoder.yudao.module.dcc.service.file.DccLocalActivationStartupSyncRunnerTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-local-activation-startup-junit-r2/TEST-cn.iocoder.yudao.module.infra.service.job.JobStartupSyncRunnerTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-obsolete-rework-ui-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g57-offline-training-entry-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g57-current-file-location-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g57-backend-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g57-backend-fingerprints-r1.json
- doc/tasks/20261002-dcc-public-backend-completion/g57-backend-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g57-offline-training-entry-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-current-file-location-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-final-junit/TEST-cn.iocoder.yudao.module.dcc.approval.DccApprovalTaskAdapterTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileDetailAuthorizationGuardTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccFileOwnerPublicApprovalTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccOfflineTrainingApprovalTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccOfflineTrainingEntryDatabaseTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationNameMetadataDatabaseTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-final-junit/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectApplicationNotificationTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-training-combination-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccOfflineTrainingApprovalTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g57-training-combination-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccOfflineTrainingEntryDatabaseTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g56-training-record-entry-readonly-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g56-linked-upload-binding-order-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g56-linked-upload-order-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g56-linked-upload-order-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g56-linked-upload-order-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-linked-upload-order-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-linked-upload-order-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccLinkedUploadBindingOrderDatabaseTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-linked-upload-order-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRejectedRevisionRetryDatabaseTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-linked-upload-order-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccSubmissionRelationPersistenceTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-linked-upload-order-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowLifecycleReminderApiTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-linked-upload-order-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowLifecycleTransactionTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g56-lifecycle-html-acceptance-readonly-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g56-current-controlled-source-contract.md
- doc/tasks/20261002-dcc-public-backend-completion/g56-current-source-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-current-source-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g56-current-source-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g56-current-source-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationNameMetadataDatabaseTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-current-source-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSelectorDatabaseTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-current-source-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationFormalQueryGuardTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-relations-reference-name-ac-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-revision-arrangement-read-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-arrangement-read-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-arrangement-read-junit-archive.json
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-arrangement-read-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-arrangement-read-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-arrangement-read-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccInitialRevisionArrangementReadTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-arrangement-read-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowSignoffAssignmentTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-arrangement-read-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowSignedArrangementTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-arrangement-read-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccSignedRelationAssignmentContractTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-arrangement-read-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationRemediationTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g56-initial-arrangement-read-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationFormalQueryGuardTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g53-bpm-latest-independent-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g53-execution-driver-readonly-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g54-empty-relations-readonly-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-scope-independent-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g55-final-independent-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g55-four-direction-independent-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g55-detail-project-projection-readonly-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g55-detail-project-projection-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g55-project-projection-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g55-project-projection-final-junit.xml
- doc/tasks/20261002-dcc-public-backend-completion/g55-project-projection-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g55-project-projection-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-assignment-scope-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-scope-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-scope-effective-red-r2.xml
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-fingerprints-r2.json
- doc/tasks/20261002-dcc-public-backend-completion/g54-version-placement-context-junit.xml
- doc/tasks/20261002-dcc-public-backend-completion/g54-version-placement-context-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowSignoffAssignmentTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccAssignmentContextHttpContractTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationRemediationTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccSignedRelationAssignmentContractTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccWorkflowSignedArrangementTransactionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g54-signoff-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationControlledEventIntegrationTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g53-latest-process-definition-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g53-latest-definition-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g53-latest-definition-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g53-latest-definition-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g53-latest-definition-junit/TEST-cn.iocoder.yudao.module.bpm.service.definition.BpmLatestProcessDefinitionTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g53-latest-definition-junit/TEST-cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g53-latest-definition-junit/TEST-cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceRegistrationCertificateOperationContractTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-query-permission-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-project-application-frontend-independent-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g49-project-application-todo-plan.md
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g49-timeline-effective-red.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-timeline-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g49-timeline-junit-archive/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectApplicationNotificationTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-timeline-junit-archive/TEST-cn.iocoder.yudao.module.dcc.approval.DccApprovalTaskAdapterTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-notify-policy-red.json
- doc/tasks/20261002-dcc-public-backend-completion/g49-notify-policy-green.json
- doc/tasks/20261002-dcc-public-backend-completion/g49-notify-sql-static-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-junit-archive/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectApplicationNotificationTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-junit-archive/TEST-cn.iocoder.yudao.module.dcc.approval.DccApprovalTaskAdapterTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-junit-archive/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectProductAttributesCreateTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-junit-archive/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectProductDecisionPersistenceTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-junit-archive/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectProductLedgerTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-junit-archive/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectProductWriteRetryTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g49-backend-junit-archive/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectConfigurationHttpPersistenceTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g43-auth-independent-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g43-runtime-helper-independent-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26.py
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26-test.py
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26-contract.md
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26-operator-plan.md
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26.review.sql
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26-readonly-preflight.sql
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26-readonly-postflight.sql
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26-impact.json
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-policy.json
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26-input-contract.json
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g43-dev-config26-final-green-r3.log

- doc/tasks/20261002-dcc-public-backend-completion/g39-migration-execution-checklist.md

- doc/tasks/20261002-dcc-public-backend-completion/g35-positive-ui-architecture-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g35-navigation-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g35-navigation-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g35-navigation-red-r2.log
- doc/tasks/20261002-dcc-public-backend-completion/g35-navigation-green.log

- doc/tasks/20261002-dcc-public-backend-completion/g34-matrix-main-flow-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g34-real-ui-coverage-audit.md
- doc/tasks/20261002-dcc-public-backend-completion/g34-real-ui-negative-runner.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g34-real-ui-observer.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g34-real-ui-contract-test.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g34-negative-decision-red.log

G34 is deferred_for_detail_phase under the latest user priority. These saved source drafts are unfinished, not validated or runtime-ready; raw RED log stays local evidence/Git-excluded.

- doc/tasks/20261002-dcc-public-backend-completion/g33-maintenance-entry-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g33-maintenance-entry-contract.md
- doc/tasks/20261002-dcc-public-backend-completion/g33-maintenance-entry-verification.json
- doc/tasks/20261002-dcc-public-backend-completion/g33-maintenance-entry-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g33-format-owned.java
- doc/tasks/20261002-dcc-public-backend-completion/g33-entry-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g33-review-boundaries-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g33-schema-self-consistent-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g33-final-regression.log
- doc/tasks/20261002-dcc-public-backend-completion/g33-final-regression-r2.log
- doc/tasks/20261002-dcc-public-backend-completion/g33-final-regression-r3.log

G33 candidate is ready_for_closeout with final101/7 isolated PASS; master status and actual execution gates remain Root-owned. Raw Maven logs are evidence retained locally and excluded from Git assets.
- doc/tasks/20261002-dcc-public-backend-completion/g32-authenticated-registration-entry-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g32-authenticated-registration-entry-review-receipt.json

- doc/tasks/20261002-dcc-public-backend-completion/g31-distribution-obsolete-runner.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g31-final-lifecycle-contract-test.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g31-final-lifecycle-contract.md
- doc/tasks/20261002-dcc-public-backend-completion/g31-ui-delivery-fingerprints.json

- doc/tasks/20261002-dcc-public-backend-completion/g30-public-version-relations-runner.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g30-ui-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g30-version-relations-contract-test.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g30-version-relations-contract.md

- doc/tasks/20261002-dcc-public-backend-completion/g29-ui-delivery-fingerprints.json

- doc/tasks/20261002-dcc-public-backend-completion/g29-collector-independent-review-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g29-collector-independent-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g29-public-lifecycle-contract-test.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g29-public-lifecycle-contract.md
- doc/tasks/20261002-dcc-public-backend-completion/g29-public-lifecycle-runner.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g29-ui-preparation-receipt.json

- doc/tasks/20261002-dcc-public-backend-completion/g28-driver-independent-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g28-driver-independent-test.py
- doc/tasks/20261002-dcc-public-backend-completion/g28-driver-independent-review-receipt.json

- doc/tasks/20261002-dcc-public-backend-completion/g26-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g26-exact-object-recovery-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g26-exact-object-recovery-plan.md
- doc/tasks/20261002-dcc-public-backend-completion/g26-exact-object-recovery-test.java
- doc/tasks/20261002-dcc-public-backend-completion/g26-exact-object-recovery.java
- doc/tasks/20261002-dcc-public-backend-completion/g26-execution-contract.md
- doc/tasks/20261002-dcc-public-backend-completion/g26-legacy-independent-review-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g26-legacy-independent-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g26-legacy-schema-inventory.json
- doc/tasks/20261002-dcc-public-backend-completion/g26-legacy-schema-review-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g26-legacy-schema-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g26-readonly-bucket-probe-test.java
- doc/tasks/20261002-dcc-public-backend-completion/g26-readonly-bucket-probe.java
- doc/tasks/20261002-dcc-public-backend-completion/g26-registration-final-independent-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g26-registration-independent-review-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g26-registration-independent-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g27-audit-26-operation-impact.json
- doc/tasks/20261002-dcc-public-backend-completion/g27-audit-policy-26-review-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g27-audit-policy-26-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g27-audit-policy-adoption-note-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g27-public-lifecycle-e2e-plan.md
- doc/tasks/20261002-dcc-public-backend-completion/g27-public-lifecycle-preparation-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g27-public-lifecycle-ui-helpers-test.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g27-public-lifecycle-ui-helpers.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g27-selector-independent-review-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g27-selector-independent-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g28-config26-impact.json
- doc/tasks/20261002-dcc-public-backend-completion/g28-config26-input-contract.json
- doc/tasks/20261002-dcc-public-backend-completion/g28-config26-operation.review.sql
- doc/tasks/20261002-dcc-public-backend-completion/g28-config26-operator-plan.md
- doc/tasks/20261002-dcc-public-backend-completion/g28-config26-quality-empty-template.json
- doc/tasks/20261002-dcc-public-backend-completion/g28-config26-quality-version.review.sql
- doc/tasks/20261002-dcc-public-backend-completion/g28-config26-readonly-postflight.sql
- doc/tasks/20261002-dcc-public-backend-completion/g28-config26-readonly-preflight.sql
- doc/tasks/20261002-dcc-public-backend-completion/g28-config26-test.py
- doc/tasks/20261002-dcc-public-backend-completion/g28-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g28-original-g22-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g28_config26.py


- doc/tasks/20261002-dcc-public-backend-completion/g25-source-input-collector.py
- doc/tasks/20261002-dcc-public-backend-completion/g25-source-input-collector-tests.py
- doc/tasks/20261002-dcc-public-backend-completion/g25-source-input-collector-contract.json
- doc/tasks/20261002-dcc-public-backend-completion/g25-source-input-collector-prepared-plan.json
- doc/tasks/20261002-dcc-public-backend-completion/g25-source-input-collector-writer-attestation-template.json
- doc/tasks/20261002-dcc-public-backend-completion/g25-source-input-collector-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g25-source-input-collector-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g25-source-input-collector-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g25-source-input-collector-offline-final.log

- doc/tasks/20261002-dcc-public-backend-completion/g25-readonly-source-bytes.java
- doc/tasks/20261002-dcc-public-backend-completion/g25-readonly-source-bytes-test.java
- doc/tasks/20261002-dcc-public-backend-completion/g25-readonly-source-bytes-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g25-readonly-source-bytes-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g25-readonly-source-bytes-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g25-readonly-source-bytes-compile.log
- doc/tasks/20261002-dcc-public-backend-completion/g25-readonly-source-bytes-final-green.log

- doc/tasks/20261002-dcc-public-backend-completion/g23-source-upgrade-driver.py
- doc/tasks/20261002-dcc-public-backend-completion/g23-source-upgrade-tests.py
- doc/tasks/20261002-dcc-public-backend-completion/g23-source-upgrade-request-template.json
- doc/tasks/20261002-dcc-public-backend-completion/g23-source-upgrade-input-contract.json
- doc/tasks/20261002-dcc-public-backend-completion/g23-source-upgrade-prepared-blocked.json
- doc/tasks/20261002-dcc-public-backend-completion/g23-source-upgrade-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g23-source-upgrade-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g23-source-upgrade-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g23-source-upgrade-offline-final.log

- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-review-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-review-green.log

- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-driver.py
- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-tests.py
- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-contract.json
- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-postflight-package.json
- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-prepared-inputs.json
- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-session-environment-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-offline-final.log

- doc/tasks/20261002-dcc-public-backend-completion/g21-bpm-policy-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g21-bpm-policy-facts.sql
- doc/tasks/20261002-dcc-public-backend-completion/g21-bpm-policy-contract.json
- doc/tasks/20261002-dcc-public-backend-completion/g21_validate_bpm_policy_facts.py
- doc/tasks/20261002-dcc-public-backend-completion/g21_build_bpm_policy_contract.py
- doc/tasks/20261002-dcc-public-backend-completion/test_g21_bpm_policy_facts.py
- doc/tasks/20261002-dcc-public-backend-completion/g21-bpm-policy-offline-fixture.json
- doc/tasks/20261002-dcc-public-backend-completion/g21-runtime-eight-proof.json
- doc/tasks/20261002-dcc-public-backend-completion/g21-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g21-offline-final.log
- doc/tasks/20261002-dcc-public-backend-completion/g21-rendering-red.log
- doc/tasks/20261002-dcc-public-backend-completion/g21-runtime-validate-final.log
- doc/tasks/20261002-dcc-public-backend-completion/g21-runtime-rejected-preserved.json

- doc/tasks/20261002-dcc-public-backend-completion/g20-migration-execution-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g13-owner-review.md
- doc/tasks/20261002-dcc-public-backend-completion/g13-review-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g13-review-test-counts.json

- doc/tasks/20261002-dcc-public-backend-completion/ui05-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/ui05-test-counts.json

- doc/tasks/20261002-dcc-public-backend-completion/g08-b1-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g08-b1-test-counts.json
- doc/tasks/20261002-dcc-public-backend-completion/ui05-reference-usage-review-proposal.md

- doc/tasks/20261002-dcc-public-backend-completion/g07-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g07-test-counts.json
- doc/tasks/20261002-dcc-public-backend-completion/g07-migration-closure.json
- doc/tasks/20261002-dcc-public-backend-completion/activation-job-registration.md

- doc/tasks/20261002-dcc-public-backend-completion/p14-p15-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/p14-p15-test-counts.json

- doc/tasks/20261002-dcc-public-backend-completion/g04-backend-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g04-backend-test-counts.json

- doc/tasks/20261002-dcc-public-backend-completion/p05-p07-delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/p05-p07-test-counts.json
- doc/tasks/20261002-dcc-public-backend-completion/p05-p07-migration-closure.json

- doc/tasks/20261002-dcc-public-backend-completion/integration-notes.md
- doc/tasks/20261002-dcc-public-backend-completion/delivery-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/test-counts.json

- doc/tasks/20261002-dcc-public-backend-completion/g46-product-identity-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g46-product-identity-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g46-product-identity-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g46-product-schema-contract.py
- doc/tasks/20261002-dcc-public-backend-completion/g46-product-schema-contract.json
- doc/tasks/20261002-dcc-public-backend-completion/g46-product-migration-fullclosure.json
- doc/tasks/20261002-dcc-public-backend-completion/g46-product-identity-runtime-plan.md

- doc/tasks/20261002-dcc-public-backend-completion/g47-upload-template-plan.md
- doc/tasks/20261002-dcc-public-backend-completion/g47-upload-template-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g47-upload-template-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g47-DccUploadEndpointHttpContractTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g47-DccFileTypeTaxonomyAdminServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g47-DccControlledFileUploadApiTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g47-DccControlledFileWorkflowServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g47-DccPublicUploadPlacementHttpContractTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g47-DccSourceUploadContextTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g47-DccProjectFileTemplateServiceImplTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g48-storage-bdd.md
- doc/tasks/20261002-dcc-public-backend-completion/g48-backend-storage-projection-plan.md
- doc/tasks/20261002-dcc-public-backend-completion/g48-storage-fingerprints.json
- doc/tasks/20261002-dcc-public-backend-completion/g48-storage-verification-receipt.json
- doc/tasks/20261002-dcc-public-backend-completion/g48-storage-junit-archive.json
- doc/tasks/20261002-dcc-public-backend-completion/g48-storage-migration-fullclosure.json
- doc/tasks/20261002-dcc-public-backend-completion/g48-storage-runtime-plan.md
- doc/tasks/20261002-dcc-public-backend-completion/g48-DccControlledFileWorkflowServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g48-DccDirectoryAccessPermissionServiceTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g48-DccDirectoryAdminServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g48-DccFileCategoryAdminServiceImplTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g48-DccPublicUploadPlacementHttpContractTest.xml
- doc/tasks/20261002-dcc-public-backend-completion/g48-G48StorageProjectionTest.xml

- doc/tasks/20261002-dcc-public-backend-completion/g56-independent-interface-integration-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g58-task-doc-entry-role-ui-plan.md

- doc/tasks/20261002-dcc-public-backend-completion/g59-core-direction-independent-source-review.md

- doc/tasks/20261002-dcc-public-backend-completion/g59-shared-state-and-reconcile-independent-review.md

- doc/tasks/20261002-dcc-public-backend-completion/execution-log.md

- doc/tasks/20261002-dcc-public-backend-completion/task.md

- doc/tasks/20261002-dcc-public-backend-completion/verification-report.md

## P05/P06/P07 verification and handoff

BDD and implementation completed in the integration tree. P05 RED lost/misrouted logical browser rows; P07 RED allowed missing-reviewer submission; P06 RED rejected the first correction checkin from failed A/2. GREEN and directed regression are recorded in verification-report.md; no task completion or production-deployment claim. Root final closeout applies after shared runtime/UI verification.

## G20 readonly migration execution preparation Review

- Given the frozen nineteen candidate SQL files and protected historical migration rows, When the planned executor selects migrations, Then only the nineteen approved identities can execute, old base/catalog/seed remain excluded, and old ledger rows are untouched.
- Given dependencies already embodied in the real restored schema/data without recorded ledger entries, When candidate preflight evaluates dependsOn, Then it proves the target facts separately without inventing APPLIED migration history or replaying initialization.
- Given the sixteen affected original tables and seventeen new tables, When first/repeat MySQL rehearsal is prepared, Then original-column/old-key payloads are compared, permitted thirty-three new configuration rows are exact, new structural tables remain empty, and new nineteen ledger rows have an explicit append/rollback boundary.
- This batch is readonly source/backup/planner Review, not SQL execution or product change. No Java/SQL/config/frontend/other Owner files, Maven/services/database/Git. Findings and reproducible memory-only planner evidence recorded in g20-migration-execution-review.md.

## G21 BPM / policy / template target-fact contracts

- Given the eight delegated external migration prerequisites and actual protected metadata, When read-only target evidence is validated, Then only exact current tenant/key/id/version/XML/deployment/model/info/policy/template and required schema/unique identities satisfy those facts; no APPLIED history is invented. Historical V1/V2 may remain suspended under the current V3 chain, and disabled BPM_REQUIRED publication is superseded by exact current P4 DIRECT policy.
- Given missing, duplicate, wrong tenant/id/version/linkage or conflicting XML/policy/template facts, When offline validator runs, Then it fails explicitly and returns no overall satisfied receipt. Capture truncation/schema/source fingerprint mismatch also fails.
- Scope: own backend task directory only SELECT query/contract JSON/validator/offline tests/docs. No DB connection, DDL/DML, Java/SQL source/schema/ledger modifications, Maven/services/Git. Root executes the eventual readonly queries.

## G21 isolated rehearsal driver BDD/TDD

- Given no --authorize-rehearsal-writes, prepare/validate/rehearse refusal creates no transport/client/DB connection and performs no database writes. The CLI flag is a technical gate, never proof of actual user permission. Root holds real approval.
- Given exact Root19 materials/25 proofs/44 dependency closure, protected922schema+210data and finalized postflight package, When offline prepare/validate runs, Then every source/backup/SQL/proof/support/postflight raw fingerprint and fixed scope is checked; incomplete/drift/source-or-foreign DB rejects before any transport.
- Given explicit authorized rehearsal and fixed clone absent, When restore/first/repeat executes, Then only dcc_intqms_g18_rehearsal is created/written; source read facts/ledger remain untouched. First/repeat compare every original16-row payload, exact33 config addition identities, new19 ledger-only append, new17 empty, and identical finalschema contract.
- Given clone exists, changed proof/backup, partial postflight/identity or first SQL error, When execution attempts, Then stop at first failure, preserve protected stdout/stderr/step receipts and never auto-delete/rollback source/shared database.
- Scope only own g21-rehearsal-* files plus own reports. Root support modules imported read-only. Offline unit tests only; no actualdriver/DB/service/Maven/Git this turn. Original database upgrade intentionally has no mode.

## G21 rehearsal independent Review R01/R02 BDD

- Given original V3 full copied business columns including form_id/print_template_setting and procdef metadata, When V4 config insert is checked, Then every copied value's exact NULL/value byte digest matches the actual restored baseline V3 source, with explicit target identity/tenant/linkage/constants. Same33 counts and repeat-stable wrong copied fields cannot pass. Protected seed/current25 fact contracts stay frozen; this proves snapshot-copy equality, not current source form/business legality.
- Given snapshot_original_rows reads original columns/rows through frozen Root support, When a read succeeds or MysqlFailure contains private_error, Then a driver RecordingMysql adapter preserves phase/table/read raw bytes and exact private stderr/hash. Support modules unchanged, first-error stops, no error payload printed to stdout or silently lost.

## G23 source upgrade preparation BDD

- Given no real clone ISOLATED_REHEARSAL_PASS receipt with complete hashed protected step artifacts, When source prepare runs, Then it produces explicit missing-real-rehearsal blocked preparation without creating a database client; a status string alone is insufficient.
- Given fresh25 actual source proof, new independent source backup, actual16+ledger source snapshot and fixed target/materials, When offline validation runs, Then raw fingerprints/source UUID/19 whitelist/full clone first-repeat proof pass and maxAgeSeconds900 is enforced as a technical freshness gate, never user approval. Clone baseline cannot substitute for source snapshot.
- Given explicit --authorize-local-test-upgrade plus valid actual clone/source material, When future source execution runs, Then only fixed local ruoyi-vue-pro receives unchanged original19 firstSQL bytes, appended new19 ledger and exact33config,17empty andsource-specific postflight. No25audit/quality scope orclone-specific database guard is reused.
- Given absentflag/staleproof/writer/foreignidentity/already-creatednewtable/candidateledger/backup-orSQL drift/firsterror, When execution attempts, Then reject beforewrites orstopfirsterror/protectraw/stdErr; never restore/recover orstopstart someone else'sservice. This turn offline tests only; Root actualpermission/freshqueries/newbackups remainpending.

## G25 readonly source bytes reader BDD

- Given actual stdin-only configId28/storage20 S3 localhost endpoint with explicitregion/pathStyle and39 exactLong file identities/rootexpected SHA/size, When streamed GetObject completes, Then outputonly id/status/actualSHA/length/http/errorCode and all39 must MATCH for exit0. No persistentobjectbody/credential/key/URL/message orbucketwriteAPI.
- Given remote endpoint/regionmissing/pathStylefalse/wrongconfig/unsafeID/count/duplicate/invalidexpectation/unknownfield/argv, When inputvalidated, Then rejectbefore S3 client/GET creation with sanitizedinputerror andexit2.
- Given missing/denied/network/streamerror/actualSHA-orlengthmismatch, When requested GET runs, Then exactsanitizedfailurestatus/http/errorcode/actualdigestlengthwherecomplete andexit1; nofallback/retry/autofix orfalsematch. Inputcredential neverfile/arg/log.
- Only ownJava/tests/docs+temporary runtime libs/classes extractedfromhashverifiedG20Jar (no Maven/productionchange). ActualDB/configcollector/GET onlyRoot; offlinefakeGETstreams testsnotlegacybyteproof.

## G25 fresh source input collector BDD

- Given sealedG23/G21/sourceproofhelpers and realclone/userauth plus explicitRootwriter-exclusion attestation, When offline prepareplan runs, Then generateonlyreadonly identity/writer/schema/env queries and fixed freshcapture/backup interfaces with no client/DB/subprocess.
- Given Rootexecutes collector after writerexclusion, When actualsource reads/proofs/completeoriginalrow baseline/new3artifact dump run, Then every DB/UUID/query/facts/toolSHA/capturetime/backup epoch binds realsource (never copiedclone); exactG23 preparedinputs areverified diskonly, no upgrade invocation.
- Given missingclone/auth/attestation, existingdestination,writer/event/transaction,factsdrift/partialcapture/dumpfailure, When collectionattempts, Then reject/stop firstfailure, retain privateerror andnever printcredentials/sourcepayload/fakePASS. ThisAgent runs offlinefakesonly; actualDB/backup/upgrade RootsoleOwner.

## G25 core verified legacy occupancy BDD/TDD

- Given legacy NULLnameclaims/currentMaster/File/infra metadata plus complete trustedsealed evidence scope and exactbyte registry, When formalnameguard reads, Then all originallyscopedversions andsourceidentities remainmatching and scopecoveragecounts/keys complete before resolved; absent/partial/35MATCH4NoSuchKey/unknown/changedsource/schema fail without fallback. OldFile/Master/claim/signature rows untouched.
- Given verifiedhistorical samefullUTF8name with multipleoldMasters, When NEW claims/preflights, Then exactgroup rejects includingrawsameMaster NEW; case/ext/trailingspace distinct. Actual ownoldMaster/selectedversion/source/hash existing revision/checkin may use verifiedgroup but not create modernunique aliases or guessedformalidentity. Exactmodern own-unsentNEWdraft replay requires complete serverdraft/sourcebody/actor context.
- Given readonlyprojection oldnamefieldNULL, When validatedscopedversion queried, Then return only exactverifiedevidenceoriginalname; modernfieldconflict/foreignsource/missingproof rejects. No historicalbackfill.
- Given modern/legacy namespace race and latecallerfailure, When finalclaimtransaction executes, Then stable tenant/fullbinary registryunique+modernunique both atomically protect; losers explicitlyfail, no partialrows or silentduplicate success.
- Given legacyowner actualobsolete20calendar-year deadline, When retain/release, Then sidecarevidenceonly updated and group remainsoccupied until everyowner released and no active/pending/WORKING/inflight source; lastowner can free stablegroup, oldclaims/history preserved.
- New standalone3table forwardDDL only, independent frozen19; registrationINSERT reviewed forRoottrustedsealedall39proof, no clientMATCHbool, noREST/UI/BPM/platform/job/newaudit25 edits. ActualRoot39body=35MATCH4NoSuchKey blocksactivation; developmentalH2fakeproof notactualregistration.
