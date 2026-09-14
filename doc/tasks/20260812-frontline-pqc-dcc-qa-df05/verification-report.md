# Verification Report

## Scope

DF05 frontend static contracts and MesQaInspectionRegulationServiceTest backend regression.

## Results

- RED:
  - node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs -> FAIL，resultType 类型边界未限制 BOOLEAN/NUMERIC/TEXT。
  - node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs -> FAIL，DCC QA 状态批量加载缺少过期响应保护。
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest#saveDraft_rejectsLegacyResultTypes" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，旧 NUMBER/CHOICE resultType 未被拒绝。
- GREEN:
  - node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs -> PASS。
  - node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs -> PASS。
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest#saveDraft_rejectsLegacyResultTypes" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS。
- Regression:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，Tests run: 8, Failures: 0, Errors: 0, Skipped: 0。
- Blockers: none
