# Execution Log

## User Intent

- QA 规程配置应是 QA 通用配置页面。
- 每个 DCC 项目代码对应一个产品。
- 当前样例对应“按压式球囊扩充压力泵”，但不能把该产品写死为页面结构。
- QA 需要在页面中区分哪些 DCC 项目已经配置 QA 规程、哪些还没有配置。
- 用户要求进行修改。

## Preflight

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md` 和 `docs/powershell-memory.md`。
- 已读取 `frontend-feature-delivery` 技能和 `references/frontend-contract.md`。
- 已读取 `backend-api-delivery` 技能和 `references/backend-contract.md`。
- 运行静态 E2E 合同前已读取 `docs/e2e-rules.md`。
- 已核对 `DccProjectCodeDO`：正式字段包括 `id`、`productMasterId`、`projectName`、`projectCode` 和 `status`。
- 已核对 `QaRegulationPage.vue`：当前页面硬编码压力泵规程来源、产品名称和检验项目初始化数据。
- 已核对 `MesQaInspectionRegulationDO`：当前正式 QA 保存模型尚无 `dccProjectCodeId`，且页面提示正式保存/发布接口未接入。

## Baseline Evidence

- `git status --short --branch` -> 工作区存在多个其它任务的 tracked、staged 和 untracked 改动。
- `git commit -m "Baseline: preserve existing worktree changes before QA regulation update"` -> PASS。
- Baseline commit: `516ef63a1`。
- 基线提交后出现的并行改动：
  - `IntRuoyiFronted/tests/e2e/edhr-batch-page-graph-tab-static.spec.js`
  - `doc/tasks/20260804-mes-item-route-selection/execution-log.md`
  - `doc/tasks/20260804-mes-item-route-selection/verification-report.md`
- 上述并行改动不属于本任务，不修改、不暂存。

## BDD And TDD

BDD: QA 按 DCC 项目代码确定产品范围 -> Given DCC 中存在启用的项目代码且每个项目代码对应一个产品 / When QA 在规程配置页选择项目代码 / Then 页面必须只读展示项目代码、项目名称和产品主数据关系，并将所选项目作为规程范围

BDD: 未选择 DCC 项目代码时阻塞发布 -> Given QA 尚未选择正式 DCC 项目代码 / When 执行发布前检查 / Then 页面必须提示项目范围未完成且不能把固定压力泵数据视为有效配置

BDD: DCC 项目代码读取失败时显式报错 -> Given DCC 项目代码接口返回错误 / When 页面加载项目选项 / Then 页面必须显示可见错误且不得切换到压力泵示例或默认项目

BDD: QA 区分已配置和待配置项目 -> Given DCC 项目代码列表已加载且后端按产品 ID 返回 QA 规程配置状态 / When QA 打开规程配置页 / Then 页面必须分区展示“已配置 QA 规程”和“待配置 QA 规程”，并允许点击项目进入对应配置

BDD: QA 状态接口失败不降级 -> Given 后端 QA 规程状态接口失败或响应缺少请求产品 / When 页面加载配置状态 / Then 页面必须显示状态加载错误且不得把项目静默归入待配置

## Milestone Evidence

- M1 completed：现有 DCC 项目 API `getProjectCodePage` 可直接提供项目选择列表；页面无需新增 mock 或跨模块临时数据源。
- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，首个失败为 `Standalone QA page must use a DCC project selector as the formal product scope.`，证明旧页面仍使用固定压力泵来源卡片。
- M2 completed：BDD 和专用静态契约 RED 已记录。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，首个失败为 `Standalone QA page must expose a DCC project QA configuration status summary.`，证明页面尚未区分已配置和待配置项目。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，首个失败为 `The QA configuration status summary must not report zero projects while DCC data is loading or failed.`，证明加载/错误态仍可能展示误导性的 0/0 汇总。
- M3 completed：`QaRegulationPage.vue` 已新增 `data-qa-regulation-config-status`、`data-qa-regulation-configured-projects`、`data-qa-regulation-unconfigured-projects`；配置状态区分能力已在页面结构中落位。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS；配置状态总览仅在 DCC 加载成功后展示。
- REGRESSION: `node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-dcc-project-code/frontend-feature-evidence.md` -> PASS，输出 `Frontend feature evidence is valid.`。
- Supporting check：只读核对后端 QA 规程模型当前以 `productId` 为正式字段；用户继续要求 QA 明确区分已配置/未配置后，将状态来源从前端模板集合提升为正式后端查询。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，失败原因为缺少 `getQaRegulationProjectStatuses` 和 `/mes/qa/inspection-regulation/project-statuses` 正式状态接口接入。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesQaInspectionRegulationServiceTest" test` -> FAIL，失败原因为新增测试引用的 `MesQaInspectionRegulationProjectStatusRespVO` / `getProjectStatuses` 尚未实现。
- M4 completed：新增 `GET /mes/qa/inspection-regulation/project-statuses`，后端按 `mes_qa_inspection_regulation.product_id` 批量返回配置状态；前端 `QcTemplateApi.getQaRegulationProjectStatuses` 和 `QaRegulationPage.vue` 改为按 `productMasterId` 状态分组，状态接口失败时显示错误。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- REGRESSION: `node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesQaInspectionRegulationServiceTest" test` -> PASS；`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-dcc-project-code/frontend-feature-evidence.md` -> PASS，输出 `Frontend feature evidence is valid.`。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260804-qa-regulation-dcc-project-code/backend-api-evidence.md` -> PASS，输出 `Backend API evidence is valid.`。
- GREEN: scoped `git diff --check` for current task paths -> PASS。
- Experience consolidation：已读取 `project-experience-consolidation` 技能；将“QA 规程配置状态必须来自产品级规程记录，不得由前端 IDI/压力泵模板硬编码判断”的可复用门禁合并到 `docs/backend-development.md`，并更新 `docs/experience-index.md` 关键词路由；未新建长期经验文档。
- GREEN: `rg -n "QA 规程配置状态|project-statuses|mes_qa_inspection_regulation\\.product_id" docs\experience-index.md docs\backend-development.md` -> PASS，能从索引命中新增门禁。

## Real E2E Verification 2026-08-05

- Preflight：已读取 `docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/worktree-restrictions.md`、`docs/task-closeout-rules.md` 和 `docs/powershell-encoding.md`。
- Runtime evidence：`8081` 前端 HTTP 200；`48081` 后端 health `UP`。
- Runtime refresh：运行中的旧 Jar `backend-runtime-control-20260804-213215.jar` 缺少 `MesQaInspectionRegulationProjectStatusRespVO.class`，直接 E2E 会误用旧后端；已只替换运行 Jar 内 `yudao-module-mes` 的本任务 6 个 class，并验证嵌套 MES jar `compress_type=stored`、新 Jar SHA256 `DEFA78D20752D4A35348A4C37C45216823627D3F5848D858A2915F00BDB86ACB`。
- Runtime refresh：`48081` 已切换到 `output/runtime/int_main/backend-runtime-control-20260805-qa-regulation-dcc-status-20260805-003532.jar`，PID `22200`，health `UP`；未记录命令行中的连接密钥。
- GREEN: `node --check tests\e2e\qa-regulation-dcc-status-real.e2e.cjs` -> PASS。
- BLOCKED: `pnpm e2e:qa-regulation:dcc-status:real` -> FAIL fast，`E2E_BLOCKED_QA_DCC_PRODUCT_BINDING: IDI DCC project code id 129 returned productMasterId null; cannot verify backend QA project-statuses split without the formal DCC-to-product binding.`。
- Supporting real-page probe：`/dcc/project-codes/page?pageNo=1&pageSize=50&status=ENABLE` -> business code `0`，`count=50`，`productBoundCount=0`；`keyword=IDI` -> `count=1`，`projectCode=IDI`，`productMasterId=null`。因此页面不会也不应调用产品级 `/mes/qa/inspection-regulation/project-statuses`。
- GREEN: `node scripts\preflight\login-preflight.mjs` with env-sourced local default login, target `/mes/pro/process-pool/qa-regulation`, target text `QA 规程配置` -> PASS。
- GREEN: `node tests\e2e\mes-edhr-qa-menu-real.e2e.js` -> PASS；`writeRequests=[]`、`consoleErrors=[]`、`pageErrors=[]`，截图 `output/playwright/20260804-qa-regulation-tab/edhr-qa-menu-real-e2e.png`。
- GREEN: `node --check tests\e2e\role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` -> PASS。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` -> PASS；先选择 DCC 项目代码 `IDI`，确认 `PQC-IDI-001`、`B/0`、`2026-01-04`、压力泵规程名称和 5 条原文摘录仍保留；`writeRequests=[]`、`consoleErrors=[]`、`pageErrors=[]`，截图 `output/playwright/20260804-qa-regulation-tab/qa-regulation-original-excerpt-real-e2e.png`。
- Blocker：本机正式 DCC 项目代码数据未建立 `IDI -> MDM productMasterId` 绑定；按无 fallback 策略，未用产品名称、固定 IDI、前端模板或默认产品代替正式绑定。