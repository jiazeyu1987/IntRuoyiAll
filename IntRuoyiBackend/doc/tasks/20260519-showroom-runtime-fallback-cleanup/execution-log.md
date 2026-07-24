# Execution Log

BDD: Remove standalone showroom runtime fallback entrypoints -> Given showroom admin/display code now relies on persistent services, When the runtime structure is inspected, Then there should be no static shared runtime singleton, no no-arg runtime/display constructor, and no in-memory approval or preview fallback path.

BDD: Preserve persistent approval and display behavior after cleanup -> Given the runtime fallback entrypoints are removed, When admin approval and display integration tests run, Then the persistent B2/B3 behavior must remain unchanged.

RED: mvn -pl yudao-module-showroom '-Dtest=ShowroomRuntimeStructureTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -> PENDING, waiting for structure test to be added.

RED: mvn -pl yudao-module-showroom '-Dtest=ShowroomRuntimeStructureTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -> FAIL, `ShowroomApiRuntime.shared()` and related fallback entrypoints still existed.

GREEN: mvn -pl yudao-module-showroom '-Dtest=ShowroomRuntimeStructureTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -> PASS
GREEN: mvn -pl yudao-module-showroom '-Dtest=ShowroomRuntimeStructureTest,ShowroomHttpApiIntegrationTest,ShowroomWorkflowApprovalTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -> PASS
