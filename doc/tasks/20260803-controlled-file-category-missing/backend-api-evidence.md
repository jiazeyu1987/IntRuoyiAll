# Backend API Evidence

## Scope

DCC controlled-file upload directory resolution for `getUploadDirectoryTree(categoryId)`, submit preparation, and doc-control confirmed directory validation.

## Contract

- API input remains formal DCC `categoryId`; taxonomy id is never accepted as category id.
- If a category has an active directory binding, existing binding subtree validation remains unchanged.
- If a category has no active directory binding, backend resolves exactly one enabled directory with code `UNCLASSIFIED` and uses it as the submit root.
- If the formal unclassified directory is absent or not unique in the enabled directory set, backend throws `FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS`.
- Directory selection still validates that the submitted directory is inside the resolved root and is a leaf when required.

## Validation

- Query-service validation covers `defaultUnclassified=true` and missing-unclassified fail-fast.
- Workflow validation covers controlled-file submit persistence under unclassified directory and missing-unclassified fail-fast.
- Existing selected-directory subtree and leaf validation remains the final gate before persisting a directory id.

## BDD

- BDD: Upload tree for unbound category -> Given a valid active DCC category has no directory binding and an enabled `UNCLASSIFIED / 未分类` directory exists When upload page requests the directory tree Then backend returns the unclassified directory as binding root with `defaultUnclassified=true`.
- BDD: Submit unbound category -> Given a valid active DCC category has no directory binding and the request directory is the unclassified directory When the user submits a controlled file Then backend persists the controlled file under that directory.
- BDD: Missing unclassified directory -> Given a valid active DCC category has no directory binding and no enabled `UNCLASSIFIED` directory exists When upload tree or submit resolves the directory Then backend fails fast with `FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS`.

## RED

- RED: targeted Maven compile/tests -> FAIL, expected reason: missing `FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS` and `DccControlledFileUploadDirectoryTreeRespVO.defaultUnclassified`.

## GREEN

- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getUploadDirectoryTree_categoryWithoutBindingReturnsUnclassifiedDirectory,DccControlledFileWorkflowServiceImplTest#submitControlledFile_categoryWithoutDirectoryBindingUsesUnclassifiedDirectory" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- GREEN: heartbeat Maven `DccControlledFileQueryServiceTest#getUploadDirectoryTree_categoryWithoutBindingAndUnclassifiedMissingFailsFast` -> PASS.
- GREEN: heartbeat Maven `DccControlledFileWorkflowServiceImplTest#submitControlledFile_bindingMissingAndUnclassifiedDirectoryMissing_throwsNotExists` -> PASS.

## Verification

- PASS: DCC main compile completed during targeted Maven run.
- PASS: four new backend method paths passed across standard and heartbeat Maven executions.

## Blockers

- Full repository completion remains blocked by unrelated dirty worktree/ahead branch state; no broad commit or push was attempted.
