# Execution Log

## User Intent

- QA 中的检验规则跟随产品，不同产品拥有不同检验规则。
- 当前页面中的既有检验规则属于按压式球囊扩充压力泵。

## Root Cause

- 正式保存 payload 已从 DCC 项目绑定读取 `productMasterId` 并提交为 `productId`。
- 页面初始化仍使用 `PRESSURE_PUMP_PROJECT_CODE = 'IDI'` 决定是否加载压力泵草稿和检验项目。
- `qaInspectionTypeRules` 是页面级单例响应式数组，切换 DCC 项目时没有按产品重置或恢复，因此一个产品的规则编辑会串到另一个产品。

## BDD / TDD

- BDD: 压力泵规则只属于正式绑定产品 -> Given `IDI` DCC 项目正式绑定压力泵产品，When QA 选择该项目，Then 页面加载压力泵规程、检验规则和检验项目，并以 `productMasterId` 作为规则状态 key。
- BDD: 不同产品规则互不串用 -> Given QA 已编辑产品 A 的首检数量，When 切换到产品 B，Then 产品 B 显示自己的空白或既有规则，不得继承产品 A 的数量；切回产品 A 时恢复产品 A 的页面草稿。
- BDD: 同一产品跨项目入口复用规则 -> Given 两个 DCC 项目代码绑定同一个 MDM 产品，When 在两个入口间切换，Then 页面复用同一个产品规则草稿而不是创建两份项目代码草稿。
- BDD: 缺少产品绑定时不套压力泵模板 -> Given DCC 项目没有正式 `productMasterId`，When QA 选择该项目，Then 页面清空规则并由现有保存门禁阻塞，不得根据 `IDI` 或产品名称加载压力泵规则。

## Preflight

- SKILL: `bug-regression-fix-loop` -> LOADED。
- SKILL: `frontend-feature-delivery` -> LOADED。
- RULE: `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md` -> READ。
- EXPERIENCE: `docs/experience-index.md` -> READ；命中 QA 产品级规程、前端静态契约隔离和共享分支并行门禁。
- WORKTREE: `D:\IntRuoyiWorktree\qa-regulation-product-rules`，branch `codex/qa-regulation-product-rules`。
- SLOT: `int_main slot 3`，frontend `8084`，backend `48084`；本任务不启动服务。
- BASELINE: worktree 从 `633361dde` 创建，该提交包含前序 QA 标题栏实现 `096651841`；worktree 初始状态 clean。

## Milestone Evidence

- M1 completed：确认 `buildQaRegulationSavePayload()` 使用 `resolveDccProjectProductId(selectedDccProjectCode)` 生成正式 `productId`。
- M1 completed：确认 `applyDccProjectToQaDraft()` 仍按 `projectCode === PRESSURE_PUMP_PROJECT_CODE` 加载压力泵模板。
- M1 completed：确认切换项目时只重置 `qaRegulationDraft` 和 `qaRegulationItems`，未重置 `qaInspectionTypeRules`。
- RED: `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> FAIL，首个失败为缺少 `QaProductRuleDraftSnapshot`，证明旧实现没有产品级规则草稿模型。
- M2 completed：任务专用静态契约同时锁定产品 ID key、切换前保存、切换后恢复、同产品复用和缺产品绑定清空规则。
- M3 completed：新增以正式 `productMasterId` 为 key 的产品草稿快照；切换项目前保存当前产品，目标产品加载独立规程字段、检验规则和检验项目，同一产品跨 DCC 项目入口复用同一草稿。
- M3 completed：压力泵既有模板只通过 `IDI` DCC 项目的正式 `productMasterId` 登记产品归属；项目代码不再直接决定当前页面加载哪套规则。
- M3 completed：缺少 `productMasterId` 时清空规程、检验规则和检验项目，并保留既有保存阻塞。
- GREEN: `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> PASS。
- REGRESSION: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- REGRESSION: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- REGRESSION: `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs` -> PASS。
- DEPENDENCY: worktree 初始无 `node_modules`；两次较短 `pnpm install` 尝试超时且未生成 `cross-env`/`vue-tsc` 链接，未修改锁文件。
- GREEN: `pnpm install --frozen-lockfile --ignore-scripts --child-concurrency=1 --reporter append-only` -> PASS，复用锁文件安装 1103 个包。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: scoped `git diff --check` -> PASS；只有 LF/CRLF 工作区提示，无 whitespace error。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-qa-regulation-product-specific-rules/bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-product-specific-rules/frontend-feature-evidence.md` -> PASS。
- GREEN: `python -X utf8` 读取全部任务 Markdown -> PASS，UTF-8 正常。
- FORMAT: 新增静态合同 `pnpm exec prettier --check` -> PASS；`QaRegulationPage.vue` 的可选整文件 Prettier 检查仍报告文件级格式差异，本任务未执行整文件自动重写，避免扩大为无关格式化改动。
- M4 completed：聚焦合同、三个相邻 QA 合同、`pnpm ts:check`、两份技能 evidence validator 和 scoped diff/UTF-8 检查均通过。
- EXPERIENCE: 已运行 `project-experience-consolidation`；将“QA 页面未保存规则状态也必须以 `productMasterId` 隔离，禁止共享可变规则数组或由项目代码直接选择当前规则”合并到既有 `docs/backend-development.md` QA 产品级规程门禁，并更新 `docs/experience-index.md` 关键词；未新建长期经验文档。
- IMPLEMENTATION COMMIT: `d99c2a0a3 fix: isolate QA inspection rules by product`；仅包含本任务生产代码、正式静态回归、任务证据和既有经验文档更新。
- CLEANUP PREVIEW: `task_closeout.py --mode preview --worktree-closeout off` -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，删除两份已归档并通过 validator 的临时技能 evidence，无 blocked/warnings。
- CLOSEOUT MODE: 主工作区 `E:\IntRuoyi` 存在并行任务脏改动，本任务不让 cleanup 脚本自动合并或清理该工作区；仅关闭脚本自动 worktree merge，后续按既有“并行主工作区远端快进融合门禁”使用独立集成 worktree 更新 `origin/int_main`。
- CLEANUP APPLY: `task_closeout.py --mode apply --worktree-closeout off` -> PASS，已删除 `bug-regression-evidence.md` 和 `frontend-feature-evidence.md`；默认保留的三份任务记录完整。
- MAINLINE SYNC: `13224eadd` 将最新 `origin/int_main`（`adc862527`）合入本任务分支；远端新增只涉及前序 QA 版本发布任务记录，无冲突。
- POST-MERGE REGRESSION: 产品级规则聚焦合同、QA 页面合同、末检适用性合同、版本发布标题栏合同 -> 全部 PASS。
- POST-MERGE GREEN: `pnpm ts:check` -> PASS。
- POST-MERGE GUARD: `branch-runtime-port-guard.ps1` 与 `git diff origin/int_main...HEAD --check` -> PASS。
- TASK BRANCH PUSH: `origin/codex/qa-regulation-product-rules` -> `bee130fc8`，任务实现、清理和验证记录均已远端保存。
- MAINLINE PUSH: `origin/int_main` -> `bee130fc8`，确认包含 `d99c2a0a3` 实现、`38db4720c` cleanup、`13224eadd` 主线同步和 `bee130fc8` 验证记录。
- ORIGINAL WORKTREE REMOVAL: `D:\IntRuoyiWorktree\qa-regulation-product-rules` 的 Git 注册已先行移除；确认无 `.git`、无目标进程、`8084/48084` 无监听后，对残留 `node_modules` 使用任务专用空目录 `robocopy /MIR` 清空，并删除精确目标目录；最终 `Test-Path=False`。
- ORIGINAL SLOT RELEASE: `qa-regulation-product-rules` 登记项已在物理目录删除后标记 `active=false`，记录 `deletedAt`、`cleanupTask=20260805-qa-regulation-product-specific-rules`。
- INTEGRATION WORKTREE REMOVAL: `codex/qa-regulation-product-rules-integration` 与 `origin/int_main` 均为 `bee130fc8`，`merge-base --is-ancestor`、未推送提交计数、clean status、端口 guard、`8086/48086` 无监听和目标进程检查均通过；`git worktree remove --force` -> PASS，最终 Git 注册和物理目录均不存在。
- INTEGRATION SLOT RELEASE: `qa-regulation-product-rules-integration` 登记项已在目录删除后标记 `active=false`，记录 `deletedAt`、`cleanupTask=20260805-qa-regulation-product-specific-rules`。
- FINAL REGISTRY CHECK: 端口登记表 JSON 解析 -> PASS；两个任务登记项均 inactive，活动项 `profile/slot`、前端端口和后端端口重复数均为 `0`。
- M5 completed：清理、提交、任务分支推送、远端主线融合、原任务 worktree 残留删除、集成 worktree 删除和两个槽位释放全部完成。

## Blockers

- 无。
