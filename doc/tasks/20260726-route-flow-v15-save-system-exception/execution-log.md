# Execution Log

## User Intent

用户报告：保存“球囊扩张压力泵”草稿版本 V15 时页面连续提示“系统异常”，截图位置为 MES 系统 > 生产管理 > 工艺流程 > 流转关系图，当前查看草稿版本 V15。
用户补充：草稿应该可以不限制修改，修改并保存之后仍应能继续修改同一个草稿版本。

## BDD / TDD

- BDD: 保存草稿版本 V15 不应系统异常 -> Given 已打开球囊扩张压力泵草稿版本 V15 的流转关系图, When 用户点击保存, Then 系统应保存有效配置或返回可解释的校验错误, 不应返回通用 500 系统异常。
- BDD: 保存错误只提示一次 -> Given 保存按钮会触发关系图校验、关系图保存和工序属性保存, When 任一内部 API 返回业务错误, Then axios 自动错误提示应关闭，子组件不应在 rethrow 前重复 toast，页面只显示一次来自外层保存入口的错误。
- BDD: 草稿保存后仍可继续编辑 -> Given 已打开球囊扩张压力泵草稿版本 V15, When 用户点击普通保存, Then 只保存当前草稿，不弹出“立即提交发布”确认，不调用提交发布流程，用户仍留在 DRAFT 草稿编辑上下文继续修改；只有显式“提交发布”入口会进入审批/发布流程。
- BDD: 复报保存仍不能返回通用系统异常 -> Given 用户在草稿版本 V15 流转关系图页面点击普通保存, When 后端任一保存相关接口失败, Then 必须定位具体失败接口和真实错误原因，并用正式修复让普通保存成功或返回可解释业务校验，不允许继续只显示通用“系统异常”。
- BDD: 显式保存草稿 BATCH 表单槽位后应读回草稿快照 -> Given DRAFT 路线版本已通过表单槽位保存接口显式保存 `batchUseConfigs.formBindings`, When 页面重新读取该 DRAFT 版本的 BATCH flow-config, Then 应返回草稿已保存快照，而不是被当前工序设置覆盖或读回为空。

## Milestone Notes

- 2026-07-26: 已读取 `bug-regression-fix-loop`、`docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/powershell-memory.md`。
- 2026-07-26: 初始 Git 状态显示 `int_main...origin/int_main [ahead 20]` 且存在多个并行脏改动；本任务将避免覆盖并行改动，若进入提交阶段需按项目基线门禁处理。
- 2026-07-26: 真实页面普通保存当前未复现后端 500；此前直接 API 保存已把 V15 关系图 `graphVersion` 从 12 推进到 13，后续真实页面保存又推进到 14，因此本任务停止继续真实写入 V15，改用 Playwright 路由拦截模拟保存接口业务 500。
- 2026-07-26: 根因定位为保存失败提示链路重复：`axios` 业务 500 自动 toast，`RouteFlowGraphDesigner` 在 `validateBeforeSubmit`/`saveFromParent` catch 后再次 toast 并 rethrow，`RouteFormContent.handleSubmitRequest` 外层 catch 第三次 toast。
- 2026-07-26: 实施最小前端修复：关系图校验/保存、路线排产配置保存、工序排产/批记录配置保存 API 增加 `options` 并在聚合保存链路传 `ignoreErrorMessage: true`；`RouteFlowGraphDesigner` 保存入口改为只抛错，由 `RouteFormContent` 统一提示。
- 2026-07-26: 经验沉淀已合并到 `docs/frontend-development.md#前端保存链路重复错误提示门禁`，并在 `docs/experience-index.md` 增加关键词路由。
- 2026-07-26: 一个过宽的本地调试 JSON artifact 已删除；当前任务证据不保留登录请求明细、token 或密码。
- 2026-07-26: 补充定位草稿保存后不可继续编辑风险：`RouteEditPage.handleSaved` 在普通保存成功后默认调用 `confirmSubmitRouteCandidateVersionAfterSave`，弹出“草稿已保存，是否立即提交发布？”；用户若确认会把 DRAFT 推进审批/发布，随后页面按非 DRAFT 只读规则锁定编辑。
- 2026-07-26: 实施草稿保存解耦修复：移除 `RouteFormSavedPayload` / `promptRouteVersionSubmit` 载荷和 `confirmSubmitRouteCandidateVersionAfterSave` 保存后发布确认；`RouteFormContent.submitForm` 成功后只 `emit('success')`，`RouteEditPage.handleSaved` 只标记草稿已保存并清理直建草稿退出 query。
- 2026-07-26: 经验沉淀已合并到 `docs/frontend-development.md#前端草稿保存与提交发布解耦门禁`，并在 `docs/experience-index.md` 增加关键词路由。
- 2026-07-26: 继续收尾复核：复跑两个核心静态回归均 PASS；cleanup preview 初始会删除 `bug-regression-evidence.md`，按 bug 回归证据要求加入 `Cleanup Keep` 后复跑 preview/apply，结果为 keep `task.md`、`execution-log.md`、`verification-report.md`、`bug-regression-evidence.md`，delete/blocked/warnings 均为 none。
- 2026-07-26: 用户复报截图显示点击保存仍提示单条“系统异常”；这说明重复 toast 和保存后提交发布问题不是唯一根因，任务退回 `in_progress`，继续定位真实保存接口失败点。
- 2026-07-26: 复报排查发现 V15 草稿 BATCH 表单槽位直接保存接口返回 code 0，但随后 GET `/mes/pro/route/flow-config?routeId=922119&useType=BATCH&routeVersionId=361` 对目标工序仍读回 `formBindings: []`；根因收敛到候选/草稿读取策略仍按当前工序设置读取，未识别已显式保存的草稿 `batchUseConfigs` 快照。
- 2026-07-26: 实施后端正式修复：保存 DRAFT BATCH 候选快照时写入 `batchRecordBindingSnapshotExplicit` 标记，读取 DRAFT BATCH flow-config 时仅在该标记为 true 时优先返回草稿快照；待审批/待发布版本继续读取当前工序设置，避免把 legacy snapshot 当作显式保存草稿。
- 2026-07-26: 第一次后端修复过宽，完整 `MesProRouteFlowConfigServiceImplTest` 暴露 PENDING_APPROVAL / READY_TO_PUBLISH 两个相邻场景从期望 `FB-LIVE` 变成 `FB-SNAPSHOT`；随后收窄为 DRAFT + 显式标记，完整测试类通过。
- 2026-07-26: 经验沉淀已合并到 `docs/backend-development.md#edhr-批次任务配置来源门禁` 的“草稿 BATCH 快照读写对称边界”，并在 `docs/experience-index.md` 增加关键词路由。
- 2026-07-26: 已读取 `docs/local-runtime.md` 与 `docs/worktree-restrictions.md`；当前未停止或重启 48081 运行态，避免误操作其他 worktree 进程。后端代码级验证已通过，真实页面加载本次后端修复需按本地运行态门禁重建/重启归属明确的后端。
- 2026-07-26: 用户要求使用 int_main 的 `芋道源码/admin` 执行真实 E2E 验证；按规则先读取 `docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/login-access.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md` 和 Playwright skill。仅记录账号标签，不记录用户提供的密码。
- 2026-07-26: int_main E2E 前置检查 BLOCKED：`8081` 为 `E:\IntRuoyi\IntRuoyiFronted` 的 Vite，HTTP 200；但 `48081` 监听 PID 57744 命令行为 `D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --server.port=48081`。按当前规则，`D:\IntRuoyiWorktree\` 下的 worktree 不能占用 `48081`，必须 fail fast，不得强杀、不得随机换端口、不得冒充 `int_main` 后端验证成功。因此未进入浏览器登录、未执行保存写入。
- 2026-07-26: 用户继续要求恢复 int_main 48081。重新读取本地运行态、worktree、任务收尾和 PowerShell 编码规则并复查端口，PID 57744 仍为同一 `D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726` Jar。该进程不属于当前任务，且规则要求发现 worktree 占用 48081 时 fail fast、不得强杀，因此本次恢复未执行停止或启动动作。

## Verification Evidence

- RED: `node tests/e2e/mes-route-flow-save-error-single-toast-static.spec.js` -> FAIL, expected reason: 关系图校验 API 尚不支持 `ignoreErrorMessage` 请求选项，保存失败可能被多层 toast。
- RED: `node tests/e2e/mes-route-draft-save-stays-editable-static.spec.js` -> FAIL, expected reason: 普通保存成功后仍会从 `RouteEditPage.handleSaved` 隐式触发提交发布确认。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 显式保存后的 DRAFT BATCH 草稿快照仍被当前工序设置覆盖，期望 `FB-DRAFT-SAVED`，实际 `FB-LIVE`。
- GREEN: `node tests/e2e/mes-route-flow-save-error-single-toast-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-draft-save-stays-editable-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-candidate-save-publish-prompt-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> first FAIL after over-broad snapshot preference, PENDING_APPROVAL / READY_TO_PUBLISH expected `FB-LIVE` but got `FB-SNAPSHOT`; fixed by requiring DRAFT + explicit marker.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，34 tests, 0 failures。
- REGRESSION: `node tests/e2e/mes-route-draft-save-stays-editable-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/mes-route-flow-save-error-single-toast-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/mes-route-candidate-save-publish-prompt-static.spec.js` -> PASS。
- GREEN: Playwright local page interception -> PASS；登录本机 `127.0.0.1:8081` 后打开 `/mes/pro/route/edit/922119?tab=flow&routeVersionId=361&routeVersionNo=V15&routeVersionStatus=DRAFT`，拦截 `/admin-api/mes/pro/route-process-flow/validate` 和 `/admin-api/mes/pro/route-process-flow/save` 返回成功，断言 validate/save 各 1 次、`/admin-api/mes/pro/route-version/submit-publish` 请求数为 0，页面仍显示“当前查看：草稿版本 V15”，且未写入 V15。
- GREEN: Playwright local page interception -> PASS；登录本机 `127.0.0.1:8081` 后打开 `/mes/pro/route/edit/922119?tab=flow&routeVersionId=361&routeVersionNo=V15&routeVersionStatus=DRAFT`，拦截 `/admin-api/mes/pro/route-process-flow/validate` 返回业务 500，断言可见保存错误提示数量为 1，且未写入 V15。
- REGRESSION: `node tests/e2e/route-batch-record-save-contract-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/mes-route-edit-unsaved-candidate-discard-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/mes-route-flow-graph-only-static.spec.js` -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS。
- BLOCKED (non-target legacy contract): `node tests/e2e/mes-production-config-candidate-gate-static.spec.js` -> BLOCKED before assertions because the test still reads historical `yudao-ui-admin-vue3/...` paths that do not exist in current `E:\IntRuoyi\IntRuoyiFronted`; not used as current task verification.
- REGRESSION: `git diff --check -- <task files>` -> PASS，仅输出 CRLF 提示。
- EXPERIENCE: `rg -n "保存系统异常重复提示|前端保存链路重复错误提示门禁|save error single toast|草稿保存后仍可修改|前端草稿保存与提交发布解耦门禁" docs/experience-index.md docs/frontend-development.md` -> PASS。
- EXPERIENCE: `rg -n "草稿 BATCH 快照显式保存|batchRecordBindingSnapshotExplicit|草稿保存系统异常|草稿 BATCH 快照读写对称边界" docs/experience-index.md docs/backend-development.md` -> PASS。
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260726-route-flow-v15-save-system-exception --mode preview` -> PASS，保留任务核心记录和 bug 回归证据，无删除项、无阻塞、无警告。
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260726-route-flow-v15-save-system-exception --mode apply` -> PASS，deleted_paths 为 none。
- BLOCKED: int_main real E2E preflight -> 8081 frontend OK, 48081 backend is occupied by `D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726\...\yudao-server-exec.jar`, not `E:\IntRuoyi` int_main backend; E2E stopped before login/write per worktree port rule.
- BLOCKED: restore int_main 48081 -> PID 57744 remains owned by an unrelated `D:\IntRuoyiWorktree\...` backend; current rules prohibit force-stopping that process, so `E:\IntRuoyi` backend cannot be started on 48081 until the owning task stops or relocates it.

## Blockers

- 提交/推送未完成：当前分支 `int_main...origin/int_main [ahead 20]`，且存在大量非本任务并行脏改动；按项目门禁，提交前需先处理或基线提交非本任务改动，本次未执行。
- 2026-07-26 复核：当前分支为 `int_main`，`origin` 指向 `https://github.com/jiazeyu1987/IntRuoyiAll.git`；由于工作区仍存在大量非本任务脏改动且分支领先远端 20 个提交，未继续执行提交/推送。
- 运行态加载未完成：本次未重启 48081 后端；若要让页面立即验证本次后端修复，需先确认 48081 PID 归属并按 `docs/local-runtime.md` 隔离构建 Jar 加载门禁重建/重启。
- int_main 真实 E2E 未执行：`48081` 当前被 `D:\IntRuoyiWorktree\` 下的 worktree Jar 占用，违反 int_main 基准端口归属要求；需要先恢复 `E:\IntRuoyi` 后端占用 `48081` 后才能使用 `芋道源码/admin` 做真实页面验证。
