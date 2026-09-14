# DF07 Independent Test Report

- Result: **PASS**
- Worktree: `D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df07`
- Role: independent tester only
- Allowed write performed: this file only

## Scope Check

`git diff --name-status` showed only the DF07 QA regulation service contract, implementation, and service test as touched implementation/test files:

```text
M	IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationService.java
M	IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java
M	IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceTest.java
```

Git emitted LF-to-CRLF working-copy warnings for those three files. They were warnings only and did not change the command result.

## Requirement Verification

- PASS: The new service contract accepts `dccProjectCodeId + qaRegulationId + qaRegulationVersionId`.
- PASS: The implementation loads the DCC project by `dccProjectCodeId`, loads the QA regulation by `qaRegulationId`, verifies the regulation belongs to that DCC project, loads the QA version by `qaRegulationVersionId`, and verifies the version belongs to the QA regulation.
- PASS: The implementation allows only `PUBLISHED` and `RETIRED` locked QA versions.
- PASS: Wrong DCC ownership, wrong QA/version ownership, missing version, and `DRAFT` version are rejected.
- PASS: Disabled DCC can still read historical locked QA version; the new locked-version path only checks that the DCC project row exists and does not require current `ENABLE`.
- PASS: The new locked-version path does not call current QA selection, does not require current DCC ENABLE, and does not infer from product/material/route name.
- PASS: The new locked-version path does not validate MES route-process existence.
- PASS: The new locked-version path returns QA process rows only and does not assemble item/equipment details.

## Diff Evidence

Added production hunks include the locked-version method and helpers:

```text
+    List<MesQaInspectionRegulationProcessDO> getLockedVersionProcessesForOrder(
+            Long dccProjectCodeId, Long qaRegulationId, Long qaRegulationVersionId);
+    public List<MesQaInspectionRegulationProcessDO> getLockedVersionProcessesForOrder(
+            Long dccProjectCodeId, Long qaRegulationId, Long qaRegulationVersionId) {
+        MesQaInspectionRegulationDO regulation = requireLockedRegulation(dccProjectCodeId, qaRegulationId);
+                requireLockedVersion(regulation.getId(), qaRegulationVersionId);
+        List<MesQaInspectionRegulationProcessDO> processes = processMapper.selectListByVersionId(version.getId());
+    private MesQaInspectionRegulationDO requireLockedRegulation(Long dccProjectCodeId, Long qaRegulationId) {
+        if (dccProjectCodeId == null || dccProjectCodeMapper.selectById(dccProjectCodeId) == null) {
+        MesQaInspectionRegulationDO regulation = regulationMapper.selectById(qaRegulationId);
+        if (!Objects.equals(regulation.getDccProjectCodeId(), dccProjectCodeId)) {
+    private MesQaInspectionRegulationVersionDO requireLockedVersion(Long qaRegulationId, Long qaRegulationVersionId) {
+        MesQaInspectionRegulationVersionDO version = versionMapper.selectById(qaRegulationVersionId);
+        if (version == null || !Objects.equals(version.getRegulationId(), qaRegulationId)) {
+        if (!Set.of(STATUS_PUBLISHED, STATUS_RETIRED).contains(version.getLifecycleStatus())) {
```

Added test hunks cover the important behavior:

```text
getLockedVersionProcessesForOrder_returnsRetiredQaProcessesWithoutEnabledDccCheck
getLockedVersionProcessesForOrder_rejectsRegulationOutsideDccProject
getLockedVersionProcessesForOrder_rejectsVersionOutsideRegulation
getLockedVersionProcessesForOrder_rejectsDraftVersion
verifyNoInteractions(itemMapper, itemEquipmentMapper)
```

## Command Evidence

- PASS: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Surefire evidence: `MesQaInspectionRegulationServiceTest` ran 12 tests with 0 failures, 0 errors, 0 skipped.
- PASS: `git diff --check` exited 0.

## Production Diff Scan

Diff-only scan of added production lines found no matches for:

```text
product
material
formBindings
selectEnabledList
fallback
兼容
兜底
默认成功
MesRouteProcess / routeProcess / route-process existence validation
item/equipment assembly
```

## Final Result

PASS. No DF07 verification blockers found.
