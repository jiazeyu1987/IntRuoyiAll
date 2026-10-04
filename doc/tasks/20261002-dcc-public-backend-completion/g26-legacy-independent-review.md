# G26 independent legacy occupancy and recovery caller review

## Scope and evidence boundary
Readonly review of integration legacy NameClaim service/SQL/mapper/migration/tests and the existing registration proposal. No production edits, Maven, real DB, object operation, service or Git by this reviewer. Production owner is `legacy_occupancy_core`; Root owns actual migration/registration/recovery. Findings describe the source observed during concurrent development; owner fixes and actual test evidence must be reviewed before closing them. These are static findings, not claimed live business reproduction.

## R01 — P1: outer evidence can borrow another scope's verified claim
Observed `DccSourceNameReservationMapper.java` methods `verifiedNames` (line 16), `verifiedOwnerName` (line 18), and added `activeLegacyNumberOwners` (line 24) select outer evidence `e` but only ask whether its claim `c` satisfies `RESOLVED_CLAIM`. That nested expression may select any VERIFIED scope; it does not bind the outer evidence's `verification_scope_id` to that same verified scope.

Trigger: a claim/file is complete under VERIFIED scope 1, while PREPARED scope 2 contains the same claim/file and a different candidate original name. Scope 1 still makes `c` resolved, so scope 2's unactivated row can appear in `verifiedNames` or be counted as a verified owner. `requireSourceName` then receives two names and rejects an otherwise valid historical source; other unverified names can be incorrectly considered owned. Preparation must never change current effective identity.

Required repair: outer evidence belongs to an explicitly VERIFIED valid scope and the exact coverage expression binds that scope/claim. Merely joining a scope status without full valid coverage is insufficient. Add actual Mapper/H2 tests with two scopes: PREPARED different-name same-file evidence must not change `countUnresolved=0`, sole verified-name result or owner/number decisions. Owner has received the issue and added the two-scope regression; its RED/GREEN was still pending at review snapshot.

## R02 — P1 retained original number must use the old Master's formal identity
The actual pre-C sealed claims have no `dcc_project_code_id`, `file_type_taxonomy_leaf_id`, or `normalized_file_number` fields. The C migration adds them DEFAULT NULL and preserves all old rows. The sealed Masters have full formal identity for 19 of 25; six are incomplete. Therefore new-name-only legacy occupancy cannot rely on `selectActiveByNumber` reading old claim fields to reserve these existing logical numbers.

Required current-scope guard: any other active verified legacy owner with the same frozen formal project/leaf/number blocks a new logical file even if it uses a different filename. Read the explicit Master facts in evidence, never infer the missing six. Owner has added `activeLegacyNumberOwners` and a true migration-shape fixture whose old claim formal fields remain NULL. R01's scope binding also applies to this new query. The original Master fields/claims must remain unchanged.

Clarification of initial reviewer message: with a generic old claim whose formal three fields are non-NULL, `releaseExpiredIdentity` preserves `deleted=0` while `selectActiveByNumber` and `uk_dcc_c_number` continue reserving its number; this can permanently prevent number reuse. **That generic shape is not established for these actual 25 migrated claims.** The original sealed dump lacked the new columns; Root must verify actual current NULL counts instead of treating this inference as a fresh DB query.

## R03 — existing end-of-retention number reuse is a broader C lifecycle gap
`DccControlledFileWorkflowServiceImpl.loadOrCreateMaster`, lines 2546–2552, rejects NEW whenever `selectByNewLogicalIdentity` finds a historical Master. `20260906_dcc_new_file_lifecycle_p1.sql` creates `uk_dcc_new_logical_file_identity` on tenant/project/leaf/number permanently, independent of release status. Thus a sidecar-only service test that manually creates a second Master with the same number does not prove the actual public NEW flow can reuse the number after all required 20-year releases.

This predates the new sidecar and requires Root's explicit lifecycle/identity design review. Do not weaken the old modern unique constraint, mutate historical Master/claim identities, or claim a selector query alone fixes it. Current legacy tests can prove sidecar retaining/releasing correctly while explicitly leaving the broader public end-of-term reuse unverified. Root and owner have been informed.

## Checked directions that are correct
- `claimIdentity` defaults to server-derived NEW; legacy group rejects it even if Master IDs happen to match. The explicit existing-version context validates selected persisted tenant/Master/formal identity and verified source name. Modern draft replay requires actor/source/actual unsent NEW identity instead of a same-Master bypass.
- Partial nullable formal identity comparisons in `VALID_SCOPE` explicitly test NULL-vs-value for old claim and Master project/leaf/number; missing source File/infra keys are explicitly rejected. No raw NULL global allow flag was introduced.
- Existing `requireLockedLifecycleFile` acquires Master before File; claim/retention/release acquire Master before name. Same-Master operations therefore share the authoritative serialization lock. Shared names do not introduce a reverse cross-Master lock in the inspected paths. Activation's multiple-Master/name ordering is still proposal-level and needs Root final implementation review.
- Release requires actual complete verified evidence, expired actual retention, no current active pointer, and terminal version statuses. It releases only this owner’s evidence; shared name registry becomes NONE only after no unreleased owner remains. Missing actual obsolete time remains occupied. New modern reuse may bind the stable registry after final owner release while old evidence/claim stay preserved.
- Existing old-name source projection reads evidence, does not backfill File; original active name/number constraints remain strict for modern data. Different case/extensions use complete binary key.

## Configuration identity limitation, not a new current-scope stopper
`VALID_SCOPE` checks current infra id/configId/name/path/size and saved File source SHA, but not current `infra_file_config` endpoint/bucket/region/pathStyle/storage/deleted values; the stored `metadata_identity_sha256` is length-checked, not re-derived from current locator config. A changed configuration could point the same key to another object even though those SQL checks pass.

For the actual current scope, every source uses config 28 and `FileConfigServiceImpl.updateFileConfig/deleteFileConfig` formally rejects the protected `ShowroomProtectedFileRules.FILE_CONFIG_ID=28`. Root has also frozen the actual bucket SHA/endpoint/region and source storage facts. Therefore this is not a demonstrated normal API drift path for the current batch. Registration should freeze these actual non-secret locator facts and retain the limitation; future generic scopes using unprotected configs cannot claim the same protection without an explicit guard. Do not add an unrelated infra compatibility patch based on this review.

## Tests and registration plan still needing final owner/Root review
The current inspected tests cover real Mapper counts, many incomplete/NULL/source-drift mutations, exact names and rollback/NEW concurrency. Owner has been adding a real two-scope PREPARED contamination test, full multi-owner/20-calendar-year expiry fixture, and registry race fixture. Review their actual RED/GREEN and source changes; inspection of test names is not PASS.

No formal activation API currently exists: `DccLegacySourceNameOccupancyService` is read-only diagnostics. The old proposal's prepare/activate API remains a proposal. Root registration must prove the exact sealed primary-key sets plus all 39 actual body proofs, live metadata preimages, actual protected locator identity, actual user historical policy, permission and real configuration audit in one transaction. Scope/evidence hash strings and counts alone do not authenticate client JSON claiming MATCH. New standalone migration/registration are separate from the already executed 19 migrations.

## Recovery caller review (Root master script)
Reviewed `g26-object-recovery-runner.py` without editing it. Three initial fixes were sent to Root and the latest source was re-read:

1. Exact error-code whitelist now replaces ASCII-only validation; HTTP and actual write flags have exact scalar-type checks. SDK error strings cannot become printable custom secrets.
2. After Java exits, Root writes a safe uncertain receipt containing exit/time/stdout/stderr lengths and SHA before parsing, so unexpected stderr/schema failure cannot leave the action falsely RUNNING or imply zero effect. Raw diagnostics are not persisted.
3. Original source versions and current metadata use exact four-ID/four-version sets, without dictionary overwrite shortcuts, and compare `versionNo` plus source identity to sealed facts.

At final readonly recheck, no remaining stopper was found for the bounded three-object recovery caller. Java additionally enforces exact bucket/config/key/body scope, all-three 404 preflight and no unconditional retry. Actual user permission must arrive before recover mode; no actual object action by this reviewer. After the three-object recovery, Root still must re-run independent 39-object proof before marking complete legacy bytes verified.
