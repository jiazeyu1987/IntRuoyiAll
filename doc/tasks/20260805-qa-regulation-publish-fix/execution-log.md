# Execution Log

## User Intent

- 用户要求“进行修复”，针对 `AC-M09 | QA | 维护检验规程` 当前不符合项，补齐正式维护、发布、不可变版本和发布失败校验链路。
- 用户反馈当前 QA 页面一次性展示内容过多，希望 QA 页面通过 Tab + 标准列表模板形式展示；本次限定为前端 QA 页面信息架构与标准列表模板改造，不修改后端保存/发布接口。
- 用户补充截图口径：QA 项目选择区只显示 `DCC 项目代码` 下拉选择框；选择项目后再显示对应的适用范围、检验规则、检验项目和发布检查，不再显示之前的已配置项和未配置项。
- 用户追问黄框字段是否都可以不用设置：确认路线版本、路线工序、路线 ID、路线版本 ID、路线工序 ID、工序 ID、SOP、生产系数、示例订单数、批记录绑定等不应由 QA 手工设置；产品绑定工艺路线后必须从正式工艺路线和工序配置自动带出。
- 用户进一步要求“支持手动绑定工艺路线”：当产品尚未绑定工艺路线或需修正绑定时，QA 页面需要提供显式路线选择与绑定动作，但路线版本、质检工序、SOP、生产系数和批记录绑定仍由正式路线配置自动解析，不改回黄框字段手工录入。
- 用户反馈截图中手动绑定下拉“不能选择”：当前 QA 下拉把已启用/已发布路线置灰为“已启用，仅回显”，需要改为可选择，并保证后端保存不再走产品维护页的启用路线拦截。
- 用户反馈截图中手动绑定保存失败：前端调用 `/admin-api/mes/pro/route-product/save-qa-regulation-route-by-item` 返回 `请求地址不存在`，需要修复本机 48081 运行态未加载 QA endpoint 的问题。
- 2026-08-05 follow-up：用户反馈顶部黄框里的内容不显示；本轮限定为前端布局回归，要求 `DCC 项目代码` 选择内容显示在顶部 QA 标题黄框区域内，不扩大后端接口、路线绑定或检验项目数据范围。
- 2026-08-05 follow-up：用户反馈截图红框里的内容不显示；本轮限定为隐藏红框标注的顶部说明副标题、绿色正式接口提示、项目选择与页签之间的空白带、页签与表格之间的空白带，保留标题、DRAFT、DCC 项目代码选择、Tab 和检验项目表。
- 2026-08-05 follow-up：用户要求“不显示发布检查的tab”；本轮只从顶部 QA 页签导航移除“发布检查”，保留现有发布校验、草稿保存和发布接口代码，不扩展后端或数据范围。

## Baseline

- `git status --short --branch` 显示进入任务前已有大量前后端、测试和任务文档改动。
- `5486d9ba9`：Baseline commit，保存 71 个进入本任务前的既有改动。
- `fc5e98ffe`：Baseline commit，保存岗位矩阵分析残余文档更新。
- `515798d74`：Baseline commit，保存并发 AC 任务文档更新。
- `f6ea8f545`：并行任务于本轮验证期间创建 `chore: preserve dirty worktree baseline`，吞入本次 `QaRegulationPage.vue` 页签删除、`role-matrix-qa-regulation-tab-static.spec.cjs` 和本任务文档，同时包含大量其它任务文件；当前分支因此 `ahead 1`，本任务不将其声明为独立实现提交、不直接推送。
- 仍观察到 `doc/tasks/20260805-job-matrix-compliance/*` 被并发任务继续写入；本任务不触碰这些文件，提交时只选择性暂存 AC-M09 文件。

## BDD Scenarios

- BDD: 保存 QA 规程草稿 -> Given QA 用户填写产品、路线版本、工序、版本号、首检/巡检/末检规则和检验项目 When 调用保存草稿 Then 后端持久化 DRAFT 规程和 DRAFT 版本但不发布。
- BDD: 发布完整 QA 规程 -> Given 草稿包含首检、巡检、末检和完整检验项目 When 调用发布 Then 后端生成 PUBLISHED 版本、写入 `currentVersionId`、返回不可变发布版本。
- BDD: 缺少必要规则发布失败 -> Given 草稿缺少首检、巡检或末检规则 When 调用发布 Then 后端 fail-fast 返回业务错误且不生成 PUBLISHED 版本。
- BDD: 发布版本不可变 -> Given 规程已发布 When 尝试覆盖同一版本草稿或修改发布版本 Then 后端拒绝并保持原发布快照不变。
- BDD: 前端正式保存发布 -> Given QA 页面已选择 DCC 项目代码并填写完整规程 When 点击保存草稿或发布 Then 调用正式 API，失败时页面显示错误，成功时刷新后台状态。
- BDD: QA 页面默认聚焦总览 -> Given QA 用户进入独立 QA 规程配置页 When 页面加载 Then 默认只展示总览页签中的 DCC 项目范围和适用范围，规则、项目、发布检查和 PQC 预览不再首屏一次性直铺。
- BDD: QA 规则和项目标准列表化 -> Given QA 用户切换到检验规则或检验项目页签 When 查看和编辑列表 Then 内容通过 `UnifiedListTemplate` 承载，并保留原规则编辑、项目新增、项目删除和原文依据选择器。
- BDD: QA 发布检查标准列表化 -> Given QA 用户切换到发布检查页签 When 查看完整性检查和 PQC 任务预览 Then 完整性检查与 PQC 预览通过 `UnifiedListTemplate` 分区展示，保存草稿和发布规程操作仍在发布检查页签内可见。
- BDD: QA 项目选择区只保留下拉框 -> Given QA 用户进入页面 When 尚未选择 DCC 项目代码 Then 项目选择区只显示必填的 DCC 项目代码下拉框，不显示项目详情、配置状态、已配置列表或待配置列表。
- BDD: QA 内容选中后展示 -> Given QA 用户选择一个 DCC 项目代码 When 项目选择成功 Then 页面显示 Tab，并可查看该项目对应的适用范围、检验规则、检验项目和发布检查。
- BDD: QA 适用范围自动带出 -> Given DCC 项目对应产品已绑定正式工艺路线 When QA 用户选择 DCC 项目代码 Then 页面从正式路线、路线版本、路线工序、排产配置和批记录配置加载路线版本、质检工序、正式批记录表单和 SOP/工艺要求，并以只读适用范围展示。
- BDD: QA 黄框字段禁止手工配置 -> Given QA 用户选择 DCC 项目代码 When 页面展示适用范围 Then 不显示路线版本、路线工序、路线 ID、路线版本 ID、路线工序 ID、工序 ID、SOP、生产系数、示例订单数和批记录绑定等手工输入项。
- BDD: QA 缺正式路线范围阻断保存发布 -> Given DCC 项目未绑定正式工艺路线、缺激活版本、缺质检工序或存在多个质检工序 When QA 用户保存草稿或发布 Then 页面显示正式路线范围错误并阻断保存/发布，不用默认值或旧字段冒充成功。
- BDD: QA 手动绑定工艺路线 -> Given DCC 项目已绑定 MDM 产品但尚未绑定当前工艺路线 When QA 用户选择一个已发布/已启用且有 ACTIVE 版本的工艺路线并点击手动绑定 Then 页面调用 QA 专用产品-工艺路线绑定 API 写入绑定，重新读取产品当前绑定，并从正式路线版本、质检工序、排产配置和批记录配置带出适用范围。
- BDD: QA 手动绑定失败可见 -> Given 用户选择的路线没有当前生效版本、绑定 API 失败或绑定后无法读取当前产品路线 When 用户点击手动绑定 Then 页面显示可见错误并继续阻断保存/发布，不使用所选路线本地值冒充绑定成功。
- BDD: 顶部黄框显示项目选择内容 -> Given QA 用户进入规程配置页 When 页面渲染顶部 QA 标题区 Then 顶部区域只保留标题、DRAFT 状态和必填 `DCC 项目代码` 选择框，不显示副标题或绿色接口提示。
- BDD: 红框说明和空白带隐藏 -> Given QA 用户查看规程配置页 When 页面渲染顶部项目选择区、页签和检验项目表 Then 不显示副标题、绿色正式接口提示、项目选择与页签之间的空白带、页签与表格之间的空白带。
- BDD: 发布检查页签隐藏 -> Given QA 用户已选择 DCC 项目代码 When 页面渲染顶部 QA 页签导航 Then 只显示总览、检验规则和检验项目，不显示“发布检查”页签，且现有发布校验与保存发布实现不被替换或降级。

## RED / GREEN Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest" test` -> FAIL，修复前缺少 QA 规程保存/发布 VO、service 方法、错误码与 mapper 方法，证明 AC-M09 发布闭环未实现。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，MES 生产代码编译通过，确认 QA 规程保存/发布服务实现、VO、Controller、Mapper 与错误码生产链路可编译。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，前端 QA 独立页已接入正式草稿保存/发布 API，旧“未写入后台”阻断提示已移除。
- BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`testCompile` 期间 `yudao-module-mes/target/classes` 多个 class 文件报 `NoSuchFileException`；同一主工作区同时存在其它非本任务 Maven 进程写入同一 `target`。
- BLOCKED: `mvn -pl yudao-module-mes -am "-DskipTests" compile` 后接 `mvn -pl yudao-module-mes -am "-Dmaven.compiler.testIncludes=**/MesQaInspectionRegulationServiceTest.java" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT，20 分钟未返回；检查时仍有其它非本任务 Maven 测试在 `E:\IntRuoyi\IntRuoyiBackend` 写入同一模块目标目录。
- BLOCKED: `pnpm ts:check` -> TIMEOUT，604 秒未返回；本任务残留 `pnpm ts:check`/`vue-tsc` 进程已按任务边界停止，未停止其它前端 dev server。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，旧 QA 页面缺少 `UnifiedListTemplate` 导入和 Tab 分区，断言 "Standalone QA page must use the standard UnifiedListTemplate for dense QA lists." 失败。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 页面已改为总览/检验规则/检验项目/发布检查页签，并接入四个标准列表模板。
- GREEN: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> PASS，系统标准列表模板接入点更新为 88，显式隐藏筛选列表更新为 14。
- GREEN: `pnpm ts:check` -> PASS，前端 Vue/TypeScript 类型检查通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md` -> PASS，frontend feature evidence 有效。
- GREEN: QA 页面专属排序接线断言 -> PASS，输出 `PASS QA standard list sort wiring`。
- REGRESSION: `node tests\e2e\unified-list-template-all-headers-sortable-static.spec.js` -> FAIL，失败清单为大量既有页面缺少排序 helper 接线；QA 页面聚焦扫描已显示四个新增列表均接入 `sortColumnAttrs` 与 `handleTemplateSortChange`，该全局历史失败不作为本次完成门禁。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，选择区仍保留旧项目详情/配置状态结构时，断言 "QA project selector area must only keep the required DCC project code select row." 失败。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 项目选择区只保留 1 个必填 `DCC 项目代码` 下拉框，Tab 和内容通过 `v-if="selectedDccProjectCode"` 在选中后展示，并禁止旧已配置/待配置状态列表。
- GREEN: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> PASS，选择区收窄后标准列表模板系统契约仍通过。
- GREEN: `pnpm ts:check` -> PASS，选择区收窄与旧状态逻辑删除后 Vue/TypeScript 类型检查通过。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，旧 QA 页面未从正式工艺路线 API 加载适用范围，且仍可出现黄框字段手工配置入口，断言正式路线范围自动带出和禁止手工黄框字段失败。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 页面选择 DCC 项目后调用正式工艺路线/路线版本/路线工序/排产配置/批记录配置链路，展示只读适用范围，并阻止黄框字段手工输入。
- GREEN: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> PASS，正式路线范围自动带出后标准列表模板系统契约仍通过。
- GREEN: `pnpm ts:check` -> PASS，正式路线范围自动带出、保存发布阻断和黄框字段移除后 Vue/TypeScript 类型检查通过。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，截图口径要求支持手动绑定工艺路线时，旧页面没有路线选择器、`saveRouteProductByItem` 正式绑定调用和绑定后重新解析路线范围。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 适用范围区域新增 `data-qa-regulation-manual-route-bind`，通过 `getRouteItemBindingList` 候选和 QA 专用绑定 API 写入当前产品绑定，绑定后重新走正式路线范围解析。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，截图反馈“不能选择”时，旧页面仍调用 `saveRouteProductByItem`，并用 `CommonStatusEnum.ENABLE` 将已启用路线禁用为“已启用，仅回显”。
- RED: `node tests\e2e\qa-regulation-manual-route-selectable-static.spec.cjs` -> FAIL，截图反馈“不能选择”后新增聚焦静态契约，当前 QA 手动绑定 `<el-option>` 缺少显式 `:disabled="false"`，无法锁住已发布/已启用路线必须可选的回归要求。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProductServiceImplTest,MesProRouteProductBindFromWorkOrdersTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 QA 绑定后端回归初次到达 Surefire 后失败于测试断言消息参数，证明新 QA 专用绑定测试已被执行。
- GREEN: `mvn -rf :yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest,MesProRouteProductBindFromWorkOrdersTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，20 个目标 JUnit 通过，覆盖 QA 新建绑定、修正既有绑定、缺 ACTIVE 版本失败、Controller QA endpoint 和不调用产品维护页 `validateRouteNotEnable`。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 下拉不再禁用已发布/已启用路线，不再显示“已启用，仅回显”，前端调用 `saveQaRegulationRouteProductByItem` 并重读正式产品路线绑定。
- GREEN: `node tests\e2e\qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS，QA 手动绑定下拉选项显式 `:disabled="false"`，不复用产品维护页的 `CommonStatusEnum.ENABLE` 置灰逻辑，标签展示“可绑定”并调用 QA 专用绑定 API。
- GREEN: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> PASS，手动绑定工艺路线能力接入后标准列表模板系统契约仍通过。
- GREEN: `pnpm ts:check` -> PASS，手动绑定工艺路线相关 Vue/TypeScript 类型检查通过。
- RED: 运行态日志 `2026-08-05 20:16:16` -> FAIL，`/admin-api/mes/pro/route-product/save-qa-regulation-route-by-item` 在旧 48081 Jar 中返回 `NoResourceFoundException: No static resource ...`，复现截图 `请求地址不存在`。
- RED: 运行 Jar 检查 -> FAIL，`backend-runtime-control-20260805-172627.jar` 内嵌 `yudao-module-mes-2026.04-SNAPSHOT.jar` 的 `MesProRouteProductController.class` 缺少 `save-qa-regulation-route-by-item` 字符串；源码和 `target/classes` 已包含该 endpoint，说明根因是运行态 Jar 未刷新。
- BLOCKED: `powershell -ExecutionPolicy Bypass -File IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> TIMEOUT，15 分钟未返回；诊断 Maven PID 2004 线程栈显示卡在 `IncrementalBuildHelper.beforeRebuildExecution -> WinNTFileSystem.delete0`，仅停止本任务 Maven/restart PIDs，未停止其它 worktree Java 进程。
- GREEN: 运行 Jar 检查 -> PASS，当前 48081 进程 `backend-runtime-control-20260805-team-leader-employee-profile-hotpatch-20260805-203537.jar` 内嵌 MES 模块包含 `save-qa-regulation-route-by-item`、`saveQaRegulationRouteProductByItem` service/interface/impl 方法。
- GREEN: `http://127.0.0.1:48081/actuator/health` -> PASS，后端状态 `UP`。
- GREEN: 登录态 API 探针 -> PASS，带本机默认测试登录态调用 `POST /admin-api/mes/pro/route-product/save-qa-regulation-route-by-item` 且使用无效 `routeId=-999999`，返回 code `1040501000` / `工艺路线不存在`，不再返回 `请求地址不存在`，且不写入真实绑定。
- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，预期原因：新增黄框显示合同要求 `DCC 项目代码` 选择框位于顶部 QA 标题黄框内，旧布局将选择框拆成黄框下方独立 `ContentWrap`。
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，顶部 `ContentWrap data-qa-regulation-dcc-project` 现在同时包含标题、正式接口提示、`DCC 项目代码` 选择框和加载失败重试区。
- GREEN: `node tests/e2e/qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS，手动绑定下拉仍显式可选并保留 QA 专用保存 API。
- GREEN: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS，末检不适用依据合同未受黄框布局调整影响。
- GREEN: `pnpm ts:check` -> PASS，顶部黄框布局和路线候选接口调整后 Vue/TypeScript 类型检查通过。
- REGRESSION BLOCKED: `node tests/e2e/unified-list-template-empty-tabs-system-static.spec.js` -> FAIL，当前系统标准列表模板接入点为 89，而既有合同锁定 88；这是并行新增接入点计数漂移，不由本轮 QA 顶部布局变更产生。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260805-qa-regulation-publish-fix/task.md doc/tasks/20260805-qa-regulation-publish-fix/execution-log.md` -> PASS，仅有 Git CRLF 工作区提示，无 whitespace error。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md` -> PASS。
- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，预期原因：截图红框隐藏合同要求顶部项目区使用 compact wrapper 且不渲染 `qa-regulation-page__subtitle` / `data-qa-regulation-api-ready`，旧页面仍显示副标题和绿色提示。
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，顶部副标题、绿色正式接口提示已移除，项目区和页签区使用 compact wrapper 去掉红框空白带。
- GREEN: `node tests/e2e/qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS，红框隐藏样式调整后 Vue/TypeScript 类型检查通过。
- REGRESSION BLOCKED: `node tests/e2e/unified-list-template-empty-tabs-system-static.spec.js` -> FAIL，仍为当前系统标准列表模板接入点 89 vs 合同 88 的并行计数漂移。
- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，预期原因：新增合同要求顶部 QA 页签导航不再声明 `label="发布检查" name="verification"`，旧源码仍显示该页签。
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，顶部 QA 页签只保留总览、检验规则和检验项目，静态合同明确禁止 `label="发布检查" name="verification"`。
- GREEN: `node tests/e2e/qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS，手动路线绑定相邻链路未受页签导航调整影响。
- GREEN: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS，末检适用性相邻链路未受页签导航调整影响。
- GREEN: `pnpm ts:check` -> PASS，最新共享工作区 Vue/TypeScript 类型检查通过，先前 `TeamLeaderWorkbenchPage.vue` 并行类型阻塞已解除。
- GREEN: 本机真实只读 Playwright -> PASS，使用 `芋道源码/admin` 登录 `http://127.0.0.1:8081`，选择 `IDI` 后页签文本严格为 `["总览","检验规则","检验项目"]`，`writeRequests=[]`、`pageErrors=[]`。
- REGRESSION BLOCKED: `node tests/e2e/unified-list-template-empty-tabs-system-static.spec.js` -> FAIL，当前并行接入点计数为 91，既有合同锁定 88；本轮未新增或删除 `UnifiedListTemplate` 接入点。
- REGRESSION BLOCKED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` 最新复跑先失败于并行标题栏任务新增的状态标签 CSS 属性顺序断言；源码同时具备 `flex-shrink: 0` 和 `margin-left: auto`，该失败不属于本次页签行为。按前端静态契约隔离门禁新增 `qa-regulation-publish-tab-hidden-static.spec.cjs`，只覆盖三个页签、禁止“发布检查”和保留正式保存/发布实现。
- GREEN: `node tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs` -> PASS，专用最小合同确认顶部严格只有三个页签，且 `saveQaRegulationDraft` / `publishQaRegulation` 正式实现仍保留。
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` 最新复跑 -> PASS，并行标题栏合同调整后大型 QA 合同恢复通过。
- BDD: QA 适用范围截图三色框修复 -> Given QA 用户选择 DCC 项目代码并查看适用范围 When 页面渲染基础信息、路线绑定和路线摘要 Then 黄色“工艺路线来源”说明块不显示，基础字段区采用统一间距网格，手动工艺路线选择框默认选中正式产品路线绑定。
- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，预期原因：旧页面仍渲染 `title="工艺路线来源"` 黄色说明块，且基础字段区缺少 `data-qa-regulation-basic-form` / `qa-regulation-page__basic-grid` / 绑定回填断言。
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，适用范围黄色说明块已移除，基础字段区使用统一网格，选择 DCC 项目和手动保存后均以正式 `routeProduct.routeId` 回填蓝框下拉默认值。
- GREEN: `node tests/e2e/qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- REGRESSION BLOCKED: `pnpm ts:check` -> FAIL，最新全量类型检查被非本任务 `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue` 阻塞：缺少 `submissionMultiFilterDefinitions`、`submissionMultiFilter`、`applySubmissionMultiFilter`、`resetSubmissionMultiFilter` 和 `queryFormRef`。本轮未修改该文件，不扩大修复范围。
- REGRESSION BLOCKED: `node tests/e2e/unified-list-template-empty-tabs-system-static.spec.js` -> FAIL，仍为当前系统标准列表模板接入点 89 vs 合同 88 的并行计数漂移。

## Experience Consolidation

- `docs/frontend-development.md` -> UPDATED，合并 QA 新增 4 个 `UnifiedListTemplate` 后标准列表系统接入点 88、显式隐藏筛选 14 的长期门禁证据。
- `docs/backend-development.md` / `docs/experience-index.md` -> UPDATED，将“产品维护页已启用路线不可维护”和“QA 规程手动绑定必须允许已发布路线”拆成两条门禁；QA 绑定必须调用 `saveQaRegulationRouteProductByItem`，后端校验 ACTIVE 版本但不调用 `validateRouteNotEnable`。
- `docs/backend-development.md` / `docs/experience-index.md` -> UPDATED，补充 QA 规程选择 DCC 项目时必须用正式 `routeProduct.routeId` 回填手动绑定下拉默认值，手动保存后也必须以重读结果作为默认绑定。
- `project-experience-consolidation` -> REVIEWED，本次不新建长期经验文档，相关通用约束已合并到既有 MES 工艺路线产品绑定状态门禁。
- `project-experience-consolidation` -> REVIEWED for top yellow-box display follow-up；`docs/frontend-development.md` 已有截图样式/黄框静态契约门禁，`docs/backend-development.md` 和 `docs/experience-index.md` 已有 QA 手动绑定正式路线候选门禁，本轮不新增长期经验文档。
- `project-experience-consolidation` -> REVIEWED for publish-tab removal；本次仅删除一个业务页签入口，现有 `docs/frontend-development.md` 的截图局部静态契约与真实 E2E 门禁已覆盖，无新增可复用工程经验，不更新长期经验文档。

## Verification Evidence

- `mvn -pl yudao-module-mes -am "-DskipTests" compile`：PASS，`BUILD SUCCESS`，完成时间 `2026-08-05T12:03:48+08:00`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProductServiceImplTest,MesProRouteProductBindFromWorkOrdersTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：首轮到达 Surefire 并暴露新增测试断言缺少消息参数；修正后标准 `-am` 复跑两次因 Maven 进程超时无新 Surefire 结果，已只停止任务自有 PID 47976 和 50448。
- `mvn -rf :yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest,MesProRouteProductBindFromWorkOrdersTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
- `node tests\e2e\qa-regulation-manual-route-selectable-static.spec.cjs`：PASS，输出 `PASS qa-regulation-manual-route-selectable-static`。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：PASS，输出 `PASS role-matrix QA regulation standalone page static contract`。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：当前复跑 BLOCKED，失败于 `Pressure-pump IDI seed data must contain all 22 PDF 5.1 process inspection rows. 5 !== 22`，与本次 QA 手动绑定下拉可选性聚焦链路不同；本次使用 `qa-regulation-manual-route-selectable-static.spec.cjs` 覆盖截图回归。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：本轮复跑 PASS，新增黄框布局合同、22 条 PDF 数据合同和 QA 手动绑定正式接口合同均通过。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：红框隐藏本轮复跑 PASS，新增断言确认副标题、绿色提示和页签空内容不再渲染。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：三色框本轮复跑 PASS，新增断言确认黄色说明 alert 不渲染、红框基础字段区统一网格、蓝框下拉按正式 `routeProduct.routeId` 回填默认绑定。
- `pnpm ts:check`：最新复跑 BLOCKED，失败点均在非本任务 `TeamLeaderWorkbenchPage.vue` 的并行改动，未指向本轮 `QaRegulationPage.vue`。
- `pnpm ts:check`：最新复跑 PASS，先前非本任务类型阻塞已解除。
- 本机真实只读 Playwright：PASS，`芋道源码/admin` 选择 `IDI` 后顶部页签严格为“总览 / 检验规则 / 检验项目”，无后台写请求、无 pageerror。
- `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js`：最新复跑 BLOCKED，当前系统接入点为 91，既有合同锁定 88；与本次页签声明删除无关。
- `node tests\e2e\qa-regulation-publish-tab-hidden-static.spec.cjs`：PASS，输出 `PASS qa-regulation-publish-tab-hidden-static`。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：最新复跑 PASS，并行标题栏断言调整后大型合同已恢复。
- Git 归属复核：`f6ea8f545` 包含本次实现且混入大量并行任务文件；当前 `int_main...origin/int_main [ahead 1]`，本任务未推送。
- `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js`：PASS，输出 `PASS: unified list template empty condition tabs system contract`。
- `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js`：本轮复跑 BLOCKED，当前系统接入点数量为 89，旧合同锁定 88；记录为并行接入点计数漂移，不作为本轮黄框布局修复通过证据。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/bug-regression-evidence.md`：PASS，输出 `Bug regression evidence is valid.`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md`：PASS，输出 `Frontend feature evidence is valid.`
- `pnpm ts:check`：PASS，前端类型检查通过。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md`：PASS，输出 `Frontend feature evidence is valid.`。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/backend-api-evidence.md`：PASS，输出 `Backend API evidence is valid.`。
- 48081 运行态 QA endpoint 探针：PASS，当前运行 Jar 包含 QA endpoint，登录态无效路线请求返回正式业务校验而非 404。
- QA 适用范围黄框字段静态契约：PASS，确认源码包含 `loadQaRouteScopeFromProject`、正式工艺路线 API 调用、只读 `data-qa-regulation-route-scope-auto` 展示和 `qaFormalRouteScopeReady` 保存/发布阻断，且不再存在黄框字段输入控件。
- QA 手动绑定工艺路线静态契约：PASS，确认源码包含 `ProRouteApi.getRouteItemBindingList`、`ProRouteProductApi.saveQaRegulationRouteProductByItem`、`loadQaRouteScopeFromRouteBinding`、`data-qa-regulation-manual-route-bind` 和绑定失败可见错误，并禁止 `CommonStatusEnum.ENABLE` 禁用已发布路线。
- `git diff --check -- <AC-M09 实现文件和经验文档>`：PASS，无 whitespace error，仅有 Git CRLF 工作区提示。
- 目标 JUnit 未通过环境门禁：主工作区持续存在非本任务 Maven 测试进程，导致 `target/classes` 缺失和后续专属 JUnit 超时；按规则未强停他人任务。

## Blockers

- 当前共享工作区仍有并发源码、测试和文档写入，后续提交需选择性暂存本任务文件。
- 后端目标 JUnit 需要在没有其它 `E:\IntRuoyi\IntRuoyiBackend` Maven 进程写入 `yudao-module-mes/target` 时复跑。
