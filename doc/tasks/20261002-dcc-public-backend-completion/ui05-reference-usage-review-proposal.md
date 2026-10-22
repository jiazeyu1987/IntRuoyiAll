# UI-05 reference usage details — read-only proposal for Root Review

Status: original G08-B1 proposal retained for Review history. Root subsequently approved the minimal usage-page implementation with reference/destination labels/fixed selected version/canPreview, omitting canCancel/folderPath. That approved minimal contract is implemented in the existing backend and recorded in integration-notes.md; the broader optional fields below remain proposal only. HTML `docs/product/dcc-final-requirements.html#flow-11` requires opening the reference project count to view project, folder and used version. Existing reference identity stays fixed until an explicit separately approved following policy exists.

## Actual baseline

- `DccProjectReferenceController`: `/dcc/project-file-references/usage?selectedFileId=...` calls `DccProjectReferenceService.getUsage`; response has Master id, `COUNT(DISTINCT project_id)` and referenced boolean. The source exact selected file is tenant-bound and checked with formal relation name visibility. This aggregate contains no project/folder names.
- Existing root list endpoint requires one destination project/folder and returns `ReferenceView(reference, selectedVersion, referenceProjectCount)`. It cannot supply reverse usage details for a source file across destinations without another query. `DccRelationStore.referenceProjectCount` counts active rows of `dcc_project_file_reference` by tenant/Master; cancellation physically removes the relation, with the true cancelled fact preserved separately in GxP audit.
- Actual reference table has tenant_id/project_id/folder_id/master_id/selected_controlled_file_id/created_by and unique `(tenant_id,project_id,folder_id,master_id)`. Existing `idx_dcc_reference_master(tenant_id,master_id,project_id)` supports the reverse usage lookup. It contains no copied project/folder labels, no auto-follow flag and no reference-time attribute snapshot. `DccLatestControlledFileResolver.resolveSelected` reads the exact stored selected version; `resolveLatest` must not be used for usage rows.
- Formal target scope is B's `DccProjectAccessService.assertProjectViewerOrAbove` / `listReadableProjectIds` plus its hard scope. Reference create/cancel stays governed by the target project's unique `projectLeaderUserId`, not any OWNER/admin menu bypass.

## Proposed smallest interface extension (requires Root approval)

Add one readonly paged endpoint under the existing reference controller, suggested `/dcc/project-file-references/usage-page`, request `selectedFileId:string,pageNo,pageSize`. No action, identity creation, follow-latest toggle or unrelated business table. Resolve source exact selected version and tenant/Master first, then apply the same source name visibility as current `/usage`. Do not require management detail or body access.

Response root fields:

| Field | Meaning |
| --- | --- |
| tenantId, sourceControlledFileId, masterId | Exact string identities of the authorized source lookup; another source history version in the same Master identifies the same usage set. |
| referenceProjectCount | Existing global tenant/Master distinct target-project aggregate, preserving the current orange/count rule. |
| visibleReferenceProjectCount | Distinct destination projects within the actor's formal readable project scope. |
| total, list | Authorized reference-row count and real SQL page. One row per project/folder/reference, so one project with two folders is two list rows but one distinct project count. |
| detailsRestricted | True if the aggregate indicates usage beyond the authorized destination scope; UI explicitly shows that detail scope is restricted rather than pretending the list contains every project. |

Row fields: `referenceId,projectId,projectName,folderId,folderName,folderPath,masterId,selectedControlledFileId` (all Long identities serialized strings), `fileNumber,fileName,versionNo,status,controlled,pendingEffect,executable` for the **stored selected version**, plus independent `canPreview` for that exact selected body and `canCancel` for that target project's current unique enabled leader. No source storage path/token/hash, private application BPM, guessed creator display name or default attributes. Stable original relation IDs and target-project identity permit the existing cancel request to be reviewed; the command still revalidates current referenceId, leader, folder, explicit confirmation and reason.

Project/folder labels are current formal destination labels, not claimed historical snapshots. `folderPath` is built from B-owned same-tenant/same-project parent facts, with missing parent/cycle/inconsistent row explicitly rejected; no NAS path inference. The selected file's version/name/status remain actual fixed version facts and may now be OBSOLETE/SUPERSEDED. Show that state and preserve traceability; never swap to Master.latest or grant preview solely because a reference exists.

## Authorization and completeness boundary

Recommended default for Root approval: source name-visible actors may see existing aggregate count, but destination labels/path/details require the existing destination project viewer scope. Query count/page must use the identical authorized project set before pagination. Do not add source OWNER/admin visibility over every other project's labels without explicit business authorization. This policy means the existing aggregate can exceed visible detail counts; label this clearly. If the user expects source-name visibility to reveal all target project/folder labels, that is a separate explicit cross-project discovery decision for Root/user confirmation, not an inferred permission.

For permitted rows, revalidate reference tenant, destination project/folder relation, source Master and exact selected file tenant/Master; inconsistent formal rows fail with a specific relation identity error. Forbidden projects are outside the declared authorized detail scope, not silently dropped as malformed data. Missing permitted project/folder/version records must not be hidden as empty-success. Infrastructure errors propagate. Name-visible yet preview-denied actors still see allowed relation labels and `canPreview=false`; clicking a body retains exact existing binary guards.

Use readonly REPEATABLE_READ for aggregate, authorized detail counts and page coherence during concurrent create/cancel. SQL direct one-to-one parent joins preserve one relation per row. Required count/page predicates include tenant/Master/authorization and exact reference identity. Avoid page-after-memory filtering, first-row/folder guesses, API/detail calls to fill labels, and latest-controlled selectors replacing fixed selections. Existing idx_dcc_reference_master and target-project/folder/Master unique constraint already support lookup; no new index or schema migration is proposed.

## Proposed acceptance checks after approval

BDD/TDD should cover one project in two folders with one project count, two distinct projects, same Master with different stored selected versions, latest changing without reference following, selected version obsoleted but retained, wrong tenant/Master/folder, destination name permission without body/edit, restricted destination scope with honest visible/global counts, exact long identity HTTP, stable count/page ordering, concurrent cancellation and response state. The existing cancel tests must prove that only the current target-project leader can remove that exact reference and other folder/project references remain. No historical cancellation usage rows are fabricated from audit text.

This document is the only UI-05 change in the current G08 batch. Implementation and runtime/real UI evidence require a separately reviewed scope; G07 activation registration remains prepared only.
