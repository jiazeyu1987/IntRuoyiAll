# 问题 5：排产工单强制完成语义

## Status

completed

- 用户已批准正式规则：现有动作是有独立权限控制、可撤销的强制关闭，不是正常生产完成证明。
- 保留既有 API、权限和后端强制完成逻辑；日期、物料、当前工序为空或真实数量未完成均不新增阻断。
- 本问题只修改排产工单页面用户可见文案、聚焦静态合同和真实流 E2E 定位器。

## Feature Goal And Non-Goals

- Goal：让排产员明确区分正常生产完成与有权限人员执行的强制关闭，避免把“完成”按钮误解为已经具备日期、物料、工序或报工完成证据。
- Goal：在执行前和执行后同时告知汇总进度覆盖、真实工序进度保留和可撤销边界。
- Non-goal：不增加日期、物料、当前工序、报工、PQC 或批记录校验。
- Non-goal：不修改 API 路径、请求字段、权限码、后端状态流转、正常“已完成”状态和完成状态筛选。
- Acceptance ID：Issue 5 / FORCE-FINISH-COPY-01。

## Bug

- 排产工单列表把有独立权限、会把汇总覆盖为 100% 且可撤销的强制关闭动作显示为普通“完成/人工完成”，用户无法从动作名称判断它不代表真实工序已经完成。

## Expected

- 动作、弹窗、确认、完成后提示和追溯统一显示“强制完成/撤销强制完成”，并明确强制关闭、汇总 100%、真实进度保留和可撤销边界。

## Reproduction

- 变更前运行 `node tests\e2e\mes-pro-schedule-order-force-finish-copy-static.spec.js`，首个断言在行内操作仍显示“完成”时失败。

## Root Cause

- 后端功能本身一直是强制汇总完成且可撤销，但前端沿用了普通完成动作词和“人工完成”内部语义，没有把状态覆盖与真实工序进度的差异暴露在操作入口。

## Approved Rule

1. 所有该动作的用户可见“人工完成/完成”统一改名为“强制完成”；正常状态筛选中的“已完成”不改名。
2. 弹窗和二次确认必须明确说明：这是有权限人员执行的强制关闭；强制完成后汇总按 100% 展示；真实工序进度保留；该操作可撤销。
3. 撤销入口统一显示“撤销强制完成”，撤销后按真实工序进度恢复汇总状态。
4. `POST /mes/pro/schedule-order/manual-finish`、`POST /mes/pro/schedule-order/revoke-manual-finish`、权限码和后端状态流转保持不变。
5. 不因承诺交期、最晚开工、计划开工、计划完成、物料清单、当前工序或未完成数量为空/不足而拒绝强制完成。

## Scope

- API：`POST /mes/pro/schedule-order/manual-finish`
- 权限：`mes:pro-schedule-order:manual-finish`
- Controller：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/scheduleorder/MesProScheduleOrderController.java`
- Request VO：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/scheduleorder/vo/MesProScheduleOrderActionReqVO.java`
- Service：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderServiceImpl.java`
- Error codes：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java`
- Service tests：`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderServiceImplTest.java`
- Existing real-flow E2E：`IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-manual-finish-real-flow.e2e.js`

## Preserved Formal Contract

当前代码和测试能证明的人工完成前置条件只有：

1. 请求必须有排产工单 ID 和非空操作原因。
2. 排产工单必须存在。
3. 排产工单不能已冻结。
4. 排产工单不能已经人工完成。
5. 排产工单状态不能是 `CANCELED` 或 `FINISHED`；因此 `PREPARE`、`SCHEDULED`、`IN_PROGRESS` 当前均可人工完成。
6. 通过后将 `manualFinished` 置为 true，将状态强制置为 `FINISHED`，并将汇总进度锁定为 100%；真实工序报工进度保留，撤销时重新计算。

当前正式合同没有要求：

- `promiseDate`、`latestStartTime`、`plannedStartTime`、`plannedEndTime` 非空；
- 存在生产用料清单；
- 能解析出当前工序；
- 存在生产任务或报工；
- 实际完成数量达到任意比例；
- PQC、批记录或其它执行证据完成。

## Prior Contract Evidence

- `MesProScheduleOrderServiceImplTest#manualFinish_shouldForceFinishedSummaryAndWriteTraceLog` 构造了进度 50%、未完成数量 60，且没有计划开始/完成时间的 `IN_PROGRESS` 工单，并明确断言人工完成成功、状态变为 `FINISHED`、汇总进度变为 100%。
- `MesProScheduleOrderServiceImplTest#manualFinish_shouldLockAggregateTotalByEnabledProcessCount` 同样在真实完成量不足时验证强制完成语义。
- `mes-pro-schedule-order-manual-finish-real-flow.e2e.js` 专门选择 `uncompletedQuantity > 0` 的未完成真实工单执行人工完成，并断言完成后汇总进度为 100%；它没有以日期、物料、当前工序或执行证据完整作为候选条件。
- 页面文案明确说明“该工单已人工完成，列表按 100% 展示；以下工序仍显示真实报工进度”，进一步表明当前动作是汇总状态覆盖，不是对实际生产完成事实的证明。
- `IntRuoyiBackend/docs/system/mes-scheduling-domain-contracts.md` 定义了新建、预览、应用、重排和报工链路的缺配置失败规则，但没有定义人工完成前置条件。不能把其它写入场景的门禁自动扩展到人工完成。

## Data Ownership Findings

- 四个日期字段位于排产工单主表对象，但没有文档说明哪一个是人工完成必填项；待排产工单的计划开始/完成时间为空可能是正常状态。
- 生产用料清单摘要由 Controller 分页响应组装时通过 `MesKingdeeProductionMaterialListMapper` 查询，不是 `MesProScheduleOrderDO` 的持久化字段，当前人工完成 Service 没有该依赖。
- 当前工序是分页响应根据排产工序快照和进度动态推导的展示字段，不是排产工单主表字段；显示 `-` 可能表示缺快照、没有启用工序、没有剩余工序或计算条件不足，不能把这些不同原因合并成一个完成门禁。
- 人工完成当前刻意允许覆盖真实未完成进度。若改成“必须有完成执行证据”，必须先明确人工完成与正常自动完成的职责边界。

## BDD / TDD Evidence

- `BDD: 强制完成动作语义清晰 -> Given 用户查看尚未强制关闭的排产工单 / When 用户打开强制完成动作 / Then 行内按钮、弹窗标题、原因字段和确认按钮均显示“强制完成”，并说明这是有权限人员执行的强制关闭。`
- `BDD: 强制完成影响范围清晰 -> Given 用户确认强制完成排产工单 / When 页面显示二次确认或完成后的工序详情 / Then 页面明确说明汇总按 100% 展示、真实工序进度保留且可撤销。`
- `BDD: 撤销强制完成语义清晰 -> Given 排产工单已被强制关闭 / When 用户查看或执行撤销动作 / Then 页面统一显示“撤销强制完成”，并说明撤销后按真实工序进度恢复汇总状态。`
- `BDD: 强制完成不扩大后端门禁 -> Given 工单日期、物料、当前工序为空或真实数量未完成 / When 有权限用户强制完成 / Then 继续沿用现有 API 和后端强制关闭语义，不新增前端空值拦截或后端校验。`
- `BDD: 首屏完成状态保持空条件 -> Given 排产工单使用标准列表首屏空筛选合同 / When 页面首次加载 / Then completionFilter 初始为 undefined，且不得预置隐藏的 INCOMPLETE 条件。`
- `RED: node tests\e2e\mes-pro-schedule-order-force-finish-copy-static.spec.js -> FAIL, 行内操作仍显示“完成”，未明确“强制完成”。`
- `GREEN: node tests\e2e\mes-pro-schedule-order-force-finish-copy-static.spec.js -> PASS, 强制完成/撤销文案、风险说明、权限和 API 包装合同一致。`
- `RED: node tests\e2e\mes-pro-schedule-order-manual-finish-static.spec.js -> FAIL, 旧断言仍要求 completionFilter='INCOMPLETE'，与 20260805 首屏空条件合同冲突。`
- `GREEN: node tests\e2e\mes-pro-schedule-order-manual-finish-static.spec.js -> PASS, 合同改为要求 undefined 且禁止隐藏 INCOMPLETE 默认值。`

## UI Entry Points And Data States

- Route/page：MES 排产工单列表 `src/views/mes/pro/scheduleorder/index.vue`。
- 未强制关闭：有 `mes:pro-schedule-order:manual-finish` 权限时显示“强制完成”。
- 已强制关闭：有 `mes:pro-schedule-order:revoke-complete` 权限时显示“撤销强制完成”，详情显示强制关闭说明。
- Dialog：强制完成和撤销强制完成共用原对话框状态，不增加新请求、loading、empty 或 error 分支。
- API/data：继续使用 `manualFinishScheduleOrder`、`revokeManualFinishScheduleOrder` 和 `manualFinished*` 响应字段；接口异常继续沿用现有请求层错误处理，不隐藏失败。

## Validation

- 请求校验：当前由 `@NotNull id`、`@NotBlank reason` 和 Service 非空原因校验共同承担。
- 权限校验：当前 Controller 使用独立权限 `mes:pro-schedule-order:manual-finish`。
- 状态校验：当前仅拒绝冻结、已人工完成、已取消和已完成工单。
- 新增前置校验：按用户批准规则不新增；没有添加默认值、空值推断、异常吞并或替代数据源。
- Responsive：对话框表单标签宽度从 88px 调整为 128px，容纳“撤销强制完成原因”；警告文案使用现有 `el-alert` 自适应换行。
- Accessibility：动作保留原生 `el-button` 可访问名称，提示使用 `el-alert show-icon`；真实流改为按完整按钮名称定位。
- Loading/empty/error：保留原 `manualFinishSaving` loading、必填原因提示和请求异常传播；本次无新增空状态。
- Permission：保留 `mes:pro-schedule-order:manual-finish` 与 `mes:pro-schedule-order:revoke-complete`，未扩大权限。

## Verification

- `node tests\e2e\mes-pro-schedule-order-force-finish-copy-static.spec.js` -> RED FAIL；首个失败为行内按钮未显示“强制完成”。
- 同命令在最小实现后复跑 -> GREEN PASS。
- `node --check tests\e2e\mes-pro-schedule-order-manual-finish-real-flow.e2e.js` -> PASS。
- `pnpm exec prettier --check tests\e2e\mes-pro-schedule-order-force-finish-copy-static.spec.js` -> PASS。
- `git diff --check -- <Issue 5 前端文件>` -> PASS；仅有 Git 的 LF/CRLF 提示，无空白错误。
- `node tests\e2e\mes-pro-schedule-order-manual-finish-static.spec.js` -> RED FAIL 后 GREEN PASS；仅修正过期默认筛选断言，强制完成合同保持不变。
- `node tests\e2e\mes-pro-schedule-order-force-finish-copy-static.spec.js` 在相邻合同修正后复跑 -> PASS。
- `node tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> BLOCKED；测试引用当前工作区不存在的 `src/views/mes/pro/route/RouteFlowConfigPanel.vue`，在读取页面依赖时 `ENOENT`，尚未执行强制完成断言。
- `pnpm exec eslint <Issue 5 前端文件>` 与 `pnpm ts:check` -> NO RESULT；共享工作区同时有其他 agent 的 eslint/vue-tsc 进程，本任务命令运行约 10 分钟无输出后仅中止自身会话，未处理其他任务进程。
- Vue SFC 独立编译检查 -> BLOCKED；当前 pnpm 安装未暴露可直接加载的 `@vue/compiler-sfc` 模块，返回 `MODULE_NOT_FOUND`，未切换编译器或伪造通过。
- 真实写入 E2E -> NOT RUN；需要测试租户、排产员和管理员账号并会执行强制完成后撤销，留给主 agent 的集成验证，不以 API-only 替代。

## Blockers

- Issue 5 实现无业务规则阻塞。
- 主任务放行前仍需在无并发前端校验进程时复跑 lint/typecheck，并由主 agent 处理或确认排产主合同缺少 `RouteFlowConfigPanel.vue` 的剩余基线阻塞。
- 真实 E2E 需要获批测试账号和可回滚的真实未完成排产工单；未满足时不能宣称真实页面路径通过。

## Resolved Business Decision

1. 该动作是可撤销的强制关闭，不是正常生产完成证明。
2. 允许来源状态、原因必填、冻结门禁和权限保持现状。
3. 日期、路线展示、当前工序、物料和真实完成数量不作为新增门禁。
4. 正常状态继续使用“已完成”，仅强制关闭动作和追溯文案使用“强制完成”。
5. 真实工序进度不被改写；汇总按 100% 展示，撤销后恢复真实汇总。

## Changed Files

- `IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue`：动作、弹窗、确认、toast、详情提示、追溯字段和操作类型统一为强制完成语义。
- `IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-force-finish-copy-static.spec.js`：新增聚焦 RED/GREEN 合同。
- `IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-manual-finish-real-flow.e2e.js`：更新真实路径按钮、弹窗、toast 和撤销后状态定位器。
- `IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-manual-finish-static.spec.js`：更新相邻静态文案合同，并将过期默认 INCOMPLETE 断言修正为 20260805 批准的首屏空完成状态合同。
- `IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-pool-static.spec.js`：更新主合同中的强制完成文案，保留 Issue 2 的“完成状态”改动。
- `doc/tasks/20260807-smart-scheduling-issue-fixes/issue-5-manual-finish.md`：规则、BDD/TDD 和验证证据。

## Impact

- 用户可见动作不再把强制关闭伪装成正常“完成”，但持久化字段、操作类型编码和 API 仍保留 `manualFinish* / MANUAL_FINISH`，避免破坏后端合同。
- 剩余风险：测试服务器尚未部署本改动；长确认文案需由主 agent 在桌面和窄屏真实页面检查换行、按钮布局和弹窗高度。

## Design Constraints

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；把现有强制关闭语义明确暴露给用户，同时保持后端正式合同不变。
- 是否存在临时补丁或绕过：否。
