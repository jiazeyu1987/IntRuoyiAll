# Execution Log

## User Intent

用户指出按压式压力泵的“3. 清洗工序”是有 QA 工序/QA 检验项目的，但一线 PQC 页面截图显示“当前工序缺少发布态QA检验项目”。需要修复正式映射链路，而不是把该提示当成正确空态。

## BDD

BDD: 清洗工序展示发布态 QA 项目 -> Given 按压式压力泵生产订单选择一线 PQC 的“3. 清洗工序”，且当前产品/路线版本/路线工序/工序存在发布态 QA 规程项目；When 页面加载该工序 PQC 上下文；Then 后端返回该清洗工序的 inspectionItems，前端不显示“当前工序缺少发布态QA检验项目”。

BDD: 无正式 QA 项目的工序仍显示缺失提示 -> Given 某工序没有匹配当前产品、路线版本、路线工序和工序的发布态 QA 规程项目；When 页面加载该工序 PQC 上下文；Then 前端继续显示缺 QA 项目提示且不可正式提交。

## Evidence

- Screenshot evidence: production order `CODX-AO5-20260807-05`, product `按压式球囊扩充压力泵`, quantity `10`, process `3. 清洗工序`, page message `当前工序缺少发布态QA检验项目`.
- Read-only DB evidence, local Docker MySQL `int-ruoyi-mysql` / `ruoyi-vue-pro`:
  - Work order `980026`, product `924008 / IDI / 按压式球囊扩充压力泵`, tenant `1`.
  - Active order `39`, route `980091 / RT000028-IDI`, route version `622 / V1 / ACTIVE`.
  - Current product route includes route process `980633`, process `922987`, process name `清洗工序`, sort `3`.
  - Current active order `39` has `PENDING` PQC tasks `211-214`, all bound to route process `980631`, process `922985`, process name `粗洗工序`, regulation version `36`.
  - Exact current product/route/version QA regulation query returns only regulation `36` for `980631 / 922985 / 粗洗工序`, item count `3`, all `equipment_required=false`.
  - Published `清洗工序` QA regulations exist for product `902149 / 球囊扩张压力泵`, route `922119`, route versions `448` and `627`; they do not match the screenshot order product `924008`, route `980091`, route version `622`, or route process `980633`.
  - Query over `按压式%压力泵` products shows published QA regulations only for product `924008` rough-wash route processes `980631` and `980675`; no published cleaning QA regulation is mapped to any matching `按压式%压力泵` product.
- Code decision: keep process picker rooted in the current product route process list, then attach `pqcTaskId` / QA item context only when a formal `PENDING` PQC task exists for the same `routeProcessId + processId`; do not borrow QA items from another product or route.
- No database writes were made.

## RED

- RED: The reported page/data reproduced the defect condition: selecting route process `980633 / 922987 / 清洗工序` on order `CODX-AO5-20260807-05` had no formal `PENDING` PQC task and no exact published QA regulation, while a related product/route did have cleaning QA. The prior implementation risk was hiding formal route processes behind active-order task/snapshot scope; the new regression protects against that route-process picker collapse. A pre-change failing test run was not preserved in this handoff state.

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayProductLineRouteProcessesEvenWhenOnlyOneProcessHasPqcTask" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS, 1 test.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS, 32 tests.
- GREEN: `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java doc/tasks/20260808-pqc-cleaning-qa-items-missing` -> PASS except Git LF-to-CRLF normalization warnings.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260808-pqc-cleaning-qa-items-missing\verification-report.md` -> PASS, `Bug regression evidence is valid.`
- GREEN: `task-closeout-cleanup --mode preview` -> status `ready`, blocked `<none>`, warnings `<none>`.
- GREEN: `task-closeout-cleanup --mode apply` -> status `applied`, deleted 5 current-task temporary artifacts and kept only core task records.

## Experience Consolidation

- Existing long-term gate already covers this issue: `docs/backend-development.md#MES PQC 项目级检验快照门禁`, indexed by `docs/experience-index.md`.
- No new experience document was created because the durable rule already states PQC process selection must show product-route full process list, attach context only to formal `PENDING` tasks, and must not borrow QA items from another product/route.

## Blockers

- None currently.
