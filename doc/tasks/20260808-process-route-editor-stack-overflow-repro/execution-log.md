# Execution Log

## User Intent

用户反馈：工艺路线里进入编辑器触发前端栈溢出；每次进入编辑器新增 2 条 `RangeError: Maximum call stack size exceeded`，来源 `form-designer-3YqQ_Q1F.js`，同时出现 6 条重复“没有该操作权限”提示和 1 条自动布局提示。当前请求是确认能否复现。

## BDD Scenarios

- BDD: 进入工艺路线编辑器不应产生堆栈溢出或重复权限提示 -> Given 已登录本机默认身份并进入工艺路线页面 / When 打开工艺路线编辑器 / Then 页面不应新增 `RangeError: Maximum call stack size exceeded`，不应重复弹出 6 条“没有该操作权限”，自动布局提示最多应符合一次性预期。

## Milestone Updates

- in_progress: 已建立复现任务记录，准备读取经验索引和运行态前置条件。
- in_progress: 已读取适用规则与经验索引；本轮按真实 E2E、Playwright 浏览器前置、目标链路归因、Vite 动态导入、`int_main 8081/48081` URL 门禁执行复现。
- completed: 已确认本机 `8081/48081` 运行态可用，并通过 Playwright 真实登录默认本机身份进入页面；密码和 token 未写入日志。
- completed: 复现脚本修正登录落点为 `/`，避免登录后提前进入工艺路线页导致列表接口监听错过响应。
- completed: 已完成工艺路线编辑页两次进入、BPM form-create 设计器页两次进入，并保存 JSON 与截图证据。
- completed: 当前本机 `int_main` 未复现用户描述的栈溢出、重复权限提示或自动布局提示。
- completed: 已按 project-experience-consolidation 收尾要求，将 Playwright 登录重定向与目标接口监听顺序经验合并到既有 E2E 规则和经验索引，未新建长期文档。
- completed: 用户追加询问测试服务器是否可复现；已读取服务器、登录和 E2E 门禁，授权范围限定为测试服 `172.30.30.58` 只读真实页面复现，不执行 SSH、发布、重启或远端数据写入。
- completed: 测试服前端 HTTP 200、后端 `/actuator/health` 返回 `UP`；已用同一 Playwright 脚本复跑 route 和 BPM form-create 两个入口。
- completed: 测试服工艺路线编辑页复现核心栈溢出：两次进入每次新增 2 条 `RangeError`，来源 `assets/form-designer-3YqQ_Q1F.js`；未捕获权限 toast 或自动布局 toast。
- completed: 根因定位为 `src/plugins/formCreate/index.ts` 全局 import/install `@form-create/designer`，使 MES 工艺路线编辑页这类非设计器页面加载 `form-designer` chunk。
- completed: 已新增静态合同 `IntRuoyiFronted/tests/e2e/mes-route-form-designer-global-import-static.spec.cjs`，先 RED 锁定全局 FcDesigner 安装，再 GREEN 验证只允许 BPM/Infra 设计器页面局部 import。
- completed: 已移除全局 `app.use(FcDesigner)`，并在 Infra 表单构建页补局部 `import FcDesigner from '@form-create/designer'`；BPM 设计器页已有局部 import。
- completed: 已完成本机修复后 route/BPM 真实页面复验，均未新增 `RangeError`；`pnpm ts:check` 和 evidence validators 均通过。

## Verification Evidence

- `PROCESS_ROUTE_REPRO_SCENARIO=route node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs` -> PASS, 两次进入 `/mes/pro/route/edit/980091?tab=flow`，目标路线 `RT000028-IDI / 按压式球囊扩充压力泵`。
- route summary -> `rangeErrorCount=0`, `pageRangeErrorCount=0`, `permissionResponseCount=0`, `permissionUiMessageCount=0`, `autoLayoutUiMessageCount=0`, `formDesignerResourceCount=18`。
- route observed warnings -> 每次进入新增 6 条 warning：2 条 Vue component instance key 枚举 warning，4 条 VueFlow `Edge source or target is missing`，涉及 `980644->process-end`、`process-start->980631`、`process-start->980633`、`process-start->980634`。
- route artifacts -> `artifacts/process-route-editor-repro-route.json`, `artifacts/route-editor-entry-1.png`, `artifacts/route-editor-entry-2.png`。
- `PROCESS_ROUTE_REPRO_SCENARIO=bpm node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs` -> PASS, 两次进入 `/bpm/manager/form/edit`，该入口实际加载 `@form-create/designer`。
- bpm summary -> `rangeErrorCount=0`, `pageRangeErrorCount=0`, `permissionResponseCount=0`, `permissionUiMessageCount=0`, `autoLayoutUiMessageCount=0`, `formDesignerResourceCount=18`。
- bpm artifacts -> `artifacts/process-route-editor-repro-bpm.json`, `artifacts/bpm-form-editor-entry-1.png`, `artifacts/bpm-form-editor-entry-2.png`。
- 源码定位 -> `RouteFlowGraphDesigner.vue` 含“自动布局”提示与 VueFlow 画布；`@form-create/designer` 出现在 `src/views/bpm/form/editor/index.vue` 与 `src/views/infra/build/index.vue`，工艺路线编辑页未直接嵌入 `fc-designer`。
- 经验沉淀 -> 已更新 `docs/e2e-rules.md#playwright-登录重定向与目标接口监听门禁` 和 `docs/experience-index.md` 关键词路由。
- `Invoke-WebRequest http://172.30.30.58:8081/` -> PASS, HTTP 200；`Invoke-RestMethod http://172.30.30.58:48081/actuator/health` -> PASS, `UP`。
- `PROCESS_ROUTE_REPRO_BASE_URL=http://172.30.30.58:8081 PROCESS_ROUTE_REPRO_SCENARIO=route PROCESS_ROUTE_REPRO_ARTIFACT_PREFIX=test-server-route- node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs` -> PASS, 两次进入 `/mes/pro/route/edit/922119?tab=flow`，目标路线 `RT000028 / 球囊扩张压力泵`。
- test-server route summary -> `rangeErrorCount=0`, `pageRangeErrorCount=4`, `permissionResponseCount=0`, `permissionUiMessageCount=0`, `autoLayoutUiMessageCount=0`, `formDesignerResourceCount=12`。
- test-server route pageErrors -> 4 条均为 `RangeError: Maximum call stack size exceeded`，stack 指向 `http://172.30.30.58:8081/assets/form-designer-3YqQ_Q1F.js:1:21119` 及同 chunk 后续递归调用。
- test-server route artifacts -> `artifacts/test-server-route-process-route-editor-repro-result.json`, `artifacts/test-server-route-route-editor-entry-1.png`, `artifacts/test-server-route-route-editor-entry-2.png`。
- `PROCESS_ROUTE_REPRO_BASE_URL=http://172.30.30.58:8081 PROCESS_ROUTE_REPRO_SCENARIO=bpm PROCESS_ROUTE_REPRO_ARTIFACT_PREFIX=test-server-bpm- node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs` -> PASS, 两次进入 `/bpm/manager/form/edit`。
- test-server bpm summary -> `rangeErrorCount=0`, `pageRangeErrorCount=0`, `permissionResponseCount=0`, `permissionUiMessageCount=0`, `autoLayoutUiMessageCount=0`, `formDesignerResourceCount=12`。
- test-server bpm artifacts -> `artifacts/test-server-bpm-process-route-editor-repro-result.json`, `artifacts/test-server-bpm-bpm-form-editor-entry-1.png`, `artifacts/test-server-bpm-bpm-form-editor-entry-2.png`。
- RED: `node tests\e2e\mes-route-form-designer-global-import-static.spec.cjs` -> FAIL, expected reason: `setupFormCreate` still imported and installed FcDesigner globally, so non-designer pages could load `form-designer` chunk.
- GREEN: `node tests\e2e\mes-route-form-designer-global-import-static.spec.cjs` -> PASS, global plugin has no designer import/install and BPM/Infra designer pages keep local imports.
- Regression source check: `rg -n "@form-create/designer|app\.use\(FcDesigner\)|<fc-designer" src\plugins\formCreate\index.ts src\views\bpm\form\editor\index.vue src\views\infra\build\index.vue` -> PASS, hits only BPM/Infra `<fc-designer>` and local imports.
- TypeScript: `pnpm ts:check` -> PASS, exit code 0.
- Local after-fix route: `PROCESS_ROUTE_REPRO_BASE_URL=http://127.0.0.1:8081 PROCESS_ROUTE_REPRO_SCENARIO=route PROCESS_ROUTE_REPRO_ARTIFACT_PREFIX=local-after-fix-route- node doc\tasks\20260808-process-route-editor-stack-overflow-repro\process-route-editor-repro.e2e.cjs` -> PASS, `pageRangeErrorCount=0`, `permissionUiMessageCount=0`, `autoLayoutUiMessageCount=0`.
- Local after-fix BPM: first run hit login wait timeout before target page; retry with `PROCESS_ROUTE_REPRO_ARTIFACT_PREFIX=local-after-fix-bpm-` -> PASS, `pageRangeErrorCount=0`.
- Evidence validator: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260808-process-route-editor-stack-overflow-repro/bug-regression-evidence.md` -> PASS, `Bug regression evidence is valid.`
- Evidence validator: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260808-process-route-editor-stack-overflow-repro/frontend-feature-evidence.md` -> PASS, `Frontend feature evidence is valid.`
- Diff hygiene: `git diff --check -- <task touched paths>` -> PASS, no whitespace errors; Git reported only LF-to-CRLF normalization warnings for two touched frontend files.
- Build limitation: `pnpm build:local` -> manually interrupted after long runtime with no failure output; observed warnings only were Vite CJS API deprecation and stale Browserslist data, so build is not claimed as PASS.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-process-route-editor-stack-overflow-repro --mode preview` -> PASS, keep `task.md`, `execution-log.md`, `verification-report.md`, `process-route-editor-repro.e2e.cjs`; delete only task-owned temporary artifacts/evidence; blocked `<none>`, warnings `<none>`.
- Cleanup apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-process-route-editor-stack-overflow-repro --mode apply` -> PASS, deleted task-owned screenshots, JSON result artifacts, and temporary evidence files; linked worktree `False`, no merge/remove action.

## Blockers

- 测试服已复现核心 `RangeError`，但尚未部署本地前端修复；部署后需要用同一路径复测测试服 route 编辑页是否从 `pageRangeErrorCount=4` 归零。
- 本轮未复现“没有该操作权限”toast 和自动布局 toast；若用户仍看到这两类提示，需要按对应账号、角色、候选版本入口单独复现。
