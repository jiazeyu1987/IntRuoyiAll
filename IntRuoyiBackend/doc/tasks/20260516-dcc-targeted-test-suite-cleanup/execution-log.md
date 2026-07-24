BDD: targeted controlled-file preview regressions can compile and run -> Given the DCC module contains unrelated new tests and drifted fixtures, When the targeted controlled-file regression command runs, Then unrelated compile drift no longer blocks `DccControlledFileQueryServiceTest` and `DccControlledFilePreviewDownloadApiTest`.

BDD: cleanup preserves current protected preview behavior -> Given the live protected preview route is already verified, When cleanup changes production code or tests, Then the DCC preview watermark contract and preview/download behavior stay covered by the targeted regression pair.

BDD: fail fast on out-of-scope missing feature tests -> Given newly added tests depend on production API that does not exist in the checked-out main code, When cleanup reaches those tests, Then the task records them as a separate blocker instead of silently inventing the missing feature.

RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest,DccControlledFilePreviewDownloadApiTest" test` -> FAIL, initial compile was blocked by unrelated training/signature drift: checked exception handling in `DccTrainingTaskServiceImpl`, missing import in `DccControlledFileQueryServiceImpl`, ambiguous `insert(any())`, stale `distributionMedium` test usage, and stale assertion/setup mismatches in DCC tests.

GREEN: minimal cleanup applied -> PASS, targeted Maven now compiles the DCC module and reaches execution of `DccControlledFilePreviewDownloadApiTest` and `DccControlledFileQueryServiceTest`.

GREEN: `DccControlledFilePreviewDownloadApiTest` -> PASS during targeted Maven run.

GREEN: `DccControlledFileQueryServiceTest` -> reached runtime assertions after cleanup, proving compile/test drift no longer blocks the target pair at the original failure depth.

RED: targeted Maven still fails before a final green because `DccControlledFileUploadNameOptionApiTest` and `DccControlledFileUploadNameOptionQueryServiceTest` reference missing production types (`DccControlledFileUploadNameOptionRespVO`) and a missing upload-name-option feature API outside this cleanup scope.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileUploadNameOptionApiTest,DccControlledFileUploadNameOptionQueryServiceTest" test` -> PASS, the resumed upload-name/version-linkage feature slice now satisfies the formerly blocking missing-feature tests.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest,DccControlledFilePreviewDownloadApiTest,DccControlledFileUploadNameOptionApiTest,DccControlledFileUploadNameOptionQueryServiceTest" test` -> PASS, the targeted DCC backend regression suite is fully green.
