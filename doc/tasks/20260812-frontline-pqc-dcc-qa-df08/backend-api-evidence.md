# Backend API Evidence

## Scope

- Service slice: DF08 QA process inspection item aggregation for frontline PQC process list.
- Owned scope: QA item/equipment assembly segment and MesQaInspectionRegulationServiceTest coverage.

## API Contract And Data Contract

- Input contract: a locked QA published version already resolved by upstream flow.
- Output contract: QA process rows include QA-owned process identity and aggregated business inspection items.
- Aggregation key: qaProcessId + itemCode.
- Type row rule keys: FIRST, PATROL_AM, PATROL_PM, FINAL; PATROL_AM and PATROL_PM remain separate.
- Result type contract: BOOLEAN, NUMERIC, TEXT only.
- Source data contract: retain published item fields, source fields, equipment options, and resultType.

## Auth, Permissions, Validation, Error Behavior

- DF08 does not add or change permissions.
- DF08 does not infer QA from product/material/route.
- DF08 does not validate QA process existence against MES route processes.
- Missing formal upstream locked QA version remains an upstream failure, not a DF08 fallback.

## Required Config, Services, Fixtures, Migrations

- No schema migration.
- No new item-type table.
- Required fixture is unit-test fixture inside MesQaInspectionRegulationServiceTest.

## BDD Scenarios

- BDD: 锁定 QA 版本按 QA 自有工序聚合检验项目 -> Given locked QA release with QA-owned processes and published items When frontline PQC list reads process items Then items are grouped by qaProcessId + itemCode and retain publication/source/equipment/resultType/type rows.
- BDD: PATROL_AM 与 PATROL_PM 不合并 -> Given same item has PATROL_AM and PATROL_PM type rows When aggregating Then both rule keys remain separate.
- BDD: resultType 只允许正式枚举 -> Given resultType BOOLEAN/NUMERIC/TEXT When aggregating Then those values are returned without defaulting.
- BDD: 压力泵 QA 版本聚合口径 -> Given pressure pump QA version fixture When aggregating Then result has 8 QA processes, 18 business items, and 51 type rows.

## RED Command

RED: cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df08\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 新增 DF08 断言显示 rule-key 顺序返回 [FIRST, FINAL, PATROL_AM, PATROL_PM]，未按 FIRST/PATROL_AM/PATROL_PM/FINAL 保留。

## GREEN Command

GREEN: cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df08\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

## Contract Or Integration Verification

- git diff --check -> PASS, exit 0; only Git CRLF working-copy warnings were emitted.
- Forbidden scan over backend code diff -> PASS, no forbidden fallback/compat/item-type/product/material/route-process/MES route existence-check patterns.
- Backend API evidence validator -> PASS, Backend API evidence is valid.

## Observability Touchpoints

- Unit test assertions expose aggregation counts and forbidden merge behavior.

## Blockers And Downstream Skill Needs

- None at task start.
- None after verification.
