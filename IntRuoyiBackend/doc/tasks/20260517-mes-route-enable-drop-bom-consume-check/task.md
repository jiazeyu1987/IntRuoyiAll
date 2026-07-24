# Task: MES 工艺流程启用移除 BOM 消耗限制

## Goal

去除 MES 工艺流程启用时对“产品必须配置工序 BOM 消耗”的前置限制；当工艺路线已经具备工序和关键工序时，即使产品未配置 route-product BOM 消耗，也允许启用工艺路线。

## Scope

- 仅修改 `yudao-module-mes` 中工艺路线启用校验逻辑与对应回归测试。
- 保留“必须存在工序”和“必须存在关键工序”的原有限制。
- 不改动工艺路线页面、BOM 配置页面、导入逻辑或历史数据。

## Previous Task Check

- Previous backend task: `doc/tasks/20260517-mes-pro-route-list-owner-last-process/task.md`
- Status before this task: blocked due explicit user reprioritization before RED tests started.
- Impact: no unfinished implementation from the previous route-list task is allowed to mix into this route-enable defect fix.

## BDD

BDD: enable route without configured route-product BOM consumption -> Given a route already has at least one process and one key process plus a bound product without any route-product BOM rows, When an operator enables the route, Then the route status is updated to enabled and no `产品 {} 未配置工序的 BOM 消耗` error is thrown.

BDD: keep existing process prerequisites when enabling route -> Given a route is missing all processes or missing any key process, When an operator enables the route, Then the existing process prerequisite errors remain unchanged.

## Milestones

- [x] M1: Add a failing regression test for enabling a route without route-product BOM consumption.
- [x] M2: Remove the BOM-consumption enable gate while keeping the process and key-process checks intact.
- [x] M3: Run targeted regression tests and the repository TDD compliance gate.
- [x] M4: Update evidence, blockers, and final task status.

## Expected Verification

- `mvn -pl yudao-module-mes -Dtest=MesProRouteServiceImplTest test`
- `python tool/verify_tdd_compliance.py --task-dir doc/tasks/20260517-mes-route-enable-drop-bom-consume-check --all-changed`

## Current Status

Completed. The BOM-consumption enable gate is removed, regression tests are green, and the TDD plus bug-evidence gates passed.

## Final Verification

- `mvn -pl yudao-module-mes -Dtest=MesProRouteServiceImplTest test` -> PASS
- `python tool/verify_tdd_compliance.py --task-dir doc/tasks/20260517-mes-route-enable-drop-bom-consume-check --all-changed` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260517-mes-route-enable-drop-bom-consume-check\bug-regression-evidence.md` -> PASS

## Blockers

None.
