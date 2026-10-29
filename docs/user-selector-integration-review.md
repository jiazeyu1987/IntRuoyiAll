# 统一人员选择器接入与审查记录

日期：2026-10-27。范围：本会话确认的 BPM、系统管理、DCC、MES 与表单中心选人场景。

## 申请人关系页签扩展（2026-10-28）

- 19:41用户授权后已重启共享8081/48081，原后端包补载真实IntAuth凭据；19:43后端启动，真实登录成功。本任务重复8101/48101已关闭。真实菜单创建测试角色成功，两种纯关系保存并完整刷新重读通过；固定用户+两种关系组合保存、刷新读取及关系预选均通过。保留任务专用角色 `E2E申请人关系20261028_1915`，未绑定业务路线。此结果解除下列历史记录中的创建/持久化阻塞；实际申请执行E2E和最终后端单元测试仍未完成。详见E2E记录顶部共享服务复测段。
- 后续修复：启动脚本提供显式 `-IntAuthEnvFile` 安全加载真实服务凭据，已启动IntAuth8020与独立后端48101，脚本检查通过，未重启共享48081。独立8101前端受E盘严重I/O排队影响，Playwright首个页面响应等待180秒仍超时，未执行新角色创建/保存；不能宣称原创建错误已通过业务复测。补跑四类后端测试停在依赖模块资源复制，已停止本任务Maven，仍不算最终测试通过。详见E2E记录后续修复段。
- 18:19—18:31真实页面补验：当前运行服务已使关系入口可用，实际路径为“系统管理 → 角色管理 → 审批角色 → 维护分配”。关系双选、与固定人员组合确认、选择器取消隔离及取消维护后重新读取原配置均通过。创建本任务专用角色被 `IntAuth position sync config is missing` 阻塞，因此没有在现有业务角色上保存，关系持久化及审批执行 E2E 仍未通过。详细边界与截图见 `docs/user-selector-e2e-20261028.md`。
- 重启恢复核对：代码保留。重启前最后一轮 Maven 日志止于 Java 原生内存分配失败，不能记为通过；较早一轮运行解析 8 项、配置包 3 项、控制器 2 项通过，但分配持久化 13 项因 Spring 上下文中的 `CodegenTableMapper` 类加载失败而未执行成功。新增能力声明测试仍需以最终复测为准。恢复后发现其他任务正在同目录 Maven 打包，已暂停本任务 Maven，避免共享 target 并发改写；持续等待后共享构建仍未释放，后端最终复测保持阻塞，已终止本任务等待脚本，没有留下无人监视的自动测试任务。前端 6 个涉及文件 ESLint 通过；全量 relaxed 类型检查先因本次设置的 4GB 堆上限不足而退出，改按仓库标准 8GB 后通过（退出码 0）。恢复期间磁盘 I/O 排队明显。
- `UserSelectV2` 增加显式 `allowRelationships` 配置及 `relationships` 绑定。启用后弹窗提供“人员 / 关系”页签，可同时勾选“申请人的直接主管”“申请人的部门负责人”。两类结果分别通过人员 ID 与关系类型传递；`selection-change` 提供同一次确认的完整结果，取消不会修改配置。
- DCC审批角色/位置的“维护分配”增加“选择人员或关系”入口。可组合固定用户、组织岗位和动态关系；关系保存为 `DIRECT_SUPERVISOR` / `DEPT_LEADER`，不把当前操作人的主管解析为固定账号。现有 assignment_type 为 varchar(32)，无需新增数据库列。
- 直接主管读取系统用户的独立直属主管关系；部门负责人读取申请人所属部门的正式负责人。运行时按每次申请的申请人解析，与既有固定用户/岗位合并并去重，仍接受原审批阶段和账号有效性校验。缺少关系、账号停用或缺少执行上下文明确报错，不替换成另一种主管。
- 原“编制人直接主管”运行解析也改为读取独立直属主管关系，修正此前与部门负责人混用的实现；原“部门负责人”关系保持按部门配置读取。已有流程保存的人员快照不在本次改动中重写。
- 配置接口返回 `supportedAssignmentTypes`。只有运行后端明确支持动态类型，审批位置页面才开放关系配置；旧服务缺少该能力字段时页面显示尚未启用，并阻止保存动态规则。普通只接受用户 ID 的业务表单默认不开启关系页签。
- 配置导入导出保留关系类型；关系规则携带固定 userId/systemPostId 或未知类型时拒绝，校验在替换旧分配前执行。
- 前端行为测试覆盖关系与账号分离、关系回显、组合人数限制、取消隔离及普通账号字段不接受关系。后端增加按不同申请人解析、两种主管身份不同、混合固定用户去重、缺少关系拒绝执行、规则持久化及配置包往返测试。
- 本地48081仍由其他任务的运行包占用，本次没有替换或重启；本段描述代码接入，未宣称新规则已经在当前运行服务生效。

## 多部门人员范围扩展（2026-10-28）

- 部门树支持多选；人员范围提供“全部 / 组员 / 主管”。“全部”沿用归属部门成员查询；“组员”在此基础上排除所选范围内正式配置的主管；“主管”按部门的 `leaderUserId` 查询，可包含归属其他部门的主管。同一人主管多个部门时去重，未配置主管的部门不推断上级主管。
- “包含子部门”决定是否将所选部门的子部门纳入范围。业务固定部门范围、权限角色、岗位、启用状态等条件继续生效。
- 多人模式提供“选择筛选结果全部人员”，覆盖全部分页、合并已有选择、去重并跳过禁选/停用人员。超过人数上限或某页失败时不部分修改已选结果。
- 业务预加载候选仍仅在原候选集合内筛选；部门主管元数据未提供时，“组员/主管”选项禁用并说明原因，不能猜测账号身份。
- 后端 `UserSelectionQueryTest` 13项通过；前端选择器行为测试覆盖共用主管、跨部门主管、无主管、三个部门成员、批量201人跨页、人数上限及失败时不部分更新。全量前端 relaxed 类型检查通过。
- 当前本地48081由另一个租户初始化任务的包占用，运行包尚不含本次扩展；未停止该任务服务。上述代码验证不等于已部署或真实接口E2E通过。

## 2026-10-28 本地部署跟进

- 用户授权部署新增后端接口。完整 `mvn -pl yudao-server -am package -DskipTests` 构建成功（本次执行编译和打包，未重跑全部测试）。
- 构建期间其他启动操作恢复了同一工作区的本地后端。仓库启动脚本检测到 48081 已占用后明确退出，本任务没有再次停止正在运行的服务。
- 最终核验运行进程为 53256，端口 48081，profile 为 local；运行包为 `output/runtime/int_main/edhr-2026-10-28-09-52-00/yudao-server-exec.jar`。
- 运行包内 `UserController`、`UserPageReqVO`、`UserSelectionRespVO`、`AdminUserServiceImpl`、`AdminUserMapper` 五个类逐一按 SHA-256 与本次构建比较，全部一致，包含新增 selection-page / selection-list 接口。
- Playwright 通过外来文件评审真实页面选择人员并重新打开选择器；页面触发 selection-list 返回 HTTP 200、code=0、1 条用户数据，预选保持。未提交评审业务。管理员使用原管理分页，因此 selection-page 的普通业务账号调用仍未单独做真实页面验收。
- 此跟进解除下文历史记录中的“新增接口尚未部署到本地共享后端”限制；不代表远程三服发布或所有业务全链路 E2E 通过。

## 结论与边界

上述主动选人入口已接入 `UserSelectV2` 或其统一弹窗包装。原先已使用 V2 的入口继承本次核心能力。主线程审查并修复了属性透传覆盖弹窗显示、64 位 ID 精度、固定范围、失效历史候选、懒加载失败、表单禁用、首次分页及历史人员查询等问题。

这是代码接入与针对性验证结论，不是所有业务流程端到端通过或已发布结论。新增后端查询接口尚未通过本任务重启部署到共享运行服务；没有提交/推送、发布或提交测试业务变更。

## 场景覆盖

以下路径以 `IntRuoyiFronted/src/` 为根；多人指控件支持多选，不替代各业务必填规则。

| 业务操作 | 选择方式 / 接入点 | 候选与提交边界 |
|---|---|---|
| 流程发起、下一节点审批人 | `components/UserSelectForm/index.vue` 包装统一 Dialog；原调用保留 | 节点及选中用户数组继续交原流程接口 |
| 转办、委派、加签、抄送 | `views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue` | 转办/委派单 ID，加签/抄送数组；原候选列表与服务校验保留 |
| 流程发起人员、管理员、用户组 | `views/bpm/model/form/BasicInfo.vue`、`views/bpm/group/UserGroupForm.vue` | 既有用户列表显式传入；用户组/模型保存结构保留 |
| 节点指定人员、抄送与无人审批指定人 | `components/SimpleProcessDesignerV2/src/nodes-config/`、`components/bpmnProcessDesigner/package/penal/` | USER 模式统一；角色/部门/表达式继续原对象规则 |
| 外来评审参与人、DCC 转办/加签、纸质接收、生效申请审批人 | `views/dcc/controlled-file/external-review/index.vue`、`detail/index.vue` | 原有 V2 调用保留；业务提交权限与流程结构保留 |
| 审批角色成员、路线指定用户、审签/查看矩阵 | `views/dcc/controlled-file/positions/index.vue`、`routes/components/RouteForm.vue`、`categories/components/CategoryMatrixDialog.vue`、`CategoryViewMatrixDialog.vue` | 单行用户与多人多行配置保持；不把角色/岗位当用户 |
| 目录、类别与项目授权 USER 分支 | `components/DirectoryAuthorizationTabPanel.vue`、`categories/components/CategoryPermissionRulesTab.vue`、`basic-data/components/ProjectCodeTabPanel.vue` | 指定用户分支统一，其余主体类型保留 |
| 项目负责人、审核人、指派人 | `basic-data/components/ProductCatalogTabPanel.vue`、`ProjectAttributeConfigurationDialog.vue`、`ProjectReviewerConfiguration.vue` | 原业务目录与 ID 类型保留；历史自由文本负责人不是账号选择 |
| 文件负责人、会签指派、关联整改负责人、自定义申请矩阵 | `detail/ApprovalFileOwnerPicker.vue`、`workflow/SignoffAssignmentPanel.vue`、`detail/DetailRelationArrangements.vue`、`relations/DccRelationArrangements.vue`、`upload/CustomApplicationMatrixEditor.vue` | 显式业务候选；字符串 Long ID 不转 Number；保留原加载/禁用与业务校验 |
| 注册证到期提醒 | `views/dcc/registration-certificate/config/ReminderConfigDialog.vue` | 原 V2，多阈值接收人数组与限定列表保留 |
| 整表/辅助行填写人及辅助填写人添加 | `views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`、`views/form-center/template/components/FormTemplateFillConfigDialog.vue` | 个人模式统一，角色模式保留；候选加载失败显式报错；原责任 JSON 保存结构保留 |
| 工艺路线填写人、附件负责人、生产组长、放行/归档责任人 | `views/mes/pro/route/RouteFlowGraphDesigner.vue`、`RouteTaskRulesDialog.vue` | 正式用户列表作为候选，历史姓名仅回显；懒加载失败清空候选并禁确认 |
| 流程干预、上市放行交接、批次作废审批人 | `views/mes/pro/edhr-flow-intervention/FlowInterventionPage.vue`、`views/mes/pro/production-release/components/MarketReleaseHandover.vue`、`views/mes/pro/edhr-batch/BatchExecutionListPage.vue` | 干预/交接单人，作废按节点多人；交接仅使用服务返回的合格人员 |
| 报工、流转卡、四类检验、包装、点检保养、盘点、安灯、人员条码 | 相应 MES 原 V2 页面 | 原调用与提交结构保留；条码 change 回调继续取得人员对象 |
| 班组成员、车间/仓库负责人 | `views/mes/cal/team/CalTeamMemberList.vue`、`views/mes/md/workstation/workshop/WorkshopForm.vue`、`views/mes/wm/warehouse/WarehouseForm.vue` | 保留原岗位筛选、成员与负责人字段 |
| 部门主管、直属主管、主管交接、临时角色授权、公司范围授权、站内信 | `views/system/dept/DeptForm.vue`、`views/system/user/UserDirectSupervisorDialog.vue`、`SupervisorHandoverDialog.vue` 等 | 保留直属主管版本校验及 Long 字符串；临时授权/公司范围等提交原接口 |
| 按人员查询历史记录与矩阵反查 | 13 文件、14 处查询控件显式 `allow-inactive` | 只读查询可以选停用历史用户；主动业务选人默认禁止停用人员 |

## 统一控件契约

- 入口：`views/system/user/components/UserSelectV2.vue`；弹窗：`UserSelectDialogV2.vue`；选项类型/规则：`userPicker.ts`。
- `modelValue` 为 `string | number` 或数组，候选 ID 原类型保留。数字 ID 必须安全，64 位 ID 使用整数字符串。
- 单选/多选、`min`/`max`、`disabledIds`、`excludeCurrentUser`、`clearable`；继承 ElForm 禁用状态。
- 通用目录支持姓名/账号、部门与子部门、权限角色、岗位、状态。管理查询权限控制手机与创建时间字段；无该权限使用精简身份接口。
- `deptId`、`scopeRoleId`、`scopePostId` 为固定范围，搜索重置不能移除；服务查询按交集处理，空交集不能全量返回。
- `userOptions`（包括空数组）或 `loadUserOptions` 明确进入业务候选模式，失败不能切换全量目录。`candidateLoading`/`candidateError` 阻止确认；`selectedUsers` 仅提供历史回显。
- 业务候选可提供 `departmentOptions`/`roleOptions`/`postOptions`。未提供组织树时，只从候选自身的部门 ID/名称产生直系部门筛选，不推断层级；没有角色/岗位身份数据时不伪造对应筛选结果。
- 已选项独立于分页，移除后不会被初始值复活；超过 200 人的精简回显分批请求。重新打开与候选更新检查失效人员。
- 后端新增 `/system/user/selection-page`、`/system/user/selection-list`，精简返回与原 simple-list 等效身份字段及状态；不放宽用户管理分页权限，不替代业务提交授权。

## 主线程审查发现及修复

1. 输入层 `modelValue` 被透传到弹窗根组件，覆盖弹窗显示状态：改为属性白名单，弹窗关闭隐式属性继承，增加真实 Vue 渲染点击测试。
2. 单纯 Number 转换会损坏长 ID：统一支持字符串 ID，保留提交类型。
3. 移除标准列表造成配置回归：恢复 UnifiedListTemplate、七列及宽度持久化；创建时间保留日期格式化。
4. 旧已选数据可绕过候选资格更新：确认核对最新候选与固定范围；历史人员只回显。
5. 路线懒加载失败仍保留旧候选：失败清空并显式阻止确认，重开可重试。
6. 原 el-select 继承表单禁用但新控件未继承：使用 Element Plus 表单禁用上下文并拦截打开、清空、延迟确认。
7. 200 人回显上限、不可清空和人数配置：分批回显，落实不可清空与 min/max 合法性检查。
8. 首次显示 20 条/页但仅加载 10 条：初始页大小改为标准模板的 20。
9. 全局禁选停用用户影响历史查询：仅查询入口显式允许停用用户，主动选人默认不允许。

## 验证

- 最终核心修复后的全量前端 relaxed 类型检查通过，进程退出码 0；日志为 `.tmp/user-picker-tscheck-final.log`。
- System Maven reactor 编译和 `UserSelectionQueryTest` 4 项通过：角色/岗位交集、固定部门不可越界、清空筛选保留范围、固定角色/岗位交集。
- `node scripts/user-picker-behavior.test.cjs` 通过：Long ID、范围、跨页、人数、失效候选、加载失败、异步竞态、405 人回显、表单禁用及真实渲染打开弹窗等。
- `node tests/e2e/user-select-standard-list-template-static.spec.js` 通过。
- DCC 七组 + MES 交接/路线两组集中回归 79 项：78 通过，1 条旧产品审核入口断言失败。该断言要求旧 review 按钮，当前并行改动已经迁往审批中心，本次未恢复旧业务入口或放宽断言。
- 表单中心四项既有静态契约通过；MES 整表规则另有既有第 178 行失败，子 Agent 使用 HEAD 原组件复现相同失败。
- 菜单迁移检查通过；本次仅补内部复用 `system:user:query` 的免迁移说明。
- Playwright 真实页面：直属主管单选→确认回显→重新打开保留预选，通过；外来文件评审跨页选择两人→确认多人回显，通过。两条均取消/离开业务表单，未保存关系或提交评审。
- 真实页面期间共享列表组件曾因其他改动的 slot lint 返回 500，后续工作区修复后恢复；本任务未改该共享组件。
- 未执行所有业务保存操作的全链路 E2E；新增精简后端接口与新增固定范围参数尚未在共享运行服务重新部署验证。

## 明确保留与已知边界

- 减签实际选择子任务 ID，同一人可能有多个任务，保留任务选择器并展示任务身份，不将其合并成人员列表。
- 角色、部门、岗位、表达式以及历史自由文本不是具体账号，不改为人员选择器。
- 生产组长工作台另有正式工/PQC两处专用远程搜索，本次未纳入先前枚举操作的迁移；CRM人员下拉、手工输入人员编号亦未扩展处理。
- 演练预检弹窗没有打开入口且候选从未赋值，保留原死入口，不宣称可用选人场景。
- DCC 审批角色分配原保存服务仅检查审批岗位后写入用户 ID，未校验账号有效性；统一控件不构成该服务端校验缺口已修复的证明。审批路线 USER 保存则保留原 `validateUserList` 校验。

结论：已确认场景的统一接入与核心行为可审查，代表性真实选人交互通过；全业务 E2E、共享运行服务更新及上述既有测试/服务边界需分别处理，不能标记为全部运行态验收通过。
