# DF07 Verification Report

## Result

- Status: ready_for_closeout.
- Scope verified: locked QA version process read service.
- No fallback, downgrade, product/material inference, route-process existence validation, or item/equipment aggregation was introduced.

## Evidence

- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL in isolated RED worktree, expected missing getLockedVersionProcessesForOrder(Long, Long, Long).
- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 12 tests, 0 failures, 0 errors, 0 skipped.
- Static: git diff --check -> PASS.
- Forbidden scan: production diff scan -> PASS, no product/material/formBindings/selectEnabledList/fallback/兼容/兜底/默认成功/routeProcess/MesRouteProcess/itemEquipment/equipment.
- Independent verification: independent-test-report.md -> PASS.
- Backend evidence validator: validate_backend_api.py -> PASS.

## Business Conclusion

- DF07 now provides the formal backend reader for order-locked QA process lists.
- The reader uses only DCC project code + QA regulation + QA version ownership and version status.
- QA process identity remains QA-owned and is not checked against MES route process identity.
