# Feature

T4 SP-1 生产组长前端合同与页面：在真实生产组长工作台按双 100% 门禁提交生产放行申请，展示统一五状态、PQC 待办回执、结构化 blocker，并在响应不确定或刷新失败时保留成功事实和原幂等键。

## Non-goals

- 不实现 PQC 审批、批次创建、三类映射、四报告上传或管理者代表放行。
- 不启动本地服务、不写业务数据；真实 Playwright 写入链路留在 T11 的任务自有数据阶段。
- 不改 T3 已冻结的后端接口。

## Acceptance

- `AC-01`：生产和过程检验均达到正式 100% 才允许提交。
- `AC-04`：SP-1 只展示申请与 PQC 待办，不展示伪下游对象。
- `AC-06`：响应不确定时按 activeOrderId 查询权威回执并保留幂等键。
- `AC-27`、`AC-28`：真实页面入口、字符串 ID 和稳定用户状态反馈。
- `AC-29`：后端结构化 blocker 必须完整展示，不能被请求层丢弃。

## Entry And Ownership

- 路由页面：生产组长工作台的“活跃订单”模块。
- 页面：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- API：`IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`。
- 命名合同：`IntRuoyiFronted/tests/e2e/sp1-production-release-contract.spec.cjs`。
- 请求错误载荷：`IntRuoyiFronted/src/config/axios/service.ts`，仅补齐正式 `CommonResult.data` 透传。

## API And Data States

- POST `/mes/pro/process-pool/team-leader/active-order/release/apply`：返回申请、冻结路线、PQC 待办、状态、快照哈希和版本；所有 Long ID 按字符串接收。
- GET `/mes/pro/process-pool/team-leader/active-order/release/get?activeOrderId=...`：响应不确定时的唯一权威回执来源。
- 五状态：`PQC_RELEASE_PENDING`、`PQC_RELEASE_REJECTED`、`REPORT_UPLOAD_PENDING`、`MANAGER_RELEASE_PENDING`、`RELEASED`。
- 错误：读取 `CommonResult.data.stage/currentStatus/blockers`，不得把 blocker 当成功状态。

## BDD

- BDD: 双 100% 才可提交 -> Given 活跃订单生产或过程检验任一未达到正式 100% / When 生产组长查看完工操作 / Then 按钮不可提交并显示具体缺失进度；两者均为 100% 且没有既有申请时才可提交。
- BDD: SP-1 只确认申请和 PQC 待办 -> Given 双 100% 且当前组长有权限 / When 确认完工 / Then 页面只接受申请编号、PQC 待办编号、状态、快照和版本，不要求批次、报告任务或最终放行事务。
- BDD: 失败必须展示结构化 blocker -> Given 后端返回生产进度、权限、快照或幂等 blocker / When 请求失败 / Then 页面显示 blocker 类型、原因、建议和对象定位，不把失败显示为成功。
- BDD: 不确定响应按权威回执恢复 -> Given POST 超时或响应不完整 / When 页面无法确认提交结果 / Then 使用同一 activeOrderId 查询正式回执，保留原幂等键且禁止重复申请；已确认成功后的列表刷新失败不得覆盖成功事实。

## RED

- RED: `pnpm test sp1-production-release-contract` -> FAIL；首个失败为 `Missing persistent release status: PQC_RELEASE_PENDING`。旧前端仍使用 `BLOCKED / PENDING_RELEASE_APPROVAL`、伪下游 ID 和活跃订单列表恢复，符合预期。

## GREEN

- GREEN: `pnpm test sp1-production-release-contract` -> PASS；SP-1 命名合同通过。
- GREEN: `node src/api/mes/pro/processpool/teamLeaderReleaseApplication.static.spec.cjs` -> PASS；相邻 API/页面合同通过。
- GREEN: `node tests/e2e/team-leader-active-order-release-application-static.spec.js` -> PASS；幂等、正式回执和刷新分层合同通过。
- GREEN: `pnpm test e2e:team-leader-workbench:static` -> PASS；工作台相邻合同通过。
- GREEN: `pnpm ts:check` -> PASS；`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0。

## Verification

- 命名静态合同、两组相邻放行合同、工作台合同、TypeScript 检查、结构化错误/权限/重复提交锁定和 `git diff --check` 均通过。
- 真实多账号 Playwright 写入验证由 T11 在确认账号、数据和成对运行态后执行。

## Blockers

- 无。worktree 初始缺少 `node_modules`，已使用锁文件执行 `pnpm install --offline --frozen-lockfile --reporter=silent`，未改 lockfile；随后类型检查通过。

# T6 Feature

SP-2 PQC 工作台前端：在现有 eDHR 候选待办页处理 `PQC_PRODUCTION_RELEASE` 任务，完成批准、拒绝、版本/幂等门禁、结构化错误展示、权威回执恢复和正式下游摘要。

## T6 Non-goals

- 不实现四报告上传、管理者代表放行或可追溯页面；这些分别属于 T8、T10。
- 不把动态表单、旧批次或页面缓存当成正式生产放行回执。
- 不启动服务、不写业务数据；真实多账号 Playwright 路径留在 T11。

## T6 Acceptance

- `AC-06`：响应不确定时按 applicationId 查询权威回执，未确认时锁定操作。
- `AC-07`：只有冻结候选和当前启用权限同时满足才有处理入口。
- `AC-08`：批准后展示正式批次和四报告任务摘要。
- `AC-09`：拒绝原因必填，拒绝后终止且没有重申请动作。
- `AC-27`、`AC-28`：使用真实候选待办入口，所有 Long ID 按字符串处理。
- `AC-29`：结构化 blocker 保留类型、原因、建议和对象定位。

## T6 Entry And Ownership

- 页面：`WorkTaskBoardPage.vue` 的“候选审核”页签。
- API：`src/api/mes/pro/productionRelease/index.ts`。
- 共享任务合同：`src/api/mes/pro/edhr/workTask.ts`。
- 命名合同：`tests/e2e/sp2-pqc-production-release-contract.spec.cjs`。

## T6 API And Data States

- POST `/mes/pro/production-release/pqc/approve`：提交申请、待办、权威版本、ASCII 幂等键和可选意见。
- POST `/mes/pro/production-release/pqc/reject`：同上，并要求非空拒绝原因。
- GET `/mes/pro/production-release/get?applicationId=...`：打开弹窗和不确定响应恢复的唯一权威回执。
- 只有 `PQC_RELEASE_PENDING` 可提交；`REPORT_UPLOAD_PENDING` 展示批次和四报告，`PQC_RELEASE_REJECTED` 展示终止原因。

## T6 BDD

- BDD: 只有冻结候选能看到 PQC 决策 -> Given 当前用户在候选待办页且任务为 PQC TODO / When 查看操作 / Then 显示通过和拒绝，普通任务、非候选页和终态任务不显示。
- BDD: 拒绝原因必填且拒绝终止 -> Given 拒绝弹窗 / When 原因为空或有效 / Then 前者阻止请求，后者只接受拒绝终态且没有重申请操作。
- BDD: 批准回执展示正式下游摘要 -> Given 权威版本批准 / When 返回待上传报告 / Then 必须展示正式批次和恰好四个报告任务。
- BDD: 版本冲突和不确定响应不得误报成功 -> Given 结构化 blocker 或响应不确定 / When 页面处理失败 / Then 展示 blocker；仅权威 GET 确认后展示成功，否则锁定。

## T6 RED

- RED: `node tests/e2e/sp2-pqc-production-release-contract.spec.cjs` -> FAIL；生产放行前端 API 不存在。
- RED: `pnpm test sp2-pqc-production-release-contract` -> FAIL；统一命名测试入口未登记。

## T6 GREEN

- GREEN: `pnpm test sp2-pqc-production-release-contract` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: 四组 eDHR 工作台相邻静态合同 -> PASS。
- GREEN: `pnpm test sp1-production-release-contract` -> PASS。

## T6 Experience Checks

- 响应式：沿用现有表格、分页和 Dialog 约束；操作列扩展到 190px，按钮文字不重叠。
- 可访问性：使用表单校验、状态 Alert、loading 和权限指令；错误不只依赖颜色表达。
- 加载/空态/错误：沿用工作台 loading 与空列表；决定弹窗具有独立加载态、blocker 和不确定响应锁定态。
- 权限：候选查询、任务类型/状态/业务范围和正式权限同时约束入口。
- E2E：真实角色写入链路留在 T11，不以静态合同冒充真实 E2E。

## T6 Blockers

- 实现与本地验证无功能 blocker。
- 提交门禁被并发 worktree 的非法 slot 20 登记阻塞；未修改其它任务登记，T6 尚未暂存或提交。

# T8 Feature

SP-3 四报告上传前端：三类负责人从候选待办进入批次详情，分别完成来料检报告、灭菌报告、成品检报告和成品检记录；附件准备与完成严格使用申请版本和幂等键，第四份完成后展示管理者代表阶段已建立。

## T8 Non-goals

- 不实现管理者代表最终批准或 trace 页面；属于 T9/T10。
- 不提供 skip、删除、覆盖、批量暂存或默认成功路径绕过四报告后端门禁。
- 不以静态合同冒充真实三账号文件上传；真实 E2E 留 T11。

## T8 Acceptance

- `AC-14`：四类报告按当前冻结候选独立展示，成品检负责人看到两个独立任务。
- `AC-15`、`AC-16`：附件、灭菌批号、候选权限、版本、幂等和字符串 ID 均为强制门禁，页面不提供绕过入口。
- `AC-17`、`AC-18`：前三份只显示版本推进；第四份展示正式放行事务和管理者代表待办。
- `AC-27`、`AC-28`、`AC-29`：复用真实候选待办/批次详情入口，具备加载、错误、禁用和成功反馈。

## T8 Entry And Ownership

- 候选入口：`WorkTaskBoardPage.vue` 的候选待办操作列。
- 上传与完成：`BatchExecutionDetailPage.vue` 的四报告节点右侧操作区和完成弹窗。
- API：`src/api/mes/pro/edhr/workTask.ts`、`src/api/mes/pro/edhr/batchExecution.ts`。
- 命名合同：`tests/e2e/sp3-production-release-report-upload-contract.spec.cjs`。

## T8 API And Data States

- GET `/mes/pro/edhr-work-task/candidate-todo-page`：按 `nodeTypes` 和 `batchExecutionId` 返回当前用户的报告候选待办、字符串 ID、节点名称和申请版本。
- POST `/mes/pro/edhr-batch-execution/task/special-node/attachment/prepare-upload`：multipart 携带报告 batchTaskId、expectedVersion、稳定幂等键和真实文件；prepare 不增加版本。
- POST `/mes/pro/edhr-batch-execution/task/special-node/complete`：携带同一批次任务、expectedVersion、完成幂等键、附件证据和可选灭菌批号；前三份返回 `REPORT_UPLOAD_PENDING`，第四份返回 `MANAGER_RELEASE_PENDING`。

## T8 BDD

- BDD: 三类负责人只见自己的四报告待办 -> Given 四报告已初始化 / When 三类负责人进入候选待办 / Then 只显示本人 1/1/2 个任务及当前版本。
- BDD: 报告附件准备必须使用版本与幂等 -> Given 当前用户是冻结候选 / When 选择文件 / Then prepare 带版本和稳定键，回执校验字符串 ID、哈希和留存证据。
- BDD: 四报告完成不可绕过 -> Given 当前节点属于四报告 / When 查看或完成 / Then 无 skip/delete/批量暂存/覆盖入口，缺附件或灭菌批号时阻止提交。
- BDD: 第四份回执展示管理者阶段 -> Given 前三份已完成 / When 第四份成功 / Then 展示放行事务和管理者代表待办已建立。

## T8 RED And GREEN

- RED: `node tests/e2e/sp3-production-release-report-upload-contract.spec.cjs` -> FAIL；首个失败为候选查询缺少 `nodeTypes?: string[]`。
- GREEN: `pnpm test sp3-production-release-report-upload-contract` -> PASS。
- GREEN: `pnpm test sp2-pqc-production-release-contract`、`pnpm test sp1-production-release-contract` -> PASS。
- GREEN: 批次详情 workTaskId 入口和共享 FormCenter 导航静态合同 -> PASS。
- GREEN: `pnpm ts:check`、`git diff --check -- IntRuoyiFronted` -> PASS。

## T8 Experience Checks

- 响应式：沿用现有批次详情三栏布局和 Dialog 尺寸；按钮使用现有稳定操作区，不新增浮层或嵌套卡片。
- 可访问性：候选加载、权限错误、成功阶段使用可读 Alert 和按钮禁用原因；结果不只依赖颜色。
- 加载/空态/错误：候选查询有独立 loading/error；非候选为空时保持只读，网络或回执异常直接显示，幂等键保留供同载荷重试。
- 权限：工作台和详情都要求候选 TODO、正式业务范围、四类 nodeType、批次/任务一致和权威版本。
- E2E：真实多账号上传需要任务自有批次、三类账号和测试附件，按计划在 T11 执行。

## T8 Blockers

- 实现与本地目标验证无功能 blocker。
- 两条既有特殊节点静态合同与主分支当前接口/弹窗名称已不一致；本任务未修改其旧断言，执行日志已记录首个基线失败。
- Git 提交仍被共享 branch runtime guard 的无关 worktree 非法 slot 20 登记阻塞；暂存区为空，未 push。

# T10 Feature

SP-4 管理者代表最终放行与可追溯前端：管理者代表从现有候选工作待办进入专用最终放行弹窗，提交事务版本、冻结待办、稳定幂等键和正式签核证据；成功后即时刷新只包含 `RELEASED` 的可追溯列表。

## T10 Non-goals

- 不恢复已退役的 eDHR 私有审批列表；现有 `ApprovalPage` 继续跳转统一审批中心，最终放行使用当前正式候选工作待办入口。
- 不为生产放行管理者任务提供拒绝、退回或撤回按钮。
- 不生成伪电子签名证据；页面要求录入正式签核证据哈希，缺失时禁止提交。

## T10 Acceptance

- `AC-20`、`AC-21`：入口同时依赖候选待办查询、`RELEASE_APPROVE + RELEASE_TRANSACTION + TODO` 上下文和 `mes:pro-edhr-release:approve` 权限。
- `AC-22`：专用操作区和弹窗只有最终放行动作，无拒绝、退回或撤回。
- `AC-24`、`AC-25`：请求携带字符串事务/待办 ID、权威事务版本、稳定幂等键、签核证据和审批意见，并只接受 `RELEASED` 权威回执。
- `AC-26`、`AC-27`：成功事实先确认，再刷新候选列表；刷新失败单独提示，不改报审批失败。
- `AC-28`、`AC-29`：Release Long ID 全链路按字符串处理；结构化 blocker 与不确定回执分别处理。

## T10 BDD And TDD

- BDD: 只有管理者冻结候选可最终放行 -> Given 当前用户从候选待办读取 `RELEASE_APPROVE` 任务 / When 任务是当前租户冻结候选、状态 TODO 且作用域为 `RELEASE_TRANSACTION` / Then 才显示带正式权限的“最终放行”按钮。
- BDD: 首版最终阶段无拒绝 -> Given 管理者最终放行任务 / When 查看操作区和弹窗 / Then 只存在批准，不存在拒绝、退回或撤回。
- BDD: 最终批准按权威回执确认 -> Given 事务为 `PENDING_APPROVAL` / When 提交事务 ID、待办 ID、版本、幂等键和正式签核证据 / Then 只有回执为 `RELEASED` 且身份/签核一致才显示成功；响应不确定时必须 GET 回执复核。
- BDD: 可追溯只显示正式放行 -> Given 打开表单追溯的放行页签 / When 查询分页 / Then 固定同时发送 `completedTraceOnly=true` 与 `releaseStatus=RELEASED`。
- RED: `node tests/e2e/sp4-manager-release-trace-contract.spec.cjs` -> FAIL；首个失败为 Release API 的事务 ID 仍是 `number`，且无管理者专用入口和双条件 trace 合同。
- GREEN: `pnpm test sp4-manager-release-trace-contract` -> PASS。

## T10 Verification

- `pnpm test sp1-production-release-contract`、`sp2-pqc-production-release-contract`、`sp3-production-release-report-upload-contract`、`sp4-manager-release-trace-contract` -> 全部 PASS。
- release transaction、form trace tabs/actions、owner return 和 precheck 相邻静态合同 -> 全部 PASS。
- `pnpm ts:check` -> PASS；`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0。
- `git diff --check` -> PASS；新增扫描命中仅为正式 `TODO` 状态和既有错误消息参数移除，不存在新增 fallback、default-success、FIXME、秘密或模拟成功。

## T10 Blockers

- 实现与本地静态/类型验证无功能 blocker。
- 真实多账号页面写入、即时 trace 和反向角色路径需要 T11 的账号、测试数据和成对运行态。
- Git 提交仍受共享 branch runtime guard 的无关非法 slot 20 登记阻塞；暂存区为空，未 push。
