# Verification Report

## Scope

验证用户反馈：工艺路线进入编辑器时是否能在本机 `int_main` 复现前端栈溢出、重复权限提示和自动布局提示。

Status: completed; test server route editor reproduced the core `RangeError`, and local source has been fixed by removing global FcDesigner installation.

## Environment

- Frontend: `http://127.0.0.1:8081`
- Backend: `http://127.0.0.1:48081`
- Test frontend: `http://172.30.30.58:8081`
- Test backend health: `http://172.30.30.58:48081/actuator/health`
- Browser: local Chrome through Playwright
- Login identity: local default `芋道源码/admin`; password and token intentionally not recorded
- Task evidence: summaries retained in `task.md`, `execution-log.md`, and this report; task-owned temporary screenshots/JSON were cleaned after closeout, while `process-route-editor-repro.e2e.cjs` is kept for deployment retest.

## Commands

- `PROCESS_ROUTE_REPRO_SCENARIO=route node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs`
- `PROCESS_ROUTE_REPRO_SCENARIO=bpm node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs`
- `PROCESS_ROUTE_REPRO_BASE_URL=http://172.30.30.58:8081 PROCESS_ROUTE_REPRO_SCENARIO=route PROCESS_ROUTE_REPRO_ARTIFACT_PREFIX=test-server-route- node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs`
- `PROCESS_ROUTE_REPRO_BASE_URL=http://172.30.30.58:8081 PROCESS_ROUTE_REPRO_SCENARIO=bpm PROCESS_ROUTE_REPRO_ARTIFACT_PREFIX=test-server-bpm- node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs`
- `node tests\e2e\mes-route-form-designer-global-import-static.spec.cjs`
- `pnpm ts:check`
- `PROCESS_ROUTE_REPRO_BASE_URL=http://127.0.0.1:8081 PROCESS_ROUTE_REPRO_SCENARIO=route PROCESS_ROUTE_REPRO_ARTIFACT_PREFIX=local-after-fix-route- node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs`
- `PROCESS_ROUTE_REPRO_BASE_URL=http://127.0.0.1:8081 PROCESS_ROUTE_REPRO_SCENARIO=bpm PROCESS_ROUTE_REPRO_ARTIFACT_PREFIX=local-after-fix-bpm- node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260808-process-route-editor-stack-overflow-repro/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260808-process-route-editor-stack-overflow-repro/frontend-feature-evidence.md`

## Result

本机 `int_main` 未复现用户描述的问题。

- 工艺路线编辑页：两次进入 `http://127.0.0.1:8081/mes/pro/route/edit/980091?tab=flow`，路线 `RT000028-IDI / 按压式球囊扩充压力泵`。
- 工艺路线结果：`rangeErrorCount=0`, `pageRangeErrorCount=0`, `permissionResponseCount=0`, `permissionUiMessageCount=0`, `autoLayoutUiMessageCount=0`。
- form-create 设计器页：两次进入 `http://127.0.0.1:8081/bpm/manager/form/edit`。
- form-create 结果：`rangeErrorCount=0`, `pageRangeErrorCount=0`, `permissionResponseCount=0`, `permissionUiMessageCount=0`, `autoLayoutUiMessageCount=0`。

测试服务器复现核心栈溢出。

- 测试服连通性：前端 HTTP 200，后端 health `UP`。
- 测试服工艺路线编辑页：两次进入 `http://172.30.30.58:8081/mes/pro/route/edit/922119?tab=flow`，路线 `RT000028 / 球囊扩张压力泵`。
- 测试服工艺路线结果：`rangeErrorCount=0`, `pageRangeErrorCount=4`, `permissionResponseCount=0`, `permissionUiMessageCount=0`, `autoLayoutUiMessageCount=0`。
- 测试服 stack：4 条 pageerror 均为 `RangeError: Maximum call stack size exceeded`，来源 `http://172.30.30.58:8081/assets/form-designer-3YqQ_Q1F.js`，两次进入各新增 2 条。
- 测试服 BPM form-create 设计器页：两次进入 `http://172.30.30.58:8081/bpm/manager/form/edit`，`pageRangeErrorCount=0`。

## Fix Verification

根因已定位到前端全局插件注册：`IntRuoyiFronted/src/plugins/formCreate/index.ts` 在应用启动时全局 `import FcDesigner from '@form-create/designer'` 并 `app.use(FcDesigner)`，导致 MES 工艺路线编辑页也加载生产构建中的 `form-designer` chunk。修复方式是移除全局 FcDesigner 注册，仅让实际设计器页面局部 import。

- Changed source: `IntRuoyiFronted/src/plugins/formCreate/index.ts` 移除全局 `@form-create/designer` import 和 `app.use(FcDesigner)`。
- Changed source: `IntRuoyiFronted/src/views/infra/build/index.vue` 增加页面局部 `import FcDesigner from '@form-create/designer'`；BPM 表单编辑器原本已有局部 import。
- Regression test: `IntRuoyiFronted/tests/e2e/mes-route-form-designer-global-import-static.spec.cjs` 先 RED 后 GREEN，锁定全局插件不得安装 FcDesigner，同时 BPM/Infra 必须保留局部 import。
- Static contract rerun: `node tests\e2e\mes-route-form-designer-global-import-static.spec.cjs` -> PASS。
- TypeScript: `pnpm ts:check` -> PASS, exit code 0。
- Local route after fix: `local-after-fix-route-process-route-editor-repro-result.json` -> `pageRangeErrorCount=0`, `permissionUiMessageCount=0`, `autoLayoutUiMessageCount=0`。
- Local BPM after fix: `local-after-fix-bpm-process-route-editor-repro-result.json` -> `pageRangeErrorCount=0`，证明实际表单设计器入口仍可进入且未新增栈溢出。
- Evidence validators: bug regression evidence and frontend feature evidence both PASS。
- Build caveat: `pnpm build:local` 未完成，长时间运行无失败输出后手动中断；本报告不将 build 记为 PASS。

## Observations

- 工艺路线编辑页每次进入新增 6 条 console warning，但不是用户反馈的 `RangeError`：2 条 Vue component instance key 枚举 warning，4 条 VueFlow `Edge source or target is missing`。
- VueFlow 缺失边涉及 `980644->process-end`、`process-start->980631`、`process-start->980633`、`process-start->980634`。
- 工艺路线编辑页源码使用 `RouteFlowGraphDesigner.vue` 和 VueFlow；用户提到的 `form-designer-3YqQ_Q1F.js` 更接近 `@form-create/designer` 相关入口。
- `RouteFormContent.vue` 会在 `tab=flow` 时安排一次 `autoLayoutOnEntry`；active 生效版本页面因不可编辑会提前返回，因此本机 active 路线未出现“已按当前关系自动布局”提示。
- 测试服 route 页在进入工艺路线编辑器时确实加载 `form-create-CfdN5T7Y.js` 与 `form-designer-3YqQ_Q1F.js`，并在 `form-designer` chunk 内递归触发栈溢出。
- 测试服本轮没有捕获到“没有该操作权限”接口响应、UI toast 或自动布局 toast，因此这两部分还不能判定已复现。
- `formDesignerResourceCount` 在本机修复后 route 仍包含 form-create runtime/auto-import 资源计数，不等同于 `@form-create/designer` 组件全局安装；静态合同以源码边界锁定真正的 designer import/install。

## Closeout

- Cleanup preview/apply passed with no blocked paths or warnings.
- Kept: `task.md`, `execution-log.md`, `verification-report.md`, and `process-route-editor-repro.e2e.cjs`.
- Deleted: task-owned temporary screenshots, JSON result artifacts, and temporary skill evidence files after their summaries were copied into the retained reports.
- Git/worktree: current workspace is not a linked worktree, so no merge or worktree removal was performed.

## Follow-Up

- 测试服尚未部署本地前端修复；部署后需复跑 test-server route 脚本，期望 `pageRangeErrorCount=0`。
- 若用户仍看到 6 条“没有该操作权限”和 1 条自动布局提示，需要补充当时账号/角色、候选草稿版本入口或更精确点击路径；本轮 test-server route/BPM 自动采集未捕获这两类提示。
