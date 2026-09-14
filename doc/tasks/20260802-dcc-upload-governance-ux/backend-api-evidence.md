# Backend API Evidence - DCC 上传治理体验优化

## Scope

- DCC detail projection and DCC approval-center task summary adapter.

## Endpoint, service, job, or handler scope

- DCC controlled file detail response projection: DccControlledFileRespVO and DccControlledFileQueryServiceImpl.
- DCC approval center provider: DccApprovalTaskAdapter and ApprovalTaskSummary.

## API contract and data contract

- Detail response now projects sourceFileId, originalFileId, publishedFileId, stampedFileId from DccControlledFileDO.
- ApprovalTaskSummary now carries businessContextTags.
- DCC adapter builds tags for 文件编号、版本、分类、当前节点、盖章、分发.

## Auth, permissions, validation, and error behavior

- No permission broadening was introduced.
- DCC approval center tag generation requires formal file number, version, category, and task name; missing required business context fails fast.
- Deleted DCC historical rows use explicit deleted-record tags rather than querying absent file data.

## Required config, services, fixtures, and migrations

- No schema migration required.
- DccApprovalTaskAdapter now requires DccFileCategoryMapper injection.
- Unit tests mock formal category records rather than defaulting category labels.

## BDD scenarios

- BDD: 审批中心行增强 -> Given DCC todo/done tasks, When approval center summary is built, Then businessContextTags include file number, version, category, node, stamp and distribution context.
- BDD: 详情页受控浏览联动 -> Given controlled file detail is loaded, When frontend renders linkage, Then backend-projected published/stamped/source/original file IDs are available.

## RED command and expected failure

- RED: node tests/e2e/dcc-upload-governance-ux-static.spec.js -> FAIL, backend/frontend contracts missing required projections and businessContextTags before implementation.
- RED: mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL initially on two old samples missing version/category formal context.

## GREEN command and passing result

- GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> BUILD SUCCESS, Tests run: 10, Failures: 0, Errors: 0.

## Contract or integration verification

- node tests/e2e/dcc-upload-governance-ux-static.spec.js -> PASS.
- Backend unit test verifies DCC approval adapter rows still resolve while carrying formal businessContextTags.

## Observability touchpoints

- Signature failure diagnostics remain user-visible in dialog inlineError, preserving backend error detail.
- No new logging or metrics introduced.

## Blockers and downstream skill needs

- Closeout commit/push blocked by unrelated dirty workspace; no backend functional blocker remains for this slice.

## Validation

- DccApprovalTaskAdapterTest verifies DCC todo/done/deleted rows and businessContextTags.
- Static contract verifies backend detail projection fields are exposed.

## Verification

- PASS: mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- PASS: node tests/e2e/dcc-upload-governance-ux-static.spec.js
