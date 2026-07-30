# Execution Log

## User Intent

- 用户指出“工艺路线的版本里，要可以看到正在进行的草稿”，截图显示版本弹窗当前只展示 `ACTIVE`/已替代等历史版本，候选版本工作区提示存在草稿但表格中不可见草稿行。

## Rule And Skill Intake

- 使用 `bug-regression-fix-loop`：这是用户报告的版本弹窗展示缺陷，需要复现、RED/GREEN 和回归验证。
- 使用 `frontend-feature-delivery`：修复范围是前端用户可见版本弹窗列表状态展示。
- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md` 中与 `工艺路线版本`、`候选版本`、`DRAFT`、`草稿` 相关门禁。

## BDD Scenarios

BDD: 版本弹窗展示进行中草稿 -> Given 工艺路线版本列表包含 `DRAFT` 草稿和已生效历史版本 / When 用户打开工艺路线版本弹窗 / Then 表格可以看到正在进行的草稿版本，并仍能看到已生效历史版本

BDD: 非允许候选版本仍不展示 -> Given 工艺路线版本列表包含 `CANCELLED`、`REJECTED`、审核中和待生效等非允许状态 / When 用户打开工艺路线版本弹窗 / Then 表格不显示这些非允许状态，避免只删除旧过滤造成列表污染

BDD: 删除草稿后重新编辑新建草稿 -> Given 版本弹窗中存在当前 `DRAFT` 草稿 / When 用户点击“删除草稿”并确认 / Then 前端调用正式取消候选版本接口、刷新后隐藏已取消草稿；When 用户再次从路线列表点击“编辑” / Then 系统基于当前 `ACTIVE` 版本创建新的 `DRAFT` 草稿

## Milestone Updates

- 2026-07-30：创建任务目录和初始任务文档；记录历史门禁冲突点，本次需求需要将旧 effective-only 口径调整为草稿可见口径。
- 2026-07-30：定位根因在 `IntRuoyiFronted/src/views/mes/pro/route/index.vue` 的 `visibleRouteVersions` 过滤集合，旧实现只允许 `ACTIVE/SUPERSEDED`，导致 `DRAFT` 草稿被表格过滤。
- 2026-07-30：新增并运行 RED 静态合同 `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js`，失败原因符合预期：缺少包含 `DRAFT` 的 `ROUTE_VERSION_WORKSPACE_VISIBLE_STATUS_SET`。
- 2026-07-30：最小实现修复为正向允许 `DRAFT/ACTIVE/SUPERSEDED`，并将旧真实 E2E 脚本重命名为草稿可见口径，避免继续沿用“active-history-only”旧业务命名。
- 2026-07-30：用户补充要求当前草稿可删除，删除后再次点击“编辑”应基于当前已发布版本生成新草稿；计划锁定为 DRAFT 行显示“删除草稿”、删除前确认、成功后刷新并隐藏已取消草稿。
- 2026-07-30：将 DRAFT 行泛化“取消”操作改为“删除草稿”，确认后继续调用正式 `cancelRouteCandidateVersion`；成功路径复用 `runRouteVersionAction`，提示“删除草稿成功”并刷新版本弹窗与路线列表。
- 2026-07-30：保留 `REJECTED` 的“按意见修改”语义，不把“删除草稿”扩展到其它候选状态。
- 2026-07-30：补充编辑入口合同，确认 `OPEN_ROUTE_VERSION_STATUSES` 不包含 `CANCELLED`，重新编辑通过 `ensureSameSourceDraftCandidateForProductionConfig` 使用当前 `activeRouteVersionId` 创建新草稿。
- 2026-07-30：补充后端回归 `cancelDraftThenCreateCandidate_shouldCreateNewDraftFromCurrentActiveVersion`，覆盖原草稿变为 `CANCELLED`、新草稿版本号递增、来源为当前 active。
- 2026-07-30：并行基线提交 `67282a868c449ee0ea652491cfd45dc448b258e9` 已将本任务实现和测试与非本任务改动混合提交；按共享分支门禁记录异常，不擅自重写历史。
- 2026-07-30：同文件并行路线列表布局改动出现后，重新运行三项目标静态合同和 `pnpm ts:check`，均通过；未修改、暂存或提交并行布局 hunks。
- 2026-07-30：执行 `project-experience-consolidation` 复核；“共享分支并发基线提交门禁”和 Playwright 浏览器前置已有正式归宿，本次不新增长期经验文档。

## Verification Evidence

- RED: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js` -> FAIL, expected reason: 旧实现没有 `ROUTE_VERSION_WORKSPACE_VISIBLE_STATUS_SET`，仍隐藏 `DRAFT` 草稿。
- GREEN: `node --check tests/e2e/mes-route-version-list-draft-visible-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js` -> PASS，输出 `PASS: mes route version list shows active drafts and effective history`。
- GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-route-version-workspace-static.spec.js` -> PASS。
- RED: `node tests/e2e/mes-pro-route-version-workspace-static.spec.js` -> FAIL before delete-draft implementation，expected reason: 缺少 DRAFT “删除草稿”入口、确认和刷新合同。
- GREEN: `node tests/e2e/mes-pro-route-version-workspace-static.spec.js` -> PASS，覆盖“删除草稿”按钮、确认文案、用户取消、取消接口和双列表刷新。
- GREEN: `node tests/e2e/mes-route-list-edit-create-candidate-static.spec.js` -> PASS，覆盖 `CANCELLED` 不复用及新草稿来源当前 active。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，17 tests，0 failures，0 errors。
- GREEN: `mvn.cmd "-Dtest=MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" surefire:test` -> PASS，17 tests，0 failures，0 errors；用于标准生命周期阻塞期间直接复验已编译目标测试。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node --check tests/e2e/mes-route-version-list-draft-visible-real.e2e.js` -> PASS。
- GREEN: `git diff --check -- <task-owned files>` -> PASS with CRLF warning only。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\2026-07-30-route-version-draft-visible\bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence E:\IntRuoyi\doc\tasks\2026-07-30-route-version-draft-visible\frontend-feature-evidence.md` -> PASS。
- GREEN: UTF-8 readback for `task.md`、`execution-log.md`、`bug-regression-evidence.md`、`frontend-feature-evidence.md`、`verification-report.md` -> PASS。
- GREEN: 删除草稿实现完成后再次运行两项 evidence validator -> PASS。
- GREEN: 删除草稿实现完成后再次运行 `git diff --check -- doc/tasks/2026-07-30-route-version-draft-visible` -> PASS with CRLF conversion warnings only。

## Blockers

- BLOCKED: `node tests/e2e/mes-route-version-list-draft-visible-real.e2e.js` with `MES_ROUTE_VERSION_LIST_E2E_BASE_URL=http://127.0.0.1:8081` and backend `http://127.0.0.1:48081` failed before page interaction because Playwright Chromium executable is missing at `E:\Int\DevCache\playwright-browsers\chromium_headless_shell-1223\chrome-headless-shell-win64\chrome-headless-shell.exe`。
- RESOLVED: 标准 Maven 首次曾在非本任务 `MesFrontlineWorkstationPostRouteBindingSourceTest.java` 因缺少 `MesFrontlineWorkstationPostRouteBindingSource` 阻塞；并行任务补入该类后按 stale-blocker 门禁重跑标准命令，目标测试 17 项全部通过。该并行类不属于本任务，不纳入本任务提交。
- BLOCKED: commit/push/closeout cannot be completed safely；本轮最终验证快照为 `int_main...origin/int_main [ahead 18, behind 8]`，且并行基线提交 `67282a86` 已混入本任务文件与其它任务文件。
- BLOCKED: 当前共享工作区继续出现非本任务改动，且 `index.vue` 与 `mes-route-list-edit-create-candidate-static.spec.js` 存在同文件并行布局改动；本任务不触碰、不暂存这些 hunks。
