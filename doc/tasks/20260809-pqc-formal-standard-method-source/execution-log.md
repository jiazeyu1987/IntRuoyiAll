# PQC 弹框正式来源修复执行日志

## User Intent

- 用户确认当前“接收标准 / 检验方法”弹框显示的不是预期正式内容，并要求进行修改。
- 截图证据：清洗工序弹框显示“清洗-默认首检规则”“符合/不符合”及“V21工序本地测试夹具补齐”。

## BDD

- BDD: 正式 QA 文本进入一线 PQC 弹框 -> Given 当前工序和检验类型存在已发布 QA 规程项目，When PQC 用户查看该项目的接收标准和检验方法，Then 卡片与弹框显示该正式项目维护的标准和方法文本。
- BDD: 测试夹具不得冒充正式 QA 内容 -> Given规程项目内容带有本地测试夹具来源或默认补齐标记，When 后端构建一线 PQC 运行态，Then 不得把该项目作为正式可执行检验项目返回。
- BDD: 正式配置缺失时失败可见 -> Given 当前待检任务没有可用的正式 QA 项目，When 加载工序运行态，Then 后端明确报错或前端明确显示未配置，不得使用默认首检规则补齐。

## Command Intent

- 只读检查现有实现、历史任务和本地 fixture SQL，定位夹具生成及接口映射路径。
- 后续测试和修改命令将在执行前记录其验证目的，不记录任何凭据。

## Milestone Updates

- 2026-08-09：建立任务目录、目标、BDD、预期验证和设计约束。
- 2026-08-09：已确认当前截图文字由 `m6-local-runtime-qa-pqc-formal-fixture.sql` 生成，接口别名仍直接映射 `standardText/inspectionMethod`。
- 2026-08-09：进一步确认 QA 页面表格按多工序维护正式项目，但旧发布实现把全部项目写入单个 `routeProcessId`；同时页面读取当前编辑态路线工序，而当前订单冻结的是激活路线版本 V27 的路线工序 ID。
- 2026-08-09：新增按激活版本逐工序发布的前端回归合同，准备执行 RED。
- 2026-08-09：根据正式 QA 页面及并行任务约束，将 `清洗/精洗` 明确绑定为“精洗、清洗”，排除未配置的“粗洗”。
- 2026-08-09：首次本地修复曾生成版本 53 并将任务 296-299 绑定到正式文本；Playwright 在订单 `881MO090889`、工序“3. 清洗工序”上确认标准与方法弹框正确。
- 2026-08-09 02:17:29：并行任务 `20260809-frontline-pqc-hide-unconfigured-processes` 命中相同旧 `CODX_QA` 夹具规程 41，将规程和版本 53 退役，并将任务 296-299 取消。停止后续数据库写入，未覆盖并行结果。
- 2026-08-09：用户明确回复“允许”，授权软删除退役夹具规程 41 以释放唯一键，并新建正式 `MES_QA` 清洗规程和新的待检任务；旧版本、旧项目和已取消任务须保留审计。
- 2026-08-09：按精确前置条件执行授权修复事务；软删除旧规程 41，新建正式规程 53、正式版本 54 和待检任务 344-347，未修改旧版本、旧项目或已取消任务。
- 2026-08-09：最终 Playwright 真实页面仅显示 `1. 清洗工序`；首检为 `0/5`，巡检为 `0/113`，标准和方法弹框均来自正式发布 QA 规程快照。
- 2026-08-09：`task-closeout-cleanup` preview/apply 通过；保留三份核心任务记录、修复/回滚 SQL 和两张最终弹框截图，清理任务内临时证据文件、旧截图及含登录快照的 `.playwright-cli` 目录。
- 2026-08-09：删除容器内任务临时文件 `/tmp/codex-create-formal-cleaning-qa.sql`；未停止任务开始前已运行的本地前后端服务。
- 2026-08-09：将“QA 多工序正式发布与退役夹具唯一键必须隔离”经验合并到 `docs/backend-development.md`，并在 `docs/experience-index.md` 增加关键词路由。

## Verification Evidence

- RED: `node tests/e2e/qa-regulation-process-scoped-publish-static.spec.cjs` -> FAIL，预期原因：当前 `QaRouteScopeAutoSource` 只保留一个编辑态路线工序，尚无激活版本工序集合和逐工序 payload 构建逻辑。
- GREEN: `node tests/e2e/qa-regulation-process-scoped-publish-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`、`qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs`、`qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS；直接运行未设置 8GB 堆参数的 `pnpm exec vue-tsc` 曾 OOM，随后按项目脚本使用 `NODE_OPTIONS=--max-old-space-size=8192` 完整通过。
- RED: `node tests/e2e/pqc-cleaning-formal-source-repair-static.spec.cjs` -> FAIL，预期原因：尚未创建精确数据修复和回滚 SQL。
- GREEN: `node tests/e2e/pqc-cleaning-formal-source-repair-static.spec.cjs` -> PASS。
- RED: `node tests/e2e/qa-regulation-process-scoped-publish-static.spec.cjs` -> FAIL，预期原因：旧映射错误地把“粗洗”纳入 `清洗/精洗` 正式范围。
- GREEN: `node tests/e2e/qa-regulation-process-scoped-publish-static.spec.cjs` -> PASS；正式范围仅包含“精洗、清洗”。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，47 tests，0 failures/errors。
- GREEN: Playwright 实际页面 -> 订单 `881MO090889` / `3. 清洗工序` 的“接收标准”和“检验方法”弹框显示 `PQC-ID-001 (G/0)` 正式文本，来源为“发布 QA 规程快照”，页面正文不含“本地测试夹具”或“默认首检规则”。截图位于 `output/playwright/20260809-pqc-formal-standard-method-source/`。
- GREEN: 授权修复事务 -> PASS；正式规程 `53/MES_QA/PUBLISHED` 指向版本 `54/G/0/PUBLISHED`，版本 54 含三条正式项目；新任务为 FIRST 1 条/5 件、PATROL 2 条/226 件、FINAL 1 条/3 件，且新任务明细数为 0。
- GREEN: 最终 Playwright 实际页面 -> PASS；订单 `881MO090889` / `1. 清洗工序` 的标准为“弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。”，方法为“正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。”，来源为“发布 QA 规程快照”，页面不含测试夹具和默认首检规则。
- GREEN: 最终回归 -> 6 个前端定向静态合同全部 PASS，`pnpm ts:check` PASS，任务自有文件 `git diff --check` PASS。

## Blockers

无。并行任务冲突已按用户授权处理，旧审计数据完整保留。
