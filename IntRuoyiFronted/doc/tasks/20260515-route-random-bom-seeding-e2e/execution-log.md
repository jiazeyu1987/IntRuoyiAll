# Execution Log: 两条工艺路线随机 BOM 消耗播种与启停验证

BDD: imported routes can be enabled after deterministic random BOM seeding -> Given `ROUTE-XLSX-00001` and `ROUTE-XLSX-00002` already have at least one key process but may still miss product BOM master data or route-product BOM rows, When the operator runs the authenticated runtime seeding flow, Then each route product gains a deterministic random BOM candidate pool, each route process gains one route-product BOM row, and both routes can be enabled and disabled successfully from the real route list page.

- M1: Completed. The previous frontend task `doc/tasks/20260515-route-last-process-key-flag-toggle-e2e/task.md` is blocked by missing product BOM master data, and this follow-up task directly addresses that blocker.
- M2: Completed. This task document and execution log were created before any new production data changes for the BOM seeding flow.
- RED baseline: inherited from `doc/tasks/20260515-route-last-process-key-flag-toggle-e2e/execution-log.md`, where `ROUTE-XLSX-00001` failed enable with `产品 PTCA球囊扩张导管 未配置工序的 BOM 消耗`.
- M3 GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session route-bom-seed-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-random-bom-seeding-e2e\scripts\probe-auth-storage.mjs` -> PASS, authenticated runtime storage exposed a valid `ACCESS_TOKEN` wrapper that the seeding script could unwrap.
- M3 GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session route-bom-seed-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-random-bom-seeding-e2e\scripts\probe-item-pool.mjs` -> PASS, live item-master rows were available, but the imported material-like rows were labeled `itemOrProduct = PRODUCT`, so route remark material codes were used as the primary deterministic seed pool.
- M4/M5 GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session route-bom-seed-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-random-bom-seeding-e2e\scripts\seed-route-random-boms.mjs` -> PASS, both routes received non-empty product BOM master data and every process received at least one route-product BOM row.
- M4/M5 GREEN summary:
  - `ROUTE-XLSX-00001`: product `PTCA球囊扩张导管`, `5` master BOM rows created, `24` route-product BOM rows created
  - `ROUTE-XLSX-00002`: product `冠状动脉棘突球囊扩张导管`, `5` master BOM rows created, `26` route-product BOM rows created
- M6 GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session route-bom-seed-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-random-bom-seeding-e2e\scripts\verify-route-toggle-e2e.mjs` -> PASS, both routes successfully toggled disable -> enable -> disable on the live page, and each update-status API response returned `code=0`.
- M7 GREEN: scoped task artifacts are ready for a frontend-only commit.
