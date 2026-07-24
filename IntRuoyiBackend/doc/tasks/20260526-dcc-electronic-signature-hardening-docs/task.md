# Task: DCC 电子签名强化功能文档包

## Goal

为 DCC 电子签名强化功能创建实现前文档包，覆盖产品需求、用户路径、验收标准、前端设计、后端/API 设计、数据模型、安全部署、BDD 场景、严格 TDD 计划、E2E 路径和真实测试数据要求。

## Scope

- 规划签名绑定文件版本与内容摘要。
- 规划电子签名授权从默认启用改为明确授权。
- 规划授权变更审计。
- 规划签名含义标准化。
- 规划签名证据防篡改摘要。
- 规划签名信息进入导出、归档或受控副本证据。
- 规划密码错误失败审计、连续失败锁定和告警。
- 本任务只交付文档，不修改生产代码或数据库脚本。

## Previous Task Check

- Root task `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260526-dcc-electronic-signature-hardening-docs\task.md` 已创建。
- Previous related DCC signature task `doc/tasks/20260516-dcc-electronic-signature-management/task.md` is completed.
- Previous backend task `doc/tasks/20260525-dcc-nas-active-task-stuck/task.md` is completed and does not block this documentation task.

## Milestones

- [x] M1: Create task package in the backend worktree.
- [x] M2: Review current DCC signature implementation and evidence.
- [x] M3: Create product requirements docs.
- [x] M4: Create system design docs.
- [x] M5: Create BDD/TDD/E2E/test-data acceptance docs.
- [x] M6: Run document validators and commit this task's files.

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-dcc-electronic-signature-hardening-docs\ruoyi-vue-pro\doc\tasks\20260526-dcc-electronic-signature-hardening-docs`
- `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-dcc-electronic-signature-hardening-docs\ruoyi-vue-pro\doc\tasks\20260526-dcc-electronic-signature-hardening-docs`
- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-dcc-electronic-signature-hardening-docs\ruoyi-vue-pro\doc\tasks\20260526-dcc-electronic-signature-hardening-docs`
- `rg -n "Purpose and Scope|Evidence Reviewed|Design Blockers|Test Blockers" doc/tasks/20260526-dcc-electronic-signature-hardening-docs`

## Current Status

Completed. Reviewer final gate passed after second-round subagent repairs. The document package is released for implementation in this worktree; this task only changes documentation and does not modify production code or database scripts.

## Blockers And Impact

- 无仍阻塞文档包复审的已知文档 blocker。
- 后续实现/E2E 前置条件：测试租户用户密码、真实 DCC 文件、真实审批任务、真实前端入口和 Playwright 脚本需要在实现任务中准备；缺失时必须失败并记录影响，不得使用 mock E2E、备份数据、接口直写或测试专用 UI 替代真实用户路径。
- Reviewer gate result: PASS. The docs can drive the stated DCC electronic signature hardening goals without intentional side effects, are structured for BDD + strict TDD + subagent-driven delivery, and now have self-consistent logic plus clear API/frontend/data contracts.

## Final Verification

- PASS: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-dcc-electronic-signature-hardening-docs\ruoyi-vue-pro\doc\tasks\20260526-dcc-electronic-signature-hardening-docs`
- PASS: `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-dcc-electronic-signature-hardening-docs\ruoyi-vue-pro\doc\tasks\20260526-dcc-electronic-signature-hardening-docs`
- PASS: `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-dcc-electronic-signature-hardening-docs\ruoyi-vue-pro\doc\tasks\20260526-dcc-electronic-signature-hardening-docs`
- PASS: `git diff --check`
- PASS: residual blocker scan for boolean-compatible response, unresolved lock/hash blockers, taskId numeric examples, stale URL wording, and `controlledFileId` / `revisionId` example mismatch returned no matches.
- PASS: `rg --no-ignore -n "Purpose and Scope|Evidence Reviewed|Product Blockers|Design Blockers|Test Blockers" doc\tasks\20260526-dcc-electronic-signature-hardening-docs`
- PASS with closeout limitation: `task_closeout.py --task-id 20260526-dcc-electronic-signature-hardening-docs --mode preview` kept all formal documentation and found no delete candidates; automatic apply/merge is blocked because current branch `task/20260526-dcc-electronic-signature-hardening-docs` cannot be fast-forward merged into `int_main`.

## Cleanup Keep

- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/product/prd.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/product/user-flows.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/product/acceptance-criteria.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/system/frontend-design.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/system/backend-api-design.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/system/data-model.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/system/config-security-deployment.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/acceptance/bdd-scenarios.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/acceptance/tdd-plan.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/acceptance/e2e-plan.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/acceptance/test-data.md`
- `doc/tasks/20260526-dcc-electronic-signature-hardening-docs/docs/acceptance/subagent-driven-plan.md`
