# G27 prospective 26-operation audit configuration review

## Result and exact delta
prepared_for_root_review — No authoritative policy edit, SQL execution, database action, quality approval or business action by this reviewer. The actual formal parser compared original frozen 33-operation policy against prospective 34-operation candidate and proved **all 13 fields of every original 33 operation remain identical**. Only `dcc.controlled-file.legacy-name-occupancy.activate` is added. Original G22 25-operation impact/receipts are preserved and are not overwritten or called approved.

| Item | Exact value |
|---|---|
| Original authoritative candidate raw SHA | 776905347c7726983db317eda110908f6762aa1351a8564e1e130da19b0d59df |
| Prospective 34-operation candidate raw SHA | 661af676e1406e86659806af8be8f46abd17d101d871d3af8d5b3b736873c894 |
| Prospective canonical source coverage SHA | 3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d |
| Policy version | 2026-10-dcc-integration-01 |
| Approval header | PENDING-REVIEW-20261001 — still an unapproved placeholder |
| Formal total policy operations / literal source annotations | 34 / 12 |
| DCC total operations | 27: existing publish + 26 missing configurations |
| New operation | dcc.controlled-file.legacy-name-occupancy.activate |

The candidate lives at `doc/tasks/20261002-dcc-detail-integration/g26-gxp-policy-prospective.yaml`. Formal source gate on the unchanged authoritative original is **expected RED** because the real new literal annotation is not registered; evidence is `g27-audit-original33-red.log`. The same actual formal gate on prospective candidate is **GREEN**, 34/12 and the SHA above; evidence is `g27-audit-prospective34-green.log`. This comparison imports/runs the repository `IntRuoyiBackend/script/gxp_audit_coverage_gate.py`; it does not reimplement an abbreviated parser.

## New exact operation payload
| Field | New value |
|---|---|
| operationId | dcc.controlled-file.legacy-name-occupancy.activate |
| sourceType | SERVICE_METHOD |
| sourceLocator | cn.iocoder.yudao.module.dcc.service.file.DccLegacySourceNameRegistrationService#activateVerifiedScope |
| domain / subjectType / actionType | DCC / DCC_LEGACY_SOURCE_NAME_SCOPE / ACTIVATE |
| reasonPolicy / signaturePolicy | REQUIRED_CATEGORY_AND_TEXT / NOT_REQUIRED |
| statePolicy / retentionClass | ABSENT_TO_PRESENT / GXP_CONTROLLED_DOCUMENT |
| testIds | [LEGACY-ACT-01, LEGACY-ACT-02, LEGACY-ACT-03] |
| owner / applicability | dcc-owner / GXP |

The actual method and literal annotation exist. Its immutable raw source/test fingerprints are in the owner's G26 delivery bundle. Real Gxp append, authenticated same-tenant document-control actor, scope/evidence/registry rollback and replay were exercised in isolated Spring/H2 registration tests. Final G26 immutable log records 17 classes/579 tests, with registration22 and occupancy39 all pass/no skip. Later selector batch's new RED can overwrite current Surefire XML; use the frozen final log/receipt for this scope, not a sum of stale/new report XML.

NOT_REQUIRED describes this technical configuration event; it does not waive the separate actual quality electronic signature needed to activate a policy. No actor, approval time or QA signature is fabricated. Existing admin was supplied as proposed approval account; the user's actual response is **尚未批准**.

## Exact prospective database impact
Machine-readable full payload is `g27-audit-26-operation-impact.json`:

- Tenant 1 / real local `ruoyi-vue-pro` only, subject to fresh source UUID/schema/preimage confirmation.
- Maximum **26 INSERT** rows in `gxp_audit_policy_operation`; no UPDATE/DELETE and exact repeat adds zero. These are original missing25 plus the one newly required operation.
- Existing `dcc.controlled-file.publish` actual row remains on `2026-09-approved-01`, byte-for-byte unchanged. Other operations/tenants, original old quality version and business/history/BPM/signatures remain untouched.
- At most **one separate candidate-version registration** in `gxp_audit_policy_version`, only after genuine exact-candidate/hash/source-coverage approval facts are ready and that concrete write is authorized. Operation installation does not create approval facts or directly insert audit event/history.
- No runnable SQL is prepared/executed by this subtask. Actual DML tools must independently implement preimage/conflict/permission/audit/repeat/rollback checks against the final approved candidate; existing G22 exact25 script cannot simply be widened without review.

Original user's conditional exact25+max1 authorization does **not** silently authorize a 26th operation or approval of a changed raw policy hash. It remains a historical approved scope whose quality condition is unmet. The new candidate must be reviewed as a concrete 26-operation scope; do not copy old approval reference, lower count to hide the new annotation, or use publish/project configuration operation IDs to audit unrelated name registration.

## Root's reviewable transition
1. Verify this delta payload/actual method/tests/immutable log and raw original/prospective policy hashes; confirm all original 33 operation fields unchanged and only the new technical action added.
2. Root may perform the reversible code/config edit replacing the **unapproved authoritative candidate** with these exact prospective bytes within the already authorized development goal. Preserve original G22 frozen artifacts and annotate supersession; never relabel old source coverage hash as the new one. This child does not edit it.
3. Re-run formal actual coverage on authoritative final path and refresh exact impact/count/hash/source closure. Prospective GREEN and a copy operation alone do not register runtime policy rows.
4. Review genuine quality account/time/record/signature basis for exact final candidate and coverage hash. Currently not approved; no guessed admin approval timestamp/reference and no quality registry row.
5. Prepare the concrete schema/preimage/backup/isolated first/repeat/conflict/runtime effect plan for 26 operation rows and max1 quality-version row, then Root follows the actual approval boundaries. Actual configuration execution, new sidecar DDL, full39 source registration and UI E2E remain distinct actions.

## Coverage limits and remaining runtime facts
The formal gate validates declared source locator existence and literal `GxpWriteOperation` annotations. Its canonical report covers policy version/operation ID/source type/locator/domain and discovered literal locator; this is not a scan proving every dynamic call, job, script or database writer, nor proof that the current runtime Jar/DB policy is registered. Full operation-payload comparison separately checks all 13 fields, including reason/signature/state/retention/test/owner/applicability that the report hash does not encode.

Actual legacy source scope still has 35 MATCH and 4 NoSuchKey, and the exact three object restorations remain unapproved. New registration rejects that incomplete receipt before any sidecar write. Existing G26 isolation tests/prospective source coverage do not prove actual byte restoration, long-term WORM, policy quality approval, actual application startup, all frontend lifecycle business E2E or final worktree merge.

## Root code/config adoption note (later state, separate from preparation receipt)
Root has now adopted the exact prospective bytes as the authoritative **unapproved development candidate**. Its raw SHA is `661af676e1406e86659806af8be8f46abd17d101d871d3af8d5b3b736873c894`; the actual formal gate on the authoritative path passed 34 operations/12 literal annotations with canonical report SHA `3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d`. Original 33-policy bytes and G22 preparation artifacts were preserved by Root as historical evidence; this child did not edit them or overwrite its earlier preparation-stage receipt.

This reversible source configuration adoption is not QA approval or database registration. Actual 26-operation write authority is not extended from the original25 scope; no quality time/reference/signature was invented, no operation/version rows were written, and object restoration remains unapproved. The earlier expected RED records the original33 stage and does not describe the now-adopted candidate.
