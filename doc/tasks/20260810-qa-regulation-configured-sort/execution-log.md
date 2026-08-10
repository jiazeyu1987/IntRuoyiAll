# Execution Log

## Intent

用户要求修改 QA 规程配置页面产品下拉排序：不是固定让“按压式球囊扩张压力泵”和“球囊扩张压力泵”排第一、第二，而是所有已经配置 QA 规程的产品都排在前面；草稿状态下前端已有数据时也应按同一口径判断。

## Environment

- Workspace: `D:\IntRuoyiWorktree\20260810-qa-regulation-configured-sort`
- Branch: `codex/20260810-qa-regulation-configured-sort`
- Runtime profile: `int_main`
- Reserved slot: 11
- Frontend port: 8092
- Backend port: 48092

## BDD

- BDD: configured QA products are prioritized -> Given the product dropdown contains products with and without QA regulation configuration, When the dropdown options are built, Then every configured product appears before unconfigured products and no specific product name is hardcoded as first or second.
- BDD: draft regulations still use configured data -> Given the selected QA regulation is DRAFT and the product candidate data contains configuration status, When the dropdown is opened, Then DRAFT status does not exclude that product from the configured-priority group.

## RED/GREEN Evidence

- RED: node tests/e2e/qa-regulation-project-configured-dropdown-real.e2e.cjs -> FAIL, expected reason: 真实页面下拉未加载 ID / 球囊扩张压力泵 / 112，说明默认第一页 50 条候选不足，已配置排序没有完整候选输入。
- GREEN: node tests/e2e/qa-regulation-project-configured-dropdown-static.spec.cjs -> PASS.
- GREEN: node tests/e2e/qa-regulation-project-configured-dropdown-real.e2e.cjs -> PASS.
- GREEN: node tests/e2e/qa-regulation-header-project-select-static.spec.cjs -> PASS.
- GREEN: node tests/e2e/qa-regulation-project-last-copy-static.spec.cjs -> PASS.
- GREEN: node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs -> PASS.
- GREEN: pnpm ts:check -> PASS.
- GREEN: git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-project-configured-dropdown-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-project-configured-dropdown-real.e2e.cjs -> PASS.

## Milestone Updates

- Worktree created under `D:\IntRuoyiWorktree\` and slot reserved through `reserve-worktree-slot.ps1`.
- Worktree changes prepared for int_main integration; E:\IntRuoyi main workspace is dirty with parallel task changes, so final merge must follow the project parallel-worktree integration gate.
- Static regression tightened to require complete DCC candidate loading before configured-first sorting.
- Read-only real Playwright regression added and passed on int_main: 119 dropdown candidates loaded; IDI and ID are configured; configured group appears before unconfigured group.
- Task status set to ready_for_closeout after verification passed.
- In task worktree, pnpm install --frozen-lockfile --reporter append-only -> PASS; node_modules restored from local cache, lockfile unchanged.
- In task worktree, pnpm ts:check -> PASS after dependency restoration.
- Project experience consolidated into docs/backend-development.md and docs/experience-index.md.
- Final int_main integration and closeout remain pending because the main workspace has unrelated parallel-task staged and unstaged changes.

## Blockers

- None currently.
