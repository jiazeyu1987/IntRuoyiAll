# Execution Log

## Intent

用户确认：顶部“工序”切换不应跳转到流程/批次详情页，而应保留在当前填写页；这里的查看切换暂不考虑权限，所有人都可以切换查看，用于先理顺业务流程。

## Preflight

- Read `docs/task-closeout-rules.md`.
- Read `docs/frontend-development.md`.
- Read `docs/e2e-rules.md`.
- Read `docs/powershell-encoding.md`.
- Read `docs/experience-index.md`; copied applicable gates into `task.md`.
- Used skills `frontend-feature-delivery` and `bug-regression-fix-loop`; read `SKILL.md` and references.

## BDD

- BDD: stay on fill page when switching process -> Given the user is on `/mes/pro/feedback/edhr-execution/form`, When clicking the top process switch and choosing any process in the same batch/order, Then the route remains the execution form page and only the process context changes.
- BDD: permission-free viewing switch -> Given the selected process is not currently openable, has no active work task, or has no execution record, When it is selected from the process switch, Then the user can still switch to view that process context without seeing `缺少可查看执行记录或工作任务` and without granting save/submit permissions.

## RED

- Pending.

## GREEN

- Pending.

## Regression

- Pending.

## Blockers

- None currently.

## Current Status

in_progress
