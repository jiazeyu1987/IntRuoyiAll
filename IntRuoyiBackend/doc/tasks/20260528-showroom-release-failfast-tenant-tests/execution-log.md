# Execution Log

BDD: 映射产品物料缺失阻断发布 -> Given 展厅已发布公司和展厅，且展厅映射了一个缺少当前发布音频物料的产品 / When 发布当前展厅 release / Then 发布失败并返回包含产品标识和缺失物料原因的异常，不跳过该产品或展厅。

BDD: 展厅发布测试具备租户上下文 -> Given 后端 JUnit 直接调用 narration/release publisher service / When 测试读写租户隔离表 / Then 测试显式设置并清理测试租户上下文，验证失败只来自业务断言而不是缺少租户编号。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest#shouldFailFastWhenMappedProductNarrationIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected `IllegalStateException` but nothing was thrown; old code logged `SHOWROOM_RELEASE_SKIP_PRODUCT ... productCode=P-104 reason=SHOWROOM_TARGET_NOT_FOUND: live product EN narration not found` and continued publishing.

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest#shouldFailFastWhenMappedProductNarrationIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test.

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductContentTest,ShowroomCompanyContentTest,ShowroomHallContentTest,ShowroomVersionBundleServiceTest,ShowroomReleasePublisherServiceTest,ShowroomProductCoverImageServiceTest,ShowroomPersistentNarrationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 19 tests.

REGRESSION: `mvn -pl yudao-module-showroom "-Dtest=ShowroomRelease*Test,ShowroomVersionCenter*Test,ShowroomPublicReleaseScopeContractTest,ShowroomReleaseAdminPublishIntegrationTest,ShowroomReleaseAutoPublishSchedulerTest,ShowroomVersionCenterControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> initially FAIL, 59 tests run and 2 old tests expected skip semantics. Updated those tests to the new fail-fast contract.

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomRelease*Test,ShowroomVersionCenter*Test,ShowroomPublicReleaseScopeContractTest,ShowroomReleaseAdminPublishIntegrationTest,ShowroomReleaseAutoPublishSchedulerTest,ShowroomVersionCenterControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 59 tests.

GREEN: `git diff --check` -> PASS, no whitespace errors.

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260528-showroom-release-failfast-tenant-tests\bug-regression-evidence.md` -> PASS, evidence is valid.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-showroom-release-failfast-tenant-tests --mode preview` -> PASS, cleanup preview keeps `task.md`, `execution-log.md`, and `bug-regression-evidence.md`; no delete, blocked, or warning entries.
