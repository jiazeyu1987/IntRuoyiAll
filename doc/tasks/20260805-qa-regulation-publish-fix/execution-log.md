# Execution Log

## User Intent

- 用户要求“进行修复”，针对 `AC-M09 | QA | 维护检验规程` 当前不符合项，补齐正式维护、发布、不可变版本和发布失败校验链路。
- 用户反馈当前 QA 页面一次性展示内容过多，希望 QA 页面通过 Tab + 标准列表模板形式展示；本次限定为前端 QA 页面信息架构与标准列表模板改造，不修改后端保存/发布接口。
- 用户补充截图口径：QA 项目选择区只显示 `DCC 项目代码` 下拉选择框；选择项目后再显示对应的适用范围、检验规则、检验项目和发布检查，不再显示之前的已配置项和未配置项。
- 用户追问黄框字段是否都可以不用设置：确认路线版本、路线工序、路线 ID、路线版本 ID、路线工序 ID、工序 ID、SOP、生产系数、示例订单数、批记录绑定等不应由 QA 手工设置；产品绑定工艺路线后必须从正式工艺路线和工序配置自动带出。
- 用户进一步要求“支持手动绑定工艺路线”：当产品尚未绑定工艺路线或需修正绑定时，QA 页面需要提供显式路线选择与绑定动作，但路线版本、质检工序、SOP、生产系数和批记录绑定仍由正式路线配置自动解析，不改回黄框字段手工录入。

## Baseline

- `git status --short --branch` 显示进入任务前已有大量前后端、测试和任务文档改动。
- `5486d9ba9`：Baseline commit，保存 71 个进入本任务前的既有改动。
- `fc5e98ffe`：Baseline commit，保存岗位矩阵分析残余文档更新。
- `515798d74`：Baseline commit，保存并发 AC 任务文档更新。
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
- BDD: QA 手动绑定工艺路线 -> Given DCC 项目已绑定 MDM 产品但尚未绑定当前工艺路线 When QA 用户选择一个已有当前生效版本的工艺路线并点击手动绑定 Then 页面调用正式产品-工艺路线绑定 API 写入绑定，重新读取产品当前绑定，并从正式路线版本、质检工序、排产配置和批记录配置带出适用范围。
- BDD: QA 手动绑定失败可见 -> Given 用户选择的路线没有当前生效版本、绑定 API 失败或绑定后无法读取当前产品路线 When 用户点击手动绑定 Then 页面显示可见错误并继续阻断保存/发布，不使用所选路线本地值冒充绑定成功。

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
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 适用范围区域新增 `data-qa-regulation-manual-route-bind`，可选择已有当前生效版本的工艺路线并通过 `saveRouteProductByItem` 写入当前产品绑定，绑定后重新走正式路线范围解析。
- GREEN: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> PASS，手动绑定工艺路线能力接入后标准列表模板系统契约仍通过。
- GREEN: `pnpm ts:check` -> PASS，手动绑定工艺路线相关 Vue/TypeScript 类型检查通过。

## Experience Consolidation

- `docs/frontend-development.md` -> UPDATED，合并 QA 新增 4 个 `UnifiedListTemplate` 后标准列表系统接入点 88、显式隐藏筛选 14 的长期门禁证据。
- `project-experience-consolidation` -> REVIEWED，本次截图口径属于 QA 页面局部信息架构变更，已由任务静态契约锁定“选择区只保留一个必填下拉框、禁止旧状态列表”；未发现需要新增长期经验文档的通用门禁。

## Verification Evidence

- `mvn -pl yudao-module-mes -am "-DskipTests" compile`：PASS，`BUILD SUCCESS`，完成时间 `2026-08-05T12:03:48+08:00`。
- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`：PASS，输出 `PASS role-matrix QA regulation standalone page static contract`。
- `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js`：PASS，输出 `PASS: unified list template empty condition tabs system contract`。
- `pnpm ts:check`：PASS，前端类型检查通过。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md`：PASS，输出 `Frontend feature evidence is valid.`。
- QA 适用范围黄框字段静态契约：PASS，确认源码包含 `loadQaRouteScopeFromProject`、正式工艺路线 API 调用、只读 `data-qa-regulation-route-scope-auto` 展示和 `qaFormalRouteScopeReady` 保存/发布阻断，且不再存在黄框字段输入控件。
- QA 手动绑定工艺路线静态契约：PASS，确认源码包含 `ProRouteApi.getRouteSimpleList`、`ProRouteProductApi.saveRouteProductByItem`、`loadQaRouteScopeFromRouteBinding`、`data-qa-regulation-manual-route-bind` 和绑定失败可见错误。
- `git diff --check -- <AC-M09 实现文件>`：PASS，无 whitespace error，仅有 Git CRLF 工作区提示。
- 目标 JUnit 未通过环境门禁：主工作区持续存在非本任务 Maven 测试进程，导致 `target/classes` 缺失和后续专属 JUnit 超时；按规则未强停他人任务。

## Blockers

- 当前共享工作区仍有并发源码、测试和文档写入，后续提交需选择性暂存本任务文件。
- 后端目标 JUnit 需要在没有其它 `E:\IntRuoyi\IntRuoyiBackend` Maven 进程写入 `yudao-module-mes/target` 时复跑。
