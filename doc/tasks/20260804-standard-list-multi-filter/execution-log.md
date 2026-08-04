# Execution Log: 标准列表模板支持多维度筛选

## User Intent

- 用户要求按照已讨论的“配置驱动多条件筛选栏”方案进行开发、设计和验证。

## BDD Scenarios

- BDD: 多条件筛选配置渲染 -> Given 页面提供多个筛选定义 / When 用户打开标准列表模板 / Then 模板按配置渲染默认可见筛选项和更多筛选入口，不要求页面手写额外筛选布局。
- BDD: 多条件筛选参数提交 -> Given 用户同时设置文本、下拉和日期范围筛选 / When 点击查询 / Then 前端只提交正式配置映射出的 query params，并将 `pageNo` 重置为 1。
- BDD: 多条件筛选重置 -> Given 用户已经设置多个筛选条件 / When 点击重置或清除条件 / Then 前端清空所有配置驱动筛选参数并重新加载第一页。
- BDD: 排产工单真实页启用多维筛选 -> Given 用户打开 MES 排产工单页面 / When 排产工单主列表渲染 / Then 标准列表模板显示多维筛选控件，并保留原动作栏、表格插槽和分页能力。
- BDD: 排产工单多维筛选提交正式参数 -> Given 用户在排产工单主列表同时填写排产工单号、来源生产工单号和完成筛选 / When 点击多维筛选查询 / Then 列表请求携带正式 `code`、`erpWorkOrderCode`、`completionFilter` 参数且不发送临时 `multiFilters` 参数。
- BDD: 排产工单多维筛选重置 -> Given 排产工单主列表已有多个多维筛选条件 / When 用户点击多维筛选重置 / Then 页面清除筛选条件、回到第一页并通过真实列表接口重新加载。
- BDD: 条件 Tab 动态增删 -> Given 标准列表模板启用多维筛选 / When 用户点击红框区域右侧加号 / Then 组件新增一个条件 Tab，并在左侧减号点击时删除当前 Tab。
- BDD: 条件 Tab 交集查询 -> Given 用户在多个已填写条件 Tab 中选择不同筛选字段和值 / When 点击查询 / Then 前端把所有已填写 Tab 条件一起映射为正式 query 参数，作为交集条件提交。

## Command And Evidence Log

- Loaded frontend feature delivery skill and required project rules before implementation.
- Initial git status: repository is dirty and ahead of origin with many unrelated existing changes; current task will only touch standard list template files and `doc/tasks/20260804-standard-list-multi-filter/`.
- RED: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> FAIL, expected reason: `多维度筛选组件必须存在。`
- GREEN: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> PASS, unified list template multi-filter static contract.
- GREEN: `node tests/e2e/unified-list-template-static.spec.js` -> PASS, existing unified list template static contract remains intact.
- GREEN: target TypeScript syntax transpile check for `useTableMultiFilter.ts`, `UnifiedListTemplate/index.vue`, `TableMultiFilter/index.vue`, and `TableMultiFilter/MultiFilterField.vue` -> PASS.
- BLOCKED: `pnpm ts:check` -> FAIL before current task files on unrelated existing QA template errors: missing `QaInspectionRegulationPublishedVersionVO`, missing `QaInspectionRuleVO`, and missing `getPublishedQaRegulationVersion` export in `@/api/mes/qc/template`.
- BLOCKED: target `pnpm exec eslint ...` did not complete after several minutes with no output; task-owned ESLint process was stopped and recorded as a tool/runtime blocker, not as a pass.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-standard-list-multi-filter/frontend-feature-evidence.md` -> PASS.
- GREEN: `git diff --check -- <task-owned files>` -> PASS, with Git LF/CRLF working-copy warnings only.
- PROJECT_EXPERIENCE: 已读取 `project-experience-consolidation` 技能并搜索现有经验归宿；本次复用 `docs/frontend-development.md#前端静态契约隔离门禁`，没有新增长期经验文档的必要。
- E2E PREFLIGHT: 已读取 `docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/worktree-restrictions.md` 和 Playwright 技能；`npx` 可用，本机 Chrome 可用。
- E2E PREFLIGHT: `8081` 由 `E:\IntRuoyi\IntRuoyiFronted` Vite 进程监听，`48081` 由 `E:\IntRuoyi\output\runtime\int_main` Java 进程监听；前端 HTTP 200，后端 health `UP`。
- E2E REGRESSION: `node scripts/preflight/login-preflight.mjs --base-url http://127.0.0.1:8081 --target-path /system/user --target-text 快速过滤 ...` -> PASS，使用本地 `.env` 默认登录来源且未记录密码；标准列表真实页面可登录并显示快速过滤。
- E2E BLOCKED: `rg -n "showMultiFilter|multiFilterDefinitions|useTableMultiFilter|TableMultiFilter" IntRuoyiFronted/src IntRuoyiFronted/tests/e2e` 只命中模板、hook、组件和静态契约；当前没有任何真实业务页面传入 `showMultiFilter` / `multiFilterDefinitions`。按缺入口门禁，不能用静态合同、API-only 或临时测试控件冒充多维筛选真实 E2E 通过。
- GIT NOTE: 本任务源码文件已被并发基线提交纳入：`71177c0a5` 包含 `TableMultiFilter`、`MultiFilterField`、`useTableMultiFilter` 和 `UnifiedListTemplate` 改动；`b59f5baf4` 包含 `unified-list-template-multi-filter-static.spec.js`。当前工作区仅剩本任务 E2E 证据文档未提交。
- USER DECISION: 用户指定“排产工单”页面作为多维筛选真实业务页面 pilot，解除此前“无真实启用页”的 E2E 阻塞。
- PREFLIGHT: 适用经验门禁新增 `docs/powershell-memory.md#共享分支并发基线提交门禁`；本轮只修改排产工单多维筛选、任务专用静态合同和本任务文档，不混入并行任务改动。
- RED: `node tests/e2e/schedule-order-main-multi-filter-static.spec.js` -> FAIL, expected reason: 排产工单主列表包装组件缺少多维筛选正式类型和模板透传。
- GREEN: `node tests/e2e/schedule-order-main-multi-filter-static.spec.js` -> PASS，排产工单主列表已开启多维筛选并映射正式 query params。
- RED: `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> FAIL, expected reason: 真实页面中 multi-filter 被快速筛选和操作栏挤压成 `0` 宽不可见。
- GREEN: 修复 `UnifiedListTemplate` 多维筛选布局后，`node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> PASS，静态合同锁定 multi-filter 不再允许 `0` 宽回归。
- GREEN: `node tests/e2e/unified-list-template-static.spec.js`、`node tests/e2e/mes-schedule-order-sync-tab-static.spec.js`、`node tests/e2e/mes-schedule-order-replan-visible-filter-static.spec.js` -> PASS。
- GREEN: target SFC/TS syntax transpile check -> PASS。
- GREEN: `pnpm ts:check:schedule` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS；筛选请求含 `code`、`erpWorkOrderCode`、`completionFilter=ALL`，重置请求清除这些参数，目标写请求数 `0`、目标 HTTP 错误数 `0`、runtime issues `0`。
- E2E NOTE: 一个首屏 `completionFilter=INCOMPLETE` GET 被后续请求 supersede 并记录为 `net::ERR_ABORTED`，单独归因，不作为目标链路失败。
- GIT NOTE: `IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue` 中另有同文件非本任务 diff（同步工单 quick-filter handler），未作为本任务改动或验收结论。
- PROJECT_EXPERIENCE CLOSEOUT: 已按 `project-experience-consolidation` 规则把真实 E2E 暴露的 multi-filter `0` 宽复合工具栏布局问题沉淀到 `docs/frontend-development.md#统一列表复合工具栏布局门禁`，并更新 `docs/experience-index.md` 关键词。
- CLEANUP: `task_closeout.py --task-id 20260804-standard-list-multi-filter --mode preview` -> PASS，keep 为 `task.md`、`execution-log.md`、`verification-report.md`、真实 E2E 脚本和 `result.json`，delete 仅为临时 `frontend-feature-evidence.md`。
- CLEANUP: `task_closeout.py --task-id 20260804-standard-list-multi-filter --mode apply` -> PASS，已删除临时 `frontend-feature-evidence.md`。
- GIT BLOCKER: `git status --short --branch` 显示 `int_main...origin/int_main [ahead 10]` 且存在大量并行任务修改/未跟踪文件；本轮不执行 commit/push，避免将非本任务改动混入标准列表多维筛选收尾。
- CHANGE REQUEST: 用户反馈原固定多条件栏方式不好，要求改为条件 Tab + 加减号，查询取所有已填写/激活 Tab 的交集；已记录 `docs/changes/20260804-standard-list-multi-filter-condition-tabs.md`，决策为 Accept。
- RED: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> FAIL, expected reason: 旧实现缺少 `table-multi-filter__tabs-row`、`el-tabs`、`addConditionTab` 和当前 Tab 字段选择器。
- RED: `node tests/e2e/schedule-order-main-multi-filter-static.spec.js` -> FAIL, expected reason: 排产工单 wrapper 仍保留页面级 inline filter 数量特例，默认完成状态条件缺少稳定 `id`。
- GREEN: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> PASS，标准列表多维筛选已改为条件 Tab + 加减号，移除旧“更多筛选” popover 和 chip 摘要。
- GREEN: `node tests/e2e/schedule-order-main-multi-filter-static.spec.js` -> PASS，排产工单页面不再传页面特例 `multiFilterMaxInlineFilters`，默认完成状态条件使用稳定 `id: 'completionFilter'`。
- GREEN: `node tests/e2e/unified-list-template-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-replan-visible-filter-static.spec.js` -> PASS。
- E2E PREFLIGHT: `where.exe npx` -> PASS；`http://127.0.0.1:8081/` -> HTTP 200；`http://127.0.0.1:48081/actuator/health` -> `UP`。
- E2E PREFLIGHT: `node scripts/preflight/login-preflight.mjs ... --target-path /mes/pro/schedule-order --target-text 排产工单` -> PASS，使用本地 `.env` 默认登录来源且未记录密码。
- GREEN: `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS；三个条件 Tab 提交为 `completionFilter=ALL`、`code=SCH-CODEX-FACTOR-20260708093210-20260710-0001`、`erpWorkOrderCode=CODEX-FACTOR-20260708093210`，重置请求清除这些参数，目标写请求数 `0`、目标 HTTP 错误数 `0`、runtime issues `0`。
- E2E NOTE: 一个首屏 `completionFilter=INCOMPLETE` GET 被后续筛选请求 supersede 并记录为 `net::ERR_ABORTED`，单独归因，不作为目标链路失败。
- GREEN: `pnpm ts:check:schedule` -> PASS。
- GREEN: `node --check doc\tasks\20260804-standard-list-multi-filter\schedule-order-multi-filter-real.e2e.cjs` -> PASS。
- GREEN: `git diff --check -- <task-owned files>` -> PASS，仅有 Git LF/CRLF 工作副本警告。
- BLOCKED: `pnpm ts:check` -> FAIL before current task files on unrelated concurrent `BatchPqcLeaderWorkbenchPage.vue(3,26)` error: `Type '"pqcLeader"' is not assignable to type 'EdhrBatchRecordTab'`。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-standard-list-multi-filter/frontend-feature-evidence.md` -> PASS。
- PROJECT_EXPERIENCE CLOSEOUT: 已按 `project-experience-consolidation` 规则复用 `docs/frontend-development.md#统一列表复合工具栏布局门禁`，补充条件 Tab、稳定 condition id、重复正式参数校验、交集查询和禁止页面级 `maxInlineFilters` 特例；同步更新 `docs/experience-index.md` 关键词。
- CLEANUP: `task_closeout.py --task-id 20260804-standard-list-multi-filter --mode preview` -> PASS，keep 为 `task.md`、`execution-log.md`、`verification-report.md`、`frontend-feature-evidence.md`、真实 E2E 脚本和 `result.json`，delete 仅为旧失败产物 `artifacts/schedule-order-multi-filter-real/error.txt`。
- CLEANUP: `task_closeout.py --task-id 20260804-standard-list-multi-filter --mode apply` -> PASS，已删除旧失败产物 `artifacts/schedule-order-multi-filter-real/error.txt`。
