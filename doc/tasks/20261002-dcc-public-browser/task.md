# DCC public project browser integration

## Objective
Connect the public browser to authorized project discovery, logical folders, controlled candidates, pinned references and selected-version operation entry.

## Milestones
1. Read rules and frozen contracts; inspect existing public page and independent modules.
2. Record BDD, execute targeted RED, implement project panel and exact operation entry.
3. Execute focused Node/Vue wrapper tests and local lint; report integration boundaries to Root.

## Expected Verification
- Targeted Node tests of actual SFC handlers and API wrappers with transport fixtures; not E2E.
- Local ESLint on owned production files and git diff --check.
- Root owns full type/build/backend verification. No Maven, E2E, services, real DB or Git commit/push here.

## Current Status
in_progress

## BDD
- Given authorized projects including empty projects, when opening project browsing and a logical folder, then getProjectDiscoveryPage and getProjectFolders/buildProjectFolderTree supply identities; the browser uses projectFolderId and keeps server total.
- Given global browsing, when searching, then no project or folder filter is sent and old requests cannot overwrite a new scope.
- Given a target project and current enabled actor, when referencing/canceling, then only projectLeaderUserId equality grants UI write entry; pinned selected IDs, exact cancellation and server project counts are retained.
- Given a controlled source, when inspecting use, then source color/count comes from usage; name access never grants preview.
- Given a legal older WORKING version with edit permission and no checkout, when opening its application, then that exact string ID enters detail; latest-only frontend gate is absent and server guards remain.
- Given an API contract error, when loading projects/folders/files/references, then the current visible area reports the real failure and does not substitute an empty success.

## 设计约束检查
- Branch verified codex/20261001-dcc-integration at C:/IntRuoyi/20261001-dcc-integration; main directory and original ABCD trees read-only.
- Own browser/index.vue, independent browser/project components and tests only. Shared wrappers/detail/backend coordinated with their owners.
- Reference selected version is fixed; relation current uses latestControlled including pending; historical snapshots are separate.
- Long identities remain strings; projectFolderId is not NAS directoryId.
- No role/OWNER/admin bypass; enabled actor comes from official enabled-user endpoint, not frontend role inference.
- User authorization for parallel agents came through Root. No nested agents.

## Added BDD for upload handoff
- Given accepted project A folders remain while project B confirmation is pending, when computing folder tree or cancelling, then no A rows are validated under B and cancellation restores A unchanged.
- Given a pending folder request, when the project is cleared, then request generation invalidates it and loading is immediately released.
- Given an identical folder ID with a different projectCodeId, when finding the selected folder, then it is rejected.
- Given an owned earlier WORKING version, when the server explicitly marks actionProjection.actionLocked, then the application entry remains disabled.

## Open dependencies
- The existing selector API is latest-controlled only. Root requires a distinct all-file project browser projection including WORKING and in-flight versions. Current new tab is explicitly labelled controlled files; it is not accepted as the full requirement.
- Current relation response omits canPreview/project/folder display fields and editable capability. Backend advised formal detail composition for preview; full relation parent integration remains to coordinate with detail Owner and Root.

## G03 integration continuation

The backend_closure agent has frozen browserScope GLOBAL / PROJECT_FOLDER at the existing browser-page URL; the complete contract is in the backend completion task integration-notes.md. This removes the first open dependency. Root is coordinating the frontend transition; no frontend child is recorded as started yet.

BDD: Given the same logical Master has WORKING, in-flight and controlled versions, When the public project folder/global list loads, Then browserScope returns and displays every authorized version with a distinct controlledFileId row identity, real SQL total and truthful lifecycle/body permissions. Reference/operation selectors continue selectorScope and latest-controlled rules. Reject foreign tenant/folder or unsafe Long data without replacing it with empty success; keep stale-response rejection.

Review also found upload currently lacks editable signoff-department controls, and route-preview request still only defines selectedSignoffUserIds. Its final submission service already uses evaluateDepartments. Completing department selection must align preview with that actual set; do not let an obsolete default-only preview block a valid edited matrix or claim the edited route has been checked.

## G04 actual upload departments

Root is the current writer for upload and its API/helper/tests; no frontend child was started. Backend route-preview now accepts selectedSignoffDepartmentIds, and resolution uses the explicit selection before looking up removed default leaders. Null means configured defaults; an empty selection is invalid. The six affected backend classes pass locally.

BDD: Given the default matrix and enabled department directory, When category is selected, Then its actual departments populate an editable multi-select. When the user adds/removes departments, the preview and frozen submit payload use exactly that selection and preserve Long identity. A late response for old category/selection cannot mark the new form ready. Empty selection, unavailable department directory and unacknowledged readiness block submission. Existing upload confirmation and external-review scope remain truthful.

## G05 upload confirmation and approval closure

Root delegated upload/index.vue, upload/submitter.ts, upload/signoff-departments.ts and upload-only tests to upload_closure. Root retains shared API, runtime, DB, full type/build, E2E and Git ownership. The task remains in_progress; this delivery only supplies ready_for_closeout evidence to Root.

BDD: Given a complete real upload with actual project attributes, logical folder, original source name, related versions, selected signoff departments and resolved matrix approvers, When the user clicks submit, Then the detached confirmation lists every v1.4 required fact and the separate controlled/effective date notice. Cancel preserves input and sends no submission; a change in actual attributes, approvers, file, folder or other confirmed context rejects the request.

BDD: Given the official enabled-user directory and MATRIX_APPROVAL resolved identities, When route preview is ready, Then the page displays those exact enabled accounts separately from editable signoff departments and preserves Long strings. Missing, duplicate, disabled, unsafe or unmatched accounts block submission with a visible reason; no admin/first-user substitute is introduced.

BDD: Given a new upload or an empty effective date during a name reset, When entering the effective date, Then it starts empty and requires explicit input; name resets do not fill it with today. An earlier date is not described as formally allowed while past-date handling remains unconfirmed. Version text distinguishes checkin working versions from formal partial/replacement approval.

G05 milestone: ready_for_closeout for Root review, with 42 upload tests, owned lint and whitespace checks passing. The task Current Status remains in_progress until Root closes the integrated runtime, E2E and cleanup obligations.

## G06 public document-control pending distribution

Root delegated workbench/index.vue, workflow/PendingWorkflowDistributionList.vue, workflowLifecycle.ts if needed, and their directed tests to upload_closure. Shared workflow.ts, applicationRead.ts, detail/browser/backend remain owned by others; G05 upload assets are preserved.

BDD: Given the current official account explicitly has doc_control and distribute permission, When opening the public workbench or refreshing its pending-distribution panel, Then the mounted component calls the real workflow-lifecycle pending-distribution API with the chosen remindersOnly boolean, displays separate controlled and preset effective dates, preserves server effective-date order/classification, and labels future controlled documents as forbidden for execution before their effective date.

BDD: Given super_admin without explicit doc_control, missing distribute permission, or a revoked account context, When rendering or refreshing, Then no pending-distribution read/navigation is authorized and no role helper promotes super_admin to doc_control. Formal Long selected file identity navigates unchanged to detail/{id}?mode=manage.

BDD: Given network/missing reminder configuration/invalid lifecycle evidence, When reading the panel, Then the error is visible and no empty-success count or old PENDING_MANUAL_DISTRIBUTION list replaces the failure. On refresh/re-entry a formally distributed item disappears; a late previous-context result cannot overwrite current rows or metric facts.

G06 milestone: ready_for_closeout for Root review. Mounted public workbench plus workflow-directed regression is 39 PASS, three owned files lint errors=0/warnings=0, and existing workbench static contracts pass. Shared task remains in_progress; runtime, real E2E, types/build, Git and cleanup stay with Root.

## G07 project browser default and reference-name color

Root delegated browser/index.vue, browser/ProjectBrowserPanel.vue, browser/project-browser.ts, relations/DccProjectReferences.vue and corresponding tests. applicationRead.ts belongs only to detail_closure; coordinated third browser wrapper argument carries { latestVersionOnly, status }, with no selectorScope change. G05/G06 assets remain untouched.

BDD: Given authorized files with latest controlled, WORKING, in-flight and historical versions, When the public project browser first loads, Then browserScope requests latestVersionOnly=true and shows the latest controlled view. When the user explicitly selects all versions, working status, an approval stage or a historical status, Then browserScope requests latestVersionOnly=false with the exact official selected status; server total/rows/identities stay unchanged, reference/operation selectorScope remains latest-controlled, and late old-filter responses cannot replace a current view.

BDD: Given a source Master has a formal referenced usage count or an actual reference entry exists in the folder, When displaying its filename, Then the filename itself is orange. When the final formal reference is cancelled and usage returns zero, Then the source filename restores its ordinary color and the exact cancelled entry is removed. Distinct-project usage, immutable selected version, enabled target-project leader authority and cancellation identity remain unchanged; badge-only color does not satisfy filename color.

Read-only boundary: report whether the existing project reference list has body/operation entry and which formal permission fields would be required if absent; do not invent canPreview or change referenced version identity to fill the gap.

G07 milestone: ready_for_closeout for Root Review. detail_closure delivered the third browser options wrapper and removed the shared API dependency; actual browser/relations/reference regression is 62 PASS, three owned files lint 0/0 and owned whitespace validation PASS. Shared task remains in_progress for Root's full integration obligations.

## G08 UI-01 saved pinned-reference read entry

Root delegated ProjectBrowserPanel.vue, project-browser.ts, DccProjectReferences.vue, project-reference-contract.ts and corresponding tests. Shared applicationRead/detail/backend remain other Owners; prior G05-G07 outcomes remain intact. browserVersionOptions return type must use the formal ProjectBrowserOptions union.

BDD: Given a saved reference points to selectedControlledFileId V and the source project-detail read is denied, When loading its reference row, Then the existing relation-permissions(V) supplies source project/folder and real canPreview after tenant/Master/project/file/version equality validation; no latest resolver or source getProjectDiscovery read occurs. Name/count/orange reference identity remain visible when canPreview=false and the body action is disabled.

BDD: Given a saved exact reference row, When browsing its body, Then the selected version is rechecked and the exact V opens the formal preview path. When requesting read-only trace, Then the selected version receives its separate stronger getControlledFile authorization; rejection remains visible without navigation or hiding the reference, and successful response identity must equal V before entering the existing browser traceability route. Preview success does not grant trace authorization.

BDD: Given source selection/context changes while permission or trace reads are pending, When old response completes, Then it cannot navigate or replace current reference-row evidence. Foreign tenant/Master/project/file/version permissions reject visibly; unsafe Long identities reject without coercion.

BDD: Given formal batch reference creation succeeded but the newly added permission-projection follow-up fails, When returning feedback, Then it explicitly states already saved/read failure, closes the completed creation chooser and requires a real reference refresh rather than allowing duplicate submission or treating the write as failed.

G08 UI-01 milestone: ready_for_closeout for Root Review. Actual browser/reference/relations regression is 71 PASS, four owned production files lint 0/0, and owned whitespace validation PASS. Saved-reference preview remains P15-authorized; readonly trace uses an independent formal strong-detail read. All-types/build, runtime and real E2E remain Root-owned, and shared task stays in_progress.

## G15 read-only 12-flow audit and UI E2E preparation

Root assigned a read-only current-source review and task-owned Playwright script/plan preparation. No production/source/API/Java writes, service start, actual E2E, secret reads, API/DB business actions, all-types/build or Git. Main task/goal matrix and G12/G13 reviews reside in the int_qms root and are read-only references; this task retains its own audit/plan/scripts.

BDD: Given current G10-G14 integrated sources and HTML flows 01-12, When auditing public entry, identity, permission and failure contracts, Then record current method/file anchors and concrete remaining prerequisites without repeating closed historical gaps or claiming real runtime success.

BDD: Given registered int_qms slot6 8067/48067 and credentials supplied only through runtime environment, When Root later runs the prepared script, Then login/navigation/business selection use actual visible UI and task-owned objects, no API/SQL/fetch/request/mock action substitute. Missing menus, accounts/signature/configuration/project/template/type/folder facts fail with explicit blockers; screenshots and trace begin only after login and do not persist credentials.

Expected preparation checks: Node syntax for task script, AST/structure checks excluding direct network/SQL/mock and secret-output patterns, documented flow scope/source anchors, and git visibility/Cleanup Keep. These are preparation checks only, not E2E PASS.

Latest user confirmation for planning: calendar Asia/Shanghai; advance reminder 7 days through workbench; activation check each minute. Shared Docker MySQL/Redis recovery is authorized to Root; this child does not operate Docker or deploy migrations/config.

## G16 public checkin working-version-only repair

Root assigned browser/index.vue and owned checkin handler/render tests only. Checkin generates a working minor version; formal PARTIAL/REPLACEMENT submission remains a separate specific-body detail action. No shared API/backend/selector/basic-data change; G15 read-only review/preparation continues afterward.

BDD: Given the real public checkin dialog, When selecting and submitting a checked-out specific file with a ready source ticket or explicit changed remark, Then only MINOR is visible and sent; no MAJOR control, large-version success text or formal revision submission occurs. Exact file/master/checkout/session/ticket boundaries and server-owned logical-folder inheritance remain required.

BDD: Given stale upload or write responses, unsafe/foreign checkin identities, another user's checkout or changed session, When the handler runs or finishes, Then no stale response can attach to/close a newer dialog or replace current UI facts. Real write success followed by read failure remains explicitly already saved; backend validation is not weakened.

BDD: Given version business wording on the public browser, When displaying current or latest controlled identity, Then use 受控版本/current executing controlled semantics; unrelated legal expiry terminology remains untouched.

G16 milestone: ready_for_closeout for Root Review. Current checkin-directed 24 PASS, two selected-WORKING entry regression cases PASS, actual browser SFC script/template compile PASS, browser/index lint 0/0 and diff whitespace PASS. Shared task in_progress; Root owns final types/build and runtime/UI validation.

## G44 bounded upload route blocker display

ready_for_closeout for Root review of this display-only increment; shared task remains in_progress. Given two formal blockers have the same message but different stages/people, When the actual upload readiness subtree renders, Then each displays returned stage number/name, actual person/name and exact account ID plus unchanged reason; missing facts are explicitly未记录. No guard, API, role or fallback changes. Effective RED3/4→GREEN4; affectedupload threefiles27PASS andowned eslint exit0. Root owns types/realHMR/mainline acceptance and finalGit/cleanup; oldG20seal retained, thissingleassetnewseal in g44-route-blocker-display-fingerprints.json.

## Cleanup Keep

- doc/tasks/20261002-dcc-public-browser/g49-project-application-entry-bdd.md
- doc/tasks/20261002-dcc-public-browser/g49-project-application-entry-fingerprints.json
- doc/tasks/20261002-dcc-public-browser/g49-project-application-entry-type-fix-bdd.md
- doc/tasks/20261002-dcc-public-browser/g49-project-application-entry-fingerprints-r2.json
- doc/tasks/20261002-dcc-public-browser/g49-project-approval-key-fields-bdd.md
- doc/tasks/20261002-dcc-public-browser/g49-project-application-entry-fingerprints-r3.json

- doc/tasks/20261002-dcc-public-browser/g48-frontend-single-folder-plan.md
- doc/tasks/20261002-dcc-public-browser/g48-frontend-single-folder-bdd.md
- doc/tasks/20261002-dcc-public-browser/g48-frontend-single-folder-fingerprints.json

- doc/tasks/20261002-dcc-public-browser/g47-free-file-upload-plan.md
- doc/tasks/20261002-dcc-public-browser/g47-free-file-upload-bdd.md
- doc/tasks/20261002-dcc-public-browser/g47-free-file-upload-fingerprints.json

- doc/tasks/20261002-dcc-public-browser/g46-project-product-frontend-bdd.md
- doc/tasks/20261002-dcc-public-browser/g46-project-product-frontend-fingerprints.json

- doc/tasks/20261002-dcc-public-browser/g44-route-blocker-display-bdd.md
- doc/tasks/20261002-dcc-public-browser/g44-route-blocker-display-fingerprints.json

- doc/tasks/20261002-dcc-public-browser/dcc-public-ui-acceptance.e2e.cjs
- doc/tasks/20261002-dcc-public-browser/verify-ui-acceptance-preparation.cjs
- doc/tasks/20261002-dcc-public-browser/g15-current-public-flow-review.md
- doc/tasks/20261002-dcc-public-browser/g15-ui-acceptance-plan.md
- doc/tasks/20261002-dcc-public-browser/g21-build-candidate-manifest.py
- doc/tasks/20261002-dcc-public-browser/g21-git-candidate-manifest.json
- doc/tasks/20261002-dcc-public-browser/g21-git-asset-review.md
- doc/tasks/20261002-dcc-public-browser/.gitattributes
- doc/tasks/20261002-dcc-public-browser/g22_gxp_configuration.py
- doc/tasks/20261002-dcc-public-browser/test_g22_gxp_configuration.py
- doc/tasks/20261002-dcc-public-browser/g22-gxp-local-test-configuration-plan.md
- doc/tasks/20261002-dcc-public-browser/g22-gxp-25-operation-impact.json
- doc/tasks/20261002-dcc-public-browser/g22-gxp-25-operation-config.review.sql
- doc/tasks/20261002-dcc-public-browser/g22-gxp-quality-version-registration.review.sql
- doc/tasks/20261002-dcc-public-browser/g22-gxp-readonly-preflight.sql
- doc/tasks/20261002-dcc-public-browser/g22-quality-approval-input.template.json
- doc/tasks/20261002-dcc-public-browser/g22-formal-policy-source-coverage.txt

G15 preparation milestone: ready_for_closeout for Root review. All 12 current flow mappings, source anchors and bounded actual-UI scopes are documented; Node syntax and AST safety/dependency/cleanup-keep checks PASS. Actual E2E remains not run; shared task stays in_progress.

## G19 reviewer configuration actual-role guard

Root assigned only ProjectReviewerConfiguration.vue and reviewer-owned tests. Formal backend reviewer config service requires hasAnyRoles(doc_control) and project-code:update; shared checkRole deliberately includes super_admin promotion and must remain unchanged. Project owner configuration/detail/browser/API/Java/SQL are outside ownership.

BDD: Given actual current userStore roles/permissions and the real existing checkRole implementation, When super_admin-only or a doc_control account without project-code:update opens/saves reviewer configuration, Then entry is absent and handlers perform zero configuration/directory reads and zero writes. A real logged-in doc_control+formal update permission account reads existing configuration/enabled users and saves exact selected Long/reason after confirmation.

BDD: Given an opened configuration read/confirmation/write under actor A, When role/permission is lost, actor changes, dialog closes or component unmounts, Then that generation becomes invalid; after waiting no old read may populate current state, no old confirmation may write as new actor, and no old success/error may close, unlock or overwrite a new actor/dialog. Confirm cancellation retains unchanged input; directory/network errors remain visible and block save.

Expected checks: actual SFC setup plus rendered entry, existing real permission/helper implementation at fixture cache/store boundary, official API-wrapper transport fixtures, reviewer tests/regression, owned lint/SFC compile and normalized source fingerprint. No real E2E/types/build/service/DB/Git/Maven.

G19 milestone: ready_for_closeout for Root Review. Effective real-helper/SFC RED 5 FAIL/2 PASS → reviewer/project confirmation regression 32 PASS; owned component lint 0/0, actual SFC compile and diff whitespace PASS. Source fingerprint below; shared task remains in_progress for Root integration/runtime/closeout.

## G21 read-only Git candidate and task-asset preparation

Root assigned a read-only review of main int_qms and integration tree status/diffs/import/freeze/receipt manifests plus preserved non-task assets. Only this task's inventory/manifest/review may be written. No staging/commit/reset/delete/cleanup, production edits, other-task edits or tests/build/service/database execution.

BDD: Given two trees at the recorded task baseline and the six workspace-specific preserved assets, When classifying candidate paths, Then exclude these exact non-task entries, carry formal import/necessary cross-module evidence for legitimate BPM/system/infra DCC changes, and list uncertain paths separately rather than staging by broad directory.

BDD: Given ignored task-owned scripts/tests, raw logs/screenshots/build sandboxes and old task records, When preparing candidate manifest, Then record explicit repository-relative path, provenance/hash/ignore state and final review questions; only retained reproducible scripts and final report/contract evidence are candidates, generated byproducts remain excluded from commits. Repeated task states and cleanup receipts are inspected, not changed or interpreted as final goal completion.

G21 preparation milestone: ready_for_closeout for Root Review. Exact two-tree candidate/manual/exclusion/force-add manifests and cross-module/duplicate-task/cleanup review delivered; structural validation PASS, six preserved hashes unchanged, staged lists empty. Source/status snapshot may drift while Root and other Owners continue, so final stop-write rescan remains mandatory. Shared task stays in_progress.

## G22 tenant1 25 missing DCC GxP operation preparation

Root supplied actual read-only runtime finding: tenant1 currently only has active dcc.controlled-file.publish; candidate registry has26dcc operations. Child may prepare exact25 operation configuration SQL/impact/safe generator/validator tests in this task only; no policy/Java/coreSQL/DB/migration/Root writes. G21 Git manifest remains frozen and is not rerun by this batch.

BDD: Given the formal parser's26dcc candidate fields and the real core/table shape, When preparing tenant1 configuration, Then exactly25 missing operation IDs and every source/domain/subject/action/reason/signature/state/retention/testIds/owner/applicability field are preserved. Existing publish and all other rows remain untouched; no business event/permission/BPM/quality-version/coverage registration is fabricated.

BDD: Given actual operation rows, When validating/applying a reviewed future package, Then all25 absent allows25 inserts; an exact complete repeated package is0inserts/0updates/0deletes; any row/version/active/deleted/payload conflict causes whole-operation fail before writes. Wrong tenant, policy/source hash, schema or missing real quality approval blocks. Candidate approvalReference remains genuinely pending; never synthesize approval or call configuration setup a business E2E result.

Expected validation: actual formal parse/source resolution and recorded schema/core identity, own offline validator negative tests/SQL structure, idempotence/conflict/no non-task DML inspection. No SQL execution/DB connection/Maven/build/service/Git or E2E.

BDD: Given an independently confirmed actual QA/quality approver, exact candidate raw SHA/source-coverage hash and explicit local-test version-registration authorization, When preparing the separate version registration, Then at most one tenant1 gxp_audit_policy_version row may be inserted; exact replay changes zero rows, conflicts stop without overwriting, and no developer/admin/old approval reference is substituted. Absence of the approval facts remains pending rather than a successful approval.

BDD: Given existing temporary objects or a nontransactional target engine, When considering future configuration SQL, Then fail before permanent DML and never delete an unrelated temporary table. Only the procedure's own successfully-created temporary table may be cleaned up; actual MySQL execution remains Root's separately authorized verification.

BDD: Given the formal parser's canonical report bytes, When generating the durable coverage artifact on Windows, Then its actual file SHA must equal the hash bound to quality registration; implicit CRLF conversion or any extra newline must not alter the approved bytes.

G22 preparation milestone: ready_for_closeout for Root Review only. Separate25-operation and maximum1quality-version templates, impact/full payload, blank actual-approval input, readonly preflight and formal-source report are prepared. Effective offline RED -> 12 PASS. Real QA person/basis/signature/time, independent local-test write authorization and fresh MySQL/role/schema verification remain pending; no DB/business E2E claim. G21 Git manifest is frozen; shared task remains in_progress.
