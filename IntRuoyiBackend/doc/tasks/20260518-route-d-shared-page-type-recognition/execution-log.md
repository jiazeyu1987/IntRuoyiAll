BDD: Route D generic process-page header split -> Given a Route D PDF table set contains a segment title row that matches the shared page-type header pattern but is not present in the fixed title list, When Route D recognition parses and splits the flattened rows, Then it should still split that segment and preserve the generic process-page title instead of collapsing adjacent segments.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteDRecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `recognize_splitsGenericProcessHeaderNotInFixedTitleList` exposed `route_d_expected_15_templates_actual_14`, proving Route D still depended on the fixed title list for segmented process-page headers.

GREEN: `javac @D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\yudao-module-mes\target\route-d-main-javac.args` -> PASS after wiring Route D header detection and title extraction to the shared page-type rule for single-cell title rows.

GREEN: `javac @D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\yudao-module-mes\target\route-d-test-javac.args` -> PASS

GREEN: `java @D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\yudao-module-mes\target\route-d-junit-launcher\runner-java.args` -> PASS, 4 tests started and 4 tests successful in `MesProBatchRecordRouteDRecognizerTest`.

Verification note:
- Re-running the standard Maven test command after the fix is currently blocked by unrelated compile errors in other `yudao-module-mes` sources owned by parallel work, so final task verification used isolated compilation of the owned Route D sources plus a direct JUnit Platform launch of `MesProBatchRecordRouteDRecognizerTest`.
