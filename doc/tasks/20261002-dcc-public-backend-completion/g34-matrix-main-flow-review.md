# G34 matrix main-flow readonly review

Conclusion: **no new actionable bug and no open findings in this mainline isolation scope**. Current legacy save/import/delete and batch projection stay inside the explicit LEGACY scope; formal NEW/REVISION/OBSOLETE lookup and existing node/history payloads are not rewritten by those paths. This is an independent source review, not a reproduced test/E2E/MySQL PASS. Root subsequently verified the owner's final68/3-class BUILD SUCCESS with zero failure/error/skip; this reviewer did not execute those tests. Final production Admin/CategoryMapper hashes below match Root's freeze.

## Scope and source anchors

- `DccCategoryApprovalMatrixAdminServiceImpl.saveApprovalMatrix:238–255`, `importApprovalMatrix:257–261`, `deleteApprovalMatrix:263–280`: public mutations are Spring `@Transactional(rollbackFor=Exception.class)`. Save/import invoke `prepareMatrixSaveContext:593` first; delete calls `lockMatrixCategory:1113`. The helper requires current tenant ID and exact category row before mutation. `DccFileCategoryMapper.selectMatrixCategoryForUpdate:14–16` has explicit tenant/id/not-deleted predicates and `FOR UPDATE`. Lock acquisition and persistence therefore belong to the existing Spring physical transaction; no self-invocation creates a separate transaction.
- `prepareMatrixSaveContext:612`, `previewApprovalMatrix:171`: version allocation uses `selectMaxVersionNoIncludingDeletedByActionType(categoryId,"LEGACY")+1`. `DccCategoryApprovalRouteMapper:78–85` filters category plus exact action and deliberately has no deleted filter, so historical logically deleted versions remain in allocation. Other action versions do not increase the legacy version number. Normal tenant isolation is provided by the formal MyBatis tenant interceptor; the locking query additionally pins tenant explicitly.
- `persistApprovalMatrix:1079–1110`: inserts an explicit LEGACY root, validates its inserted ID/result, deactivates only other active LEGACY roots of the same category, and inserts only nodes under the new root. It contains no node update/delete and no snapshot update. Failed root/deactivation/node persistence throws; the transaction can roll back the prior permission-rule cleanup and replacement. No typed root or existing typed node is selected for a write.
- `deleteApprovalMatrix:265–279`: deactivates only active LEGACY roots; retains roots/nodes and does not delete formal action rows/nodes or historical snapshots. Existing legacy-managed REVIEW/APPROVE permission cleanup remains the previous endpoint's responsibility; this fix adds no typed action grant or history rewrite.
- `getActiveMatrixPositionIdsByCategoryIds:196–236`: query now selects exact LEGACY+active and filters effectiveTime<=actual now (null means effective); only selected legacy root IDs feed node aggregation. It cannot project positions from NEW/REVISION/OBSOLETE or a future LEGACY root. Single legacy reads already used the mapper's legacy selector and remain unchanged.
- `previewApprovalMatrix:158–176`: validates/reads category and derives preview/version, without `lockMatrixCategory`, inserts or updates. Current lock helper is reachable only through write preparation/delete. Preview does not take the new write row lock.
- `DccControlledFileApprovalRouteAssigneeResolver.resolveRoute:110–143`: explicit action selects `selectLatestActiveByCategoryIdAndActionType`; validates action-specific node structure. Missing typed route fails rather than borrowing LEGACY or another action. This resolver source is unchanged by the isolation batch. `DccCategoryApprovalRouteMapper:40–52` filters exact action/active/effective and `:55–65` confines the old no-action lane to legacy.

## Reviewed tests and evidence limits

The three new initial scenarios at `DccCategoryApprovalMatrixAdminServiceImplTest:889`, `:897`, `:904` mix typed roots with legacy save/import/delete and batch reads. I also read the owner's bounded late node rollback `:908`, concurrent same-category import `:916` and effective legacy projection `:924` additions. They call the real Spring service/H2 mappers while identity-resolution read ports are isolated. The typed fixtures compare original route payloads and check existing node counts; the production path inspection supplies the separate observation that no existing node/snapshot writer is called. This reviewer did not execute Maven or claim those assertions passed.

The existing deleted-version scenario in `DccApprovalRouteAdminServiceImplTest:707` verifies historical version allocation through the same action-scoped mapper. Assignee resolver tests were read to confirm the formal action selection boundary; no new resolver behavior was requested. Actual MySQL locking, real page maintenance and new upload/revision/obsolete submissions remain Root runtime acceptance.

Future-effective LEGACY replacement activation policy is outside this requested fix. The current persistence path still deactivates previous LEGACY roots regardless of the new effective date; that was part of the endpoint's prior behavior, not the typed-isolation mutation repaired here. This review does not ask for a new activation mechanism or claim that separate policy is solved. No wider typed-admin concurrent-save redesign or UI wording work is included.

## Read snapshot hashes

Read after the owner's production formatting; all paths are under the integration worktree. These are review snapshot hashes, not a new frozen-delivery manifest.

| File | Raw SHA256 |
|---|---|
| DccCategoryApprovalMatrixAdminServiceImpl.java | 11a6b75ba7b26781b84bd383797bb6d17faf44ca865bc8e8d92b774c2ac66da2 |
| DccFileCategoryMapper.java | 673044f6c0e529f85167e0a4fea35ac5117740ea6ca2af054a38af7cdf6eb86f |
| DccCategoryApprovalRouteMapper.java | 90f2c3631fbd2ad11b024ca2dcbdf831281e227b52e727106cd00183c6939879 |
| DccControlledFileApprovalRouteAssigneeResolver.java | d2d4a8d0dc2126cd135d1131cf97dac17f7a219e7c6db09980ae9548cc27bc5c |
| DccCategoryApprovalMatrixAdminServiceImplTest.java | 0638b8c6ceae8c7a3df13109f18758f247126810cf32b49458901c7a94c18d61 |
| DccApprovalRouteAdminServiceImplTest.java | c5af2b2cb7d3fda22cf348da57bed5773df6b3bafcdb6c472d533afd31afb6ba |
| DccControlledFileApprovalRouteAssigneeResolverTest.java | b5e1708a9b6061dadd38b1889c1e2a0e2f275d9864f078f4ff9bade4ebfe9ba5 |

Only this review record/task retention list was written. No production edit, Maven/test, browser, database, service or Git write occurred. Deferred G34 detail runners remain untouched and unvalidated.
