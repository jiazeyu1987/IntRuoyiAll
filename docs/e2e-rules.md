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

### Worktree 隔离运行态 URL 门禁

- Trigger: 主工作区默认端口被并行任务占用、旧 jar 未加载当前接口、或真实 E2E 需要使用已登记 worktree slot 端口运行。
- Preflight check: 同时显式传入前端和后端 URL，且端口必须来自同一 runtime slot；脚本要校验本机地址、显式端口和前后端端口配对关系，不能只覆盖前端或只覆盖后端。
- Blocker: 只传一个 URL、前后端端口不属于同一 slot、未确认端口监听命令行归属目标 worktree、或后端业务接口返回配置缺失/404 时必须停止并记录真实原因，不得静默切回 8081/48081 或 API-only。
- Verification: 记录 base URL、backend URL、端口登记项、前端 HTTP 200、后端 health UP、关键目标接口业务响应、真实页面断言，以及任务结束后的端口释放结果。
- Forbidden action: 禁止强停并行 48081、随机换端口、只看 health 就宣称目标 Controller 已加载、或用未配对的 frontend/backend URL 造成前端访问旧后端。
- Evidence: `doc/tasks/20260726-edhr-release-dossier-requirement-switches/execution-log.md`，48081 旧 jar 返回新增接口 404 后，使用 slot 5 的 8086/48086 成对 URL 完成真实 E2E。

### Windows 换行与脚本行为同步

- Trigger: 修改 `tests/e2e/*static.spec.js`、真实 `*.e2e.js` 脚本、Windows worktree 融合后出现静态合同在目标 worktree 自身失败、CRLF/LF 差异或废弃弹窗流程断言。
- Preflight check: 先在目标 worktree 和当前工作区分别运行同一静态合同；读取源码时对只检查模板片段的静态合同统一归一化 CRLF 为 LF；确认真实 E2E 脚本与当前页面真实用户路径一致。
- Blocker: 若静态合同在目标 worktree 自身也失败，必须先判断是合同过期、换行误判还是产品实现失败；不得把目标 worktree 自身失败直接当作融合漏项。
- Narrow fix: 若当前任务只修一个窄范围页面缺陷，而同一个宽静态合同存在无关既存失败，先保留失败证据，再新增或运行聚焦本缺陷的独立静态合同；不得为了通过宽合同顺手改无关产品逻辑或断言。
- Verification: 更新静态合同后必须重跑目标 worktree 涉及的全部静态合同；涉及真实 E2E 脚本行为变更时，至少用静态合同断言真实脚本等待的 API、点击的按钮和禁止的旧弹窗步骤。
- Forbidden action: 禁止为通过静态合同改产品文案或 DOM 顺序；禁止保留真实脚本里的废弃确认弹窗、签名密码输入或 API-only 替代页面点击。
- Evidence: `doc/tasks/merge-jiluben-worktree-20260724/verification-report.md`。

### Schema-backed E2E 迁移与字段可选态门禁

- Trigger: 真实 E2E 验证新增 schema 字段支撑的页面能力、工作台上下文字段、单元格链接、字段矩阵、合成来源字段、`source_type`、`source_field_code`、`sourceFields`、或页面接口返回 `Unknown column` / `系统异常`。
- Preflight check: 浏览器路径前先核对当前后端连接库已应用本任务正式迁移；若页面展示合成字段矩阵，E2E 必须断言可见文本和可交互态同时存在，例如 `.is-source-selectable`、选中态、目标单元格选择和主动作按钮 enabled。
- Blocker: 缺迁移列、接口 500、字段文字可见但没有可选 class、点击字段后选中态不变、或只读账号需要写入保存才能证明行为时必须停止并记录；不得把“页面看得到字段”当成可选择或可保存通过。
- Verification: 证据需包含 schema 列核对结果、真实前端入口 URL、租户/用户标签、字段白名单数量、目标页可见断言、可选/选中态断言、主动作按钮状态，以及是否发送 MES 写请求。
- Forbidden action: 禁止用 API-only、mock response、绕过页面直连 URL、忽略 schema 缺列、只断言文本不断言可选态、或在 `芋道源码/admin` 基线数据上保存规则冒充写入 E2E。
- Evidence: `doc/tasks/20260726-work-order-field-cell-link/verification-report.md`。
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


## 全局开关类 E2E 恢复门禁

- Trigger: Playwright 验证全局开关、共享配置、租户级开关、系统级配置或任何影响后续用户路径的运行态状态切换。
- Preflight check: 切换前读取并记录原始状态；脚本必须有 `finally` 恢复逻辑，恢复后再用独立 API 或页面断言确认状态回到原始值。
- Blocker: 关闭/开启断言通过但恢复失败、恢复后接口值不一致、或页面仍显示变更后的状态时，必须立即执行受控恢复并记录失败位置；不得把产品断言 PASS 当作完整 E2E PASS。
- Verification: 证据必须同时包含变更态断言、恢复动作结果、恢复后页面或接口复验；恢复使用 API 时必须说明它是 cleanup，不得替代真实页面变更路径。
- Forbidden action: 禁止留下全局开关关闭、禁止记录密码/token、禁止用未复验的 `finally` 假设恢复成功。
- Evidence: `doc/tasks/20260725-edhr-global-recordbook-switch/verification-report.md`。



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

### eDHR 工作任务 FormCenter 动态表单夹具门禁

- Trigger: 运行或修改 `edhr-work-task-process-advance-real.e2e.js`、个人工作台 `edhr-work-task/my-page` 到 FormCenter 动态表单的真实 E2E，或出现 `生产工单不存在`、`当前工艺路线工序未配置默认批记录报表`、`eDHR 批次工序任务被阻塞`。
- Preflight check: 夹具必须创建任务自有真实 `mes_pro_work_order` 并贯穿 `batch_execution/work_task`；FormCenter 动态路线表单任务必须 `batch_record_report_id` 为空、`form_binding_key` 非空、`form_template_id/form_template_version_id/form_center_instance_id` 完整；首工序全部同工序任务必须 `root_process_flag=true`，下一工序必须写入 `predecessor_route_process_id`。
- Blocker: 缺少真实工单、把 FormCenter binding key 塞进 `batch_record_report_id`、动态任务缺 FormCenter 上下文、首工序非 root、下一工序无 predecessor、或页面点击未限定目标可见行时必须停止修复夹具；不得放松后端 `task/open` 校验。
- Verification: 真实 E2E 必须从个人工作台按目标批次和任务编码所在 `.el-table__body-wrapper tbody tr:visible` 点击“处理”，提交 FormCenter 抽屉后用 DB 断言当前任务完成、effect applied、下一工序 fill count 符合业务规则，并在 finally/收尾中清理 `EDHR-ADV-%` 任务自有数据。
- Forbidden action: 禁止用固定不存在工单 ID、API-only submit、直连详情 URL、点击页面第一个“处理”按钮、把动态表单降级为传统批记录、或保留明文 MySQL 密码参数。
- Evidence: `doc/tasks/20260727-edhr-process-fill-advance-optimization/verification-report.md`。

## eDHR 作废 BPM 审批真实 E2E 门禁

- Trigger: Playwright 验证 eDHR 批次作废、`void-batch-execution/approval-resolution`、`void-batch-execution/request`、审批中心 `BPM_REQUIRED`、或作废后工作台待办闭环。
- Preflight check: 作废弹窗打开前就启动 `approval-resolution` 响应等待，因为页面可能在打开弹窗时解析审批策略；提交作废前再等待 `request` 响应。若策略为 `BPM_REQUIRED`，必须通过审批中心真实页面审核，并按 `act_ru_task.PROC_INST_ID_` 的实际 `ASSIGNEE_` 映射 `system_users.username` 登录审批人。
- Blocker: 未捕获 `approval-resolution`、审批待办不属于当前 `processInstanceId`、审批人账号无法映射、审批中心列表未出现目标行、或作废后仍有 TODO/DOING/OVERDUE 工作任务时必须停止。
- Verification: 证据需包含成对 frontend/backend URL、作废列表页行级点击、`approval-resolution` 与 `request` HTTP 200、审批中心行级“审核”点击、`tasks/review` payload 锁定同一 `processInstanceId`、批次状态 `VOIDED`、变更事件 `EFFECTIVE`、活动工作任务取消、负责人工作台 `my-page/stats` 排除、旧任务链接 fail-fast、artifact JSON 路径。
- Forbidden action: 禁止把 `approval-resolution` 当作提交后才发生的请求；禁止硬编码固定审批人；禁止用接口直审、SQL 改状态、API-only 或前端隐藏替代真实审批中心路径。
- Evidence: `doc/tasks/20260727-edhr-batch-void-work-task-closure/verification-report.md`。


## eDHR 历史执行只读验证门禁

- Trigger: Playwright 需要从 eDHR 批次详情、批记录、记录本或执行记录入口打开 `/mes/pro/feedback/edhr-execution/form`，尤其是复验历史 `executionId`、`batchTaskId`、`workTaskId`、`returnPath` 或 `viewMode`。
- Preflight check: 先区分“当前活动填写”与“历史执行只读追踪”。当前活动填写必须通过页面按钮或正式 `openEdhrBatchTask` 流程获取后端返回的当前 execution/workTask 上下文；历史执行只读必须使用 `viewMode=tracking`，并使用具备对象 VIEW 权限的只读账号标签。
- Blocker: 若页面提示“当前用户不是该 eDHR 工作任务责任人”、“非当前活动表单”或 `BATCH_RECORD_EXECUTION:<id>:VIEW` 权限不足，先记录页面正文和账号/租户标签，停止该路径结论；不得把历史 executionId 直接拼成填写 URL 继续跑。
- Verification: 只读 tracking E2E 必须断言 `eDHR 追踪详情`、追踪表单区域、返回批次详情时保留 `batchExecutionId` 与 `batchTaskId`，并断言无 MES 写请求；填写页 toolbar/返回按钮可用性用真实填写路径或静态合同补充覆盖。
- Forbidden action: 禁止用 API-only、管理员写入、旧 executionId 直连填写页、忽略对象级权限、或把 read-only tracking 当作写入路径 fallback。
- Evidence: `doc/tasks/post-merge-jiluben-e2e-20260725/verification-report.md`。
## eDHR 终态批次个人待办门禁

- Trigger: 个人控制台、eDHR 工作任务、`edhr-work-task/my-page`、`edhr-work-task/stats`、`workTaskId` 打开提示“当前 eDHR 批次状态不允许该操作”，或数据库中 `mes_pro_edhr_work_task.status=TODO/OVERDUE` 但关联批次已关闭、归档、驳回或作废。
- Preflight check: 先只读核对 `mes_pro_edhr_work_task.batch_execution_id` 与 `mes_pro_edhr_batch_execution.status`；若批次为终态，`openTask` 阻断是正确保护，应检查个人待办列表、统计和审批中心候选待办是否从源头排除终态批次。
- Blocker: 若真实页面仍展示终态批次任务或统计仍计入终态批次任务，必须修复列表/统计查询；不得放松 `openTask` 的终态批次 fail-fast 校验。
- Verification: 后端回归需覆盖“同一用户同时有正常批次和终态批次 TODO 时，个人待办与统计只返回正常批次”；真实 E2E 用责任人账号进入个人控制台，断言目标终态任务不在 `my-page` 响应和页面正文中，且没有“当前 eDHR 批次状态不允许该操作”。
- Forbidden action: 禁止为了让按钮可点而允许终态批次进入填写页；禁止用前端隐藏、吞 toast、API-only 打开或改任务状态替代列表源头过滤。
- Evidence: `doc/tasks/20260726-edhr-personal-console-open-task-status/verification-report.md`。
## eDHR 单据填写人显示值门禁

- Trigger: Playwright 验证 eDHR 批次详情右侧单据卡片、损耗单、过程检验单、参数记录表、`fillableUsers`、填写人显示值。
- Preflight check: 页面断言前先通过同一登录会话的详情接口读取目标任务 `fillableUsers`，以接口当前 `displayName/nickname/username` 为页面期望值；不得硬编码配置页历史 `candidateSourceNames` 格式。
- Blocker: 若详情接口 `fillableUsers` 为空、只返回角色/部门 ID、或页面卡片显示值与详情接口当前显示值不一致，必须停止并记录接口任务、页面可见卡片和账号/租户标签。
- Verification: 真实 E2E 同时记录批次编码/ID、命中任务、接口填写人、页面卡片可见文本和无 MES 写请求检查。
- Forbidden action: 禁止把旧配置页候选名称、当前登录人、创建人、更新人或账号拼接格式当作页面期望值；禁止把 API-only 断言当成单据卡片显示通过。
- Evidence: `doc/tasks/20260725-edhr-route-form-filler-e2e/real-e2e-evidence.md`。
## eDHR 路线表单跳过口径门禁

- Trigger: 修改或验证 eDHR 批次详情右侧路线表单卡片、损耗单、过程检验单、参数记录表、`isOptionalTask`、`canSkipOptionalTask`、`requiredPolicy`、`requiredFlag`、`SKIP` 动作、无 `OPEN_FORM` 的只读查看动作，或错误“必填路线表单不允许跳过”。
- Preflight check: 先核对详情任务的 `requiredPolicy` 和 `allowedActions`；只有 `requiredPolicy === 'OPTIONAL'` 且后端返回 `SKIP` 动作时，前端才允许显示或执行“跳过表单”。若账号无 `OPEN_FORM` 但任务存在 `formCenterInstanceId/formTemplateId` 等查看上下文，必须通过真实卡片点击验证“查看表单”只读抽屉，而不是只用预览接口或直连历史 execution 代替；动态表单选中态也不得调用 legacy `/task/preview` 批记录预览接口。
- Blocker: 若前端用 `requiredFlag=false`、非必填进度口径、表单槽位类型、当前载体选择或本地状态推断可跳过，必须停止并改为后端 `requiredPolicy + allowedActions` 口径。
- Verification: 至少运行聚焦静态合同，断言 `isOptionalTask` 通过 `isOptionalRouteFormTask` 对齐 `requiredPolicy === 'OPTIONAL'`，并断言必填损耗单点击路径调用打开填写而非跳过接口；涉及无填写权限但有查看权限时，真实 E2E 必须断言卡片主动作是“查看表单”、抽屉动作按钮全部禁用，未触发 `/task/preview`、`/task/open`、`/task/special-node/skip` 或表单中心写请求，且页面没有“必填路线表单不允许跳过”红色错误。
- Forbidden action: 禁止为了避开“必填路线表单不允许跳过”而吞掉后端错误、隐藏按钮错误、改文案、API-only 直开历史 execution，或把必填表单改成可跳过。
- Evidence: `doc/tasks/20260725-edhr-loss-form-open-action/verification-report.md`。
## eDHR 右侧红框元信息隐藏门禁

- Trigger: 修改 eDHR 批次详情右侧栏、单据卡片、`edhr-batch-detail__primary-fill-meta`、`primaryFormFillMetaItems`、填写人/提交时间摘要、工艺路线配置右侧 `data-flow-panel="selected-field-detail"` 或截图红框区域。
- Preflight check: 先区分“单据卡片内填写人”与“右侧独立填写元信息红框”；删除红框时必须同时确认 `edhr-batch-detail__rail-process-form-filler` 和 `resolveTaskCardFillersText(task)` 仍保留。若修改工艺路线 `batchRecordFormNames` 字段明细，必须确认字段值、链接和节点红绿边框都使用显式槽位匹配，不得把缺少 `formSlotType` 的其它表单默认归入 `MAIN`。
- Blocker: 若源码仍存在 `primary-fill-meta`、`primaryFormFillMetaItems`、`showPrimaryFormFillMeta`、`resolvePrimaryFormFillersText` 或 `resolvePrimaryFormSubmitTimesText`，不得声明红框已删除；若单据卡片填写人被一起删除，必须停止并修复。若 `batchRecordFormNames` 仍通过带默认 `MAIN` 的 `normalizeRecordBindingSlotType` 过滤右侧明细或节点绑定状态，不得声明批记录表单红框过滤完成。
- Verification: 至少运行 `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` 和 `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js`，一个确认红框无残留，一个确认单据卡片填写人保留。涉及工艺路线批记录表单字段明细时，还必须运行 `node tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js`。
- Forbidden action: 禁止把右侧独立红框移动到其他一级区域伪装删除；禁止为了通过宽静态合同顺手修改与红框无关的审批/提交逻辑。
- Evidence: `doc/tasks/20260725-hide-edhr-right-fill-meta-redbox/bug-regression-evidence.md`；`doc/tasks/20260726-batch-record-detail-panel-form-filter/bug-regression-evidence.md`。
## Element Plus 下拉选择门禁

- Trigger: Playwright 在 Element Plus `el-select` 中选择租户、工单、工艺路线、角色、用户或其他写入型业务对象。
- Preflight check: 优先按页面可见业务唯一文本定位选项，例如租户名称、工单编码、路线编码/名称/ID；填入搜索词后必须等待目标 `.el-select-dropdown__item:visible` 出现并点击该选项。若 `el-select` 位于 `el-popover`、抽屉内局部弹层或 click-outside 容器中，必须确认下拉面板归属不会触发外层误关闭，必要时使用受控可见状态和 `:teleported="false"` 静态合同锁定。
- Blocker: 如果只填输入框后按 Enter 未触发真实选项选择、目标选项未出现、页面显示文本与脚本断言字段不一致，或选择项点击导致外层 Popover 在确认动作前误关闭，必须停止并记录下拉可见文本、弹层状态和相关接口响应，不得继续提交写入。
- Verification: 对写入结果使用 UI 响应和最终只读 API/DB 核验；涉及发布版/草稿版差异时，必须核验落库版本 ID、版本号、快照 JSON 和当前草稿仍存在。Popover 内下拉还必须验证“选择后保持打开、确认成功后显式关闭”。
- Forbidden action: 禁止把接口数组下标、隐藏 value、输入框残留文本、API-only 选中或坐标点击当作真实页面选择。
- Evidence: `doc/tasks/20260724-batch-execution-published-route-runtime-update/execution-log.md`；`doc/tasks/20260726-route-flow-copy-popover-stability/execution-log.md`。

### Element Plus 上传控件门禁

- Trigger: Playwright 通过 Element Plus `el-upload`、隐藏 `input[type=file]`、拖拽上传区或 Word/附件导入弹窗执行真实文件上传。
- Preflight check: `setInputFiles` 后必须断言可见上传列表出现目标文件名，或断言页面已发出目标上传请求；未看到文件列表时不得直接点击提交并长时间等待响应。
- Blocker: 文件名未进入上传列表、上传请求未触发、导入按钮只触发表单校验、或页面停留在空上传控件时必须记录 BLOCKED；不得改用 API-only 上传替代真实页面路径。
- Verification: 证据需包含真实文件路径、页面入口、上传接口、文件列表断言、请求触发断言、最终响应或阻塞截图。
- Forbidden action: 禁止只因为 `input.files.length > 0` 就认定 Element Plus 组件状态已接收文件；禁止等待接口超时后不记录文件列表状态。
- Evidence: `doc/tasks/20260727-shared-word-parser-real-e2e/verification-report.md`。

### Element Plus 选择框显示门禁

- Trigger: 修改 Element Plus `el-select` 多选字段、`el-input-number` 数字步进控件、弹窗内多列配置表单、角色/人员/租户/目标项等较长业务名称的输入或选中标签显示。
- Preflight check: 先按 `label-width + grid-template-columns + gap` 核算真实输入区宽度；关键字段必须使用专用布局类和静态合同覆盖。`el-input-number` 默认宽度可能大于网格列，必须显式设置 `width: 100%` 收敛到所在列；文本输入列需要 `min-width: 0` 和 `width: 100%`。必要时在 `el-select` 控件作用域内覆盖 `.el-select__tags-text` 默认省略宽度。
- Blocker: 若选中值或输入值在控件内仍显示为 `...`、数字步进控件溢出挤压相邻输入框、只靠 tooltip 或下拉选项完整展示、或静态合同无法锁定该字段专用布局，必须停止并修复布局。
- Verification: 静态合同或真实 E2E 必须断言目标控件有专用布局类、关键列宽足够、数字步进控件收敛到当前列、文本输入框可完整占满分配列、选中标签未继续使用默认省略宽度。
- Forbidden action: 禁止把 `collapse-tags-tooltip`、扩大整页/整弹窗、硬编码当前角色名/目标项名、只验证下拉选项文本、或只调宽一个控件但让相邻控件继续被挤压当成“显示完整”。
- Evidence: `doc/tasks/20260725-edhr-pressure-pump-v13-filler-role/verification-report.md`；`doc/tasks/20260726-codex-test-target-item-input-display/verification-report.md`。

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
- Preflight check: 真实执行前必须确认本机前端/后端入口、目标测试租户、测试管理员账号、Runner token、Codex CLI、Playwright 浏览器、Runner 本地凭据映射和测试数据清理责任；后端必须用当前 token 完成注册探针，Runner loop 必须在执行中和空闲轮询中持续 heartbeat；不得把 `codex-test-runner.mjs --loop` 进程存在当作在线证明，必须核对后端 Runner 状态或数据库 `last_heartbeat_time` 未过期。测试管理执行入口若支持按需 Runner，前端不得因旧 Runner 离线/过期直接阻断执行，必须由后端受控启动脚本完成启动、注册、能力校验和失败原因返回。
- Blocker: 任一 Runner 或租户前置条件缺失、Runner token 与后端运行态不一致、Runner 进程存在但注册失败或 heartbeat 超过后端超时阈值、测试项会写入生产/非任务租户、失败检查点没有差异描述、截图路径不在受控临时目录、并行执行包含 `parallelSafe=false` 项、执行中 heartbeat 超过后端超时阈值、或 Windows `codex.cmd` 后代进程在超时/取消后仍持有 `codex-test-result-*` 输出文件时必须停止。
- Verification: 记录 Runner 注册/领取/执行期心跳/空闲心跳/回写命令、页面执行入口、租户/用户标签、检查点结果、失败截图 artifact、最终 UI 状态和必要的只读 API 核验；空闲场景至少等待一个 heartbeat 周期后复查 heartbeat age 仍小于超时阈值；Windows Runner 必须证明 timeout/cancel 后不存在本任务 `codex-test-result-*` 子进程，执行项不遗留 `CLAIMED/RUNNING`。
- Forbidden action: 禁止把 API-only、静态合同测试、mock 截图、默认成功、Runner 离线跳过、前端硬拦截 `没有在线 Codex Runner`、裸调用 `codex` CLI、只杀 `cmd.exe` 而不处理 `node/codex.exe` 后代进程、或顺序执行降级当作真实 E2E 通过。
- Evidence: `doc/tasks/20260724-codex-test-management-delivery/verification-report.md`，2026-07-24 Codex 测试管理交付；`doc/tasks/20260725-codex-runner-void-test/verification-report.md`，2026-07-26 Runner 心跳、Windows 子进程树、取消处理修复；`doc/tasks/20260726-codex-runner-on-demand-wrapper/verification-report.md`，2026-07-26 按需 Runner 包装层。

### Codex Runner 目标测试项存在性门禁

- Trigger: 用户指定运行测试管理中的某个测试项名称，例如“作废测试”，或要求 Runner 领取并执行单个自然语言测试项。
- Preflight check: 在点击执行前，先通过真实测试管理页面按可见业务名称搜索目标项；如页面未命中，再只读核对 `system_codex_test_case` 中目标名称、状态、租户和删除标记。
- Blocker: 目标测试项不存在、被删除、禁用、租户不匹配，或名称只存在于历史任务文档/截图而非当前系统数据时，必须停止；不得自动新建占位测试项、改跑其它测试项或把 Runner 空领取当作执行成功。
- Verification: 证据需包含页面搜索总数、只读 API 或 DB 名称列表、目标租户/用户标签，以及是否创建了 executionId。
- Forbidden action: 禁止用模糊关键词误选其它测试项；禁止用 API-only 启动替代页面行级“执行”点击；禁止在缺少测试方法和目标项的情况下临时造数。
- Evidence: `doc/tasks/20260725-codex-runner-void-test/verification-report.md`。
## eDHR 本地状态样本操作审计追溯门禁

- Trigger: Playwright 验证本地状态样本、`LOCAL_STATE_SAMPLE_CREATE`、批次追溯操作审计、或只按 `batchExecutionId` 查询操作日志。
- Preflight check: 写入型 E2E 必须通过真实页面创建任务自有样本批次，并确认样本批次任务具备可用于批次追溯的对象级权限 scope（至少覆盖 `AUDIT_VIEW`）。
- Blocker: 如果操作审计行已创建，但追溯接口返回 `BATCH_EXECUTION:<id>` 对象级权限范围不存在或未启用，必须修复样本创建事务的权限 scope 绑定；不得用 SQL 补权限、API-only 或管理员绕过冒充通过。
- Verification: E2E 需断言 `/mes/pro/edhr-operation-audit/page` 请求包含 `batchExecutionId`，不包含 `objectType/objectId`，并在表格中看到目标 operationType、权限判定、结果状态和 audit hash。
- Forbidden action: 禁止只验证审计表落库而不验证批次追溯可见性；禁止把权限缺失解释为页面无数据；禁止记录登录密码。
- Evidence: `doc/tasks/20260724-batch-fda-audit-log-coverage/verification-report.md`。
