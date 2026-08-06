# Execution Log

## User Intent

- 用户反馈：选择其它产品后，QA 规程配置的检验规则仍显示与按压式球囊扩充压力泵相同的数据。
- 期望行为：检验规则跟随正式产品，不同产品的规则互相隔离；非压力泵产品不得继续显示压力泵规则。

## BDD / TDD

- BDD: 非压力泵产品不显示压力泵规则 -> Given QA 规程页面先选择压力泵产品并显示压力泵检验规则，When 切换到另一个正式 `productMasterId` 的产品，Then 检验类型规则必须清空或恢复该产品自己的草稿，不得继续显示压力泵规则。
- BDD: 压力泵规则只按产品绑定恢复 -> Given 只有 `IDI` 正式绑定产品登记了压力泵模板，When 其它项目代码或其它产品被选中，Then 不能因为项目代码、产品名称、旧页面状态或上一个产品草稿而显示压力泵规则。
- BDD: 切回压力泵产品恢复压力泵草稿 -> Given 已切换到其它产品后规则为空，When 再切回压力泵正式产品，Then 压力泵检验规则草稿应恢复。

## Preflight

- SKILL: `bug-regression-fix-loop` -> LOADED。
- SKILL: `frontend-feature-delivery` -> LOADED。
- RULE: `docs/worktree-restrictions.md`、`docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/branch-runtime-ports.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md` -> READ。
- WORKTREE: `D:\IntRuoyiWorktree\qa-regulation-product-switch-fix`，branch `codex/qa-regulation-product-switch-fix`，base `origin/int_main` = `32cc89c6be48f5df9cb0f99ece86a35b0e173516`。
- SLOT: `int_main slot 3`，frontend `8084`，backend `48084`；本任务暂不启动服务。
- MAINLINE SYNC: 创建 worktree 后发现分支落后 `origin/int_main` 4 个提交，已 `git merge --ff-only origin/int_main` 到 `4366d6d11d14cb42e4470f0552cd2349e46932ac`，pre-push branch runtime port guard -> PASS。
- EXPERIENCE: `docs/experience-index.md` -> READ；命中 QA 产品级规程门禁与前端静态契约隔离门禁，已复制适用摘要到 `task.md`。

## Milestone Evidence

- M1 ROOT CAUSE：`createEmptyQaInspectionTypeRules()` 原本返回首检/上午巡检/下午巡检/末检默认行，导致未配置产品也继承压力泵式规则结构；页面规则标签硬编码四个标签，巡检提示硬编码 5% 抽样，进一步放大“切换产品仍显示同样数据”的表现。
- M2 RED：`node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> FAIL，原因：新增静态合同要求 `createEmptyQaInspectionTypeRules()` 返回空数组，但旧实现仍返回默认首检/巡检/末检规则行。
- M3 IMPLEMENTATION：将通用首检/巡检/末检行拆为 `createBaseQaInspectionTypeRules()`，压力泵模板显式基于该 base profile 构建；`createEmptyQaInspectionTypeRules()` 改为空数组；规则标签通过 `qaInspectionTypeRules` 渲染；巡检预览通过当前产品规则 `qaPatrolPreviewText` 计算；保存时无规则产品直接阻塞。
- M4 GREEN：`node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> PASS。
- M4 REGRESSION：`node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- M4 REGRESSION：`node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- M4 REGRESSION：`node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs` -> PASS。
- M4 TYPECHECK：`pnpm ts:check` -> PASS。
- M4 WHITESPACE：`git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs doc/tasks/20260806-qa-regulation-product-switch-rules/task.md doc/tasks/20260806-qa-regulation-product-switch-rules/execution-log.md` -> PASS，只有 Git CRLF 工作区提示。
- M4 VALIDATOR：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260806-qa-regulation-product-switch-rules/bug-regression-evidence.md` -> PASS。
- M4 VALIDATOR：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-qa-regulation-product-switch-rules/frontend-feature-evidence.md` -> PASS。
- M4 CLEANUP PREVIEW：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-qa-regulation-product-switch-rules --mode preview` -> BLOCKED，原因：主工作区 `E:\IntRuoyi` 脏状态不能接收 ff-only merge，且当前分支当时尚未包含最新 `origin/int_main`。
- M4 CLEANUP APPLY：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-qa-regulation-product-switch-rules --mode apply --worktree-closeout off` -> PASS，删除临时 `bug-regression-evidence.md` 与 `frontend-feature-evidence.md`，保留核心任务记录。
- EXPERIENCE：已按 `project-experience-consolidation` 搜索现有长期经验文档；本次经验已由 `docs/backend-development.md#QA 规程配置状态必须来自产品级规程记录` 覆盖，无需新增经验文档。
- IMPLEMENTATION COMMIT：`bb9cb14fb fix: isolate QA regulation rules by product`，文件清单：`QaRegulationPage.vue`、`qa-regulation-product-specific-rules-static.spec.cjs`。
- MAINLINE MERGE：`36951d8fd Merge remote-tracking branch 'origin/int_main' into codex/qa-regulation-product-switch-fix`，自动合并 `QaRegulationPage.vue` 后无冲突。
- POST-MERGE GREEN：`node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> PASS。
- POST-MERGE REGRESSION：`node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- POST-MERGE REGRESSION：`node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- POST-MERGE REGRESSION：`node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs` -> PASS。
- POST-MERGE TYPECHECK：`pnpm ts:check` -> PASS。
- PUSH PREFLIGHT：`scripts\preflight\branch-runtime-port-guard.ps1` -> PASS；待推送最大对象 115606 bytes，未超过 GitHub 100 MB 限制。
- PUSH RETRY：首次 `git push -u origin codex/qa-regulation-product-switch-fix` 因 TLS unexpected EOF 失败；按 GitHub 443 门禁检查 proxy、`Test-NetConnection` 和 `git ls-remote origin HEAD` 均可用后重试。
- PUSH：`git push -u origin codex/qa-regulation-product-switch-fix` -> PASS，远端分支 `origin/codex/qa-regulation-product-switch-fix` 已创建并设置 upstream。

## Blockers

- Worktree/slot 删除收尾阻塞：`task-closeout-cleanup` auto 模式需要把当前分支 ff-only 合并回主工作区，但 `E:\IntRuoyi` 当前存在无关脏改动；为避免触碰并行任务或用户改动，本任务不删除 worktree、不释放 slot 3。
