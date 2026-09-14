# Execution Log

## 2026-07-31

- 用户要求将真实系统 `生产填写` 页面改成与两个已上传 HTML 原型一致：`frontline-production-operator-1920.html` 对应有设备工序，`frontline-production-operator-1920-no-device.html` 对应无设备工序。
- 已读取规则：`docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取技能：`frontend-feature-delivery`、`replicate-frontend-ui`。
- 当前边界：允许修改 `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`、相邻前端静态合同和本任务文档；保护后端、API wrapper、DTO/schema、数据库、mock/seed 数据。
- Git 预检：用户已删除此前阻塞的 `.git/index.lock`；`git status --short --branch` 显示当前分支 `int_main` 已领先 `origin/int_main` 10 个提交，工作区无未提交改动。

## BDD

- BDD: 有设备生产填写 -> Given 当前生产工序绑定 1 到 3 台设备 When 一线员工打开生产填写页 Then 页面顶部只显示工序、员工、主页，主体左侧显示完成数量、只读损耗数量和七类不良明细，右侧显示最多三台设备及其参数输入。
- BDD: 无设备生产填写 -> Given 当前生产工序没有设备 When 一线员工打开生产填写页 Then 页面不显示设备空状态面板，数量和七类不良明细占满主体区域。
- BDD: 损耗数量自动汇总 -> Given 员工调整任一不良类型数量 When 不良数量变化 Then 损耗数量显示七类不良数量合计，员工不需要单独填写损耗数量。
- BDD: 原型约束 -> Given 生产填写页渲染 When 页面首屏展示 Then 不显示上工序输入数量、生产工单、统计说明或弹窗式不良录入。

## RED/GREEN

- RED: `node src\views\mes\pro\feedback\frontline-template-render.spec.cjs` -> FAIL, expected reason: 旧生产填写页面未包含 `完成数量`、内联 `不良明细` 和无设备铺满布局。
- GREEN: `node src\views\mes\pro\feedback\frontline-template-render.spec.cjs` -> PASS。
- GREEN: `node src\views\mes\pro\feedback\frontline-template-switch.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260731-frontline-production-fill-html-match\frontend-feature-evidence.md` -> PASS。

## Implementation Notes

- 生产填写页顶部保持 `工序 / 员工 / 主页`，未加入工单或生产订单显示。
- 有设备工序主体按目标 HTML 分为左侧 `填数量` 和右侧 `填设备`；设备列表继续使用真实设备来源并限制最多 3 台。
- 无设备工序不再显示 `本工序无设备` 占位面板，数量与不良明细区域铺满主体。
- `损耗数量` 改为只读显示，来自七类不良数量合计；七类不良为内联输入，不弹窗。
- 正式生产模板、记录本 payload 和资源池事件均已移除 `PREVIOUS_PROCESS_INPUT_QUANTITY` / `previousProcessInputQuantity`，不再要求员工手填或提交上一工序输入数量。
- 未修改数据库、mock 或 seed 数据；本次只调整前端/后端合同、提交拆分链路和相邻测试。

## 2026-07-31 追加变更：移除上工序输入数量字段

- 用户明确要求：不让员工手填“上工序输入数量”，去除 `PREVIOUS_PROCESS_INPUT_QUANTITY`。
- 变更边界：从生产简化模板正式合同、前端 API 常量、生产默认字段、页面提交 payload 和后端模板目录中移除该字段；不引入自动填充、默认值或 fallback。
- BDD: 生产模板不含上工序输入数量 -> Given 一线员工打开生产填写或提交生产简化模板 When 前端构造 payload 且后端校验字段 Then `PREVIOUS_PROCESS_INPUT_QUANTITY` 不在允许字段内，员工只提交设备、设备参数、完成数量和损耗数量。
- BDD: 旧字段作为未知字段拒绝 -> Given 客户端仍提交旧字段 `PREVIOUS_PROCESS_INPUT_QUANTITY` When 后端构建生产简化模板 payload Then 返回 `PRO_FRONTLINE_TEMPLATE_FIELD_INVALID`，不静默接受旧字段。

## Runtime / Browser Notes

- 本机 `8081` 前端和 `48081` 后端端口均在监听，前端根页面 HTTP 200。
- 未执行登录后真实页面写入 E2E；当前任务按静态合同和 TS 完成页面结构验证，未在真实业务数据上提交。
- 经验沉淀检查：`docs/e2e-rules.md` 已有“静态合同与真实 E2E 同步门禁”，覆盖按稳定 class/data 属性截取模板片段的风险；本次不新增长期经验文档。


## RED/GREEN 追加字段移除

- RED: `node src\views\mes\pro\feedback\frontline-template-render.spec.cjs` -> FAIL, expected reason: API field codes / recordbook submit API still exposed previous-process input quantity.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=ProductionTemplateContractTest,FrontlineTemplatePayloadContractTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolSubmitEventServiceAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: production template, recordbook split payload, and process-pool quantity fragment still retained previous-process input quantity.
- GREEN: `node src\views\mes\pro\feedback\frontline-template-render.spec.cjs` -> PASS.
- GREEN: `node src\views\mes\pro\feedback\frontline-template-switch.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=ProductionTemplateContractTest,FrontlineTemplatePayloadContractTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolSubmitEventServiceAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests, 0 failures, 0 errors.
- SCAN: `rg -n "PREVIOUS_PROCESS_INPUT_QUANTITY|previousProcessInputQuantity|previousInputQuantity|上工序输入数量" IntRuoyiFronted\src IntRuoyiBackend\yudao-module-mes\src\main IntRuoyiBackend\yudao-module-mes\src\test` -> PASS, remaining matches are negative assertions in tests only.

## Evidence Validators

- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260731-frontline-production-fill-html-match\frontend-feature-evidence.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260731-frontline-production-fill-html-match\backend-api-evidence.md` -> PASS.

## Long-Term Requirement Docs Sync

- 更新 `docs/inception/project-brief.md`、`docs/inception/evidence-inventory.md`、`docs/acceptance/production-line-process-pool/bdd-scenarios.md`、`tdd-plan.md`、`e2e-plan.md` 和 `open-questions-blockers.md`，将当前口径同步为：一线员工不手填上工序输入数量；该数量由工序池/FIFO 链路派生。
- 保留早期“曾讨论上工序输入数量”的历史证据，但补充最新纠正覆盖旧口径，避免后续按旧模板开发。
