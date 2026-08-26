# Test Plan

## BDD Scenarios

- `BDD: completion receipt is the only backfill source -> Given Tx-A has committed BACKFILL_SUCCEEDED; When Stage2.5 creates a batch; Then it does not call dossier writers and uses receipt result IDs.`
- `BDD: missing receipt blocks -> Given completion returns no valid receipt; When Stage2.5 continues; Then it fails fast before batch creation.`
- `BDD: conditional loss evidence -> Given receipt has actual loss or NO_LOSS; When snapshot is built; Then loss links and status match the receipt without generating a loss report.`

## Commands

- RED: Stage2.5 contract test before implementation.
- GREEN: Stage2.5 contract test after implementation.
- REGRESSION: `mvn -o -pl yudao-module-mes -am -DskipTests compile` and focused MES tests.

