# 执行日志

INFO: experience-index -> matched `docs/powershell-memory.md`, `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`, frontend-feature-delivery。

GREEN: experience-preflight -> PASS, 已读取 PowerShell、统一前端样式、前端交付和证据合同；本任务只做前端静态合同与页面逻辑改动，不执行真实 E2E、服务器写入、数据库写入或发布。

BDD: 工序表单填写方式清晰可见 -> Given 当前工序 `recordCategory=BATCH_RECORD` When 用户查看批次详情 Then 页面显示“填写方式：表单”，打开入口进入工序表单，追溯参数包含 `fillCarrier=FORM`。

BDD: 记录本填写方式清晰可见 -> Given 当前工序 `recordCategory=INTERNAL_RECORD` When 用户查看批次详情 Then 页面显示“填写方式：记录本”，打开入口进入记录本引用，追溯参数包含 `fillCarrier=RECORDBOOK`。

BDD: 未配置填写方式失败可见 -> Given 当前工序缺少 `recordCategory` When 用户查看批次详情 Then 页面显示“填写方式：未配置”，打开入口禁用并提示配置缺失，不默认进入表单或记录本。

BDD: 红框位置使用批记录记录本控件 -> Given 用户在批次执行详情查看左侧工序列表 When 工序存在表单或记录本填写方式 Then 原“未知”位置显示“批记录 / 记录本”双选项控件，而不是在右侧详情里显示。

BDD: 追溯按填写方式过滤上下文 -> Given 用户从当前工序进入主数据追溯 When 追溯详情打开 Then 列表/详情携带并展示当前 `fillCarrier`，用于区分表单追溯和记录本追溯。

RED: `node tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js` -> FAIL, 页面尚未展示“填写方式”，缺少 `resolveFillCarrierLabel`、`fillCarrier`、记录本入口和追溯详情展示。

GREEN: `node tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js` -> PASS, 批次详情已按 recordCategory 映射填写方式，记录本入口和追溯上下文合同通过。

GREEN: `node tests\e2e\edhr-process-evidence-fusion-static.spec.js` -> PASS, 工序操作台保留工序上下文、分组证据链和完整明细入口。

GREEN: `node tests\e2e\edhr-domain-trace-ui-static.spec.js` -> PASS, 主数据追溯详情保留业务化展示并展示填写方式。

GREEN: `node tests\e2e\edhr-approval-consistency-static.spec.js` -> PASS, 相邻审核/追踪合同通过。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS, 前端类型检查通过。

INFO: adjacent-type-fix -> PASS, `TrackingPage.vue` 补齐 `REVIEW_APPROVE` 追踪事件标签，修复现有类型合同缺口，不改变后端合同。

GREEN: task-closeout-cleanup-preview -> PASS, cleanup preview ready；保留 `task.md` 与 `execution-log.md`，清理重复证据文件 `frontend-feature-evidence.md`。

INFO: user-placement-feedback -> 用户要求不要放在详情里，改放到截图红框位置，并按图 2 做启用/未启用控件样式。

INFO: user-label-feedback -> 用户要求控件显示批记录/记录本，不显示启用/未启用；选择批记录填写批记录，选择记录本填写记录本。

GREEN: `node tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js` -> PASS, 填写方式控件已移到工序列表原红框位置，启用/未启用开关合同通过。

GREEN: `node tests\e2e\edhr-process-evidence-fusion-static.spec.js` -> PASS, 工序证据链入口仍保留同一工序上下文。

GREEN: `node tests\e2e\edhr-domain-trace-ui-static.spec.js` -> PASS, 主数据追溯详情继续展示并接收填写方式。

GREEN: `node tests\e2e\edhr-approval-consistency-static.spec.js` -> PASS, 相邻审批追踪合同未被破坏。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS, 前端类型检查通过。
GREEN: `pnpm exec eslint src\views\mes\pro\edhr-batch\BatchExecutionDetailPage.vue` -> PASS, 修复开关圆点 HTML 元素自闭合 lint 错误。

GREEN: `node tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js` -> PASS, 填写方式控件静态合同保持通过。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS, 类型检查保持通过。
GREEN: `pnpm exec eslint src\views\mes\pro\edhr-batch\BatchExecutionDetailPage.vue tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js` -> PASS, 批记录/记录本双选项控件 lint 通过。

GREEN: `node tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js` -> PASS, 控件显示批记录/记录本且不再显示启用/未启用，选择记录本时覆盖 recordCategory/fillCarrier。

GREEN: `node tests\e2e\edhr-process-evidence-fusion-static.spec.js` -> PASS, 工序证据链上下文保持通过。

GREEN: `node tests\e2e\edhr-domain-trace-ui-static.spec.js` -> PASS, 主数据追溯详情合同保持通过。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS, 类型检查通过。

INFO: user-recordbook-mode-choice -> 用户选择第 2 种：放开字段校验，但保留保存、审计和签名证据。

BDD: 记录本复用批次执行表单 -> Given 用户在批次执行工序列表选择 `记录本` When 打开当前工序 Then 进入同一个 `edhr-execution/form`，携带 `fillCarrier=RECORDBOOK` 与 `fillMode=RECORDBOOK_UNRESTRICTED`，不进入独立 `edhr-recordbook` 页面。

BDD: 记录本放开字段校验但保留证据 -> Given 当前填写页处于 `RECORDBOOK_UNRESTRICTED` 模式 When 用户保存或提交 Then 字段/附件必填和字段规则校验不阻断，但字段审计保存、审计原因、电子签名、提交签名和签名时间证据仍保留。

GREEN: `pnpm exec eslint src\views\mes\pro\edhr-batch\BatchExecutionDetailPage.vue src\views\mes\pro\edhr\ExecutionPage.vue tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js tests\e2e\edhr-process-evidence-fusion-static.spec.js` -> PASS, 记录本不受控填写相关前端文件 lint 通过。

GREEN: `node tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js` -> PASS, 批记录/记录本控件在工序列表红框位置，记录本复用 `edhr-execution/form` 并携带 `fillMode=RECORDBOOK_UNRESTRICTED`。

GREEN: `node tests\e2e\edhr-process-evidence-fusion-static.spec.js` -> PASS, 工序证据链同步为“记录本填写”且不再要求独立 `edhr-recordbook` 入口。

GREEN: `node tests\e2e\edhr-domain-trace-ui-static.spec.js` -> PASS, 主数据追溯静态合同保持通过。

GREEN: `rg obsolete-recordbook-entry-tokens BatchExecutionDetailPage.vue` -> PASS, 批次详情业务源码不再包含 `openRecordbookForSelectedProcess`、独立记录本路径、`记录本引用`、`启用`、`未启用` 等旧入口/旧文案。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS, 前端类型检查通过。

