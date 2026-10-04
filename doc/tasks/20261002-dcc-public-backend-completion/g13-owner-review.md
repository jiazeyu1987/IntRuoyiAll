# G13 independent backend owner approval Review

## Verified findings and fixes

1. Selection/real signed reason mismatch: original bind compared signature.comment to reason but did not compare Selection to the canonical owner suffix. A true v4 HMAC signature for owner A could be projected as B. Effective public-chain H2 RED now rejects different selected identity/names before owner write.
2. Same file/BPM return/reapproval: original prepare/bind permanently froze first selected owner. Real Flowable return reproduced an otherwise authorized new approval failure. Current projection now records the most recent successfully committed task/signature; every prior signed reason/account snapshot stays immutable. Actual BpmTaskServiceImpl.returnTask is used by the final suite.
3. Approval-center Native upload/revision MATRIX_APPROVAL advertised quick APPROVE/REJECT with no owner field. Directed adapter RED proved that action set; it now offers only PROCESS_IN_MODULE, retaining exact existing detail file/task/BPM routing. Direct quick context cannot manufacture a default owner.

## Public flow and transaction evidence

The new DccFileOwnerPublicApprovalTest executes real Controller POST, Workflow approve transaction, owner service, DccSignatureVerificationServiceImpl, v4 HMAC evidence service, ElectronicSignatureServiceImpl, DCC/unified Mapper inserts, BpmTaskServiceImpl approve/return, and isolated Spring Flowable engine/history. It recomputes the actual HMAC, parses canonical reason facts, and confirms the unified signed payload binds the same legacy DCC evidence hash. Unified hash is a distinct unified identity and is not mistaken for the DCC HMAC.

13 cases: actual HTTP large string file/owner IDs; Selection tampering; same-file/BPM return choosing a new owner; failure after actual Flowable complete; unified audit failure; completed-task replay; wrong actor/foreign-tenant/disabled/missing selected account; actual native revision approval; two true parallel approval tasks/actors serialized under formal Master/File locks; account rename preserving original signed facts and fresh later approval snapshots. Both original signatures remain present after successful reapproval/countersign. Same completed public task replay fails explicitly before new signatures; exact internal bind replay is read-only.

Account directory/credential validation, post/role directory, signature image and binary storage, GxP audit append, and BPM model/process lookup ports are explicit test fixtures. BpmTaskServiceImpl performs actual validate/guard/comments/variables/complete and return/history state operations; it is not a mocked approval facade. The isolated BPM model is the minimal true sequential/parallel owner-node model, not a claim that every deployed V4 BPMN listener has been E2E tested. Existing V4 workflow/checkin regression runs separately. The file's return status fixture is set to PENDING_MATRIX_APPROVAL after the actual return because this isolated engine excludes production DCC event listeners. No live UI, NAS, directory service, MySQL, scheduler or real runtime claim.

## Static boundaries confirmed

- Public validation locks Master/File, resolves actual tenant/file/task/BPM/native stage/candidate actor and readiness before owner preparation/signing. Other stages/OBSOLETE reject owner selection; no permission is granted by being selected as owner.
- Root nullable owner snapshot DO/detail/history fields read stored account names, not current directory aliases. Current projection legitimately changes on new approval; old signed facts never change on account rename.
- Revision freeze clears all seven owner approval fields. Fresh Query checkin builds a new WORKING row without owner approval facts; explicit true H2 checkin test and selected candidate freeze regression verify no inheritance, with the source snapshot retained.
- MAJOR checkin bypass absent: Query.validateWorkingCheckin permits exactly MINOR with no revisionChangeType. resolveCheckinVersion uses nextWorking, preserving formal number; existing MAJOR/OWNER/drawing/no-source-write tests pass. Formal replacement/revision approval remains separate. No speculative production rewrite.
- Root forward owner snapshot migration unchanged and unexecuted. No additional schema needed. Root retains runtime/MySQL/migration/E2E/Git decisions.

## Approved configuration

Business zone Asia/Shanghai; workbench reminder lead seven days; every-minute activation Quartz cron 0 * * * * ?. Actual keys dcc.workflow.zone-id and dcc.workflow.reminder-lead-days. Prepared paused job registration continues requiring explicit approved session values and official job-manager retry/monitor configuration; no real registration/enablement performed here.
