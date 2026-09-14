# 问题 6：物料清单或当前工序缺失时的操作矩阵

## Status

completed

- 用户已批准正式规则，列表原因与操作范围提示已按严格 BDD/TDD 完成。
- `当前工序` 是列表派生展示值，不是数据完整性字段；显示 `-` 不能区分“没有工序快照”“全部启用工序已完成”“没有启用工序”或“基础工序定义无法解析”。因此不得直接用 `currentProcessId == null` 作为统一写操作门禁。

## Approved Rule

- 生产用料清单显示“缺失”或当前工序显示 `-` 时，继续允许调整优先级、设置承诺交期、冻结和解冻。
- 入池资格只以现有正式入池检查结果为准；不得使用 `productionMaterialListCount == 0` 或 `currentProcessId == null` 新增门禁。
- 手动重排资格只以现有正式排产预检结果为准；不得改变生产用料清单缺失的现有 `WARNING` 合同，也不得把列表派生的当前工序空值升级为阻断。
- 列表必须通过可聚焦的图标 tooltip 说明原因和操作范围，不扩展表格列宽，不改变操作按钮布局。

## Scope

- 前端行操作：`IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue`
- 前端列表合同：`IntRuoyiFronted/src/api/mes/pro/scheduleorder/index.ts`
- 排产工单写服务：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderServiceImpl.java`
- 排产工单列表投影：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/scheduleorder/MesProScheduleOrderController.java`
- 自动排产/手动重排：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java`
- 排产域契约：`IntRuoyiBackend/docs/system/mes-scheduling-domain-contracts.md`

## Change Request Triage

### Request

- 来源：用户在测试服务器核验中提出“生产用料清单缺失、当前工序显示 `-` 时仍开放调整、交期、冻结等操作，需要确认允许范围是否符合业务规则”。
- 请求实质：为缺失基础数据的排产工单建立统一前后端操作矩阵。

### Baseline

- 已核对排产域契约、排产工单 Controller/Service、自动排产 Service、列表投影、前端行操作和现有聚焦测试。
- 当前基线不是“缺失即统一禁止”：用料清单缺失在排产预览/应用中是明确 `WARNING`；`当前工序` 为空只是派生展示，不能代表唯一异常。

### Classification

- 分类：已批准的产品行为澄清和可用性修复；不改变后端数据或资格合同。
- 本问题只增加行内原因与操作范围提示；人工完成规则由问题 5 独立处理。

### Impact

- 产品：明确异常展示值不等同于操作门禁，避免用户误判可执行范围。
- 设计：在现有单元格内增加可聚焦提示，不新增表格列，不改变操作按钮布局。
- 数据与 API：不改变字段、后端资格判断或用料清单缺失的 `WARNING` 合同。
- 测试：通过聚焦静态合同锁定提示可访问性，以及入池/手动重排资格函数不得读取这两个展示字段。
- 发布与运维：无数据修复、权限变更或服务操作；真实页面集成验证由主任务统一执行。

### Decision

- 决定：`accept`。用户已批准本问题范围内的正式规则；人工完成仍由问题 5 单独处理。
- Required Approvals：本问题所需产品规则已由用户批准，无新增权限、数据或发布操作授权。

### Downstream

- 已按批准规则完成前端提示和聚焦静态合同；后端 API 门禁、入池和手动重排算法均无需修改。

## Current Executable Contract

### 生产用料清单缺失

1. 列表字段 `productionMaterialListCount=0` 只表示按生产工单 ID 未查到生产用料清单；Controller 将其投影为“缺失”。
2. 调整优先级、设置承诺交期、冻结、解冻和人工完成的 Service 均不读取生产用料清单。除各动作已有的 ID、权限、原因、冻结或状态门禁外，当前均不因用料清单缺失而拒绝。
3. 入池检查验证生产工单状态、ERP 正式同步身份、唯一启用路线、激活版本、路线工序、智能排产用途、排产策略和资源产能，但不验证生产用料清单，因此当前允许缺清单工单入池。
4. 自动排产/手动重排会先尝试正式生产用料清单同步；同步后仍缺失时生成 `MATERIAL_DEMAND / WARNING / 工单缺少生产用料清单`，仍生成并应用排产任务。现有测试 `preview_shouldWarnAndKeepGeneratedTasksWhenProductionMaterialListMissing` 和 `apply_shouldWarnAndInsertTasksWhenProductionMaterialListMissing` 明确锁定该行为。
5. 因此，把清单缺失统一改成所有操作禁用，会直接推翻现有“重排仅告警并允许应用”的可执行合同。

### 当前工序为空

1. 列表的 `currentProcessId/currentProcessName` 由排产工序快照中“已启用且进度不足 100% 的第一道工序”派生，不是持久化完整性标志。
2. `currentProcessId == null` 至少可能表示：排产工序快照为空、没有启用工序、所有启用工序均已完成，或当前候选工序的基础工序定义无法解析；页面统一显示 `-`，现有响应没有原因码区分这些状态。
3. 调整优先级、设置承诺交期、冻结、解冻和人工完成当前不读取 `currentProcessId`，也不因该派生值为空而拒绝。
4. 手动重排不依赖列表派生值。它读取正式排产工序快照；缺路线、缺路线工序、缺排产工序路线快照或拓扑不完整时，由预览/应用形成明确阻断，而不是根据 `currentProcessId == null` 预判。
5. 入池发生在排产工单和排产工序快照创建之前，“当前工序为空”不适用于入池候选；入池应继续按正式路线和配置检查判断。

## Formal Evidence Alignment

- `mes-scheduling-domain-contracts.md` 明确要求预览、应用和重排使用有效排产工序快照，缺路线/工序/快照时 fail fast；这支持手动重排按正式快照问题阻断，不支持按列表 `currentProcessId` 空值统一禁用。
- 自动排产 Service 与测试明确把生产用料清单缺失定义为 `WARNING`，允许生成和应用任务；这与“缺清单就禁止手动重排”相冲突。
- `updatePriority`、`updateScheduleOrder`、`freezeScheduleOrders` 和 `unfreezeScheduleOrders` 的现有动作门禁继续生效；本修复未增加与生产用料清单或当前工序展示值的耦合。
- 生产用料清单经验门禁只规定跨环境同步的数据完整性和恢复方式，不规定排产工单行操作权限矩阵。
- 用户批准规则与现有可执行合同一致，因此只需解释状态和操作范围，不需要修改后端。

## BDD / TDD Evidence

- `BDD: 缺失基础数据原因和操作范围可见 -> Given 排产工单的生产用料清单显示“缺失”或当前工序显示“-” / When 用户悬停或键盘聚焦提示图标 / Then 页面说明当前缺失原因、仍可执行的治理操作，并明确入池和手动重排以正式检查结果为准。`
- `BDD: 缺失展示值不改变正式资格 -> Given 排产工单缺少生产用料清单或列表未解析出当前工序 / When 页面判断入池或手动重排资格 / Then 不使用 productionMaterialListCount 或 currentProcessId 增加门禁，后端现有 WARNING/阻断合同保持不变。`
- `RED: node tests\e2e\mes-schedule-order-missing-data-action-hints-static.spec.js -> FAIL, 当前源码缺少 MISSING_MATERIAL_LIST_HINT；“缺失”和“-”没有可聚焦的图标 tooltip，也没有展示批准后的操作范围。`
- `GREEN: node tests\e2e\mes-schedule-order-missing-data-action-hints-static.spec.js -> PASS。`

## Implemented Operation Matrix

| 操作 | 用料清单显示“缺失” | 当前工序显示“-” |
| --- | --- | --- |
| 调整优先级 | 保持现有允许条件 | 保持现有允许条件 |
| 设置承诺交期 | 保持现有允许条件 | 保持现有允许条件 |
| 冻结/解冻 | 保持现有允许条件 | 保持现有允许条件 |
| 入池 | 只依据现有正式入池检查 | 不使用该排产工单展示字段判断 |
| 手动重排 | 只依据现有正式排产预检，缺清单仍为 `WARNING` | 只依据正式工序快照和排产预检 |

## Implementation

- 为“生产用料清单：缺失”增加 `ep:question-filled` 图标和 `el-tooltip`，说明未查询到清单、仍允许的治理操作及入池/重排判据。
- 为“当前工序：-”增加同样可聚焦的图标 tooltip，明确列表未解析出可显示的未完成工序，且该展示值不是统一禁用判据。
- 两个触发器均有 `tabindex="0"` 和 `aria-label`，支持键盘聚焦和辅助技术读取。
- 新样式使用 `inline-flex` 和固定 14px 图标，不改变列宽、按钮或表格布局；tooltip popper 最大宽度 360px，长文自动换行。
- 聚焦静态合同明确禁止把 `productionMaterialListCount` 或 `currentProcessId` 加入入池/手动重排前端资格函数。

## Verification

- 源码与现有测试只读核对：完成。
- 后端生产代码和测试变更：无；现有 `WARNING`、入池和预检合同保持不变。
- `node tests\e2e\mes-schedule-order-missing-data-action-hints-static.spec.js`：PASS。
- `node tests\e2e\mes-schedule-order-material-list-static.spec.js`：PASS。
- `node --check tests\e2e\mes-schedule-order-missing-data-action-hints-static.spec.js`：PASS。
- `pnpm.cmd exec prettier --check tests/e2e/mes-schedule-order-missing-data-action-hints-static.spec.js`：PASS。
- `pnpm.cmd ts:check:schedule`：PASS。
- `mes-pro-schedule-order-pool-static.spec.js`：BLOCKED by unrelated missing `src/views/mes/pro/route/RouteFlowConfigPanel.vue`。
- `mes-pro-schedule-order-toolbar-layout-static.spec.js`：FAIL at unrelated旧工具栏结构断言，未命中本问题提示合同。
- 页面级 Prettier/Stylelint：现有共享文件包含任务前既有的格式和属性顺序差异；未执行自动格式化，避免覆盖并行任务改动；Stylelint 输出没有指向本次新增 selector。
- 目标 ESLint 在超过两分钟无输出后终止，未记录为通过。
- 文档结构核验：PASS；Status、Approved Rule、BDD/RED/GREEN、实施矩阵、Verification、Blockers 和 Design Constraints 均存在。
- `python -X utf8 C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --self-test`：PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence doc\tasks\20260807-smart-scheduling-issue-fixes\issue-6-missing-data-actions.md`：PASS。
- `git diff --check`（已跟踪源码）及 `git diff --no-index --check`（新测试、新问题文档）：PASS，无空白错误诊断。

## Blockers

- 本问题无业务规则或实现阻塞。
- 主 agent 真实页面初审发现首版 tooltip 单行横跨表格；补充静态断言后增加 popper 最大宽度和自动换行。当前工序提示经悬停和键盘聚焦验证，提示框为 360x72，无文字遮挡。
- 当前用户个性化隐藏了生产用料清单列；为避免写入用户偏好，本任务没有强行展示该列，物料提示由静态合同覆盖。

## Design Constraints

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；提示直接表达已批准的领域边界，不把列表派生空值误作资格判断。
- 是否存在临时补丁或绕过：否。
