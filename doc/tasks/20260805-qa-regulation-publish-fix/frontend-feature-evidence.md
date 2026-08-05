# Frontend Feature Evidence

## Feature Goal

- 将 `QaRegulationPage.vue` 的 QA 规程配置内容从一次性直铺展示改为“顶部黄框内 DCC 项目代码下拉选择 + 选中后 Tab 分区”，顶部只显示总览、检验规则和检验项目三个页签，不显示“发布检查”；现有发布校验、草稿保存和发布接口代码保持不变。路线版本、路线工序、SOP、正式批记录绑定等适用范围字段从产品绑定的正式工艺路线自动带出，不再由 QA 手工设置；产品未绑定或需修正时，允许 QA 显式选择工艺路线并写入产品当前路线绑定；适用范围说明块按截图隐藏，基础字段统一网格间距，手动绑定下拉按正式上次绑定关系默认选中。

## Non-Goals

- 不修改 QA 保存草稿、发布规程、DCC 项目代码或项目配置状态后端接口；本次仅移除页面选择区对旧已配置/待配置状态列表的展示和加载。
- 不新增黄框字段的页面级手工配置入口，不用默认值、旧字段、`formBindings` 或 mock 数据补齐缺失的正式工艺路线范围。
- 不新增路线版本、路线工序、SOP、生产系数或批记录表单的逐项手工输入；手动能力只允许绑定“工艺路线”这一正式产品路线关系。
- 不引入 mock 数据、静默降级或备用数据源。

## Requirements And Acceptance

- AC-M09：QA 可维护并发布检验规程，页面需降低信息密度且保持正式保存/发布链路。
- 验收：未选择项目时只展示必填 `DCC 项目代码` 下拉；选择后顶部只展示总览、检验规则和检验项目三个 Tab，不显示“发布检查”；旧已配置/待配置列表不再显示；适用范围从正式工艺路线自动带出，路线版本、路线工序、路线 ID、路线版本 ID、路线工序 ID、工序 ID、SOP、生产系数、示例订单数和批记录绑定不再作为手工输入项出现；产品未绑定路线时可在适用范围区域显式选择工艺路线并绑定。
- 截图回归验收：顶部项目区只保留 QA 标题、DRAFT 状态和必填 `DCC 项目代码` 选择框；红框标注的副标题、绿色正式接口提示、项目选择与页签之间空白带、页签与表格之间空白带均不显示；适用范围黄色“工艺路线来源”说明块不显示；基础字段区间距统一；手动工艺路线下拉记住并默认选中正式上次绑定关系。

## UI Entry Points And Owned Files

- Route: `/mes/pro/process-pool/qa-regulation`
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`
- Static contracts: `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`, `IntRuoyiFronted/tests/e2e/qa-regulation-manual-route-selectable-static.spec.cjs`

## API Contracts And Data States

- Existing API wrappers retained: DCC project code page, draft save, publish.
- Formal route scope APIs used by the page: `ProRouteProductApi.getRouteProductByItem(productId)`, `ProRouteApi.getRoute(routeId)`, `ProRouteApi.getRouteVersion(routeVersionId)`, `ProRouteProcessApi.getRouteProcessListByRoute(routeId)`, and `ProRouteFlowConfigApi.getProcessConfigList(routeId, 'SCHEDULE'/'BATCH', routeVersionId)`.
- Manual route binding APIs used by the page: `ProRouteApi.getRouteItemBindingList()` and `ProRouteProductApi.saveQaRegulationRouteProductByItem({ itemId, routeId })`; QA manual binding does not disable published/enabled route options. Selecting a DCC project preselects the route returned by `getRouteProductByItem(productId)` and loads route options so the default has a readable label. After saving, it re-reads `getRouteProductByItem(productId)`, uses that returned `routeProduct.routeId` as the select default, and only then applies the formal route scope.
- Data states retained: DCC project loading, load error, retry, selected DCC project, route-scope loading/error, save/publish failure messages.
- Removed from the page state: configured/unconfigured project status groups and selector-area project details.
- Save/publish state now requires `qaFormalRouteScopeReady`; missing or ambiguous formal route scope remains a visible blocking error.

## BDD Scenarios

- BDD: QA 页面默认聚焦总览 -> Given QA 用户进入独立 QA 规程配置页 When 页面加载 Then 默认只展示总览页签中的 DCC 项目范围和适用范围，规则、项目、发布检查和 PQC 预览不再首屏一次性直铺。
- BDD: QA 规则和项目标准列表化 -> Given QA 用户切换到检验规则或检验项目页签 When 查看和编辑列表 Then 内容通过 `UnifiedListTemplate` 承载，并保留原规则编辑、项目新增、项目删除和原文依据选择器。
- BDD: 发布检查页签隐藏 -> Given QA 用户已选择 DCC 项目代码 When 页面渲染顶部 QA 页签导航 Then 只显示总览、检验规则和检验项目，不显示“发布检查”页签，且现有发布校验与保存发布实现不被替换或降级。
- BDD: QA 项目选择区只保留下拉框 -> Given QA 用户进入页面 When 尚未选择 DCC 项目代码 Then 项目选择区只显示必填的 DCC 项目代码下拉框，不显示项目详情、配置状态、已配置列表或待配置列表。
- BDD: QA 内容选中后展示 -> Given QA 用户选择一个 DCC 项目代码 When 项目选择成功 Then 页面显示总览、检验规则和检验项目三个 Tab，并可查看对应内容。
- BDD: QA 适用范围自动带出 -> Given DCC 项目对应产品已绑定正式工艺路线 When QA 用户选择 DCC 项目代码 Then 页面只读展示正式路线来源、路线版本、质检工序、正式批记录表单和 SOP/工艺要求。
- BDD: QA 缺正式路线范围阻断保存发布 -> Given 正式路线绑定、激活版本或唯一质检工序缺失 When QA 用户保存草稿或发布 Then 页面显示路线范围加载失败并阻断动作，不以默认值或手工黄框字段替代。
- BDD: QA 手动绑定工艺路线 -> Given DCC 项目对应产品缺少当前工艺路线 When QA 用户选择已发布/已启用且有当前生效版本的工艺路线并点击手动绑定 Then 页面通过 QA 专用产品路线绑定 API 写入绑定，重新读取当前绑定，并从正式路线配置带出适用范围。
- BDD: QA 手动绑定失败可见 -> Given 绑定 API 失败、路线缺当前版本或绑定后读取不到产品路线 When 用户点击手动绑定 Then 页面显示错误并阻断保存/发布。
- BDD: 顶部黄框显示项目选择内容 -> Given QA 用户进入规程配置页 When 页面渲染顶部 QA 标题区 Then 顶部区域只保留标题、DRAFT 状态和必填 `DCC 项目代码` 选择框，不显示副标题或绿色接口提示。
- BDD: 红框说明和空白带隐藏 -> Given QA 用户查看规程配置页 When 页面渲染顶部项目选择区、页签和检验项目表 Then 不显示副标题、绿色正式接口提示、项目选择与页签之间的空白带、页签与表格之间的空白带。
- BDD: 适用范围说明块隐藏 -> Given QA 用户选择 DCC 项目代码 When 适用范围卡片渲染 Then 黄色“工艺路线来源”说明块不显示，正式路线错误仍通过错误 alert 可见。
- BDD: 基础字段区统一排版 -> Given QA 用户查看适用范围基础信息 When 页面渲染规程编号、规程名称、版本、生效日期和产品 Then 字段使用专用网格布局并保持统一间距。
- BDD: 手动绑定默认回填 -> Given 产品已有正式工艺路线绑定 When QA 用户选择该 DCC 项目或手动绑定后重新读取 Then 工艺路线下拉默认选中正式 `routeProduct.routeId`，不使用本地缓存或默认值。

## RED

- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:旧 QA 页面缺少 `UnifiedListTemplate` 导入和 Tab 分区，断言 "Standalone QA page must use the standard UnifiedListTemplate for dense QA lists." 失败。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:旧选择区仍保留项目详情、配置状态或已配置/待配置列表，断言 "QA project selector area must only keep the required DCC project code select row." 失败。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:旧 QA 页面未从正式工艺路线 API 带出适用范围，且黄框字段仍可能作为手工输入项出现。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:旧 QA 页面缺少手动绑定工艺路线选择器、QA 专用 `saveQaRegulationRouteProductByItem` 调用和绑定后路线范围重读。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:旧 QA 页面把 `CommonStatusEnum.ENABLE` 路线禁用为“已启用，仅回显”，导致截图中的已发布路线不能选择。
- RED: `node tests\e2e\qa-regulation-manual-route-selectable-static.spec.cjs` -> FAIL, expected reason:QA 手动绑定下拉的 `<el-option>` 缺少显式 `:disabled="false"`，无法锁住“已发布/已启用路线仍可选”的截图回归要求。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:顶部黄框显示合同要求 `DCC 项目代码` 选择框位于顶部 QA 标题黄框内，旧布局将选择框拆成黄框下方独立 `ContentWrap`。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:红框隐藏合同要求 compact wrapper、移除副标题和绿色提示，旧页面仍显示 `qa-regulation-page__subtitle` 与 `data-qa-regulation-api-ready`。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:旧页面仍渲染黄色“工艺路线来源”说明块，基础字段区缺少专用统一网格，手动下拉未从正式 `routeProduct.routeId` 回填默认绑定。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:旧源码仍声明 `label="发布检查" name="verification"`，不符合顶部只显示三个 QA 页签的要求。

## GREEN

- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 页面已具备 Tab 分区和四个 `UnifiedListTemplate` 列表。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 项目选择区只剩 1 个必填 DCC 项目代码下拉框，Tab 与内容在选中项目后显示，旧状态列表源码和 UI selector 均被禁止。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 适用范围从正式工艺路线/路线版本/路线工序/排产配置/批记录配置自动带出，黄框字段不再提供手工输入，缺正式路线范围时保存/发布被阻断。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 页面支持显式手动绑定已发布/已启用工艺路线，调用 `saveQaRegulationRouteProductByItem`，绑定成功后重新走正式产品路线绑定和路线范围解析链路。
- GREEN: `node tests\e2e\qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS，QA 手动绑定路线选项显式可选，不复用产品维护页的启用路线置灰守卫，不显示“已启用，仅回显”，并保留 QA 专用绑定 API。
- GREEN: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> PASS，系统标准列表接入点从 84 更新为 88，显式隐藏筛选列表从 10 更新为 14。
- GREEN: `pnpm ts:check` -> PASS，Vue/TypeScript 类型检查此前通过；最新复跑被非本任务 `TeamLeaderWorkbenchPage.vue` 并行改动阻塞。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，顶部区域只保留标题、DRAFT、`DCC 项目代码` 选择框和加载失败重试区，副标题、绿色提示和红框空白带均不再渲染，且手动绑定候选恢复为 `ProRouteApi.getRouteItemBindingList()`。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，适用范围黄色说明块不再渲染，基础字段区使用 `data-qa-regulation-basic-form` / `qa-regulation-page__basic-grid` 统一间距，手动绑定下拉按正式 `routeProduct.routeId` 回填默认值并加载候选标签。
- GREEN: `node tests\e2e\qa-regulation-final-applicability-static.spec.cjs` -> PASS，末检不适用依据合同未受黄框布局调整影响。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，顶部 QA 页签只保留总览、检验规则和检验项目，合同禁止显示“发布检查”页签。
- GREEN: `pnpm ts:check` -> PASS，最新共享工作区 Vue/TypeScript 类型检查通过。
- GREEN: 本机真实只读 Playwright -> PASS，`芋道源码/admin` 选择 `IDI` 后页签严格为 `["总览","检验规则","检验项目"]`，无后台写请求和 pageerror。

## Verification

- Verification: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs IntRuoyiFronted/tests/e2e/unified-list-template-empty-tabs-system-static.spec.js doc/tasks/20260805-qa-regulation-publish-fix/task.md doc/tasks/20260805-qa-regulation-publish-fix/execution-log.md doc/tasks/20260805-qa-regulation-publish-fix/verification-report.md doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md docs/backend-development.md docs/experience-index.md` -> PASS，仅有 Git CRLF 工作区提示，无 whitespace error。
- Verification: `node tests\e2e\qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS，输出 `PASS qa-regulation-manual-route-selectable-static`。
- Verification: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，输出 `PASS role-matrix QA regulation standalone page static contract`。
- Verification: `node tests\e2e\qa-regulation-final-applicability-static.spec.cjs` -> PASS，输出 `PASS qa-regulation-final-applicability-static`。
- Verification: `pnpm ts:check` -> BLOCKED，最新复跑失败于非本任务 `TeamLeaderWorkbenchPage.vue` 的 `submissionMultiFilter*` / `queryFormRef` 类型错误。
- Verification: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，输出 `PASS role-matrix QA regulation standalone page static contract`，覆盖本轮黄色说明隐藏、基础字段网格和默认绑定回填。
- Verification: `node tests\e2e\qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS，输出 `PASS qa-regulation-manual-route-selectable-static`。
- Verification: `node tests\e2e\qa-regulation-final-applicability-static.spec.cjs` -> PASS，输出 `PASS qa-regulation-final-applicability-static`。
- Verification: `pnpm ts:check` -> prior PASS before the latest unrelated `TeamLeaderWorkbenchPage.vue` type-check blocker.
- Verification: QA 页面专属排序接线断言 -> PASS，输出 `PASS QA standard list sort wiring`。
- Verification: `mvn -rf :yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest,MesProRouteProductBindFromWorkOrdersTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，20 个后端 QA 路线绑定目标 JUnit 通过。
- Verification: 全局 `unified-list-template-all-headers-sortable-static.spec.js` 仍被大量既有页面阻塞；QA 页面聚焦扫描显示四个新增列表均已接入 `sortColumnAttrs` 与 `handleTemplateSortChange`，未作为本次完成门禁。
- Verification: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> BLOCKED，当前系统标准列表模板接入点为 89，而既有合同锁定 88；记录为并行新增接入点计数漂移。
- Verification: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，覆盖本轮“发布检查”页签隐藏。
- Verification: `pnpm ts:check` -> PASS。
- Verification: 本机真实只读 Playwright -> PASS，页签为“总览 / 检验规则 / 检验项目”，`writeRequests=[]`、`pageErrors=[]`。
- Verification: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> BLOCKED，当前系统标准列表模板接入点为 91，而既有合同锁定 88；本轮未改变模板接入点数量。

## Responsive Accessibility Loading Empty Error Permission

- Preserve existing responsive `.qa-regulation-page__layout` behavior and Element Plus controls.
- Preserve visible DCC project loading/error/retry states.
- DCC project loading/error/retry states now render inside the top yellow header panel.
- Preserve visible formal route-scope loading/error states through `data-qa-regulation-route-scope-auto` and `data-qa-regulation-route-scope-error`.
- Preserve visible manual route binding loading/error states through `data-qa-regulation-manual-route-bind` and `data-qa-regulation-manual-route-error`.
- The informational route-source alert is hidden, but formal route-scope errors remain visible through `data-qa-regulation-route-scope-error`.
- The basic applicability fields use a responsive two-column grid with a single-column mobile collapse.
- Hide QA regulation tabs/content until `selectedDccProjectCode` exists.
- Preserve existing route permission contract in `remaining.ts`.

## Blockers

- Original AC-M09 backend target JUnit remains blocked by the shared Maven target issue recorded in the task log.
- `node tests/e2e/unified-list-template-empty-tabs-system-static.spec.js` is currently blocked by an unrelated 91 vs 88 system access-point count drift.
- Global `unified-list-template-all-headers-sortable-static.spec.js` still fails on unrelated historical pages; QA新增列表已单独确认排序 helper 接线。
