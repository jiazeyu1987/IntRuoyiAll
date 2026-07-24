# Execution Log: Production Work Order Row Temporary Freeze API

BDD: freeze one eligible work order -> Given a confirmed production work order is not temporarily frozen / When the row temporary-freeze API updates that work order to frozen / Then the work order persists `temporaryFrozen=true` and any open schedule tasks for that work order are cleared.

BDD: unfreeze one eligible work order -> Given a production work order is temporarily frozen / When the row temporary-freeze API updates that work order to unfrozen / Then the work order persists `temporaryFrozen=false` without silently recreating schedule tasks.

BDD: missing work order fails fast -> Given the requested work-order id does not exist / When the row temporary-freeze API is called / Then the backend reports the existing not-found error instead of returning default success.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProWorkOrderServiceImplTest,MesProWorkOrderControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, the targeted test compile could not find `MesProWorkOrderUpdateTemporaryFrozenReqVO`, proving the row-level contract and implementation were missing.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProWorkOrderServiceImplTest,MesProWorkOrderControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, controller and service tests now cover row-level freeze, unfreeze, and missing-work-order failure behavior.
