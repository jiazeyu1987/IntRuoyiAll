# Task: IntPP auto schedule first loop backend

## Goal

Implement the first closed loop of IntPP auto scheduling in IntRuoyi MES backend: preview a schedule without writing tasks, then apply the confirmed schedule into the current formal task set and keep `quantityScheduled` in sync.

## Scope

- Add minimal MES production line master data and workstation-to-line binding required by line-plus-shift scheduling.
- Add fail-fast backend scheduling models, persistence, service logic, and admin APIs for preview, apply, issues, and task dependencies.
- Reuse IntRuoyi work orders, routes, BOM, stock, workstation, and calendar data as scheduling inputs.
- Block apply when route, BOM, inventory, production line, or capacity prerequisites are missing.
- Do not implement schedule versioning, draft persistence, snapshot comparison, or compatibility fallback behavior.

## Milestones

- [x] M1: Previous backend task checked and completed before new work.
- [x] M2: Backend task documentation created in the backend repository before production code changes.
- [x] M3: RED backend tests written for preview/apply behavior, missing prerequisites, shortage blocking, dependency generation, and `quantityScheduled` synchronization.
- [x] M4: Backend schema, domain objects, service contracts, and admin API endpoints implemented.
- [x] M5: Scheduling adapter, capacity evaluation, material validation, dependency generation, and apply transaction implemented.
- [x] M6: Targeted backend verification passes.
- [x] M7: Evidence updated and backend task marked completed.
- [x] M8: Backend changes committed on `feature/auto-schedule-first-loop`.

## Expected Verification

- Preview returns a computed schedule, issues, and dependency links without writing `mes_pro_task`.
- Missing production line, shift capacity, route, BOM, or inventory data fails fast with explicit issue details.
- Material shortage blocks apply and no formal schedule is written.
- Apply preserves locked, manual, finished, or frozen work while writing the new auto-scheduled tasks.
- Apply updates `MesProWorkOrderDO.quantityScheduled` to match the published task set.

## Current Status

Completed on `feature/auto-schedule-first-loop`. Backend compile, targeted tests, local SQL patch application, real preview/apply runtime verification, repeatable demo-data replay, and replay runbook documentation all pass in `D:\wt\intsched-be`.
