# IntRuoyi E2E Rules

## 触发场景

- 编写、修改、运行或评审 Playwright E2E、真实用户路径验证、截图验收、登录后联调时，必须先读取本文件。
- 涉及登录、租户、账号时，还必须读取 `docs/login-access.md`。
- 涉及本机端口或 worktree 端口时，还必须读取 `docs/local-runtime.md` 和 `docs/worktree-restrictions.md`。

## 基本规则

- E2E 必须使用 Playwright 操作真实前端页面。
- API 只能用于最终状态核验或只读辅助检查，不得代替真实用户路径。
- 默认本机入口为 `http://localhost:8081` 或 `http://127.0.0.1:8081`。
- 写入型 E2E 必须使用已确认的测试租户和账号，并创建带任务标识、可追踪、可清理的数据。
- 只读验证必须说明使用的数据来源和只读范围。

## 缺入口处理

- 发布、审计或独立验证任务发现前端无入口时，必须 fail fast，不得临时扩大范围新增入口。
- 功能或修复任务只有在入口属于用户批准范围，且已完成 BDD + TDD 时，才允许补入口。


## 静态合同与真实 E2E 同步门禁

### Windows 换行与脚本行为同步

- Trigger: 修改 `tests/e2e/*static.spec.js`、真实 `*.e2e.js` 脚本、Windows worktree 融合后出现静态合同在目标 worktree 自身失败、CRLF/LF 差异或废弃弹窗流程断言。
- Preflight check: 先在目标 worktree 和当前工作区分别运行同一静态合同；读取源码时对只检查模板片段的静态合同统一归一化 CRLF 为 LF；确认真实 E2E 脚本与当前页面真实用户路径一致。
- Blocker: 若静态合同在目标 worktree 自身也失败，必须先判断是合同过期、换行误判还是产品实现失败；不得把目标 worktree 自身失败直接当作融合漏项。
- Verification: 更新静态合同后必须重跑目标 worktree 涉及的全部静态合同；涉及真实 E2E 脚本行为变更时，至少用静态合同断言真实脚本等待的 API、点击的按钮和禁止的旧弹窗步骤。
- Forbidden action: 禁止为通过静态合同改产品文案或 DOM 顺序；禁止保留真实脚本里的废弃确认弹窗、签名密码输入或 API-only 替代页面点击。
- Evidence: `doc/tasks/merge-jiluben-worktree-20260724/verification-report.md`。
## 禁止做法

- 禁止 mock 数据冒充真实 E2E。
- 禁止 API-only 代替前端路径。
- 禁止直接 SQL 或接口直塞绕过页面。
- 禁止修改生产租户、admin 基线数据或无关真实业务记录。
- 禁止为了测试额外添加产品上不需要的前端控件。

## 验证方式

- 记录 Playwright 命令、入口 URL、租户/用户标签、目标页面和关键断言。
- 写入型 E2E 记录测试数据标识和清理方式。
- 失败时记录实际失败位置、页面状态、网络响应或控制台错误。



## 官方登录前置与 admin-only 全量验证门禁

- Trigger: E2E 脚本调用 `scripts/preflight/login-preflight.mjs`、执行 `芋道源码/admin` 只读全量验证、或工作区融合后发现真实 E2E 登录前置脚本缺失/目标文案过期。
- Preflight check: `scripts/preflight/login-preflight.mjs` 必须存在于工作区根目录并通过真实前端登录；目标文本必须使用当前页面真实可见文案，不得沿用历史菜单标题。密码只能通过临时环境变量或命令参数传入，任务日志和证据必须脱敏。
- Blocker: 若只授权 `芋道源码/admin`，写入型、多用户、签名、放行、发布或需测试租户数据清理的 E2E 必须记录 BLOCKED；不得在 admin 基线租户上创造测试写入数据，也不得用 API-only、直连历史 execution 填写页或 mock 代替。
- Verification: 管理员只读验证应优先覆盖登录前置、批次详情、只读预览、伴随单据、表单日志、权限可见性和无 MES 写请求；当前活动填写必须走正式页面按钮或 `openTask` 返回上下文，历史只读必须走 tracking 模式。
- Forbidden action: 禁止删除或跳过官方登录 preflight；禁止把缺失 preflight 脚本当成 E2E 通过；禁止在真实脚本中保留历史默认密码；禁止把过期固定批次/任务 ID 当作长期前置。
- Evidence: `doc/tasks/20260725-full-e2e-admin-validation/verification-report.md`。
## eDHR 批次执行数据库夹具与证据文件门禁

- Trigger: 运行 `edhr-batch-execution-real-flow.e2e.js`、复跑 eDHR 批次执行真实 E2E、或脚本默认写入 `doc/tasks/<task-id>/real-e2e-evidence.md`。
- Preflight check: 默认从本机 Docker MySQL `int-ruoyi-mysql/ruoyi-vue-pro` 读取授权租户、账号、批次执行、批次任务、工作任务和执行 ID；写型验证若需调整责任人或夹具数据，必须先记录原始值、影响行数和回滚 SQL。`EDHR_BATCH_E2E_TASK_ID`、`EDHR_BATCH_E2E_EVIDENCE_FILE`、浏览器路径等只允许作为可选运行参数，不得作为工单、批次、填写值或签名密码的必需来源。
- Blocker: 本地数据库不可达、授权租户/账号不存在、无当前账号可打开的待办工作任务、目标租户未获当前任务明确授权、写入影响行数不是预期值、或证据路径会覆盖非当前任务历史 PASS 证据时，必须停止，不得进入浏览器或伪造通过。
- Verification: 记录 E2E 命令、证据文件路径、入口 URL、租户/账号标签、数据库来源、批次执行 ID、任务 ID、执行 ID、DB 写入行数、回滚方式，以及脚本 PASS/BLOCKED 结果。
- Forbidden action: 禁止把工单/批次/密码等业务数据重新改成必需环境变量；禁止记录明文密码；禁止用 mock、API-only、默认成功、生产/未授权租户或未记录的数据库直改替代真实前端路径。
- Evidence: `doc/tasks/fix-batch-record-fill-rule/execution-log.md`，2026-07-25 脚本已改为数据库夹具读取，并在用户授权的 `芋道源码/admin` 下完成真实前端 E2E。


## eDHR 历史执行只读验证门禁

- Trigger: Playwright 需要从 eDHR 批次详情、批记录、记录本或执行记录入口打开 `/mes/pro/feedback/edhr-execution/form`，尤其是复验历史 `executionId`、`batchTaskId`、`workTaskId`、`returnPath` 或 `viewMode`。
- Preflight check: 先区分“当前活动填写”与“历史执行只读追踪”。当前活动填写必须通过页面按钮或正式 `openEdhrBatchTask` 流程获取后端返回的当前 execution/workTask 上下文；历史执行只读必须使用 `viewMode=tracking`，并使用具备对象 VIEW 权限的只读账号标签。
- Blocker: 若页面提示“当前用户不是该 eDHR 工作任务责任人”、“非当前活动表单”或 `BATCH_RECORD_EXECUTION:<id>:VIEW` 权限不足，先记录页面正文和账号/租户标签，停止该路径结论；不得把历史 executionId 直接拼成填写 URL 继续跑。
- Verification: 只读 tracking E2E 必须断言 `eDHR 追踪详情`、追踪表单区域、返回批次详情时保留 `batchExecutionId` 与 `batchTaskId`，并断言无 MES 写请求；填写页 toolbar/返回按钮可用性用真实填写路径或静态合同补充覆盖。
- Forbidden action: 禁止用 API-only、管理员写入、旧 executionId 直连填写页、忽略对象级权限、或把 read-only tracking 当作写入路径 fallback。
- Evidence: `doc/tasks/post-merge-jiluben-e2e-20260725/verification-report.md`。
## eDHR 单据填写人显示值门禁

- Trigger: Playwright 验证 eDHR 批次详情右侧单据卡片、损耗单、过程检验单、参数记录表、`fillableUsers`、填写人显示值。
- Preflight check: 页面断言前先通过同一登录会话的详情接口读取目标任务 `fillableUsers`，以接口当前 `displayName/nickname/username` 为页面期望值；不得硬编码配置页历史 `candidateSourceNames` 格式。
- Blocker: 若详情接口 `fillableUsers` 为空、只返回角色/部门 ID、或页面卡片显示值与详情接口当前显示值不一致，必须停止并记录接口任务、页面可见卡片和账号/租户标签。
- Verification: 真实 E2E 同时记录批次编码/ID、命中任务、接口填写人、页面卡片可见文本和无 MES 写请求检查。
- Forbidden action: 禁止把旧配置页候选名称、当前登录人、创建人、更新人或账号拼接格式当作页面期望值；禁止把 API-only 断言当成单据卡片显示通过。
- Evidence: `doc/tasks/20260725-edhr-route-form-filler-e2e/real-e2e-evidence.md`。
## Element Plus 下拉选择门禁

- Trigger: Playwright 在 Element Plus `el-select` 中选择租户、工单、工艺路线、角色、用户或其他写入型业务对象。
- Preflight check: 优先按页面可见业务唯一文本定位选项，例如租户名称、工单编码、路线编码/名称/ID；填入搜索词后必须等待目标 `.el-select-dropdown__item:visible` 出现并点击该选项。
- Blocker: 如果只填输入框后按 Enter 未触发真实选项选择、目标选项未出现、或页面显示文本与脚本断言字段不一致，必须停止并记录下拉可见文本和相关接口响应，不得继续提交写入。
- Verification: 对写入结果使用 UI 响应和最终只读 API/DB 核验；涉及发布版/草稿版差异时，必须核验落库版本 ID、版本号、快照 JSON 和当前草稿仍存在。
- Forbidden action: 禁止把接口数组下标、隐藏 value、输入框残留文本、API-only 选中或坐标点击当作真实页面选择。
- Evidence: `doc/tasks/20260724-batch-execution-published-route-runtime-update/execution-log.md`。

## 表格行定位

- 当页面对列表进行本地排序、过滤或虚拟渲染时，Playwright 必须按页面可见的业务唯一文本定位目标行，再操作同一行的复选框或按钮。
- 不得直接用 API 返回数组下标映射前端表格行；接口排序和页面排序可能不同，会误选冻结行、错误行或无关业务数据。
- Element Plus `el-table` 存在 header/body/fixed 表格重复 DOM 时，选择行复选框必须限定在可见 `.el-table__body-wrapper tbody tr`，显式排除 `.el-table__header-wrapper` 和 `thead`；点击后必须立即断言已选业务唯一键集合，再进入“确认/应用”等写入动作。

### Element Plus 表格选择门禁

- Trigger: Playwright 需要在 Element Plus `el-table` 中勾选行复选框、批量操作、手动重排、确认应用或其他写入型流程。
- Preflight check: 在写入动作前读取可见 body 行文本，断言已选业务唯一键集合与目标集合完全一致。
- Blocker: 若选中集合缺失目标行、包含额外行，或点击坐标落在 header checkbox / indeterminate checkbox 上，必须停止并修复定位逻辑。
- Verification: 保留真实 E2E 命令、选中集合断言、写入请求参数、最终 UI/API 状态和截图/JSON 证据路径。
- Forbidden action: 禁止用表头全选、数组下标、API-only、直接 SQL 或坐标猜测绕过可见业务行定位。
- Evidence: `doc/tasks/verify-manual-reschedule-881mo-20260724/execution-log.md`，2026-07-24 手动重排修复验证。

### Codex Runner 自动测试门禁

- Trigger: 新增、修改、运行或验收 `系统管理 > 测试管理`、Codex Runner、自然语言测试方法、检查点截图或由 Codex 调用 Playwright 的自动测试流程。
- Preflight check: 真实执行前必须确认本机前端/后端入口、目标测试租户、测试管理员账号、Runner token、Codex CLI、Playwright 浏览器、Runner 本地凭据映射和测试数据清理责任。
- Blocker: 任一 Runner 或租户前置条件缺失、测试项会写入生产/非任务租户、失败检查点没有差异描述、截图路径不在受控临时目录，或并行执行包含 `parallelSafe=false` 项时必须停止。
- Verification: 记录 Runner 注册/领取/心跳/回写命令、页面执行入口、租户/用户标签、检查点结果、失败截图 artifact、最终 UI 状态和必要的只读 API 核验。
- Forbidden action: 禁止把 API-only、静态合同测试、mock 截图、默认成功、Runner 离线跳过或顺序执行降级当作真实 E2E 通过。
- Evidence: `doc/tasks/20260724-codex-test-management-delivery/verification-report.md`，2026-07-24 Codex 测试管理交付。
