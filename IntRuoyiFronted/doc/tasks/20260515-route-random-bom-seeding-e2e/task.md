# Task: 两条工艺路线随机 BOM 消耗播种与启停验证

## Goal

为 `ROUTE-XLSX-00001` 和 `ROUTE-XLSX-00002` 关联产品的每个工序补齐随机 route-product BOM 消耗配置，使两条路线在真实环境中不再因 `未配置工序的 BOM 消耗` 阻塞启用，并完成真实前端启停验证。

## Scope

- 先检查同仓库上一条任务状态；若未完成，则显式阻塞后再启动本任务。
- 在执行生产数据变更前创建任务目录、任务文档和执行日志。
- 通过真实前端登录拿到本地运行环境认证态，再调用真实后台 API 执行一次性播种。
- 当产品没有任何可选 BOM 子项时，先从系统现有启用物料中为该产品补随机产品 BOM 主数据。
- 每个缺失工序只补 1 条 route-product BOM，允许不同工序复用同一 BOM 物料，用量比例固定为 `1`。
- 播种后回到真实前端入口 `http://127.0.0.1:8081/mes/pro/route` 验证两条路线均可成功开启和关闭。
- 若缺少真实候选物料或仍存在其他业务前置条件，必须失败并记录精确阻塞，不得用 fallback 或伪造成功掩盖。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-route-last-process-key-flag-toggle-e2e/task.md`
- Status before this task: blocked.
- Impact: the previous task stopped on missing product BOM master data for `ROUTE-XLSX-00001`; this task is the direct follow-up to resolve that blocker.

## Milestones

- [x] M1: Check the previous frontend task state and create this task document before changes.
- [x] M2: Record BDD and RED verification targets before data seeding.
- [x] M3: Probe the authenticated runtime API context and inspect current candidate pools.
- [x] M4: Seed product BOM master data and missing route-product BOM rows for both routes.
- [x] M5: Validate the seeded data coverage against both routes and all route processes.
- [x] M6: Run real E2E enable/disable verification for both routes and record GREEN evidence.
- [x] M7: Commit only this task's tracked artifacts if verification fully passes.

## Expected Verification

- `ROUTE-XLSX-00001` and `ROUTE-XLSX-00002` each keep at least one key process.
- Each target route product has non-empty product BOM master data after seeding.
- Each process of each target route has at least one route-product BOM row after seeding.
- Real page `http://127.0.0.1:8081/mes/pro/route` can enable and then disable both routes successfully.

## Current Status

Completed. Both target routes now have deterministic random product BOM master data and per-process route-product BOM coverage, and both routes have passed real enable/disable verification on the live route list page.

## Important Implementation Note

- The live `mes/md/item/page` data did not expose any rows with `itemOrProduct = ITEM`; the enabled imported material-like rows were all labeled `PRODUCT`.
- To keep the seeding aligned with the two imported routes instead of selecting arbitrary full-table rows, the actual candidate pool used for master-BOM seeding was the set of route remark material codes that resolved to enabled item-master rows.
- The global `ITEM` pool remained as a coded fallback in the task script, but it was not needed in this execution because both routes had sufficient remark-code candidates.

## Final Verification Result

- RED baseline inherited from the previous blocked task:
  - `ROUTE-XLSX-00001` failed enable with `产品 PTCA球囊扩张导管 未配置工序的 BOM 消耗`.
- Auth/context probe:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session route-bom-seed-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-random-bom-seeding-e2e\scripts\probe-auth-storage.mjs` -> PASS
- Candidate-pool probe:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session route-bom-seed-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-random-bom-seeding-e2e\scripts\probe-item-pool.mjs` -> PASS, confirmed live enabled rows are present but labeled `itemOrProduct = PRODUCT`.
- Seed and coverage:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session route-bom-seed-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-random-bom-seeding-e2e\scripts\seed-route-random-boms.mjs` -> PASS
  - Seed summary:
    - `ROUTE-XLSX-00001`: product `PTCA球囊扩张导管`, created `5` master BOM rows, created `24` route-product BOM rows
    - `ROUTE-XLSX-00002`: product `冠状动脉棘突球囊扩张导管`, created `5` master BOM rows, created `26` route-product BOM rows
- Real E2E:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session route-bom-seed-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-random-bom-seeding-e2e\scripts\verify-route-toggle-e2e.mjs` -> PASS
  - Verified behavior:
    - `ROUTE-XLSX-00001`: disable -> enable -> disable all succeeded
    - `ROUTE-XLSX-00002`: disable -> enable -> disable all succeeded
    - each `PUT /admin-api/mes/pro/route/update-status` response returned `{"code":0,"msg":"","data":true}`
