# 20260707 EDHR 工序填写方式与追溯方式一致

## Task Goal
将批次执行详情中当前显示为“未知”的工序记录类型改为明确的“填写方式”，控件必须放在用户截图红框所示的工序列表原未知位置，并采用“批记录 / 记录本”双选项样式；选择批记录进入该工序批记录表单，选择记录本仍进入同一个批次执行填写表单，但按记录本不受控方式填写。工序证据链、主数据追溯和记录本填写必须携带同一填写方式上下文，避免追溯时混淆表单和记录本。

## Milestones
- [x] 创建任务文档并记录 BDD/TDD 门禁
- [x] 补充 RED 静态测试覆盖填写方式与追溯参数
- [x] 实现批次详情填写方式选择和追溯一致传参
- [x] 运行目标测试、类型检查并记录 GREEN 证据
- [x] 按任务范围提交前端改动

## Expected Verification
- 批次详情工序列表原“未知”位置不再显示模糊文本，改为 `批记录 / 记录本` 双选项控件。
- 用户选择 `批记录` 时进入该工序批记录表单；选择 `记录本` 时进入同一个批次执行表单的记录本不受控填写模式。
- 右侧摘要与详情弹层不再重复承载填写方式控件。
- 当前工序操作台的“打开工序”文案按填写方式变化：表单进入工序表单；记录本进入记录本不受控填写；未配置明确禁用。
- 工序证据链、主数据追溯、记录本填写跳转均携带 `fillCarrier`、`fillMode` 和原有工序/批次/执行上下文。
- 记录本不受控填写放开字段必填、附件必填和字段规则校验，但保留字段保存、审计原因、字段审计签名、提交签名和签名时间证据。
- 主数据追溯详情能展示当前追溯来源的填写方式。
- 不新增 fallback、mock 数据、静默降级或临时绕过。

## Current Status
completed

## Previous Task Check
- 已检查前端最近任务 `20260706-edhr-batch-role-permission-flow`，其 `task.md` 标记 completed，执行日志已记录最终状态、合并后验证与清理证据。
- 本任务在当前前端仓主工作区继续做最小前端改动；主工作区存在 unrelated 未提交/未跟踪文件，本任务不接管、不回退。

## 经验门禁
- PowerShell：已读取 `docs/powershell-memory.md`，命令使用 UTF-8，禁止 `&&`。
- 统一前端样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次只做操作台/标签级最小 UI 改动，保持蓝灰运维台风格。
- 前端特性：已读取 `frontend-feature-delivery` 和 `references/frontend-contract.md`，保留现有 API 合同与路由边界。
- BDD/TDD：先记录 Given/When/Then，再 RED -> GREEN -> REGRESSION。
- 禁止 fallback：不得把 `recordCategory` 缺失静默当作表单；缺失必须显示“未配置”并禁用入口。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，使用明确填写方式语义贯穿工序操作与追溯上下文。
- 是否存在临时补丁或绕过：否。

## BDD Scenarios
BDD: 工序表单填写方式清晰可见 -> Given 当前工序 `recordCategory=BATCH_RECORD` When 用户查看批次详情 Then 页面显示“填写方式：表单”，打开入口进入工序表单，追溯参数包含 `fillCarrier=FORM`。

BDD: 记录本填写方式清晰可见 -> Given 当前工序 `recordCategory=INTERNAL_RECORD` When 用户查看批次详情 Then 页面显示“填写方式：记录本”，打开入口进入同一个批次执行表单的不受控填写模式，追溯参数包含 `fillCarrier=RECORDBOOK`。

BDD: 未配置填写方式失败可见 -> Given 当前工序缺少 `recordCategory` When 用户查看批次详情 Then 页面显示“填写方式：未配置”，打开入口禁用并提示配置缺失，不默认进入表单或记录本。

BDD: 红框位置使用批记录记录本控件 -> Given 用户在批次执行详情查看左侧工序列表 When 工序存在表单或记录本填写方式 Then 原“未知”位置显示“批记录 / 记录本”双选项控件，而不是在右侧详情里显示。

BDD: 追溯按填写方式过滤上下文 -> Given 用户从当前工序进入主数据追溯 When 追溯详情打开 Then 列表/详情携带并展示当前 `fillCarrier`，用于区分表单追溯和记录本追溯。

BDD: 记录本放开字段校验但保留证据 -> Given 用户选择 `记录本` 填写方式 When 打开当前工序填写页 Then 页面复用批次执行表单并进入 `RECORDBOOK_UNRESTRICTED` 模式，字段/附件必填和字段规则不阻断保存提交，但字段审计保存、电子签名和提交签名仍然必须存在。

## Verification Log
- RED: `node tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js` -> FAIL，页面缺少“填写方式”、`fillCarrier`、记录本入口和追溯详情展示。
- GREEN: `node tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-process-evidence-fusion-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-domain-trace-ui-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-approval-consistency-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-fill-carrier-trace-mode-static.spec.js` -> PASS，红框位置开关控件合同通过。
- GREEN: `node tests\e2e\edhr-process-evidence-fusion-static.spec.js` -> PASS，工序证据链合同通过。
- GREEN: `node tests\e2e\edhr-domain-trace-ui-static.spec.js` -> PASS，追溯详情合同通过。
- GREEN: `node tests\e2e\edhr-approval-consistency-static.spec.js` -> PASS，审批追踪合同通过。
- GREEN: `$env:NODE_OPTIONS='"'"'--max-old-space-size=8192'"'"'; pnpm ts:check` -> PASS。

## 实现结果
- 批次详情工序列表原“未知”位置增加“批记录 / 记录本”双选项控件，直接选择当前工序填写入口。
- 右侧摘要与详情弹层不再重复显示填写方式控件。
- 当前工序操作台中打开入口按填写方式显示为“打开表单”“填写记录本”或“配置填写方式”。
- `recordCategory=BATCH_RECORD` 映射 `fillCarrier=FORM`；`recordCategory=INTERNAL_RECORD` 映射 `fillCarrier=RECORDBOOK`；缺失映射 `UNCONFIGURED` 并禁用入口。
- 选择记录本不再进入独立 `edhr-recordbook` 页面，改为携带 `fillMode=RECORDBOOK_UNRESTRICTED` 打开同一个 `edhr-execution/form`。
- 记录本不受控模式放开字段必填、附件必填和字段规则约束；字段审计保存、审计原因、电子签名、提交签名和签名时间证据保留。
- 工序证据链和主数据追溯跳转统一携带 `fillCarrier`、`recordCategory`、`fillMode`、批次、工序和执行上下文。
- 主数据追溯详情展示传入的填写方式。
- 收尾清理预览通过，`frontend-feature-evidence.md` 已作为重复证据清理候选，核心记录保留在 `task.md` 与 `execution-log.md`。
