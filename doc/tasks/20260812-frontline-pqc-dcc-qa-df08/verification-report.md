# Verification Report

## Result

ready_for_closeout

## RED

- Command: cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df08\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- Result: FAIL as expected.
- Evidence: MesQaInspectionRegulationServiceTest 13 tests, 1 failure. New DF08 assertion expected rule keys [FIRST, PATROL_AM, PATROL_PM, FINAL] but actual was [FIRST, FINAL, PATROL_AM, PATROL_PM].

## GREEN

- Command: cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df08\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- Result: PASS.
- Evidence: MesQaInspectionRegulationServiceTest Tests run: 13, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS.

## Static Verification

- git diff --check: PASS, exit 0; Git reported CRLF working-copy warnings only.
- Forbidden scan: PASS; backend code diff did not introduce fallback/compat/item-type table/product/material/route-process/MES route existence-check patterns.
- Backend API evidence validator: PASS; Backend API evidence is valid.

## Scope Check

- Modified only DF08-owned backend QA regulation aggregation code, its related test, and DF08 task documents.
- Did not add schema, item-type table, frontend projection, product/material/route inference, or QA-to-MES route-process existence validation.
