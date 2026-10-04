# G26 internal legacy registration independent review

## Scope and current status
ready_for_closeout — Bounded registration/core software review finished against frozen owner delivery and immutable final regression evidence. Root actual deployment/complete39-source proof, policy quality approval and legacy activation remain pending; a new separately assigned selector read batch is excluded from this frozen review. No production edit, Maven, actual DB or network action by this reviewer. Legacy owner alone writes/compiles/tests; Root performs actual scope registration after review and explicit authorization.

## Review contract
1. Entry requires real authenticated ADMIN context, current tenant matching that context, actual enabled same-tenant user, actual doc_control role and `dcc:controlled-file:update` permission. Missing context never falls through to Gxp's SYSTEM_ACTOR helper. Actor identity and server time come from formal sources, not caller actor/timestamp fields or invented QA identity.
2. Trusted Root sealed evidence source is explicit. Caller cannot send `bytesVerified=true`, fake MATCH rows, arbitrary filesystem receipt path or mutable client JSON and thereby activate. Original raw evidence/proof/query hashes, exact IDs and actual completed 39-source GET receipt are joined. A 35 MATCH/4 NoSuchKey receipt is categorically incomplete.
3. Exact frozen primary-key sets, all versions/source relations, expected/actual body SHA and sizes, actual metadata and non-secret object locator preimages match the current locked source. Duplicate, missing, extra or conflicting aliased rows fail before configuration writes; six incomplete formal identities are preserved as facts rather than inferred.
4. Actual transaction writes only new scope/evidence/name registry plus a distinct real Gxp configuration audit operation `dcc.controlled-file.legacy-name-occupancy.activate`. Audit append failure rolls all of these back; no legacy File/Master/claim/signature/BPM or original 25-policy edits. Same identity replay returns same receipt with no second event; same request/scope with changed payload rejects.
5. Lock order is stable Master/claim then full-byte name order. Existing MODERN occupied names conflict; no upsert overwrites it or same-Master shortcut. Activated legacy multi-owner/multi-name edges remain complete. Exact registry conflicts must result in rollback, not partial configuration.
6. New technical operation's candidate rule/hash is reviewed separately. The frozen G22 original 25 rules remain byte-identical; no fictitious quality approval timestamp/reference/signature. Prospective candidate evidence does not imply policy activated, real audit append permitted or deployment ready.

## Existing dependency observation
`GxpAuditServiceImpl.resolveActor` intentionally creates SYSTEM_ACTOR when login is missing, and missing actor info resolves names to SYSTEM_ACTOR. New activation must reject absent context before append and provide or validate current real authenticated identity; this is an entrypoint constraint, not a request to modify the shared audit subsystem. This observation was sent directly to the sole owner before implementation.

## Required evidence boundary
Static source review alone cannot close transaction rollback, trust provenance or concurrency. Final report will cite exact source fingerprints and owner actual RED/GREEN/rollback/replay proof once available. Root actual 39 source bytes and runtime configuration remain independent gates.

## Latest actual authorization boundary
Root reported the user answered “尚未批准” to the concrete three-object recovery request. No object restoration is approved or executed by this child. The original 39-object receipt remains 35 MATCH / 4 NoSuchKey and must never activate a full VERIFIED scope. This does not undo the already authorized/completed 19 database migrations or prevent authorized local code/tests/review.

Readonly recovery-launcher boundary review: the exact Root `g26-object-recovery-authorization.json` is currently absent, and recover mode checks that exact file before its DB metadata query or Java launch. The existing condition requires JSON `actualUserApproval is True`; false, missing approval, or a plain “尚未批准” answer cannot satisfy it. Reviewer did not create authorization records, launch the runner, or edit Root-owned assets.

## Source review findings and owner feedback

### R01 — raw protected artifacts need actual proof linkage
Initial `DccLegacyNameVerifiedScope.fromProtectedArtifact` checked only the manifest raw hash supplied by its internal caller. Facts/bytes/user-decision hashes and verifiedAt were metadata strings; no corresponding receipt/results had been consumed. Owner was already implementing stronger verification and was explicitly notified that the private constructor alone is not proof of provenance.

Latest source now reads actual fact/receipt/result files, requires SOURCE_BYTES_VERIFIED, sourceBytesVerified=true, reader exit 0, matching actual DB identity, results hash, sourceFacts hash and complete one-to-one result SHA/size/HTTP=200/status=MATCH/error=null. Receipt completion time is converted to Asia/Shanghai and must match stored verifiedAt; exact scope identity hash is re-derived. Manifest JSON rejects duplicate/unknown/trailing/coerced floating identities. This closes the initial source-level omission; actual negative tests and trust caller review remain pending. Current 35/4 actual receipt cannot satisfy these checks.

### R02 — exact live coverage must be rechecked under acquired locks
Initial `assertExactLiveScope` ran before Master/claim locks only. A concurrent permitted write between the set query and lock acquisition could make a complete earlier primary-key set stale. Owner now rechecks exact current NULL-claim/all-version sets after acquiring canonical Master/claim locks. Source-level fix is present; the actual concurrency test needs review before marking behavior verified.

### R03 — artifact hash and parsing must use the same captured bytes
Latest `verifyArtifacts` calls `rawHash(path)` then reads the same artifact again for parsing (facts, bytes receipt and results). The second read can observe changed bytes after the first hash check; the word “protected” in a method name does not freeze a file. The manifest loader already demonstrates the correct pattern: read bounded bytes once, hash and parse that exact immutable buffer.

Owner's latest source now reads facts/receipt/results/user decision once into bounded byte buffers, hashes and STRICT_JSON-parses those same snapshots. Result fields are exact and UTF-8 malformed decoding fails; evidence list is copied immutable. Source-level R03 is closed; actual owner test evidence/final source fingerprints remain pending. No reviewer code edit or real filesystem race was injected into Root assets.

## Inspected implementation directions
- `authorize()` executes before SQL or body reads; it rejects no login, wrong tenant/user type/visited tenant, mismatched actual enabled user/id/names, missing doc_control or update permission. This keeps the shared Gxp SYSTEM_ACTOR fallback unreachable from this entrypoint.
- `activateVerifiedScope` requires a real Spring transaction, verifies actual source environment, locks Master/claim in sorted order, checks row preimages, and re-reads actual source bytes through FileService before any scope/evidence/registry INSERT. These are actual formal boundaries, not a client boolean or test-only success flag.
- New name groups use byte order, reject an existing registry or modern claim, insert PREPARED/evidence/groups, activate VERIFIED, verify complete read guards, and append a distinct Gxp configuration event within the same annotated transaction. No REST, Runner, QA approval or old-file mutation was added in the reviewed source.
- Replay requires the exact manifest hash and VERIFIED stored scope and uses deterministic Gxp idempotency key. It does not overwrite the frozen old manifest or invent a second approval; actual rollback/replay/append tests remain pending.
- Default before-state is ABSENT and after-state describes scope/IDs/hash rather than keys/credentials. Audit receipt must be non-null. Runtime policy absence must throw/rollback; no old frozen G22 candidate may be silently reused as new quality approval.

No service behavior is marked PASS solely from these source observations. Final review awaits owner tests and frozen sources, and Root actual source evidence/authorization remain independently unresolved.

## New test source inspected (results pending)
`DccLegacySourceNameRegistrationTest` uses actual Spring transaction, real Mapper and actual GxpAuditServiceImpl with isolated H2; authentication/user/storage/environment ports are intentionally isolated doubles. Positive registration/replay compares old row Maps unchanged and actual audit actor, event ID and single ledger event. Current source 404/mismatch, live original preimage drift, foreign source ownership, absent context, inactive account, missing role/permission, foreign/visited tenant and mismatched login names are covered. Official Gxp event INSERT CHECK fault injects a real late append failure and asserts scope/evidence/registry/event/sequence rollback.

Actual incomplete 35-of-39 receipt mutation is explicitly rejected before registration and storage; duplicate result and denied historical-policy decision tests are present. One fixture's successful scope has one complete source; this validates generic unit behavior, not actual 39-body readiness or actual Root registration. Test name `loaderRejectsDuplicateActualResultAndMissingActualVersionScope` currently mutates only duplicate results; true missing-version/extra-set negatives should be explicit separate mutations rather than inferred from its name. First absent-key activation↔NEW INSERT race still needs dedicated actual transaction coverage.

Owner registration plan currently retains old text saying no method/annotation/implementation and suggesting a future Runner. Root has now authorized the internal method while explicitly keeping no REST/Runner; owner should update the final plan around actual current implementation and mark earlier design notes historical instead of leaving contradictory current-state claims. This is a document closeout gap, not permission to add a Runner.

## Actual first owner test result observed — not GREEN
The first owner Surefire report observed at 2026-10-03 20:12 reports **20 tests, 0 failures, 20 errors, 0 skipped**. The first fixture fails before the business method: `SecurityFrameworkUtils.setLoginUser(login,null)` in test setup creates WebAuthenticationDetails and dereferences the null HttpServletRequest. Later setups insert the same `dcc_controlled_file_source_ownership(tenant=1,controlled_file=20)` after the failed fixture left a row; the unique owner key rejects it. These are test setup/isolation errors and do not prove registration behavior failed or passed.

Owner and Root were notified immediately. Repair the isolated actual security request context using MockHttpServletRequest and clean task-owned fixture/source ownership across failed setups; then rerun real transaction/kernel/rollback tests. Reviewer neither modified tests nor ran Maven. Until that corrected owner result is supplied, this review cannot mark activation/replay/rollback GREEN.

## Subsequent real owner results / latest expanded run
Owner corrected fixture request/cleanup and strict formal JSON fixture serialization: manifest verifiedAt is an explicit ISO local Shanghai string derived from the actual UTC receipt; results use a dedicated ObjectMapper retaining `errorCode:null`, matching the real six-field source-byte reader. The loader protocol was not weakened to accept inferred epoch time or absent fields.

The corrected 20-case Surefire report subsequently recorded **20 tests, 0 failures, 0 errors, 0 skipped**, 15.29 seconds. This is actual isolated owner test GREEN for that version and replaces the first fixture-error report for that scope only. Actual 39-source readiness remains 35/4 and no real registration is implied.

Owner then added true missing frozen-version proof and first registration/concurrent NEW transaction tests (22 cases). The expanded 17-class run's registration report at 20:37 recorded **22 errors**, with fixture injection `fileMapper` resolving the infra mapper bean rather than DccControlledFileMapper. Source was already corrected to `@Resource(name="dccControlledFileMapper")` at reviewer inspection; a corrected expanded final run/fingerprint remains pending. Do not splice earlier 20-case GREEN into a claim that the expanded 22-case run passed.

## Final frozen software conclusion
The owner corrected that injection and completed **17 classes, 579 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS at 20:51:24, actual Maven exit 0. Independent review re-parsed the immutable `g26-final-regression.log` class summaries and verified the owner receipt/hash: raw log SHA `05f3b9aaa5cacb64fe685d1899437094151b62ea18a2cddd7ce32a09f190594a`. This includes registration **22** and legacy occupancy **39**, both actual isolated GREEN. Earlier fixture-error reports remain historical, not current failures or fake PASS.

Frozen manifest `g26-legacy-core-delivery-fingerprints.json` identifies current registration service/strict sealed factory/environment, NameClaim/SQL/mappers/time-precise DDL/tests. Source-level R01/R02/R03 are closed against that frozen source and actual supplied isolated tests. Registration proof handles six explicit fields including errorCode:null, ISO Shanghai verified time derived from actual UTC completion and microsecond protocol, exact live source/current locks, real authenticated actor/Gxp append rollback/replay and the real first registration/concurrent NEW transaction test. Explicit missing frozen version test is now present rather than merely a misleading test name.

Reviewer checked strict PermissionApi implementation: `hasAnyRoles` checks actual enabled role codes and does not use the separate super-admin shortcut method. New activation therefore does not assign admin document-control authority merely by account name. Absent authentication still fails before source reads. DDL inventory latest SHA `621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a` retains 8 DATETIME(6) columns with matching CURRENT_TIMESTAMP(6) defaults, 70 columns/2 generated/7 checks/8 closure PASS.

Owner next selector read repair legitimately changes Query and occupancy test after this freeze and may overwrite current Surefire XML with its new RED. It does not invalidate the immutable 579-run evidence for the delivered old scope, nor may its new failures be hidden by reusing that 579 claim. Independent final receipt records this expected later drift separately.

The result is **software isolation/review GREEN only**. Actual source39 stays 35 MATCH/4 NoSuchKey, restoration not approved, new3table first/repeat not executed, policy quality not approved, actual configuration activation/UIE2E/merge not performed. Public post-20-year original-number reuse remains separately documented wider lifecycle design boundary, while the confirmed 20-year occupation itself is retained. No quality signing or live result is fabricated.
