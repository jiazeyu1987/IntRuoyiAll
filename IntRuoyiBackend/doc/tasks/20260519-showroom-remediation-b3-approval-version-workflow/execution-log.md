# Execution Log

BDD: Persist showroom change request and item diffs -> Given a company or product draft is submitted for approval, When the workflow request is created, Then `showroom_change_request` and `showroom_change_request_item` must persist the target snapshot, old/new diff, item approval status, and BPM process-instance linkage field without relying on in-memory state.

BDD: Supervisor reject should stop publish and expose rejection reason -> Given a pending supervisor review request, When the supervisor rejects it with a reason, Then the change request status becomes `REJECTED`, every related item is marked rejected, the rejection reason is stored, and live content remains unchanged.

BDD: Gaoxin reject should stop publish after supervisor pass -> Given a request already moved to Gaoxin approval, When the Gaoxin approver rejects it with a reason, Then the request becomes `REJECTED`, item statuses become rejected, the reason is stored, and no new live revision is published.

BDD: Approval detail should return diff-rich detail instead of a bare request -> Given an existing change request, When `/showroom/approval/get` is queried, Then the response must include the persisted request summary, field-level old/new diff items with status, target preview data, and version-diff evidence rather than only the bare change-request row.

BDD: Final approval should publish and append version evidence -> Given a request passed supervisor and Gaoxin approval, When final approval completes, Then the target revision is published, business status advances to `PUBLISHED`, and `showroom_version_audit` records the applied field diffs.

BDD: BPM process-instance linkage should remain explicit -> Given a change request persisted in showroom tables, When business workflow data is queried, Then the request must expose the `processInstanceId` linkage slot and current business status explicitly instead of silently inventing a fallback workflow id.

RED: mvn -pl yudao-module-showroom -Dtest=ShowroomWorkflowApprovalTest,ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test -> PENDING, RED test cases will be added before the first execution.

RED: mvn -pl yudao-module-showroom '-Dtest=ShowroomWorkflowApprovalTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -> FAIL, initial run stopped in testCompile because `ShowroomApprovalDetail`, `ShowroomPersistentWorkflowService`, and `ShowroomWorkflowFacade` were still missing.

GREEN: mvn -pl yudao-module-showroom '-Dtest=ShowroomWorkflowApprovalTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -> PASS
