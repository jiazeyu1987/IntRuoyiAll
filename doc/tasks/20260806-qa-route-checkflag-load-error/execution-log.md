# Execution Log

## User Intent

- 用户在 QA 规程配置页选择 `ID / 球囊扩张压力泵 / 112` 后看到错误：`工艺路线范围加载失败：当前工艺路线未标记唯一质检工序，请先在工艺路线中维护 checkFlag。`
- 期望修复该阻断，使已绑定工艺路线的 QA 规程配置可继续加载。

## BDD / TDD

- BDD: QA 路线缺少 checkFlag 但有唯一正式批记录绑定工序 -> Given QA 规程配置页已从产品读取到正式绑定路线和 ACTIVE 版本 When 该路线工序列表没有唯一 `checkFlag=true` 但 BATCH 配置存在唯一启用的 `batchRecordReports` 工序 Then 页面不应显示 `工艺路线范围加载失败`，应继续展示路线版本和适用工序。
- BDD: QA 路线缺少 checkFlag 但已发布路线工序有唯一默认批记录投影 -> Given ACTIVE 版本发布时已把主批记录投影到 `mes_pro_route_process.batch_record_report_id` When BATCH 配置列表不可用或为空 Then QA 页面仍应按唯一路线工序 `batchRecordReportId/code/name` 定位适用工序，不继续提示维护 `checkFlag`。
- BDD: QA 路线缺少 checkFlag 且无批记录投影但有唯一关键工序 -> Given QA 规程配置页已从产品读取到正式绑定路线和 ACTIVE 版本 When 该路线没有唯一 `checkFlag=true`、没有正式 BATCH 批记录候选、但路线工序列表存在唯一 `keyFlag=true` Then 页面应按该正式关键工序加载 QA 适用范围，不继续提示维护 `checkFlag`。
- BDD: QA 路线存在多个 checkFlag -> Given 路线存在多个 `checkFlag=true` 工序 When 加载 QA 适用范围 Then 仍应 fail-fast 提示多个质检工序，避免错误选工序。
- RED: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs` -> FAIL, `QA route resolver must model formal batch-record binding as the deterministic no-checkFlag source.`
- RED: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs` -> FAIL, `QA route resolver must also honor the formal published route-process batchRecordReport projection.`
- GREEN: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs` -> PASS
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS
- GREEN: `node tests\e2e\qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` -> PASS
- GREEN: `node tests\e2e\qa-regulation-pressure-pump-screenshot-pages-static.spec.cjs` -> PASS
- GREEN: `node tests\e2e\qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs` -> PASS
- GREEN: `node tests\e2e\qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs` -> PASS
- GREEN: `pnpm ts:check` -> PASS
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-route-checkflag-fallback-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260806-qa-route-checkflag-load-error` -> PASS, only Git CRLF normalization warnings.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260806-qa-route-checkflag-load-error\bug-regression-evidence.md` -> PASS
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260806-qa-route-checkflag-load-error\frontend-feature-evidence.md` -> PASS
- RED: `node doc\tasks\20260806-qa-route-checkflag-load-error\qa-route-checkflag-real.e2e.cjs` -> FAIL, 真实页面仍显示 `手动绑定工艺路线失败：当前工艺路线未标记唯一质检工序，请先在工艺路线中维护 checkFlag。`
- RED: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs` -> FAIL, `QA route resolver must honor the formal route keyFlag marker when checkFlag and batch-record bindings are absent.`
- GREEN: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs` -> PASS
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS
- GREEN: `node doc\tasks\20260806-qa-route-checkflag-load-error\qa-route-checkflag-real.e2e.cjs` -> PASS, `ID / 球囊扩张压力泵 / 112` 手动绑定后适用范围显示 `质检工序 / 纸塑袋封口（包装）`，`consoleErrors=[]`，`pageErrors=[]`。
- GREEN: `node tests\e2e\qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` -> PASS
- GREEN: `node tests\e2e\qa-regulation-pressure-pump-screenshot-pages-static.spec.cjs` -> PASS
- GREEN: `node tests\e2e\qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs` -> PASS
- GREEN: `node tests\e2e\qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs` -> PASS
- GREEN: `pnpm ts:check` -> PASS
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-route-checkflag-fallback-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260806-qa-route-checkflag-load-error` -> PASS

## Evidence

- 2026-08-06: 已读取 `bug-regression-fix-loop`、`frontend-feature-delivery`、`docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`。
- 2026-08-06: 已读取 `bug-contract.md`、`frontend-contract.md`，并从 `docs/experience-index.md` 命中 QA 规程产品状态与 QA 工艺路线手动绑定门禁。
- 2026-08-06: `git status --short --branch` 显示共享 `int_main` 工作区已有大量非本任务脏改动；本任务只触碰 QA 路线范围相关源码、静态契约和本任务记录。
- 2026-08-06: 根因定位为 `loadQaRouteScopeFromRouteBinding` 在加载 `BATCH` 批记录配置前先调用 `resolveQaRouteProcessFromRoute`，导致多工序路线缺少唯一 `checkFlag=true` 时提前报错，未使用正式批记录绑定中可唯一定位的工序。
- 2026-08-06: 修复为先并行加载路线工序、`SCHEDULE` 配置和 `BATCH` 配置；解析顺序为唯一 `checkFlag=true` 优先，其次单一正式工序，其次唯一启用 BATCH `batchRecordReports` 工序；多个 `checkFlag` 或多个批记录候选继续 fail-fast。
- 2026-08-06: 用户复测仍报 `手动绑定工艺路线失败：当前工艺路线未标记唯一质检工序`。补充定位到发布投影会把 MAIN 批记录写入 `MesProRouteProcessDO.batchRecordReportId`，而当前前端只看 `BATCH` 配置数组；已新增路线工序 `batchRecordReportId/code/name` 唯一候选解析，多个默认批记录报表工序仍 fail-fast。
- 2026-08-06: 已按 `project-experience-consolidation` 评估长期经验归档。经验最适合合并到 `docs/backend-development.md#QA 规程手动绑定必须允许已发布路线`，但该文件已有无关脏改动，本任务不混写长期文档，避免把其它任务改动纳入当前收尾。
- 2026-08-06: 未提交/推送。原因：共享 `int_main` 工作区存在大量无关脏改动，按项目规则若提交需先建立全量脏工作区基线；该基线会纳入非本任务文件，需用户确认后再执行。
- 2026-08-07: 真实 E2E 采集到 `ROUTE-XLSX-00001 / 球囊扩张导管` 的 `SCHEDULE` 和 `BATCH` 配置均为 23 条但无 `checkFlag`、无 `batchRecordReports`、无 `batchRecordReportId` 投影；`route-process/list-by-route` 中仅 `926807 / Z830 / 纸塑袋封口（包装）` 存在 `keyFlag=true`。下一步按唯一正式 `keyFlag` 工序补齐解析并复验。
- 2026-08-07: 已补齐 QA 页面解析顺序：唯一 `checkFlag=true`、单一正式工序、唯一启用 BATCH `batchRecordReports`、唯一发布投影、唯一 `keyFlag=true`；多个 keyFlag 仍 fail-fast，不使用 `formBindings`。
- 2026-08-07: 真实 E2E 通过，结果文件 `doc/tasks/20260806-qa-route-checkflag-load-error/qa-route-checkflag-real-e2e.json` 记录 `ok=true`，截图为 `doc/tasks/20260806-qa-route-checkflag-load-error/qa-route-checkflag-real-e2e.png`。
- 2026-08-07: 按 `project-experience-consolidation` 将经验合并到既有 `docs/backend-development.md#QA 规程手动绑定必须允许已发布路线`，记录无 `checkFlag` 路线的正式解析顺序和禁止 `formBindings`/猜测。
