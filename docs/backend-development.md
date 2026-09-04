# IntRuoyi Backend Development Rules

## 触发场景

- 修改 `IntRuoyiBackend` 下的 Java、Spring Boot、Maven、接口、服务、Mapper、DO、配置、脚本或后端测试前，必须先读取本文件。
- 涉及 SQL、schema、菜单、权限、租户绑定或数据修复时，还必须读取 `docs/database-rules.md`。
- 涉及本机服务启动、端口或运行态验证时，还必须读取 `docs/local-runtime.md`。

## 项目边界

- 后端根目录：`E:\IntRuoyi\IntRuoyiBackend`。
- 使用 Java 17、Spring Boot、Maven 多模块结构。
- 主应用模块：`yudao-server`。
- 业务逻辑必须保留在所属模块内；跨模块移动或耦合必须有明确的设计理由和验证。
- 不得根据前端页面或历史实现猜测后端 schema、权限、接口或租户行为。

## 实施规则

- 先确认变更所属模块、现有 Controller/Service/Mapper/DO 边界和已有测试。
- 对功能、修复、重构和行为变更，先记录 BDD，再执行 RED -> GREEN -> REGRESSION。
- 缺少数据库、Redis、依赖、测试数据或运行配置时，必须 fail fast；不得切换数据源、返回 mock 成功或吞掉错误。
- 接口和服务错误必须通过真实响应、日志或测试暴露；不得用默认成功值掩盖失败。

## 关联从一对一扩展为一对多门禁

- Trigger: 唯一索引、绑定表或服务语义从“一个主对象一条关联”变为“一主多关联”，例如活跃订单绑定多张领料单。
- Preflight check: 先全仓搜索原有 `selectBy<主对象Id>`、单条清理、单条快照和单条证据调用点；能由来源单据 ID 精确定位的必须改为复合条件查询，需保留审计事实的必须遍历全部关联，只有明确存在主关联业务定义时才可选择单条。若需求规定在完工、审批或发布等业务动作发生时发现来源，禁止提前到对象创建阶段要求用户选择代表来源；动作事务应按正式业务键重新发现、全量预校验、验证已有正确子集并补齐缺失关联。若逐张复制外部来源表头/明细，必须先核对表头和明细的来源唯一键；复制身份须同时包含稳定的来源单据 ID/分录 ID，原始来源身份须保留在绑定或可审计 payload 中。物化记录 ID 与其多项来源证据 ID 集合必须分字段表达，禁止取来源集合第一项冒充物化记录。
- Blocker: 用排序后“第一条”替代原唯一关系、清理只删一条子关联、凭证/来源哈希遗漏其余关联、历史对象已有正确部分关联却因集合不相等无法补齐、复制多张来源时复用相同 `sourceFid`/`sourceLineKey` 触发唯一键冲突、把多项来源 ID 强制为单项或当作物化记录 ID，或并发“先查后插”唯一冲突直接向用户报错时必须停止。
- Verification: 回归覆盖第二条关联创建、已有正确子集补齐、已有多余/变化来源拒绝、同一关联并发创建的幂等回读、全量清理、复制表头和明细来源身份唯一且可反查正式来源，以及下游来源哈希、回执、批记录、放行和追溯包含全部关联；对按来源单据消费的流程，断言使用 `(主对象ID, 来源单据ID)` 精确查询，并分别断言物化 ID 与来源证据 ID 集合。
- Material-batch evidence extension: 当一对多来源用于前端展示物料批号、领料单号或追溯来源时，服务端证据必须同时返回实际命中的来源单据业务编号和稳定 ID；前端展示使用业务编号，禁止直接展示 Java `Long` 来源 ID，避免 JavaScript 精度截断。每条物料证据只能挂载该物料实际匹配到的来源单据，不能把同一生产订单下全部来源单据号复制到每个物料上。

## 验证方式

- 优先运行受影响模块的定向 Maven 测试，例如：
  - `mvn -pl yudao-module-mes -am test`
  - `mvn -pl yudao-server -am test`
- 如果指定测试类，记录 `-Dtest` 范围和 `surefire.failIfNoSpecifiedTests` 处理依据。
- 涉及 API 行为时，验证成功路径和失败路径。
- 涉及前端调用时，最后通过真实前端路径或已批准的 E2E 核对接口结果。

## 业务冻结不得被高权限操作绕过

- Trigger: 批次、工单、审批对象存在不合格冻结、合规冻结或其它明确禁止继续业务操作的权威状态，同时页面或服务存在 admin、金手指、超级管理员等通用操作绕过。
- Preflight check: 先区分“普通流程锁”与“合规冻结锁”。高权限只能绕过需求明确允许的普通流程锁；不合格冻结等无例外权威状态必须在绕过条件之外独立判断，前后端保持同一优先级。
- Blocker: 权威冻结状态正确但高权限页面不显示冻结提示、仍允许触发写操作，或后端仅靠通用高权限判断放行时必须停止。
- Diagnostic detail: 同一写入口可能被多个冻结分支拒绝时，服务端错误必须输出具体分支名和稳定业务身份；不合格评审至少带 `reviewId/sourceType/sourceId/workOrderId`，工单临时冻结至少带 `workOrderId`，禁止只返回笼统“禁止报工”。
- Verification: 静态合同证明冻结条件位于高权限绕过之外；真实 E2E 使用高权限账号创建冻结后，刷新页面仍显示冻结和禁止操作提示，后端目标写入口继续拒绝。
- Forbidden action: 禁止因为 E2E 使用 admin 就移除冻结提示断言，禁止把后端拦截当作前端可继续展示可操作状态的理由。
- Evidence: `doc/tasks/nonconformance-review-mvp-implementation/verification-report.md`。

## 系统用户角色分配高权限拦截门禁

- Trigger: `system_users` 分配角色、普通用户被赋予管理员角色或日志操作权限、`PermissionService.assignUserRole`、`RoleCodeEnum`、`USER_ASSIGN_HIGH_PERMISSION_FORBIDDEN`。
- Preflight check: 写入前先区分“当前已具备高权限”和“目标角色会引入高权限”两种状态；系统判定必须读取正式角色 code 和菜单 permission，禁止只靠前端禁用或写后回滚。管理员/日志权限的判定必须同时覆盖角色 code 与菜单 permission 两条来源，且普通用户在写入前就应被阻断。
- Blocker: 普通用户成功拿到 admin/audit/log 类角色或菜单权限、仅在保存后清理、或角色/菜单判定依赖名称模糊匹配时必须停止。
- Verification: 后端服务测试必须覆盖给普通用户分配管理员角色、给普通用户分配日志权限、已有高权限用户继续增删角色，以及角色/菜单判定的正负例；错误必须是正式服务异常而非静默过滤。
- Forbidden action: 禁止写后修正、禁止把日志菜单名称当权限来源、禁止引入 fallback 让普通用户先保存再清理。
- Evidence: `doc/tasks/20260827-login-security-controls/verification-report.md`

## MES 一线设备账号权限门禁

### 权限角色授权必须走登录用户标准权限解析

- Trigger: 一线生产填写、设备账号切换工序、压力泵全工序授权、`post workstation binding loginUserId=... postIds=...`、`hasAnyPermissionsInRoles`、权限角色已配置但仍落到岗位/工作站绑定。
- Preflight check: 修改设备账号、岗位、工作站或特殊全工序授权前，先区分“系统标准权限”与“岗位/工作站绑定”两条链路；凡需求口径是“拥有权限角色/权限即可授权”，后端判定必须从 `loginUserId` 调用标准 `PermissionApi.hasAnyPermissions(userId, permission)`，不得先取角色 ID 再做显式角色权限判断；压力泵全工序权限命中后仍必须限定到压力泵路线，非压力泵 `routeStartProductionLeaders` 路线继续按正式 USER/ROLE 快照授权。
- Blocker: 拥有目标权限的登录用户仍进入岗位/工作站绑定链路、超级管理员或动态授权语义被绕过、无权限普通用户被扩大到全工序、或错误信息只能看到岗位 ID 无法说明权限链路是否命中时，必须停止并补齐回归测试。
- Verification: 后端回归必须同时覆盖“有权限用户获得压力泵全部工序”“压力泵权限不扩大到非压力泵 route-start 路线”“无权限用户仍按岗位/工作站绑定”“旧显式角色检查会复现岗位绑定缺失错误”，并复跑一线员工切换和工作站岗位绑定相邻测试。
- Forbidden action: 禁止硬编码账号 ID、岗位 ID、角色 ID；禁止把岗位/工作站绑定失败当作权限角色授权的 fallback；禁止用前端放行、空列表成功或默认路线掩盖权限链路未命中。
- Evidence: `doc/tasks/20260803-pressure-pump-role-process-switch/verification-report.md`，运行时错误 `设备账号上下文不完整或不一致：post workstation binding loginUserId=1, postIds=[14]`；`doc/tasks/20260819-frontline-device-account-route-auth/verification-report.md`，运行时错误 `设备账号未授权当前工艺路线或工序，routeId=922119, processId=922985`。

### 生产组长工序配置必须按正式负责路线限定

- Real E2E route check: 删除负责路线后的真实页面验证必须从当前可见的生产组长路由进入，按 routeId 断言已删除路线不再由 responsible-routes 返回，并以该接口当前返回集合驱动页面路线标签和 process-config 路线集合断言；禁止继续把已删除路线名称硬编码为期望值。Evidence: doc/tasks/20260811-team-leader-stale-route-context/verification-report.md。

- Trigger: 生产组长工作台、工序配置、损耗原因、设备映射、设备参数标准、`process-config/list`、`routeStartProductionLeaders`、`mes:pro-process-pool-team-leader:maintain`、admin 工序配置看到其它工艺路线工序。
- Preflight check: 修改生产组长配置页候选工序、损耗/设备/参数维护授权前，先区分“维护入口权限”和“正式负责路线范围”：`mes:pro-process-pool-team-leader:maintain` 只能说明用户可进入维护入口，不能扩大路线工序范围；后端候选列表和直接维护断言必须只读取当前 active 路线版本 `routeStartProductionLeaders` 中命中的 `USER/USERS/ROLE` 配置。验证职责范围必须读取各路线 active `routeStartProductionLeaders`，并同时计算直接用户配置与当前账号角色命中的 `ROLE` 配置。若通过 SQL 修复 active 路线快照，写入前还必须核对目标路线是否存在 DRAFT/candidate version；已有草稿缺少同一配置时，后续发布会覆盖本次 active 修复，必须阻塞并改走正式草稿保存/发布或取得明确的数据修复范围授权。写入后必须重新只读解析当前 active version；路线发布可能把原目标 version 置为 `SUPERSEDED` 并生成新的 active version，最终验证不得继续把旧 draft/version ID 当作非目标失败条件。
- Blocker: `process-config/list` 返回未在正式负责路线内的路线工序、拥有维护权限的 admin 可直接维护非负责路线工序、无负责路线时返回全部 active 路线、或用维护权限/admin 身份替代 `routeStartProductionLeaders` 命中结果时必须停止并补后端 RED/GREEN。
- Verification: 后端回归必须覆盖“拥有维护权限也只能列出正式负责路线工序”“拥有维护权限但不在工序开始快照中直接维护 routeProcess 会被拒绝”“无维护权限仍走 USER/ROLE 快照授权”，并复跑工序配置相邻服务测试和前端新增入口静态合同；真实登录态验证工序配置时必须调用生产组长工序配置数据源 `/mes/pro/process-pool/team-leader/process-config/list`，并断言其路线名称集合等于 `/mes/pro/process-pool/team-leader/responsible-routes` 返回的正式负责路线集合。验证“账号实际配置了哪些路线的生产组长”时必须逐路读取 `/mes/pro/route/flow-config/route-start-production-leaders` 或当前 active JSON 快照，不能用维护入口列表代替。数据修复复验必须以 `tenant_id + route_id + active=1 + lifecycle_status=ACTIVE` 当前命中行为准，同时记录原写入 version 与当前 active version 的差异，并确认目标路线没有会在下一次发布时丢失配置的旧草稿。
- RouteProcess identity check: 路线重新发布后排查一线“不良/设备参数为空”时，必须同时列出 QA 发布规程绑定的 `routeVersionId + routeProcessId`、当前唯一 ACTIVE 路线版本的 `routeProcessId`，以及损耗原因和设备参数规则实际绑定的 `routeProcessId`。不得按相同 `processId`、工序名称或历史页面仍有数据显示来推断配置已继承；若配置仍落在 `deleted=1` 的旧路线工序而当前 active 路线工序计数为 0，应明确判定为当前正式配置缺失，并通过正式配置维护/迁移方案处理，不能让运行态回读旧 ID 作为 fallback。恢复旧版本配置前必须冻结目标当前配置的业务键和内容 hash；若目标已有经授权保留的当前配置且业务编码不同，按用户确认的去重边界保留目标配置并只迁移缺失工序，禁止覆盖成旧编码或把旧新两套参数同时插入。
- Active order parameter snapshot check: 已存在活跃订单的一线 runtime-config 会优先读取 `mes_pro_process_pool_active_order_process_snapshot.parameter_snapshot_json`，不会自动回读最新生产组长设备参数配置；排查“并行设备可选但参数为空”时，必须同时比对当前正式规则和该 `active_order_id` 的冻结快照。若当前正式配置已正确而快照缺项，数据修复必须限定精确活跃订单、备份原快照、按 `route_process_id + process_id + device_id + parameter_code` 重算 JSON 与 sha256，并确认无参数工序为 `[]`、无空设备占位、无孤儿快照设备。Evidence: `doc/tasks/20260902-active-order-396-parameter-snapshot-backfill/verification-report.md`。
- Active order device-selection snapshot extension: 一线设备支持单选、多选或不选时，设备组身份与 `SINGLE/MULTIPLE` 选择模式必须由正式 JSON/配置显式写入工序设备绑定，并与设备 ID 集合一起冻结到活跃订单工序快照及 SHA-256；运行配置不得按设备名称、参数重复或历史绑定顺序推断设备组。提交必须使用 `selectedDevices[]`，`SINGLE` 同组最多一台，未选设备不得携带参数读数；缺少设备选择快照或哈希不一致必须 fail-fast。同步或复制活跃订单后，必须逐活跃订单核验每条 `FROZEN` 工序快照同时具备 `device_selection_snapshot_json` 与匹配 SHA-256，尤其不能只核验旧 sim-copy。若设备多选改造后出现“当前工序未配置报工物料”等相邻错误，必须分层核对运行配置冻结快照、`batchUseConfigs.outputMaterialIds`、前端 `materialDetails` 非空子集载荷、后端提交校验和当前运行 Jar/前端热更新状态，不得把设备选择链路和报工物料链路互相补齐或推断。历史绑定不能自动生成伪设备组，必须先完成正式配置导入。Evidence: `doc/tasks/20260903-idi-json-frontline-device-integration/verification-report.md`。
- Active order device-selection snapshot repair extension: 新增设备选择快照严格校验后，真实一线 E2E 前必须按 `active_status=ACTIVE` 盘点全部活跃订单的 FROZEN 工序快照；缺失快照的旧订单要先备份，并仅从同一订单组长的正式启用设备绑定或已批准 JSON 映射重建，无法唯一确定时保持 fail-fast。不得把“当前订单被临时冻结”误判为设备快照修复失败；设备快照修复通过后仍要单独核对工单冻结门禁，再选择可正式报工的真实测试订单完成提交。Evidence: `doc/tasks/20260904-frontline-device-selection-snapshot-repair/verification-report.md`。
- Candidate snapshot stale identity repair: 候选工艺路线页面同时出现“没有操作权限”和 `PRO_ROUTE_PROCESS_IDENTITY_NOT_FOUND` 时，必须把权限链路与版本快照链路分开诊断；补菜单权限或重启服务不能修复 JSON 快照中的失效 `routeProcessId`。先逐项解析目标版本 `routeSnapshotJson.configSnapshots` 的 `flowGraph`、`scheduleConfigs`、`batchUseConfigs`、`scheduleUseConfigs`，再与当前正式路线工序按工序主数据、排序和重复工序序位证明唯一映射；任一身份不唯一必须阻塞。获得数据修复授权后，先备份完整版本行并校验压缩包和 hash，再用结构化 JSON 操作做 dry-run，证明旧引用归零、新身份全部命中、数组/对象长度不变以及产品、BOM、批记录等非目标配置 hash 不变；正式更新必须锁定目标版本状态和原快照 hash、限定精确一行、异常事务回滚，并复核没有临时过程残留。页面验收仍需使用目标账号真实登录路径，数据库/API 结果不得冒充权限提示已消失。Evidence: `doc/tasks/20260814-test-zhaojie-route-permission/verification-report.md`。
- Publish inheritance source boundary: QA 规程不是生产组长损耗原因或设备参数标准的数据源；clientRouteProcessId 只用于流程图投影引用，不得作为生产组长配置查询或继承来源；发布继承必须按冻结快照中的正式 routeProcessId 精确映射，不得按 processId、工序名称、sort 或运行态 fallback 回读旧 routeProcessId。页面、API、任务文档必须分别标注 QA 规程身份和生产组长配置身份。
- Deleted route scope boundary: 生产组长负责路线的实时计算必须先限定父路线仍未删除的 ACTIVE route version，再解析 routeStartProductionLeaders 快照；删除工艺路线后若残留孤儿 ACTIVE version，不得继续解析该快照并报 responsibleRoutes missingRouteIds。过滤后的 ACTIVE version 若最终仍加载不到路线摘要，必须 fail fast 暴露数据竞态或坏数据，不得返回默认成功。
- Forbidden action: 禁止用维护权限、前端新增弹窗默认候选、空列表成功、admin 硬编码、直接放宽所有账号、菜单文案或 API-only 说明替代正式后端授权；禁止把 admin 因入口权限能打开页面解释成其工序配置职责覆盖全部路线；禁止只改当前 active 而忽略已经存在且缺配置的待发布草稿；禁止把 `formBindings`、批记录表单或其它路线配置链路当作工序开始生产组长来源。
- Evidence: `doc/tasks/20260806-process-config-refresh-to-add-button/verification-report.md`，用户以 `芋道源码 / admin` 点击新增仍报“当前账号没有可新增的路线工序”；`doc/tasks/20260806-admin-pressure-pump-route-start-leader/verification-report.md`，路线发布后 route `922119` 的 active version 从原写入 `448` 变为 `490`，最终按当前 active `490` 与 `622` 复验通过；`doc/tasks/20260807-admin-pressure-pump-only-route-start-leader/verification-report.md`，admin 维护权限与正式职责范围分开验证，并对两条目标及四条非目标 active 路线逐路读取配置；`doc/tasks/20260807-team-leader-process-config-responsible-routes/verification-report.md`，admin 工序配置列表最终只返回两条正式负责路线下的 28 个工序；`doc/tasks/20260810-restore-team-leader-process-config-v29/verification-report.md`，恢复球囊扩张压力泵 V29 配置时保留粗洗当前 5 条参数，只补齐其余旧版本配置，最终一线页面复验通过；`doc/tasks/20260811-route-publish-chain-clarity/verification-report.md`，发布投影明确 QA 规程、`clientRouteProcessId`、`processId/sort` 与正式 `routeProcessId` 的继承边界，并通过 `芋道源码 / admin` 只读 E2E 证明生产组长配置与一线运行态读取同一当前 ACTIVE routeProcessId。

### 一线运行态 route-start 生产组长来源必须独立于班组设备绑定

- Trigger: 一线生产 runtime-config、`frontline runtime deviceId=...`、`routeStartProductionLeaders`、工作站正式设备、班组设备映射、`MesFrontlineRouteProcessCandidate.contextSource`、班组长工作台缺少负责范围上下文。
- Preflight check: 修改一线运行态候选或员工/设备配置读取前，先区分候选来源：`ROUTE_START_PRODUCTION_LEADER` 候选的负责范围来自 active 路线 `routeStartProductionLeaders` 与当前负责组长；`POST_BINDING` 设备账号候选才按设备账号岗位/工作站绑定和班组设备映射解析。工作站正式设备 ID 只说明 route-start 候选来自该工位设备，不等于班组维护设备已经映射；正式提交授权只校验工序身份、人员和模板，请求仍携带设备/工作站上下文用于提交追踪，但不得把 submittedDeviceId/submittedWorkstationId 与 route-start 或 post-binding 候选的 expectedDeviceId/expectedWorkstationId 互相比对来阻断提交，提交阶段不执行设备参数校验。
- Blocker: route-start 生产组长候选带 `deviceId` 但缺班组设备映射时直接报“班组长工作台缺少负责范围上下文”、或为消除错误把设备账号候选也改成当前登录人/空成功时必须停止。
- Verification: 后端回归必须覆盖“route-start 候选带正式设备 ID 且无班组设备映射时仍返回当前负责组长人员上下文”“设备账号 post-binding 候选仍需按设备绑定解析”“生产员工继承唯一负责组长工序不回退设备账号来源”“一线生产正式提交授权在工序合法但提交设备/工作站与授权候选设备/工作站不一致时放行”和“正式提交服务不调用设备参数校验器”。
- Forbidden action: 禁止把工作站正式设备 ID 当作班组设备维护绑定；禁止用设备账号岗位/工作站绑定、当前登录人 fallback、空设备成功或前端隐藏错误替代正式 route-start 生产组长负责范围。
- Evidence: `doc/tasks/20260807-team-leader-workbench-frontline-device-context/verification-report.md`。

### 候选流程图正式工作站与展示工作站必须分字段

- Trigger: 工艺路线候选保存/读取/发布、`routeProcessWorkstationId`、流程图节点 `workstationId`、`mes_pro_route_process.workstation_id`、一线生产提示“工艺路线工序缺少正式工作站绑定”。
- Preflight check: 修改候选流程图、版本投影或流程配置解析前，必须确认节点字段职责：`routeProcessWorkstationId` 是路线工序正式绑定，`workstationId` 仅用于可用工作站展示；候选保存、候选读取、流程配置解析和发布投影必须逐段核对正式字段是否原样传递。
- Blocker: 正式字段缺失、展示字段被写入或读取为正式字段、发布后当前路线工序工作站为空、或正式工作站不存在/禁用/与工序不一致时必须停止；不得继续发布或让一线生产静默过滤该工序。
- Verification: 后端回归必须让两个字段取不同值，并分别覆盖候选保存、候选读取、流程配置解析和发布投影；发布后只读核验当前 ACTIVE 版本、全部路线工序非空绑定，以及工作站存在、启用且 `workstation.process_id == route_process.process_id`；最终通过 `/mes/pro/feedback/frontline/device-account/processes` 和真实“一线生产”点击验证。
- Forbidden action: 禁止用展示 `workstationId`、默认工作站、相邻工序工作站、`formBindings`、批记录表单、工序开始配置、前端隐藏错误或 API-only 成功补齐正式绑定。
- Evidence: `doc/tasks/20260807-frontline-route-process-workstation-binding-fix/verification-report.md`。

## eDHR 详情回填门禁

### 路线配置有值但详情接口为空

- Trigger: eDHR、批次详情、动态表单、损耗单、工艺路线绑定、填写人、`fillableUsers`、`routeBindingId`、配置页有值但详情接口为空。
- Preflight check: 先同时核对配置接口/表中的来源字段、执行任务快照字段、详情接口组装链路和既有优先级，不得只改前端显示文案。
- Blocker: 若详情任务没有可追溯的绑定 ID、快照字段或正式规则来源，必须阻塞并补齐后端数据链路；不得从当前登录人、创建人、更新人或角色 ID 推断填写人。
- Verification: 新增后端回归测试覆盖“仅路线绑定配置填写人”场景，并同时跑相邻优先级测试，确认有效工作任务和工序规则仍优先。
- Forbidden action: 禁止前端把 `未配置` 改成配置页名称、禁止把角色/部门 ID 当用户 ID、禁止用空列表兜底掩盖缺失来源。
- Evidence: 任务 `doc/tasks/20260724-edhr-route-form-filler-backfill/`，目标测试 `MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated`。

### 批记录表单角色填写人名称回显边界

- Trigger: eDHR、批记录表单、填写人配置、小弹窗默认填写人、`candidateSourceType=ROLE`、`candidateSourceNames` 为空、候选用户已展开但角色名不显示。
- Preflight check: 修改填写规则响应前，同时核对 form-level `FILL` 规则和 cell-level 填写分配响应；角色来源必须既展开启用候选用户，也展开角色来源名称，不得只验证 `candidateUsers`。
- Blocker: `get-by-report` 对 form-level ROLE 只返回成员用户、不返回角色名，或 API 只能靠前端从用户列表反推角色名时必须停止并补齐后端响应。
- Verification: 后端回归覆盖 form-level ROLE 的 `candidateSourceNames` 与 `candidateUsers`，并用本机登录态 API 核对目标批记录表单返回业务码 `0`、角色名和候选用户数。
- Forbidden action: 禁止用前端硬编码角色名、当前登录人、创建人、角色 ID 文案拼接或空列表兜底掩盖后端响应字段缺失。
- Evidence: 任务 `doc/tasks/20260728-pressure-pump-batch-record-role-fillers/verification-report.md`，目标测试 `MesProEdhrProcessFormPermissionRuleServiceImplTest#getRuleByReport_returnsRoleSourceNamesForFormLevelFillRule`。

### 切换填写人快照读取边界

- Trigger: eDHR 批次执行填写页、“切换填写人”、协助填写人、`assistSwitchTasks`、`candidateUserSnapshot`、`getEdhrBatchExecution`、同工序 `MAIN` + 非 `MAIN` 附加表单候选、动态路线表单候选、`formBindingKey`、`formCenterInstanceId`、`activeWorkTaskId`、`workTaskId`、`assistUserId`、损耗单表单槽位、弹窗打开耗时过长。
- Preflight check: 先确认业务口径是否为批次执行创建后填写人固定；若固定，切换填写人候选必须来自执行详情返回的任务/填写人快照，而不是弹窗打开时重新拉取或重算全量批次详情；同工序存在 `MAIN` 批记录表单和非 `MAIN` 附加表单/表单槽位时，`assistSwitchTasks` 必须覆盖所有有效候选，候选来源按 active workTask `candidateUserSnapshot`、过程表单规则、工序规则、路线绑定候选源逐级核对；`available/allowedActions/activeWorkTaskId` 只能来自真实 active workTask，缺少同工序附加表单 workTask 时必须由后端正式 companion workTask 生成或详情回填链路补齐。eDHR 批次任务打开传统批记录时，必须把当前批次任务 ID 写入 `MesProBatchRecordExecutionOpenOrCreateByContextReqVO.taskId`，active 查询、active key 和 `mes_pro_batch_record_execution.task_id` 必须按 `batchExecutionId + taskId + workOrderId + routeProcessId + batchRecordReportId + batchCode` 隔离，并继续通过 `mes_pro_edhr_batch_execution_task.execution_id` 维护批次任务与 execution 的关联，避免新批次复用旧执行详情；若候选是动态路线表单任务（无 `batchRecordReportId` 且有 `formBindingKey/formTemplateId/formTemplateVersionId/formCenterInstanceId`），辅助填写行必须从任务冻结的 `FormTemplateVersionDO.jimuSchemaJson` / `edhrAssistRows` 解析，并按所选填写人的 `responsibilityScopeJson` 过滤，不得读取传统批记录 execution snapshot，且 `openTask` 响应必须携带前端运行态渲染所需的模板快照字段（如 `formTemplateJimuSchemaJson`、`formTemplateRecognizedFields`、模板元数据和实例草稿），不能要求填写人再具备模板管理查询权限。`openTask` 对代开目标附加表单的授权必须同时满足：当前用户在同批次同 `routeProcessId` 有 active FILL/REWORK 锚点任务，且请求的 `assistUserId` 是目标 workTask 的正式分配人或候选人。
- Blocker: 执行详情缺少可追溯任务快照、活动工作任务缺少 `candidateUserSnapshot`、附加表单只有候选展示但无真实 `activeWorkTaskId/workTaskId`、或无法证明候选人来自创建时快照时，必须补齐后端详情和工作任务链路；若 active 执行记录查询没有按批次和传统批记录上下文隔离，也必须阻塞；若动态路线表单任务缺完整 FormCenter 上下文、模板版本不存在、模板 ID 不匹配、`openTask` 成功后仍必须调用 `/form-center/templates/{id}/versions/{versionNo}` 才能渲染，或辅助行解析仍触发 `eDHR 批次缺少唯一批记录路线`，必须阻塞；若 `openTask` 允许无同工序锚点用户、错误 `assistUserId`、当前登录人 fallback 或外部用户打开目标附加表单，也必须阻塞。
- Verification: 运行 `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`，并配合前端 ESLint/`pnpm ts:check` 与 `mvn -pl yudao-module-mes -am "-DskipTests" compile`；涉及动态路线表单候选时，新增或复跑后端回归，断言打开响应保留 `formCenterInstanceId`、所选 `assistUserId`、过滤后的 `assistRows` 和 FormCenter 模板渲染快照，且不调用传统批记录 `openOrCreateByContext`；涉及同工序附加表单候选时，后端回归必须同时覆盖候选快照、companion workTask 创建/回填、`openTask` 同工序锚点授权成功和无锚点拒绝。
- Forbidden action: 禁止在切换填写人弹窗打开时调用全量 `getEdhrBatchExecution` 作为性能问题的替代方案；禁止在 eDHR 批次任务打开传统批记录时省略、置空或改写 `MesProBatchRecordExecutionOpenOrCreateByContextReqVO.taskId` / `mes_pro_batch_record_execution.task_id`，导致不同批次任务复用旧执行详情；禁止让动态路线表单候选回落到批记录 execution snapshot、批记录路线唯一性解析、前端缓存、空列表兜底或吞异常；禁止只展示候选但不生成真实 workTask，或用当前登录人/任务负责人兜底替代“所有有效候选人”。
- Evidence: 任务 `doc/tasks/20260727-switch-filler-snapshot-loading/verification-report.md`；任务 `doc/tasks/20260728-loss-form-switch-route-fix/verification-report.md`；任务 `doc/tasks/20260728-switch-filler-extra-form-candidates/verification-report.md`。


## eDHR 批次任务配置来源门禁

### 当前配置与发布快照边界

- Trigger: eDHR 批次执行、路线发布快照、`routeSnapshotJson`、`batchUseConfigs`、记录本/批记录融合、当前路线配置缺失或陈旧绑定、任务门禁 `available`、开始节点并行第一组、多前置汇合工序。
- Preflight check: 新建/返工批次前先同时检查当前 BATCH 工序配置是否存在、绑定是否归属启用工序配置、发布版本快照是否包含完整 `flowGraph.nodes` 与 `batchUseConfigs`；对 `EDHR_WORD_IMPORT` 候选发布，还必须确认 `configSnapshots.routeStartProductionLeaders` 与 `configSnapshots.batchRecordAttachmentOwners` 是显式数组，不能只看发布 blockers 为空。读取已有批次任务门禁时，还要核对任务 `routeProcessId` 是否被冻结快照 `flowGraph.nodes` 完整覆盖；若批次任务由当前 BATCH 工序配置生成且当前配置完整覆盖任务工序，任务门禁必须按当前路线关系图读取完整直接前置集合。
- Blocker: 只要当前 BATCH 工序配置存在，就必须使用当前配置并严格校验绑定归属；不得因为当前绑定陈旧而静默回退到发布快照。批次任务 `routeProcessId` 既不能被冻结快照完整覆盖，也不能被当前 BATCH 配置完整覆盖，或当前/冻结关系图存在孤立、成环、不可达节点时必须停止；`EDHR_WORD_IMPORT` 候选缺少 `routeStartProductionLeaders` 或 `batchRecordAttachmentOwners` 显式数组时也必须先通过正式草稿配置入口补齐，再发布；不得用单值 `predecessorRouteProcessId`、排序前一工序、默认首个 WAITING 工序或空前置集合掩盖多前置关系。
- Verification: 同时覆盖“当前配置存在优先当前绑定”“当前配置整体缺失时使用已发布快照”“陈旧绑定必须 fail fast”“legacy flat batchRecordReportId 快照可投影”“多起点第一组均 available=true”“多前置汇合工序前置未完成时 available=false”“旧冻结快照但当前配置覆盖任务工序时按当前关系图计算”的后端测试；工艺路线候选发布验证还应只读核对两个工序开始数组存在、发布后旧 active 版本变为 SUPERSEDED、新 active 版本全部工序工作站有效且工序一致。
- Route-owned extension field boundary: `batchUseConfigs.frontlineReportMaterialIds` 是候选路线版本中当前工序的批记录物料配置，旧 BATCH 工序配置表并不持有该字段。发布刷新批记录表单绑定时，必须继续以正式工序设置重建绑定，同时按 `routeProcessId` 从目标候选版本已有 `batchUseConfigs` 精确保留该字段；快照数组、元素类型、工序身份或同工序重复异常时必须 fail fast，禁止用产品 BOM、`formBindings`、默认空数组或其他工序物料补齐。
- Forbidden action: 禁止把发布快照作为通用 fallback；禁止用空绑定、默认 MAIN 或默认成功掩盖当前配置损坏。
- Evidence: `doc/tasks/merge-jiluben-worktree-20260724/verification-report.md`；`doc/tasks/20260729-edhr-parallel-start-process-highlight/verification-report.md`。

### 历史批记录只读页与活动流转门禁边界

- Trigger: 历史批记录页签、`review-timeline`、已归档批次、`BATCH_STATUS_ARCHIVED`、`routeSnapshotJson` 缺 `flowGraph.nodes` 或 `batchUseConfigs`、当前路线 BATCH 配置已删除、只读批记录预览反查当前 Jimu 报表。
- Preflight check: 先区分“终态只读历史展示”和“活动批次流转/切换工序门禁”；历史页签应读取已持久化的批次事件、任务事件、执行快照、签名、审批、附件和归档目录，不应为了展示历史而调用活动任务门禁 `buildTaskGateMap` 或重新解析当前 BATCH 流程配置。
- Blocker: 已归档历史批次因当前/冻结 BATCH 门禁配置缺失导致全部历史信息打不开，或历史执行预览因当前 Jimu 报表/当前报表绑定缺失阻断已保存执行快照展示时，必须修复历史读取边界；不得用清空全部历史、前端隐藏错误或吞异常掩盖。
- Verification: 后端回归必须同时覆盖“缺失 BATCH 门禁配置仍返回已持久化执行快照并标记只读”和“正常历史批记录仍返回任务、签名、审批、归档内容”；静态契约需防止 `review-timeline` 重新直接调用活动门禁。
- Forbidden action: 禁止把终态历史只读页签改成当前配置重算结果；禁止历史执行预览在已有 `executionSnapshotJson` / `sheetLayoutJson` 时再强制依赖当前 Jimu 报表；禁止把活动批次缺配置 fail-fast 放宽到默认成功。
- Evidence: `doc/tasks/20260803-edhr-history-missing-batch-config/verification-report.md`。

### 草稿 BATCH 快照读写对称边界

- Trigger: 路线草稿/候选版本、`routeSnapshotJson`、`batchUseConfigs`、`formBindings`、表单槽位、`flow-config/batch-record/save`、草稿保存后读回为空或仍报“系统异常”。
- Preflight check: 同时核对保存链路写入的候选快照字段、读取策略、版本生命周期状态和当前工序设置；一旦 DRAFT 草稿显式保存过 BATCH 绑定快照，DRAFT 读取必须优先返回该草稿快照，待审批/待发布版本仍按既有规则读取当前工序设置。
- Blocker: 显式保存后的 DRAFT `batchUseConfigs.formBindings` 读回被当前工序设置覆盖、读回为空、或无法区分 legacy 候选快照与本次草稿显式保存快照时，不得宣称草稿保存完成。
- Verification: 新增后端回归测试覆盖“显式保存后的 DRAFT 快照优先于当前绑定”，并同时跑完整相邻测试类，确认 PENDING_APPROVAL / READY_TO_PUBLISH 仍读取当前工序设置。
- Forbidden action: 禁止用当前工序设置作为显式保存草稿快照的 fallback；禁止用空绑定、默认 MAIN、前端隐藏或吞异常掩盖草稿快照读写不对称。
- Evidence: `doc/tasks/20260726-route-flow-v15-save-system-exception/verification-report.md`，`MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings`。

### 历史关闭候选版本只读快照边界

- Trigger: 工艺路线版本工作区“查看”、老版本工艺流程、`routeVersionId`、`CANCELLED`、`REJECTED`、`SUPERSEDED`、`PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE`、历史关系图或流程配置打不开。
- Preflight check: 先区分读取与写入状态集合；只读读取应按版本生命周期从该版本 `routeSnapshotJson.configSnapshots` 读取关系图、批记录/排产/附件负责人等冻结快照，写入仍只允许 `DRAFT` 候选版本。
- Blocker: 只读查看 `CANCELLED` / `REJECTED` / `SUPERSEDED` 被候选发布条件拦截、返回当前 ACTIVE 配置、返回空图/空配置、或写入校验因扩展读取状态而放宽时必须停止。
- Verification: 后端回归必须同时覆盖关闭历史版本读取冻结快照、候选排产快照读取、以及 `CANCELLED` 保存关系图/流程配置/排产配置仍 fail-fast；前端静态或真实路径需证明查看动作传递历史 `routeVersionId` 且禁用写控件。
- Forbidden action: 禁止把关闭候选版本当成待发布候选要求、禁止回退到当前工序设置或 ACTIVE 版本、禁止用空快照默认成功、禁止为了只读查看放宽提交/保存/发布写入守卫。
- Evidence: `doc/tasks/20260727-route-history-cancelled-version-view/verification-report.md`。

### 冻结快照附件负责人 JSON 类型边界

- Trigger: `batchRecordAttachmentOwners`、`PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID`、`批记录附件负责人配置无效`、已有批次冻结 `route_snapshot_json` 缺配置、路线版本发布后旧批次仍打不开。
- Preflight check: 先分别核对当前 ACTIVE 路线版本快照和目标批次冻结快照的 `$.configSnapshots.batchRecordAttachmentOwners`，同时检查 `JSON_TYPE` 必须是 `ARRAY`、`JSON_LENGTH` 必须等于业务要求数量；只看到配置接口返回列表不代表冻结快照可用。
- Blocker: ACTIVE 版本缺配置、批次冻结快照缺配置、JSON 被写成 `STRING` 而不是 `ARRAY`、影响行数不是精确目标行数、或缺少原始快照备份时必须停止，不得放宽打开已有批次的校验。
- Verification: 授权数据修复必须记录原始快照备份、回滚路径、`restoreRows/repairRows`、修复后 `JSON_TYPE=ARRAY` 与 `JSON_LENGTH`，再用真实页面 `打开/创建 -> 确认` 验证不再出现负责人配置错误。
- Forbidden action: 禁止把缺失负责人配置默认成功、禁止把 JSON 数组通过用户变量/字符串写成 JSON 字符串、禁止 API-only 或直接详情 URL 替代确认按钮 E2E。
- Evidence: `doc/tasks/20260727-batch-record-attachment-owner-config/verification-report.md`。

### 批次任务产品信息成员表单部分缺失边界

- Trigger: eDHR 批次执行、批记录表单“产品信息”缺失、已有批次存在 `ROUTE_FORM` 任务但同版产品信息成员表单未生成、`batchRecordDefinitionId + batchRecordVersionId`、`batch_record_sort` 唯一键冲突、产品信息未固定排在 `80`。
- Preflight check: 读取批次详情或修复任务恢复逻辑时，必须比较目标批次任务集合与已有任务集合；不能只用“已有任一 `ROUTE_FORM` 任务”判断批记录任务完整。产品信息成员表单只能从已有正式 `MAIN + BATCH_RECORD` 任务的批记录定义/版本解析，生成和恢复时 `batchRecordSort/reportSort` 必须统一为 `80`，不得从 `formBindings`、工序开始配置、当前登录人或默认槽位推断。
- Blocker: 同版产品信息成员报表存在但活跃批次详情仍不展示、恢复逻辑会重复插入同一 `batchRecordReportId`、产品信息不是 `batch_record_sort=80`、产品信息排在正式批记录表单之前，或产品信息与源表单使用相同 `batch_record_sort` 触发唯一键冲突时，必须停止并补齐后端任务恢复链路。
- Verification: 后端回归必须同时覆盖“新建批次包含产品信息成员表单”“已有工序任务但缺产品信息时详情读取补齐”“完全缺工序任务的历史恢复仍可用”，并断言产品信息固定排序 `80` 且在前序正式批记录未完成前被同工序顺序门禁阻塞。
- Forbidden action: 禁止用前端硬编码展示“产品信息”、禁止把 `formBindings` 当批记录表单来源、禁止按源表单排序 `-1` 推算产品信息位置、禁止只调整页面排序或隐藏错误来掩盖任务未持久化。
- Evidence: `doc/tasks/20260728-batch-execution-product-info-form-missing/verification-report.md`。
## eDHR 批记录版本治理规则运行态门禁

### 已发布版本治理证据与 Jimu 当前 JSON 边界

- Trigger: eDHR 打开填写、`openOrCreateByContext`、`1040750243`、批记录模板未确认填写规则、`CELL_RULE_RECONCILED`、已发布批记录版本、Jimu 报表 JSON。
- Preflight check: 先核对报表 `batchRecordVersionId`、版本 `APPROVED` 状态、migration item 中 `CELL_RULE_RECONCILED` 证据、blocking item 数量，以及当前 Jimu JSON 未确认单元格数量。
- Blocker: 版本未发布、缺少 `CELL_RULE_RECONCILED` 治理证据、存在 `BLOCKER` 或未确认 `CONFIRM_REQUIRED` 时，运行态必须继续 fail-fast，不得把当前 Jimu JSON 自动标记为已确认。
- Verification: 后端测试同时覆盖“已发布且治理通过时物化运行态规则”和“无治理证据的 legacy checkbox 仍阻塞”；真实 E2E 需打开当前填写任务并核验 execution snapshot 无未确认规则字段。
- Forbidden action: 禁止直接 SQL 修改 `jimu_report.json_str`、禁止跳过 `validateConfirmedCellRules`、禁止把 API-only 或历史 execution 直连当作打开填写成功。
- Evidence: `doc/tasks/20260724-batch-execution-published-route-runtime-update/verification-report.md`。

### 批记录单元格链接预填落库边界

- Trigger: eDHR 批记录单元格链接、`PRODUCTION_WORK_ORDER.batchCode`、生产批号目标格为空、`/batch-record-cell-link/prefill`、`cell_values_json=[]`、只读预览缺少已配置链接值。
- Preflight check: 先区分“来源字段不存在”和“链接值未落库”：同时核对来源业务表字段值、启用链接规则、目标 execution 的 `cell_values_json`、创建/打开执行记录写边界和字段审计链，不得只看前端 draft hydrate。
- Batch code source boundary: `PRODUCTION_WORK_ORDER.batchCode` 在批记录执行运行态必须读取创建/打开执行记录时已解析并写入 `mes_pro_batch_record_execution.batch_code` 的正式执行上下文批号；生产工单主表 `batch_code` 只可作为创建执行记录时的输入来源之一，不得在单元格链接落库阶段绕过执行上下文直接作为唯一来源。
- Source ownership boundary: `PROCESS_POOL_REPORT` 等来自生产组长报工确认、订单工序完成或其它专用业务写链路的来源字段，不应由通用 `/batch-record-cell-link/prefill` 自动预填接管；通用预填应跳过该来源，由对应专用服务负责读取正式业务事件、分配记录、字段映射和字段审计写入。
- Lifecycle boundary: 批记录单元格或重复行链接配置、正式一线生产事实形成、放行资料生成是三个独立阶段。配置保存只定义来源字段到目标单元格或重复记录的关系，不得创建批记录、写目标值或预占重复行；一线生产提交只形成后续可追溯的正式来源事实；若业务合同规定在生产组长“申请放行”时统一生成资料，则只能由申请放行专用事务按冻结路线/表单版本和正式提交顺序执行映射、人员取值、操作时间取值、目标结构校验与原子写入，禁止在配置保存或一线提交阶段提前物化。
- Process-pool field catalog boundary: 批记录单元格链接页选择“报工数据”时，字段目录必须来自当前路线版本/当前工序的正式一线生产运行配置和报工事件结构；当前没有单独选择生产组长时，选用设备身份字段必须按当前登录生产组长的工序设备绑定读取，参数字段再按同一路线工序、同一生产组长、同一设备的参数规则读取，禁止跨生产组长或跨路线工序混入同名工序设备。基础数量字段可以包含正式派生数量，`totalQuantity / 本次报工总量` 必须由后端按同一笔报工事件的 `outputQuantity + lossQuantity` 计算并参与数量汇总规则，不得由前端合成、跨事件相加或把缺失值当 0。除基础数量外，目录必须覆盖选用设备、设备参数实际提交值、计量有效期、清场/物料/清洁确认、提交签名用户、审核时间和审核人签名用户等已确认可链接字段；单位、上下限、状态、参考标准、默认文本和默认值属于配置/校验信息，不得进入左侧可链接字段目录。设备参数实际值必须使用正式事件路径 `deviceParameterReadings.<parameterCode>.value`，字段编码按设备名称/类型分组保留为 `deviceParameterReadings.<parameterCode>.value@deviceGroup:<deviceGroup>`；同类多台设备只展示一套同名参数，不显示 `清洗次数（B09393）` / `清洗次数（B09392）` 这类物理设备重复项，不同设备类型的参数必须分别展示。下拉或文本参数必须从实际提交值读取并保留 `textValue`，不得退回只读扁平数量字段、配置字段、前端手工字段清单或 CSS 隐藏。
- Process-pool source key boundary: 报工数据设备参数等结构化来源字段可能包含参数代码、值字段和设备组作用域，完整字段编码可能远长于普通单元格 Key。保存链接时必须使用稳定短来源 Key 作为 `sourceCellKey`，并把完整业务字段编码保留到 `sourceFieldCode` 用于回填解析；正式 MySQL schema、H2 测试 schema、本地启动迁移和真实运行库都必须同步校验字段容量，禁止截断完整字段编码、吞掉数据库异常或只靠前端字段名重建来源身份。
- Process-pool DCC current-route rule boundary: 当用户按 DCC 项目代码选择当前路线后，报工数据参数必须存在于该当前路线的正式 `routeProcessId + deviceId + parameterCode` 参数规则上；若只有源路线、旧路线或同名工序上有参数规则，应按正式数据迁移补齐当前路线，不能在运行态跨路线借用、按工序名推断、或让前端合成参数字段。
- Process-pool linkable display boundary: 正式报工事件仍可保留完整数量、人员和签名审计数据，但“报工数据”可链接字段目录必须按已确认的业务展示口径输出；被要求隐藏的放行分配数量、损耗原因名称、损耗原因编码、实际操作员工、事件设备编号、工作站编号、设备账号、提交签名编号和审核人签名编号不得进入目录，不能用 CSS 隐藏或仅在前端过滤掩盖。提交签名用户、审核时间和审核人签名用户等仍需展示的业务字段继续从正式事件/审核记录读取；隐藏展示不等于删除底层审计数据。
- Process-pool signature target boundary: 批记录表单里的签名位不一定是 SIGNATURE 控件，真实模板可能用 `操作人/日期`、`复核人/日期` 等普通文本格承载签名。保存单元格链接时必须先按 SIGNATURE 标记或签名语义标签识别签名目标格，再只允许 `PROCESS_POOL_REPORT.signatureUserId` 与 `PROCESS_POOL_REPORT.reviewSignatureUserId` 链接；报工数量、设备身份、设备参数、时间和确认字段链接签名位必须被正式业务错误拒绝，不得把签名语义格当普通可写格放行。
- Process-pool source-specific loading boundary: 批记录单元格链接页已明确选择 `PROCESS_POOL_REPORT` 且只选择了 DCC 项目代码、尚未选择 `routeProcessId` 时，后端工作台上下文必须先返回该 DCC 项目代码对应路线的 `routeProcesses`；不得预加载 `PRODUCTION_PICK_LIST`、PQC 或其它来源字段并让其它来源的 fail-fast 错误阻断“工序”下拉。
- Production pick-list source boundary: 批记录单元格链接页选择“领料单数据”时，字段目录必须按当前 DCC 项目绑定、路线产品和目标 `routeProcessId` 生成，至少覆盖物料编码、名称、规格、单位、物料批次号和领料数量。路线维护了工序物料清单时必须按该清单精确限定；整条路线未维护工序物料清单时，必须按路线产品编码读取已同步 ERP 生产用料清单作为正式产品物料目录，再绑定到用户当前配置的 `routeProcessId`，不得把其它路线或前端静态物料作为替代。申请放行解析领料单时必须按当前 DCC 路线产品、生产工单正式编号、唯一已审核单据和物料编码再次核对来源关系。同一物料存在多条明细时，只能按正式 `sourceEntryId` 升序取第一条；缺少稳定分录号、DCC/产品/工序关系不一致、领料单不唯一或物料缺失必须在任何目标资料写入前阻断。配置保存只保存来源到目标单元格的关系，不得提前生成批记录。
- Production pick-list source verification: 后端回归必须覆盖字段目录按工序筛选、同物料多明细首条确定性、唯一已审核领料单校验和异常关系 fail-fast；来源证据哈希必须参与放行幂等键，避免来源变更后复用旧结果。
- Formal binding identity boundary: 工序批记录绑定的正式身份必须从 `batch_record_report_id -> mes_pro_batch_record_report.report_id` 解析报表、定义和版本；绑定表中的 `batch_record_definition_id/batch_record_version_id` 只是冗余快照，历史值可能为空，不得作为唯一查询条件。请求带 `routeId` 时必须同时按当前路线限定同一报表的 `routeProcessId`，避免跨路线合并；正式报表元数据缺失时必须阻塞，禁止用 `formBindings`、默认 `MAIN` 文案、首条绑定或空工序字段替代。
- Route process context propagation boundary: 同一正式批记录报表可能被多条路线或多个工序复用。从工艺路线当前工序进入批记录表单列表、再进入单元格链接页时，必须全程携带并校验 `routeId + routeProcessId + targetReportId`；从某张批记录表单点击“链接”时，该表单代表右侧目标表单，不是左侧来源表单，左侧默认来源必须避开同一张目标表单并选择其它正式来源表单。切换到“报工数据”后仍须保持同一目标工序，保存单元格链接或重复行组时也必须用请求 `routeId` 解析目标工序。缺少正式路线工序上下文、目标报表与工序不匹配或解析结果不唯一时，页面应禁用“报工数据”并由后端 fail-fast，禁止退化为只显示通用字段、任取首个共享报表工序、把当前点击表单当来源表单或跨路线保存。
- Blocker: 来源值存在且链接规则启用，但目标 execution 未保存到 `cell_values_json` 时，必须把修复收敛到创建/打开执行记录的后端落库链路；若字段审计系统写入证据缺失，也必须阻塞，不能直接 update 主表。
- Idempotency schema check: 自动落库写入字段审计前必须核对幂等键列长度；语义组合键可能超过 `varchar(64)` 时，使用稳定原始组合键的 SHA-256 作为保存和查询共用键，并同时测试写入路径与重复打开查询路径恰好生成 64 位小写十六进制。
- Verification: 后端回归需覆盖创建执行记录、打开历史空 DRAFT、重复打开幂等、目标已有人工值不覆盖、来源批号缺失 fail-fast、专用来源被通用预填跳过且由专用服务回填，并复验字段审计 hash/head revision、审计批次数量和幂等键长度；真实 E2E 需同时断言打开任务响应、执行详情 `cellValuesJson`、页面目标输入值和重复打开不追加审计批次。
- Forbidden action: 禁止把 `/prefill` 返回值或前端 `hydrateDraftState` 当作已保存结果；禁止前端写空值兜底、查询接口隐式写库、直接 SQL 回填、把专用业务来源当成通用不支持字段抛错，或绕过字段审计链。
- Evidence: `doc/tasks/20260727-edhr-cell-link-auto-persist-design/verification-report.md`；`doc/tasks/20260727-edhr-cell-link-auto-persist-implementation/verification-report.md`；`doc/tasks/20260731-team-leader-workbench-prd-plan/execution-log.md`；`doc/tasks/20260812-process-pool-all-fields-cell-link/verification-report.md`；`doc/tasks/20260814-batch-record-repeat-row-link-implementation/verification-report.md`；`doc/tasks/20260814-batch-record-process-parameter-fields-fix/verification-report.md`；`doc/tasks/20260830-dcc-rough-wash-device-filter-bug/verification-report.md`；`doc/tasks/20260830-dcc-process-device-type-parameter-catalog/verification-report.md`。

## eDHR 批记录 Word 表格解析门禁

### 全局行形态优先于模板特例

- Trigger: 批记录 Word 导入、Route B/Route D 表格识别、packed 物料矩阵、操作明细区域、`生产自检`/合格标准/检验方法说明块、截图位置错位。
- Preflight check: 先用真实源 DOC 与最小合成表格复现结构偏差，定位到共享 parser/calibrator/row-type 规则；对 packed 宽单元格必须按视觉 token 处理续行，对短标题 + 长说明行必须按说明区行形态判断。
- Blocker: 缺少真实源 DOC、测试类硬编码本地 fixture 不存在、或 RED 不能稳定复现时，不得宣称修复完成；先记录缺失 fixture 和影响范围。
- Verification: 回归必须同时包含合成 RED/GREEN 和用户指定真实 DOC 样本；至少断言 packed 括号续行不新增物料项、后续物料不整体错位、操作明细区域不吞入后续说明块。
- Forbidden action: 禁止用表单名、工序名、文件名、压力泵模板名硬编码特例；禁止把缺 fixture 的结构测试当成业务逻辑失败；禁止只靠截图人工判断完成。
- Evidence: `doc/tasks/20260725-batch-record-global-table-position-fix/verification-report.md`。

### 合并单元格参数配对必须使用源物理列坐标

- Trigger: 批记录 Word 需要从操作表识别设备、参数名称、参考值或设备参数组，且表格包含跨行/跨列合并、同类多设备、不同设备共用一个设备单元格或多组参数值。
- Preflight check: 先同时打印 `columnIndex/colSpan` 与 `logicalColumnIndex/logicalColSpan`，核对参数标题、`参考值/参数` 子表头和模板值在源 Word 物理列上的重叠关系；跨行语义配对使用源物理列坐标，逻辑坐标只用于布局归一化，不得默认二者等价。
- Grouping rule: 设备参数分组先按设备名称域区分不同设备类型，再按参数列中的有序参考值向量配对；同名连续设备可共享一组参数，不同设备类型无参数时保留独立空参数组。参数向量数量无法与设备名称组唯一对应时必须 fail fast。
- Blocker: 参数标题和值被相邻列吸收、同一参数出现低频冲突值、设备组数与多值参数向量数无法唯一对应，或只能依靠产品名/工序名硬编码分组时必须停止。
- Verification: 使用真实 DOCX 与结构化目标 JSON 全量比较，同时断言工序顺序、物料、设备选项、设备组、参数名称和参考值；复杂样本至少覆盖清洗+烘干双设备域、辅助测量设备无参数以及多台同名设备共享参数三类结构。
- Forbidden action: 禁止用 `logicalColumnIndex` 直接跨行配对参数，禁止按文件名或固定工序名补齐设备参数，禁止忽略无法配对的设备或返回空组冒充成功。
- Evidence: `doc/tasks/20260902-idi-batch-record-total-json-recognition/verification-report.md`。

### 过程检验源 Word 必须先命中 profile 再拆输入框

- Trigger: 过程检验 5.0/6.0、`气密性检测工装：______`、`xxx：______`、Route E 源 Word 识别、同单元格标签加下划线填空、源 Word 带封面/记录编号/版本/页码等非过程检验表。
- Preflight check: 先确认源 Word 表头命中过程检验 profile，再让 JSON 构建器处理同单元格内的标签 + 下划线；`检验设备` 列里的 `label：______` 必须保留为源 Word 结构，不得直接走图片路线。profile 命中后必须调用 profile 的整组 `normalizeSourceTables`，让 profile 自己过滤非目标源表；不得在 Route E 中逐源表直接调用 `normalizeSourceTable` 绕过过滤逻辑。
- Blocker: profile 未命中却进入 image route、`label：______` 被压成普通静态文本、下划线填空不再生成可填写输入框、或封面/记录编号等非过程检验表被作为过程检验结果输出时，必须停止。
- Verification: OfficeCLI 查询源 Word 能看到 label / colon / underline 三个 run；后端回归同时覆盖 source profile 命中、带非目标源表时只输出过程检验表、真实 6.0 样本和 `input-text` 拆分结果。
- Forbidden action: 禁止按文件名、工序名或固定文案硬编码；禁止用图片解析 fallback 冒充源 Word 结构归一化；禁止把同单元格标签+下划线整体渲染成纯文本。
- Evidence: `doc/tasks/20260820-pressure-pump-v5-word-parser/verification-report.md`；`doc/tasks/20260820-pressure-pump-word-parse-v6/verification-report.md`。

### Form Center 导入必须同时持久化源表格布局

- Trigger: Form Center DOCX 模板导入、recognizedSchemaJson、jimuSchemaJson、sheetLayoutJson、同一 Word 表格包含多个工序、V4 横向表格与 V5/V6/V7 纵向“识别字段”预览差异。
- Preflight check: DOCX 含表格时，识别器必须从源表格生成 sheetLayoutJson 和 cellRules，导入服务必须把完整 Jimu schema 写入模板版本；多张物理表格或同一物理表格包含多个逻辑表单时，先按跨全表宽度的“工序生产记录”标题行切分候选，再按正式导入模板名称唯一选择，不能固定取第一张表。只保存识别字段列表会触发前端纵向字段兜底预览。
- Source fidelity: 选中候选后，字段提取范围必须同步收窄到该候选；标题行即使包含 `□` 也保持静态，不得把整张标题单元格识别成单选；Word 单元格内的非空段落必须保留换行；列宽、行高、横纵合并、字体、对齐、边框和斜线方向必须进入源布局，前端可填写和只读渲染器共同消费这些样式与斜线元数据。
- Blocker: 多候选无法根据正式上下文唯一匹配、真实源表格的列数/行数丢失、源布局未写入 jimuSchemaJson、斜线方向丢失，或 标签：______ 只保留为普通文本时必须停止；禁止静默回退到第一张表。
- Verification: 真实 DOCX 回归同时断言候选隔离、源表格列数和行数、横向表头、段落换行、合并范围、斜线数量与方向、位置型空白填写规则、冒号下划线拆分规则，以及导入新版本落库的 jimuSchemaJson；真实页面还要核对标题、重复物料块、生产自检、批量汇总和清场区域。
- Forbidden action: 禁止只修前端预览、手工写入模板 JSON、按文件名/产品名/工序名做特例、把标题内的选择框字符等同于填写控件，或用识别字段列表替代源表格结构。
- Evidence: `doc/tasks/20260821-pressure-pump-form-parser-v7-regression/verification-report.md`；`doc/tasks/20260831-form-center-cleaning-table-recognition/verification-report.md`。

### 表单模板 Jimu 保存回写正式版本门禁

- Trigger: 表单模板页内 Jimu 编辑器、`FORMTPL:*` 报表、`/jmreport/save`、`jimuSchemaJson.sheetLayoutJson`、草稿模板版本、用户在 Jimu 画布新增/删除单元格后要求保存回原表单模板。
- Preflight check: 先确认编辑入口使用当前表单模板版本的虚拟报表 ID `FORMTPL:<templateVersionId>`；非草稿版本点击编辑必须先生成或复用同模板草稿版本，Jimu 原生保存请求必须在后端按租户和模板版本状态校验，只允许写 `DRAFT`，且保存成功后把 Jimu 最新画布同步回该模板版本正式 `jimuSchemaJson.sheetLayoutJson`。
- Blocker: `/jmreport/save` 只更新 Jimu 报表表而没有回写模板版本、保存请求缺租户、`FORMTPL:*` 指向非当前租户或非草稿版本、保存成功但模板详情接口读回的 `sheetLayoutJson` 未变化、或回写覆盖 `cellRules/assistRows/signatureCellMarkers/fillAssignments` 等外层规则配置时必须停止。
- Verification: 后端合同必须覆盖 `FORMTPL:*` 保存过滤器、草稿写保护、保存后只替换 `sheetLayoutJson` 并保留外层配置；真实 E2E 必须从表单模板页面进入 Jimu 编辑器，临时新增或删除单元格，触发 Jimu 原生保存，再通过表单模板正式详情接口读回确认变化，最后恢复测试改动。
- Forbidden action: 禁止用直接 SQL、只改 Jimu 报表表、API-only 写模板版本、前端本地缓存、重新导入模板、发布版静默改草稿、吞掉保存失败或默认成功来冒充 Jimu 保存回写。
- Evidence: `doc/tasks/20260828-form-template-edit-button-batch-record-designer/verification-report.md`。

### 批记录/路线导入真实 fixture 覆盖范围变更边界

- Trigger: 批记录 Word、Sheet1 Excel、路线导入、真实 fixture、`NoSuchFileException`、用户明确说“不需要覆盖这个”或取消真实样本覆盖。
- Preflight check: 先区分“业务仍要求真实样本覆盖但 fixture 缺失”和“用户明确变更验收范围取消该真实样本覆盖”；前者必须阻塞并取得权威原件，后者必须删除依赖缺失真实 fixture 的测试入口，同时保留不依赖真实文件的合成 fail-fast/契约测试。
- Blocker: 缺少用户明确范围变更、无法证明删除的测试只覆盖被取消的真实样本链路、或删除后完整目标套件仍有 failure/error 时，必须停止，不得宣称完成。
- Verification: 记录用户范围变更、删除/保留的测试清单，运行目标 parser/contract 定向测试和完整模块回归；完整回归必须 `BUILD SUCCESS` 且 0 failures/0 errors。
- Forbidden action: 禁止用 `@Disabled`、Maven excludes、assumptions、空夹具、合成 workbook 或桌面候选文件冒充权威真实 fixture；禁止把真实样本覆盖取消解释成业务 fallback。
- Evidence: `doc/tasks/20260727-edhr-notify-all-valid-candidates/verification-report.md`，用户明确取消 Sheet1 Excel 真实样本覆盖后，保留 `Sheet1RouteExcelParserTest` 合成 fail-fast 测试并通过完整 `mvn -pl yudao-module-mes test`。

### 旧版本 JSON 的 fillForm/edhrCellRule 读时刷新门禁

- Trigger: 批记录截图或只读预览仍显示已修复过的错位 checkbox、V14/V14.0 等既有版本复验、`sheetLayoutJson` 的 `text` 坐标正确但页面仍渲染旧控件。
- Preflight check: 同时审计 `text/value`、`fillForm.labelText/componentFlag/valueType` 和 `edhrCellRule.label/componentFlag/valueType`；不得只检查静态文本坐标。
- Blocker: 若业务列仍残留未确认 AUTO 规则的旧 checkbox / BOOLEAN / 串列 label，必须在共享单元格规则刷新链路中修复并持久化，不得用截图裁剪、前端隐藏或表单名特例绕过。
- Verification: 回归测试必须覆盖 stale `fillForm` 被刷新、已确认 MANUAL 规则不被覆盖、密集表格业务列优先使用上方列头；真实页面验证需同时断言目标业务列 offender 为 0 并保留截图。
- Forbidden action: 禁止只重新导入新版本就宣称既有版本已修复；禁止按产品名、工序名、文件名、压力表文本写清理逻辑；禁止把 API-only 审计替代真实前端截图验收。
- Evidence: `doc/tasks/20260726-batch-record-v14-layout-regression/verification-report.md`。

### Jimu fillForm 组件类型语义优先边界

- Trigger: Jimu 编辑页右侧“当前组件”与批记录单元格语义不一致、日期/签名日期单元格显示为“多行文本”或普通文本、`fillForm.componentFlag=input-textarea` / `input-text`、`记录人/日期` / `操作人/日期` / `复核人/日期` 等签名日期宽空白格。
- Preflight check: 先审计后端 `MesProBatchRecordReportJsonBuilder` 生成的 `fillForm.componentFlag`、`edhrSignature` 与相邻/同一行标签语义；Jimu 右侧当前组件以 `fillForm.componentFlag` 为准，只有 `edhrSignature` 元数据不足以显示电子签名控件；宽合并空白格不得在语义判断前被 `isWideBlankNarrativeArea` 直接归类为 textarea。
- Formalization boundary: 识别出来的可填格子正式化时只补正式身份、映射 key 和版本关联，不得重算 `componentFlag` / `valueType`；日期、电子签名、普通文本必须沿用识别阶段语义，不能在“正式化”步骤里悄悄降级成通用文本。
- Save cell rules boundary: 批记录填写配置保存接口接收 `SIGNATURE` 规则时，必须在校验前同步生成启用的 `edhrSignature` marker；同一格改回文本/数字/日期等普通类型或旧已审核签名规则被移除时，必须清理对应签名 marker，避免规则类型与单元格元数据分离。
- Blocker: 如果无法用最小合成表格稳定复现组件类型误判，或无法证明普通叙述型宽空白格仍保持 textarea，不得宣称修复完成。
- Verification: 必须同时覆盖“签名日期宽空白格生成 `componentFlag=signature` 并保留 `edhrSignature`”和“普通高/合并叙述空白格仍生成 `input-textarea`”两个回归断言。
- Verification supplement: 新增正式化链路时优先用定点测试分别断言日期、签名、文本三类格子正式化后类型不变且幂等；如果同模块全量类已有既有失败，必须单独记录为背景噪音，不能拿它替代当前链路验证。
- Forbidden action: 禁止只改前端“当前组件”显示文案、禁止直接手工改 Jimu JSON、禁止按模板/产品/文件名硬编码日期格、禁止只把签名日期格退化成 `input-text` 或普通日期展示而丢失电子签名组件语义。
- Evidence: `doc/tasks/20260727-jimu-signature-date-cell-type/verification-report.md`。

## 统一审批中心 BPM 已办历史状态门禁

- Trigger: 审批中心“已办”、`/approval-center/tasks/page?viewType=DONE`、`BpmNativeApprovalTaskProvider`、`BPM_TASK_DONE`、历史 `HistoricTaskInstance` 缺少 `TASK_STATUS`、页面显示“系统异常”。
- Preflight check: 先对照标准 BPM `done-page` 行为确认历史任务状态字段是否可为空；统一审批中心 DONE 映射必须保留正式历史任务行，`approvalResult` 可为空表示历史记录未保存审批结果，不得把缺失状态当作整页异常。
- Blocker: DONE 映射因 `TASK_STATUS=null` 抛 `APPROVAL_RESULT_UNSUPPORTED`、删除历史任务行、返回默认“通过/驳回”、或对非空未知状态吞异常时必须停止。
- Verification: 新增后端回归覆盖缺少 `TASK_STATUS` 的 `HistoricTaskInstance` 仍返回 `BPM_TASK_DONE` 摘要且 `approvalResult/approvalRemark` 为空；同时复跑 `BpmNativeApprovalTaskProviderTest` 和 `ApprovalCenterServiceImplTest`。
- Forbidden action: 禁止用前端隐藏错误、空列表成功、过滤掉 legacy 已办任务、默认审批结果、或放宽所有未知状态来掩盖历史状态缺失。
- Evidence: `doc/tasks/20260804-approval-center-done-system-exception/verification-report.md`。

## BPM 审批模型人员配置候选人门禁

- Trigger: 审批模型人员配置、审核人/批准人、审批对象、用户/权限角色/部门/发起对象直属主管、CandidateStrategy.MIXED、BpmTaskCandidateMixedStrategy、和关系、或关系。
- Preflight check: 先确认前端保存的审批对象能映射到 Flowable 正式候选人策略。多个不同类型审批对象需要混选时，后端必须提供正式候选人策略解析用户、角色、部门和发起人直属主管，并逐项执行原有候选参数校验；和关系应由流程节点结构表达为多对象均需审批，或关系应解析为候选人并集。
- Blocker: 混合对象只存在前端 JSON、后端无法校验角色/部门/用户 ID、发起人直属主管缺少发起人上下文、候选解析为空却自动通过、未知策略被忽略、或吞掉单项解析异常时必须停止。
- Verification: 新增后端回归覆盖混合策略标识、用户/权限角色/部门/直属主管解析、发起人上下文解析、空参数失败和未知策略失败；同时复跑候选人调用器相邻测试。
- Forbidden action: 禁止用默认用户、当前登录人、空集合成功、前端候选缓存、SQL 直改 BPM assignee 或全局放宽审批人校验替代正式候选人策略。
- Evidence: `doc/tasks/20260830-approval-model-participant-config/verification-report.md`。

## 统一审批中心 DCC 已办历史快照展示门禁

- Trigger: 审批中心“已办”、`viewType=DONE`、`DccApprovalTaskAdapter`、DCC 历史审批行、受控文件历史归档或软删除数据缺少 `versionNo`、`categoryId`、分类记录或其它只用于展示的历史元数据，页面显示“系统异常”。
- Preflight check: 先区分当前 TODO/处理态必填业务数据和 DONE 历史展示快照。当前待办或可处理审批必须继续要求正式版本号、分类和业务元数据；历史 DONE 行如果正式业务已完成但展示元数据缺失，只能在上下文标签中用 `-` 表示未知展示值，并保留文件编号、节点、盖章、分发等仍可追溯字段。
- Blocker: DCC DONE 历史行因缺少纯展示字段抛 `APPROVAL_BUSINESS_VERSION_REQUIRED` / `APPROVAL_BUSINESS_CATEGORY_REQUIRED` 导致整页失败，或为了修复 DONE 而放宽 TODO/当前审批必填校验时必须停止。
- Verification: 后端回归覆盖历史 `DccControlledFileDO` 缺 `versionNo/categoryId` 时 DONE 摘要返回 `版本：-` 和 `分类：-`；同时复跑 `DccApprovalTaskAdapterTest` 与 `DccApprovalTaskTimelineAdapterTest`，再用真实 `/approval-center/done` E2E 验证 DONE API 成功且页面无“系统异常”。
- Forbidden action: 禁止用 `formBindings`、默认分类、当前登录人、前端隐藏错误、空列表成功或 catch 吞异常替代正式 DCC 历史快照展示；禁止把历史展示占位扩散到当前待办必填字段。
- Evidence: `doc/tasks/20260804-approval-center-done-system-exception/verification-report.md`。

## 统一审批中心待办聚合一致性门禁

- Trigger: 审批中心“待办”、`/approval-center/tasks/page?viewType=TODO`、左侧徽标有数量但列表为空、`ApprovalCenterServiceImpl`、provider `total > 0` 但首屏 `list` 为空、页面显示“暂无审批任务”或“0 个模块”。
- Preflight check: 同时核对模块徽标统计、统一审批中心 provider 分页响应、全局聚合窗口和前端 route query；首屏 `pageNo=1` 时 provider 返回 `total > 0` 但 `list` 为空必须视为 adapter/query 一致性错误，而不是合法空态。
- Shared-process pagination check: 多个业务域共用同一 BPM process key 时，provider 不得先按共享物理任务分页、再在当前页内按 business key 前缀过滤；必须先形成 provider 自己的语义集合和 total，再对该集合分页。若底层接口只能提供分页，provider 应稳定遍历底层页并在语义过滤后切目标页，同时对底层提前空页继续 fail fast。
- Business-summary check: BPM 原生待办如果来自业务流程变量，provider 必须从正式变量生成 `businessTitle`、`businessCode`、`businessContextTags` 和中文 `currentNodeName`；页面出现“未配置中文标题 / 业务键已配置 / 未配置中文节点”时，先核对 Flowable 变量到摘要 VO 的映射，不得只在前端追加英文流程名翻译来掩盖业务对象缺失。仅用于展示的摘要字段缺失时必须省略该字段，不得抛异常打断整页待办；用于识别业务语义的枚举值若存在但非法，仍必须 fail-fast。
- Business-summary snapshot check: 审批摘要需要显示业务名称而隐藏申请键、内部 ID 或流程实例键时，业务服务必须在提交审批时保存不可变展示快照，并在发起流程时逐项校验后写入正式流程变量；provider 应生成确定的标题、上下文标签和显式 `businessIdentifierHidden` 展示契约，前端不得按标题、标签组合或 ID 猜测是否隐藏编号。已运行流程缺少正式展示变量时必须制定可审计回填方案，禁止用申请编号、内部 ID、当前主数据或“业务键已配置”降级替代。
- Cross-view business-summary check: “我发起的”必须使用分页查询已经加载的 `HistoricProcessInstance.processVariables` 生成摘要；“已办”查询默认只加载任务局部变量，provider 必须按当前页流程实例 ID 批量读取带正式变量的历史流程实例，再生成标题、编号、上下文和中文节点。不得把流程实例名、历史任务节点名或前端占位文案当成业务摘要；已办任务找不到所属历史流程实例时必须明确失败，空页不得发起空 ID 历史查询。
- Applicant-source check: 原生 BPM 待办和已办的申请人必须分别来自所属运行中/历史流程实例的正式 `startUserId`，再交由统一审批中心用户源解析姓名；不得使用任务审核人、当前登录人、任务局部变量缺省值或前端 `--` 掩盖发起人字段缺失。待办页应按当前页任务流程实例 ID 批量读取运行中流程实例，已办页应复用当前页历史流程实例批量查询；所属流程实例缺失时必须明确失败。EDHR 等原生非 BPM 适配器必须把自身正式申请来源字段映射到统一 `initiatorUserId/initiatorUserName`，例如 `EDHR_WORK_TASK` 使用服务层返回的 `sourceUserId/sourceUserName`，不得只返回审核人导致申请人列为空。
- Blocker: 后端把 total/list 不一致返回给前端、前端隐藏查询条件导致用户看不到过滤状态、模块列表接口失败被后续请求覆盖为有效 0、或测试只断言空态文案不核对 provider 总数时必须停止。
- Verification: 后端回归覆盖 inconsistent provider 首屏 fail-fast、“我发起的”使用流程变量、“已办”批量读取历史流程变量、空页不查询和缺失历史实例 fail-fast，并复跑 `BpmNativeApprovalTaskProviderTest` 与 `ApprovalCenterServiceImplTest`；前端静态合同覆盖 route `moduleCode` / `keyword` 同步到可见筛选控件、正式标题不被占位文案替换，并复跑审批中心分页/列表相邻合同。
- Forbidden action: 禁止用前端空列表兜底、默认清空筛选、吞模块接口异常、过滤掉 provider 行、或只改徽标数量来掩盖正式待办数据链路不一致。
- Evidence: `doc/tasks/20260804-approval-center-todo-empty-list/verification-report.md`。

## 注册证上传审批入口与入库门禁

- Trigger: 注册证上传弹框、`/dcc/registration-certificates/uploads`、`UPLOAD_CERTIFICATE`、`BPM_TASK_TODO`、`dcc:registration-certificate:upload:create`、`dcc:registration-certificate:upload:approve`、保存后进入审批中心、审批通过后进入注册证列表、授权公司、`companyId`、生产方式、是否委托生产、是否自行生产、受托企业、手填产品名称、DCC 项目代码选填、注册证日期顺序、`product_master_id`、`registrant_name`、注册证通知角色、`registrationCertificateReminderDailyJob`、`mdm_role_company_scope`、`Registration certificate access BPM candidate list is empty`、`Registration certificate company scope denied`、`Registration certificate product is missing or disabled`、`Registration certificate date order is invalid`、`该任务的审批人不是你`、`角色未配置授权公司`。
- Preflight check: 先区分“首证上传提交”和旧“草稿维护/正式化”入口；上传页只采集业务要求字段和注册证文件，公司名称只能作为授权公司选择的展示文本，请求必须提交正式 `companyId` 并用申请人账号校验 `mdm_company_scope`，禁止按公司名称、展示名或输入文本反查授权公司；产品名称是注册证表单文本字段，DCC 项目代码选填；无论项目代码是否绑定产品，上传链路的 `product_master_id` 都不得由项目代码反填，必须允许为空并保存手填产品名称；上传页不再采集注册人名称时，快照 `registrant_name` 也必须允许为空，不得用公司名称、产品名称或注册证号伪造注册人名称；不得按产品名称反查或猜测 MDM 产品主数据；项目代码只校验自身存在、启用和租户一致，不校验绑定产品启用或名称一致；日期顺序必须显式校验“首次获证日期 <= 生效日期 < 有效期至”，前端保存前提示中文错误且后端保留同规则兜底；生产方式必须显式采集“是否委托生产/是否自行生产”，两项不可同时为否；委托生产为是时受托企业必须来自当前租户已启用的 MDM 受托企业主数据并随草稿保存为 ID；后端必须用专用上传 Controller 创建待审批请求并立即绑定 Native BPM；首证上传审批候选必须直接来自注册部经理角色及其 `dcc:registration-certificate:upload:approve` 权限，不得再按公司范围过滤；统一审批中心中注册证上传待办实际归属 BPM 原生审批来源，审核入口必须在确认流程变量为 `UPLOAD_CERTIFICATE` 且当前用户同时拥有注册部经理角色和上传审批权限后，将当前 `BPM_TASK_TODO` 正式接管给当前用户再调用 BPM 完成逻辑，使角色成员均可审批；审批通过后正式化时，审批记录、审计和绑定确认使用注册部经理身份，已提交草稿的公司/项目范围复核使用原上传申请人身份；审批通过后的待首次生效首证必须进入注册证当前列表。若审批通过后出现 `角色未配置授权公司：<数字>`，先确认该数字是否为 `registrationCertificateReminderDailyJob` 配置的通知角色 ID；通知收件人解析要求这些角色在 `mdm_role_company_scope` 中覆盖当前 `ACTIVE` 注册证所属公司，缺失时应通过正式迁移补齐角色授权公司范围，不得把它误判为 admin 审批权限不足或改成用户授权。
- 流程模型名维护: 注册证上传和延续共用流程标识 `dcc-registration-certificate-access`；把页面展示流程名改成中文时，只能改模型显示名、BPMN `<process name>` 和当前已发布定义/部署显示名，不得把流程标识改成中文，也不得顺手改注册部经理角色、审批权限、候选人规则或历史审批业务状态。若旧模型缺流程管理员导致正式接口拒绝保存，必须先明确这是数据前置问题；未获授权不得为了保存而新增管理员或扩大权限。
- 审批中心业务标题分类: 首证上传和延续虽然共用 `requestType=UPLOAD_CERTIFICATE` 与同一流程标识，但审批中心业务标题必须使用请求明细中的正式 `operation` 区分；发起 Native BPM 时必须把 `operation=UPLOAD_CERTIFICATE` 或 `operation=RENEWAL_CERTIFICATE` 写入独立流程变量，延续显示“注册证延续审批”，首证仍显示“注册证上传审批”。不得按幂等键前缀、中文用途、文件名、注册证状态或前端文案猜测审批类型；已运行流程缺少正式 `operation` 时，审批中心列表不得 500，必须显示通用“注册证审批”并省略缺失细分字段，同时单独评估是否需要可审计数据回填；若 `operation` 存在但不是已知值，仍必须 fail-fast。
- 注册证变更审批操作识别: 注册证变更提交同样复用 `requestType=UPLOAD_CERTIFICATE` 与流程标识 `dcc-registration-certificate-access`，但必须把 `operation=CHANGE_CERTIFICATE` 作为正式已知操作写入并由 BPM 候选预测、原生流程启动、审批中心摘要和标题共同识别，标题显示“注册证变更审批”。不得只在 DCC 服务层识别变更后就遗漏 BPM 层 operation 白名单；不得把 `CHANGE_CERTIFICATE` 误判为未知注册证审批导致“注册证审批绑定状态异常”。真实页面 E2E 需捕获提交 POST 的业务码和 requestId，并单独记录通知短信、站内信等事务完成后旁路错误，不能用吞掉通知异常来证明审批链路正确。
- 审批详情审核对象: 注册证上传和延续的列表审核人、查看/流程详情时间线、顶部当前处理人、打印或摘要必须使用同一正式审核对象字段。若审核对象来自注册部经理权限角色，后端审批详情节点和任务必须返回角色 code/name，由前端显示 `审批角色：注册部经理`；不得只返回 Flowable assignee/candidateUsers 让前端从多个角色成员里随机显示具体人员。正式角色缺失或名称异常时必须 fail-fast，不得降级为默认用户。
- 详情操作审计: 注册证详情展示首次上传人/时间、上传审批人/时间以及逐次延续操作人/时间、延续审批人/时间时，提交人必须来自与该版本正式注册证文件绑定的已审批上传申请 `requester_user_id/requested_at`，审批人必须来自对应版本 `formalized_by/formalized_at`；人员可见名称统一由系统用户正式接口解析。延续记录必须按 `targetVersionId` 逐版本关联，后续下载申请即使引用同一文件也不得参与上传人解析、不得生成重复操作记录。正式人员 ID 存在但姓名缺失时必须明确失败；历史数据缺少上传申请时只能显式暴露正式记录缺失，不得显示 ID、当前时间、默认人员或猜测文案。验证至少覆盖首次上传提交人与审批人分离、两次延续不串人/串时间、同文件已审批下载申请不污染上传记录，以及前端详情使用正式字段和统一时间格式化。
- 延续日期错误隔离: 延续注册证提交和审批正式化必须把日期类错误与基准版本冲突分开；批准日期晚于当前业务日期、批准日期晚于生效日期、生效日期不早于有效期至、首次获证日期晚于批准日期时，必须返回日期类错误，不得映射成 `Registration certificate renewal base conflict`。`renewal base conflict` 只允许用于当前有效版本、当前快照、并发写入或真实基准身份不匹配。
- 延续并发与重复状态隔离: 同一注册证的延续提交必须在事务内先锁定注册证主记录，再校验当前版本、待生效版本和开放延续申请；不同幂等键的并发请求不得同时创建有效审批。正式延续入库还必须按 D-005 判断 `effectiveDate <= businessDate`：到期候选在同一事务内立即完成当前/旧证切换并以 `CURRENT` 返回，未来日期才登记为 `PENDING_EFFECTIVE`；否则当前列表按正式状态隐藏“延续”会错误阻断下一次延续。当前列表只允许 `CURRENT` 状态展示“延续”，`PENDING_EFFECTIVE` 不得继续开放入口；绕过页面重复提交仍返回明确中文冲突。若历史并发已形成多条相同待审批流程，必须由业务方确认保留项并通过正式撤回/驳回链路处理，禁止审批第二条生成另一待生效版本，也禁止 SQL 直接改状态。
- 注册证用户提示中文化: 所有 `REGISTRATION_CERTIFICATE_*` 业务错误、控制器接口说明、页面成功/失败/校验提示、提醒阶段、业务事件站内信正文和审批/授权状态必须使用明确中文；站内信正文必须展示产品名称、注册证号、生效日期和到期日期等业务字段，不得要求用户理解事件码、业务键、租户、企业、操作人或内部 ID；前端不得直接展示后端内部状态值或英文库异常，非中文技术异常应映射为对应操作的中文失败提示并继续保留失败。`DCC`、`BPM`、`ID`、`SHA-256` 等必要术语可以保留，API 路径、权限码、请求类型、状态值、MIME 和审计机器字段不得翻译。新增注册证错误码或页面状态时，必须同步通过注册证中文文案合同。
- 注册证下载文件名日期来源: 注册证下载文件名优先使用正式版本的批准日期；首证上传链路未采集批准日期、正式版本 `approval_date` 为空时，必须使用注册证主档的首次获证日期生成服务端文件名。两者都为空时必须返回明确中文业务错误并在读取文件内容前失败；不得直接格式化空日期导致“系统异常”，也不得使用当前日期、上传时间、生效日期或前端文件名猜测日期。验证必须覆盖批准日期优先、批准日期为空时使用首次获证日期、两者都为空时 fail-fast，以及授权、公司范围和下载审计仍按原正式链路执行。
- 注册证下载审计 JSON: 下载成功和失败审计的 `detail_json` 必须通过项目标准 JSON 序列化器生成，不得手工拼接或仅转义反斜杠、双引号。请求头、浏览器 `User-Agent` 和底层异常原因可能包含控制字符；审计写入失败会让已经读取成功的文件最终返回“系统异常”。验证必须覆盖含制表符、换行符等控制字符的审计上下文可写入 JSON 列且可被标准解析器还原。
- 注册证列表读审计幂等边界: 注册证当前列表和旧证索引属于可重复刷新的读取入口，`PAGE` / `OLD_INDEX` 成功读审计遇到相同请求上下文和相同证书的重复事件键时，只能按列表刷新语义保持单条审计并继续返回列表；不得让“注册证审计事件已存在”阻断列表页面。该幂等边界不得扩展到 `DETAIL`、失败审计、提交、审批、正式化或其它写链路审计，详情重复读审计仍必须 fail-fast。验证必须覆盖重复列表刷新成功且审计只保留一条、详情重复审计仍报冲突、真实页面审批通过后返回列表不再出现审计冲突。
- 注册证当前快照唯一读取边界: 注册证主列表和当前详情期望一行业务主对象时，版本下的快照一对多关系必须按 `current_snapshot_id` 精确选取当前快照；待生效/历史版本必须按明确版本身份选取唯一最新快照，count 与 page 必须使用同一快照口径。禁止用 `DISTINCT`、前端去重、分页后去重或无确定排序的 `LIMIT 1` 隐藏快照重复。变更审批提交必须在锁定注册证主记录后检查同证件 `PENDING_APPROVAL` 变更，前端按钮只能同步展示该正式状态，不能替代后端门禁；变更履历状态以变更单 `status` 为准，审批通过显示“已变更”，并从正式变更单返回提交人/时间、审批人/时间、批件和批准日期。验证必须覆盖同一版本多快照仍只返回一行、待审批重复提交不新增事实、履历字段可追溯及前端入口受限。
- 注册证业务时间模拟与生效任务边界: 测试能力只能模拟注册证模块的业务日期，不能修改操作系统、数据库、Redis 或其它模块时间；操作、审批、审计和任务记录的时间仍使用真实时间，同时记录模拟日期。测试入口必须限定在本机/测试环境、专用测试租户、专用权限和任务自有测试证件，并由后端重复校验，不能靠前端隐藏。续证生效扫描必须复用正式激活服务且与提醒/通知阶段独立；手工触发九点任务只能调用正式任务入口，不得复制一套测试算法。模拟日期达到 `effectiveDate` 时必须完成新旧证原子切换，日期回退不得反向恢复旧证；同一租户、模拟日期和任务类型重复执行必须幂等。
- Blocker: 测试入口可操作正式环境或非测试证件、模拟日期改写全局时钟、审批/审计时间跟随未来日期、提醒角色或通知失败导致生效归档被跳过、日期回退反向修改生命周期事实、或手工按钮绕过正式租户/公司范围/版本身份校验时必须停止。
- Verification: 后端合同必须覆盖测试环境拒绝、测试租户隔离、`effectiveDate = simulatedBusinessDate` 的生效边界、未来日期保持待生效、提醒失败不隐藏生效结果、重复执行无重复生命周期事件/通知、日期回退不可逆和真实审计时间；真实页面只能使用任务自有测试数据验证，禁止用系统时间修改或 API-only 结果代替。
- Blocker: 上传接口仍复用草稿维护/正式化权限、保存后只刷新当前列表、`UPLOAD_CERTIFICATE` 未进入访问请求约束、上传公司仍按公司名称/展示名匹配授权范围、上传弹框按 DCC 项目代码自动覆盖产品名称、上传草稿把手填产品名称强制匹配启用产品主数据、项目代码绑定产品缺失/停用/名称不一致阻断上传、上传链路仍要求 `product_master_id` 或 `registrant_name` 非空、上传日期顺序错误仍透出英文或前端仍提交无效日期、上传草稿把生产方式默认成否/否、委托生产为是但没有受托企业 ID、受托企业使用自由文本绕过 MDM 主数据、审批候选仍按访问申请权限或公司范围解析、拥有注册部经理角色及上传审批权限的用户仍被 BPM 单一 assignee 报“该任务的审批人不是你”、审批通过正式化继续用审批人复核草稿公司范围、用角色范围绕过申请人草稿复核、通知角色缺少当前活跃注册证所属公司范围、用吞掉通知异常或写用户公司授权代替补齐通知角色公司范围、审批前草稿出现在正式列表、审批通过后的待首次生效首证不在当前列表、同一注册证并发形成多条开放延续审批、待生效行仍显示延续入口、审批候选为空，或审批通过未绑定注册证文件时必须停止。
- Verification: 前端静态合同锁定上传弹框字段、上传 API 提交 `companyId` 而不是 `companyName`、产品名称手填、DCC 项目代码选填且不调用 MDM 产品接口覆盖产品名称、日期顺序保存前拦截、生产方式互斥校验、受托企业候选接口和保存后进入审批中心待办全局视图，且不得强制使用 DCC 文控筛选隐藏 Native BPM 注册证待办；后端回归覆盖 `UPLOAD_CERTIFICATE` 约束、上传公司按申请人授权 `companyId` 校验、无项目代码、未绑定项目代码或已绑定项目代码时上传草稿均保留手填产品名称且 `product_master_id` 为空、日期顺序错误码中文提示、上传草稿保留生产方式和受托企业 ID、上传审批权限候选直接按注册部经理角色取人、注册部经理角色用户可接管并审核指派给他人的 `UPLOAD_CERTIFICATE` 待办、非上传注册证待办仍保留 BPM 原 assignee 校验、审批通过调用首证上传正式化且不创建访问授权、待首次生效首证进入当前列表，并断言正式化审计操作者为注册部经理、草稿范围复核人为上传申请人；延续回归必须同时覆盖未来日期保持 `PENDING_EFFECTIVE`、当天/过期日期立即切换为 `CURRENT` 且新当前版本可再次发起延续；审批标题回归必须同时断言 `RENEWAL_CERTIFICATE` 显示“注册证延续审批”以及 `UPLOAD_CERTIFICATE` 继续显示“注册证上传审批”；schema 合同必须覆盖 `dcc_registration_certificate.product_master_id` 和 `dcc_registration_certificate_snapshot.registrant_name` 均可为空，通知角色范围合同必须覆盖从 `registrationCertificateReminderDailyJob` 的 `roleIds` 读取角色、只按当前 `ACTIVE` 注册证所属公司补齐 `mdm_role_company_scope`、不硬编码角色 ID、不中断于历史 `VOIDED` 证书；真实运行库漂移时必须用正式迁移修复后再复验上传；真实 E2E 必须覆盖 admin/注册部经理角色审核原指派人为其他用户的上传待办并进入注册证列表，并至少覆盖一个由通知角色范围迁移补齐的所属公司。
- Forbidden action: 禁止用旧草稿按钮、API-only 成功、toast、列表刷新、默认审批权限、按公司名称反查授权公司、按产品名称猜主数据、强制补建产品主数据、只改后端英文文案不做前端保存前日期拦截、默认否/否生产方式、自由文本受托企业、SQL 直改 BPM assignee、SQL 直改状态、全局放开 BPM 审核人校验、吞掉通知失败、把通知角色范围缺失改成用户授权公司、空文件成功或绕过正式审批接口冒充上传审批闭环。
- Evidence: `doc/tasks/20260828-registration-certificate-upload-approval-simplify/verification-report.md`；`doc/tasks/20260829-registration-certificate-upload-production-fields/bug-regression-evidence.md`；`doc/tasks/20260830-registration-certificate-upload-flow-verification/bug-regression-evidence.md`；`doc/tasks/20260830-registration-upload-optimization/verification-report.md`；`doc/tasks/20260830-rename-registration-certificate-approval-flow/verification-report.md`；`doc/tasks/20260831-registration-renewal-approval-title/verification-report.md`；`doc/tasks/20260831-registration-renewal-approval-pending-conflict/verification-report.md`；`doc/tasks/20260831-registration-certificate-chinese-prompts/verification-report.md`；`doc/tasks/20260901-approval-flow-reviewer-display-sync/verification-report.md`；`doc/tasks/20260902-registration-change-system-exception/verification-report.md`；`doc/tasks/20260902-registration-list-audit-conflict-after-change-approval/verification-report.md`；`doc/tasks/20260902-registration-change-single-row-pending-guard/verification-report.md`。

## 注册证旧证直接查看权限门禁

- Trigger: 老证/旧证列表新增「查看」、注册部经理直接查看、`assertOldViewAllowed`、`REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID`、`dcc_registration_certificate_approver`、`申请查看`。
- Preflight check: 先区分「申请查看授权 grant」和「注册部经理直接查看」两条入口；页面新增角色按钮时，后端旧证详情访问策略必须同步读取正式角色 code，并继续读取正式证书与公司范围。
- Blocker: 注册部经理按钮只在前端显示但后端仍要求 grant、后端放行时跳过公司范围、普通用户被扩大为直接查看、或直接查看替代/删除申请查看流程时必须停止。
- Verification: 前端静态合同锁定「详情 / 查看 / 申请查看」三个入口、角色按钮和紧凑操作列；后端回归覆盖注册部经理无 grant 可看、注册部经理跨公司仍拒绝、普通用户仍需有效 grant；相邻旧证详情和文件交付测试必须通过。
- Forbidden action: 禁止用前端隐藏、超级管理员硬编码、默认成功、空 grant 兜底、放宽公司范围或删除申请查看流程来满足直接查看。
- Evidence: `doc/tasks/20260830-registration-old-cert-manager-view/verification-report.md`。

## 业务审批策略按配置执行门禁

### 表单模板升版/作废审批模式以 published 策略为准

- Trigger: 表单模板导入升版、作废审批、`FORM_TEMPLATE_UPGRADE`、`FORM_TEMPLATE_OBSOLETE`、`form-template-upgrade-v1`、`Form template upgrade requires BPM approval`、业务审批策略切换 DIRECT/SIGNATURE_REQUIRED。
- Preflight check: 先核对 `bpm_business_approval_policy` 中目标 executor 的 published 策略模式；`DIRECT` 必须直接执行 executor 的直接生效逻辑，`BPM_REQUIRED` 必须有对应流程 key（升版 `form-template-upgrade-v1`、作废 `form-template-obsolete-v1`）并启动 BPM。
- Blocker: DIRECT 仍启动 BPM、BPM_REQUIRED 未启动 BPM、BPM_REQUIRED 流程 key 为空或错误、seed 强行改写已发布 DIRECT 策略、或回归只能靠手工改库时必须阻塞。
- Verification: 运行 `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test`、`python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py script/tests/test_form_template_obsolete_bpm_policy_seed.py`，并复验 BPM_REQUIRED orchestrator 相邻测试。
- Forbidden action: 禁止把 DIRECT 当成降级或绕过强行拦截；禁止把 BPM_REQUIRED 静默直通、默认成功、前端隐藏错误、手工 update 单条数据或 seed 覆盖用户显式策略。
- Evidence: `doc/tasks/20260727-form-template-approval-mode-respects-policy/verification-report.md`。

### 业务审批策略默认视图必须用顶层开关白名单

- Trigger: 业务审批策略列表、`approvalSwitchScope`、默认显示 102 条、可开关审批业务、文控/表单/批记录审批开关、`policyMode=BPM_REQUIRED` 过滤过窄。
- Preflight check: 默认“可开关审批业务”视图必须按顶层业务 effect executor 正向白名单过滤，例如 DCC 上传/发布/作废、表单模板升版/作废、工艺路线版本发布、批记录版本发布、eDHR 批次提交审核和批次作废；页面的 `policyMode`、对象类型等筛选只能在该范围内继续过滤。
- Blocker: 默认视图用 `policyMode=BPM_REQUIRED` 导致关闭审批的 `DIRECT` 策略不可见，或只排除 `EDHR_ROUTE_FORM` 等少量明细导致表单实例、路线附件、路线表单填写等明细策略仍大量出现时必须停止。
- Verification: 后端 Mapper 回归必须同时插入顶层策略和同对象类型明细策略，断言 `approvalSwitchScope=true` 只返回白名单执行器且保留 `DIRECT`；前端静态契约必须断言默认传 `approvalSwitchScope: true` 且 `policyMode` 不默认等于 `BPM_REQUIRED`。
- Forbidden action: 禁止用对象类型泛匹配或“排除几个噪声类型”替代顶层执行器白名单；禁止把关闭审批的 DIRECT 策略隐藏；禁止把业务策略列表误当 BPM 流程定义列表。
- Evidence: `doc/tasks/20260804-bpm-policy-default-bpm-required/verification-report.md`。

### 工艺路线表单策略动作码必须长度安全

- Trigger: 工艺路线版本发布、表单槽位保存、`formBindingKey`、`routeFormActionCode`、`submit-publish` 返回 `Data too long for column 'action_code'`。
- Preflight check: 路线表单槽位发布前必须核对发布投影生成的审批策略动作码长度；当前动作码形态为 `EDHR_RF_<routeVersionId>_<formBindingKey>`，必须落在 `bpm_business_approval_policy.action_code` 列长内。批量补齐 PI/LOSS 等路线表单时，应使用稳定短 key，例如按当前路线工序生成 `PI_<routeProcessId>`、`LR_<routeProcessId>`，并在发布前计算最大动作码长度。
- Blocker: 保存成功但发布失败、长 `formBindingKey` 拼接后超过 `action_code` 列长、或无法证明动作码长度安全时必须停止，不得改 schema、直接 SQL 写库、重放发布或把 HTTP 500 当作偶发失败。
- Verification: 先只读确认 DRAFT 版本绑定计数和最大动作码长度，再通过正式保存接口修正 key，重新读取 blockers，最后执行正式 `submit-publish` 并用 API 复核最新 ACTIVE 版本及各槽位计数。
- Forbidden action: 禁止未确认来源就修改 `action_code` schema；禁止保留自动生成长 key 后反复发布；禁止用 SQL 直接改 published 状态、policy 行或 formBindings 冒充正式发布。
- Evidence: `doc/tasks/20260810-pressure-pump-bind-inspection-loss/verification-report.md`。

## eDHR 放行负责人来源门禁

### 工序结束放行负责人必须来自 RELEASE_APPROVE

- Trigger: eDHR 放行负责人、放行预检、放行审批、电子签名放行、`releaseOwnerLabel`、`RELEASE_APPROVE`、`CLOSE`、工艺路线“工序结束 > 放行责任人”。
- Preflight check: 同时核对路线级 `RELEASE_APPROVE` 规则、候选人解析结果、工作台 `releaseSummary` 和正式放行授权；展示与授权必须共用正式放行候选来源，不能只看 `stageOwnerRole` 或关闭负责人。若当前业务要求新增“管理者代表”权限角色，则最终放行候选必须从该角色解析；首版明确授权用户为 `xujianhai`，即使底层复用 `RELEASE_APPROVE` 待办类型也不能沿用旧候选来源。
- Blocker: 只配置 `CLOSE` 未配置正式最终放行候选、`RELEASE_APPROVE` 或管理者代表候选池为空、用户/角色无效、管理者代表角色未授权 `xujianhai`、或运行态仍显示“执行人”时必须停止，不得把关闭负责人、当前登录人、静态阶段角色或 `stageOwnerRole` 当作放行负责人。
- Verification: 后端回归覆盖 USER、ROLE_GROUP、角色成员可放行、关闭负责人不能越权、管理者代表角色候选解析、`xujianhai` 正向授权和缺失配置 fail-fast；前端静态契约覆盖放行预检/审批阶段读取正式放行候选展示且不兜底 `stageOwnerRole`。
- Forbidden action: 禁止新增数据库迁移修历史数据、禁止把 `CLOSE` 规则复用为放行授权、禁止前端用“执行人/QA/放行员”掩盖未配置、禁止吞掉候选人解析异常。
- Evidence: `doc/tasks/20260727-edhr-release-owner-from-end-config/verification-report.md`。

### 活跃订单申请放行资料必须只使用正式来源

- Trigger: 生产组长活跃订单“完成/完工/申请生产放行”、`active-order/release/apply`、生产进度 100%、检验进度 100%、批记录回填、过程检验单回填、损耗单回填、批次执行创建、来料检文件、灭菌文件、成品检文件、管理者代表批记录放行。
- Preflight check: 后端必须作为权威门禁核对当前用户生产组长负责范围、活跃订单生产进度和检验进度均为 100%、发布态路线快照、逐工序正式 BATCH 批记录绑定、过程检验汇集确认明细、生产工单与领料单正式对应、损耗事实、管理者代表新权限角色和申请幂等键。一线生产、一线 PQC 签名提交以及生产组长、PQC 组长复核都只形成正式来源事实，不得触发最终表单回填；只有活跃订单点击完成时，才在同一业务节点统一执行批记录回填、过程检验单回填和损耗单回填。批记录来源只能来自工序设置逐工序 BATCH 绑定、`RECORD_CATEGORY_BATCH_RECORD`、一线生产事实、生产工单和领料单；过程检验来源只能来自已确认的 PQC 汇集明细，过程检验设备字段只能使用提交/汇集明细中的 `selectedEquipmentId/Code/Name/Number` 快照；损耗单只在一线生产存在损耗时写入，无损耗时不得生成空损耗单或零损耗报告。三类回填成功后才允许创建或复用批次执行，并把一线生产、生产工单、领料单、一线 PQC 和损耗来源映射到批次执行及对应资料。批次执行创建后必须完成来料检、灭菌、成品检三类文件上传；若当前系统把成品检拆成“成品检报告/成品检记录”两个节点，则两个节点都属于成品检文件齐套要求。三类文件全部上传成功后，才允许创建或通过管理者代表批记录放行；管理者代表角色首版授权 `xujianhai`。
- Active order add rule: 生产组长加入活跃订单时，生产工单是唯一硬前置；领料单或其它单据存在时只能作为附加来源，不得成为必填门禁。没有领料单时，后端仍应返回可追溯的活跃订单上下文，只是不绑定领料单来源。
- Frontline pick-list consistency extension: 一线生产输入物料批号查询必须复用完工冻结同一正式领料单发现与完整性校验口径，即按活跃订单工单编号匹配全部 `productionOrderNo`、逐张校验已审核表头和明细来源身份；只读查询不得提前建立绑定，但也不得返回空列表、未审核批号或跳过不完整领料单来让生产先提交。输出物料不查询领料批号，只生成用户填写完成数量、损耗和进度的正式事实。
- Loss-source seam: 损耗来源若要从生产填写链路之外接入，必须通过独立 reader/port 接口承接；现有 reader 接口可以作为正式接入口继续演进，不得把写损耗报表的 writer 直接耦合到固定生产 payload 形状。
- No-loss fact closure: 无损耗不是“没有损耗单”就算完成；每个工序必须能从正式生产反馈、生产提交事件、分配记录和生产组长 APPROVED 复核读取唯一闭环。生产提交必须指向 `MES_PRO_FEEDBACK`，raw payload 必须有结构化 `lossDetails`，无损耗时为 `[]`；分配记录的 `reviewId` 和 `confirmedAt` 必须对齐对应生产组长复核的 ID 与 `reviewedAt`，否则 Flow4 completion receipt 必须阻断为 `LOSS_CONDITION_FACTS`。
- Nonconformance freeze extension: PQC 放行申请发起不合格评审时，必须在同一事务锁定申请和正式生产工单，保存工单冻结前 `temporary_frozen` 快照后再冻结。所有新增生产报工以及领料出库提交、拣货、完成入口都必须锁定工单并检查正式冻结状态，不能只在 PQC 放行按钮处检查评审单。让步放行和返工按快照恢复原冻结状态，作废保持冻结；返工和作废还必须用版本 CAS 终结放行申请并完成 PQC 待办，让步放行继续保留 PQC 电子签名节点。若历史待处置评审缺少可审计的冻结前快照，迁移必须 fail fast，禁止推断为未冻结或直接解冻。
- Simulation preflight extension: 多阶段放行模拟在创建任务自有工单、生产/PQC 事实或库存前，必须只读确认目标产品正式路线的批记录版本已批准，所有需要批记录的 `MAIN` 绑定均有正式 definition/version ID 和启用字段映射，实际生成 PQC 任务的工序均有正式过程检验绑定；预检失败时直接返回 blocker，禁止先执行整套业务写入后才发现版本 `PRECHECK_FAILED`，也禁止由模拟服务修补路线主数据。
- Blocker: 进度不足、非当前组长负责范围、一线生产或 PQC 复核事实缺失、一线输入批号与完工冻结领料单口径不一致、生产工单与领料单未正式对应、完成节点前已写入最终批记录/过程检验单/损耗单、回填前已创建正式批次执行、缺正式批记录绑定、过程检验槽位只有动态表单模板而无传统 `batchRecordReportId`、PQC 汇集未确认或无结构化明细、过程检验设备字段反查 QA 版本设备或当前最新租户设备配置、有损耗但损耗单正式映射未证明、无损耗却生成损耗单、来料检/灭菌/成品检文件任一未上传成功、缺管理者代表角色或 `xujianhai` 授权、幂等冲突、eDHR 批次或放行事务无法持久化时必须返回 blocker 或 fail fast，不得创建不完整资料或提前放行。
- Verification: 后端静态/单元回归至少覆盖成功、进度不足、非当前组长、一线生产/PQC/组长复核不触发回填、一线输入批号查询与完工冻结共用全部正式领料单校验、活跃订单完成节点一次性回填批记录和过程检验单、PQC 提交后 QA 版本设备或租户级设备配置变化时放行仍使用提交/汇集设备快照、有损耗时回填损耗单、无损耗时不写损耗单、回填后才创建批次执行、正式来源缺失、来料检/灭菌/成品检文件缺任一份阻塞、管理者代表候选为 `xujianhai`、重复申请幂等和负责人缺失；schema 测试锁定申请表唯一键、状态字段、来源快照和权限码；前端静态合同覆盖双 100% 完成按钮、确认文案、刷新和 blocker 展示；真实 E2E 必须使用任务自有双 100% 活跃订单、正式生产/PQC/领料来源、三类上传文件、`xujianhai`、签名、正式模板和可清理数据。

### eDHR 四份材料必须绑定正式来源快照

- Trigger: 四份放行材料、来料检报告、灭菌报告、成品检报告、成品检记录、MATERIALS_READY、sourceSnapshotHash、routeBindingSnapshotHash、Flow 7 来源变化、材料门禁异常、`STAGE4_INDEPENDENT_BATCH_EXECUTION`、`stage4IndependentBatchExecutionSnapshot.v1` 或 trace manifest 缺失。
- Preflight check: 创建四个材料任务时，必须从已校验的 Flow 6/Flow 7 正式来源凭证取得 sourceSnapshotHash，并分别持久化到专用材料任务来源快照字段；门禁只允许将该字段与 Flow 7 Origin/TraceLink 预检返回的来源快照比较。routeBindingSnapshotHash 只能表示路线/表单绑定配置，不得作为材料来源证明。Stage4 或预放行资料上传只能完成四个材料节点并写 release report evidence，不得复用会触发 `createNextFillAfterSpecialNodeResolved` 的普通特殊节点完成路径。若阶段被明确要求独立于上一阶段运行，必须由请求显式携带独立 input mode，先持久化完整批次执行 fixture，再从稳定版本快照的 Stage4 projection 提取材料输入，并同步生成正式 trace origin、trace links 和 manifest。
- Blocker: 材料任务缺少专用来源快照、来源快照与 Flow 7 不一致、Flow 7 来源预检缺 Origin/TraceLink、仅有路线绑定快照而没有正式来源快照、独立 fixture 缺工单/活跃订单/物料/完成回执/回填/供数记录或 trace manifest、或四份材料上传后开始创建下一普通工序填报任务时，必须阻断 MATERIALS_READY 和后续放行；历史任务不得通过默认值、路线配置快照或旧附件自动补齐。
- Verification: 后端回归必须覆盖四个材料任务创建后来源快照一致、Flow 7 来源变化后进入 MATERIALS_RECHECK_REQUIRED、专用来源快照刷新后才恢复 MATERIALS_READY，以及 routeBindingSnapshotHash 与来源哈希相同但专用字段为空时仍阻断；迁移必须以幂等 schema 合同验证专用列存在。Stage4 回归必须覆盖真实 Stage2.5 批次、显式独立 input mode、完整 fixture 快照抽取、四份材料上传、重跑只清理自身附件、`sourceSnapshotHash` 传递、独立 fixture 的 trace origin/link/manifest、未创建最终放行记录，以及未触发 Stage5 或普通工序推进。
- Forbidden action: 禁止把路线/表单配置哈希、附件哈希、批次号、当前最新来源查询结果或前端字段拼接当作已冻结材料来源；禁止为兼容历史任务写默认快照、跳过来源预检、用旧附件版本绕过门禁，或用 `completeSpecialNode` 冒充预放行资料上传；禁止在 Stage2.5 校验异常后通过 catch 隐式切换到独立 fixture。
- Evidence: doc/tasks/20260824-flow8-four-material-gate/；IntRuoyiBackend/sql/mysql/20260826_mes_edhr_material_task_source_witness.sql；doc/tasks/20260829-stage4-dossier-upload-improvement/verification-report.md；doc/tasks/20260829-stage4-independent-fixture-input/verification-report.md。

### eDHR 批次创建入口必须与正式入口合同一致

- Trigger: eDHR 批次创建弹窗、`/open-or-create`、`entryType` 缺失或“批次入口缺少 entryType”。
- Preflight check: 前端只能调用与业务入口匹配的正式接口，并提交该入口实际生成的 `entryType`、`entryBusinessId`、来源凭证、来源上下文哈希和幂等键；`MANUAL`、`PQC_INDEPENDENT`、活跃订单完成或排产入口的凭证链路必须分别由正式来源生成。只有工单、路线、批次号和备注的旧弹窗不能直接调用受 Flow 9 合同保护的创建接口。
- Active-order completion handoff: Stage2.5 或其它活跃订单完成后的建批入口只能把服务端已持久化 receipt 的 `sourceCredentialId`/receipt hash/source snapshot hash 交给 Flow6，由 Flow6 自己读取权威 receipt；禁止把完整 completion receipt 嵌套在请求体里传回建批入口。活跃订单来源场景的 `sourceContextHash` 必须使用 receipt 的 `sourceSnapshotHash`，不得使用本地重新拼出的 context hash 或请求端字段。
- Schedule boundary: 当前排产/手动重排与批记录创建是两条分开的业务；批记录只能由正式批记录入口（例如生产组长加入活跃工单后的批记录链路）生成，不得在排产完成后自动创建 eDHR 批次、自动签发 `SCHEDULED_BATCH` 凭证，或让排产提交被 eDHR 凭证幂等冲突阻断。若 `replanApply` 仍然调用 `getScheduleCompletionMissingItems` / `openOrCreateFromScheduleCompletion`，或报出“排产完成创建 eDHR 批次缺少前置条件：首任务责任来源/候选池”，就说明重排链路又误连回批记录创建，必须在排产提交分支里移除这条 eDHR 路径。未来若要恢复排产触发批记录，必须先定义正式入口合同、迁移策略和回归/E2E 证据。
- Blocker: 禁止在前端填固定 `entryType`、空字符串、默认来源 ID、伪造 receipt 或将 `formBindings`、工序开始配置、页面文案当作批次入口来源；缺少正式人工入口凭证服务时必须阻塞并明确提示，而不是继续请求后端。
- Verification: 前端合同测试必须同时锁定入口类型与完整正式字段；后端入口合同测试必须覆盖缺字段、场景不匹配和正式凭证校验；真实页面验证必须证明请求来自对应业务入口并记录实际写请求。
- Forbidden action: 禁止用 `formBindings`、默认 `MAIN`、工序开始配置、当前登录人、空资料、mock、直接 SQL、API-only、默认成功、吞异常、直接调用负责人电子签名放行、在一线提交或组长复核节点提前回填、回填前创建批次执行、无损耗生成空损耗单、三类文件未齐套预检通过、或把旧 `RELEASE_APPROVE` 候选当管理者代表来替代正式生产放行与批记录放行链路。
- Historical data gate: 切换到“活跃订单完成统一回填后创建批次执行”前，必须盘点旧流程在回填前或 PQC 审批前已创建的批次执行。只有与同一有效完成申请、正式回填证据和幂等键存在正式关联的执行才允许作为重复请求结果复用；无有效完成/回填关联的旧执行必须返回迁移 blocker，并由经批准的数据修复方案处理。回归至少覆盖“旧执行无完成回填关联时不认领、不复用、不自动删除”。
- Interface contract gate: 多阶段流程允许上下游并行开发前，必须冻结唯一状态所有者、可持久化状态与迁移审计编码的区别、同步内部端口、HTTP 请求/响应、Long ID JSON 类型、权限候选来源、事务回滚、幂等键、乐观版本、网络不确定回执和结构化 blocker。下游初始化必须与当前命令同事务；审计事件不得替代下游触发。若框架通用异常响应会丢失 blocker data，或后端/前端 Long ID 序列化类型不一致，必须先补正式响应适配器和提供方/消费方合同测试，禁止用成功码包失败、中文 msg 分支、建议接口或“状态/事件二选一”放行并行开发。
- Exception-handler gate: 领域阻塞异常若继承普通 RuntimeException，必须在实际控制器入口验证异常处理优先级；全局 `@RestControllerAdvice` 可能先将结构化 blocker 改写为 HTTP 500“系统异常”。申请/回执接口必须由控制器级处理器或框架正式适配器返回非零业务码与完整 blocker data，并覆盖真实 HTTP 回归，不能只测试 advice 方法本身。
- Shared schema ownership gate: 多阶段流程共用同一申请表、工作待办或聚合版本时，必须指定唯一迁移所有者并冻结字段类型、索引、历史数据预检和迁移顺序；消费阶段可以按 DTO/内部端口并行开发，但不得各自新增同名字段、平行关联列或工作待办版本列。复用多业务作用域表时，唯一约束只能命中目标作用域；必要时使用条件生成列，禁止用覆盖所有作用域的宽泛唯一索引破坏多签字格、归档等既有任务。
- Version ownership gate: 每个写命令必须明确 `expectedVersion` 属于哪个聚合以及查询响应如何投影该版本。工作待办没有正式版本列时，只能从受唯一关系约束的申请或放行事务读取；解析不到唯一聚合必须报数据完整性 blocker，不得用更新时间、默认 0 或其它聚合版本替代。临时上传不改变正式证据时可以只校验并原样返回版本，正式节点完成才递增。
- Interface verification: 合同测试同时覆盖字段集合、ID JSON 类型、状态前置、相同幂等键同载荷回执、同键不同载荷冲突、版本归属/冲突、共享迁移唯一所有者、条件唯一索引不影响其它业务作用域，以及下游初始化失败整事务回滚；未执行的 RED/GREEN 只能记录 expected 结果，不得预写 PASS。
- Evidence: `doc/tasks/20260808-active-order-release-dossier-implementation/verification-report.md`；`doc/tasks/20260814-active-order-release-flow-docs/verification-report.md`；`doc/tasks/20260814-sp2-pqc-release-batch-execution/verification-report.md`；`doc/tasks/20260814-release-flow-interface-contract-optimization/verification-report.md`。

## MES 排产重排保护范围门禁

### 已取消历史任务不得参与受保护任务冲突

- Trigger: 手动重排、排产预览、`PROTECTED_TASK`、提示“同一工单工序存在多个受保护任务”、同一生产工单同一工序存在多条 `CANCELED` 历史任务、用户选择当前订单后被历史取消任务阻断。
- Preflight check: 修改排产重排保护规则前，先区分工单状态与任务历史状态；工单自身已取消或已完成时必须在工单层阻断参与排产，历史 `CANCELED` 任务只保留审计，不得进入当前重排范围、受保护任务分组、替换/删除候选或前后序连线计算。`FINISHED`、`IN_PROGRESS`、`LOCKED`、`MANUAL` 和已有报工事实仍按正式保护规则处理。`mes_pro_task_schedule_ext` 记录若已软删或不存在，不能默认按 `MANUAL` 手动保留处理；只有 ext 明确存在且来源不是 `AUTO` 时才可进入手动保留分支。
- Blocker: 若已取消历史任务仍能触发 `PROTECTED_TASK` 多保护冲突、被作为可替换任务删除、被重新连线影响当前计划，或其它订单因同工序存在取消历史任务被阻断，或软删/缺失扩展记录被误判为手动保留，必须停止并补后端回归；不得用前端隐藏原因、清数据库、跳过保护检查或默认可排来掩盖。
- Verification: 后端回归必须同时覆盖“同一工单同一工序存在多个已取消历史任务时不阻断且能生成当前计划任务”和“同一工单同一工序存在多个已完成历史任务时仍阻断”；定向命令需记录 RED/GREEN，完整排产算法合同测试需通过。
- Forbidden action: 禁止把 `CANCELED` 当作强保护终态，禁止为了重排先物理删除历史任务或直接改任务状态，禁止放宽所有终态保护导致已完成任务被重排覆盖，禁止只按工序名称跨订单扫描历史任务。
- Evidence: `doc/tasks/20260828-replan-cancelled-task-protection/verification-report.md`。

## 第三方报工直报正式链路门禁

### 导入成功必须落到正式报工而不是直接进度

- Trigger: 第三方报工、李萍报工单、直接报工 Excel、`importDirectWorkReportWorkbook`、`DIRECT_WORK_REPORT`、导入结果弹框显示成功但正式报工列表无新增、排产进度疑似未增长、排产员工作台工序列表班次产能为 0、点击重排提示“排产资源缺少班次小时配置”。
- Preflight check: 先确认导入成功路径是否创建 `MesProFeedbackDO`、设置 `sourceImportRecordId`、回写导入记录 `feedbackId`、调用正式提交服务，并由正式报工状态参与排产进度汇总；第三方报工或已完成任务在手动重排里只用于计算已完成量和剩余量，不能用旧任务 `mes_pro_task.workstation_id`、旧产线或历史排产快照决定剩余工序怎么排；若工作台班次产能为 0，必须按当前路线工序 `process_id` 核对启用未删除工作站、工作站设备绑定和 `mes_dv_machinery_process` 小时产能，而不是只按工序编码找旧工作站；工作站 `shift_hours` 为空、非正数或多个当前可用工作站班次小时不一致时，手动重排必须按 `scheduleDefaultCompatibilityPolicy.defaultShiftHoursWhenMissing()` 的默认 `10.5` 小时计算，不得把缺班次小时误判为旧任务资源 blocker；手动重排后验证资源落库时，必须通过 `mes_pro_task_schedule_ext.schedule_order_id -> mes_pro_task.workstation_id` 核对新生成任务的实际资源，不能只看可能未回写的历史 `mes_pro_schedule_order_process.workstation_id` 快照。
- Blocker: 若缺少报工人、审批人、唯一未完成任务、排产工序剩余数量、正式路线工序快照、当前 `process_id` 工作站、产线或设备工序产能，不得写 `progressSourceType=DIRECT_WORK_REPORT` 或直接改进度/班次产能伪造成功；`FEEDBACK`/`FINISHED` 进度事实任务缺旧工作站或旧产线不是手动重排 blocker，真正需要阻断的是当前工艺路线剩余工序缺少可用工作站、产线或产能；工作站班次小时缺失不是 blocker，应按默认 `10.5` 计算；`IN_PROGRESS`、`LOCKED`、`MANUAL` 等仍需要作为真实受保护任务校验其现有资源。必须返回结构化跳过原因或 fail fast。
- Verification: 后端回归必须同时覆盖匹配行创建/提交正式报工、缺用户跳过、重复导入再次正式报工、超剩余跳过、导入后反馈/已完成任务只扣减剩余量且剩余任务按当前工艺路线资源生成、缺班次小时默认 `10.5` 且不掩盖缺工作站/缺产能；前端静态合同需确认导入确认后刷新正式报工列表并广播受影响排产工单刷新 payload，真实 E2E 需至少覆盖一次第三方报工导入后的手动重排预览或应用；`MesProAutoScheduleServiceImplTest#replanApply_shouldSkipEdhrBatchCreationAfterScheduleComplete` 必须通过，证明重排应用不再调用 eDHR 批次创建前置校验；跨环境补工作站数据后必须复验工作台目标工序 `shiftCapacityTotal` 为非 0 且资源链路行数可追溯；重排应用后必须记录排产工单计划时间、`mes_pro_task_schedule_ext` 任务数、空/失效工作站数、覆盖工作站数和最近一次重排快照。
- Forbidden action: 禁止用导入记录直接进度、前端假新增、默认成功、空列表刷新或 API-only 结果替代正式报工持久化链路。
- Evidence: `doc/tasks/20260801-third-party-feedback-import-list-progress/verification-report.md`；`doc/tasks/20260802-test-server-replan-protected-task-workstation/verification-report.md`；`doc/tasks/20260802-test-server-replan-shift-hours-duration/verification-report.md`；`doc/tasks/20260806-replan-current-route-after-feedback/verification-report.md`；`doc/tasks/20260806-replan-shift-hours-default-regression/verification-report.md`；`doc/tasks/20260828-schedule-replan-all-worktree-e2e/verification-report.md`。

### 生产组长报工管理造数必须补齐工序池时间线

- Trigger: 生产组长报工管理随机数据、`team-leader/submission/page`、`MesTeamLeaderWorkbenchService.getSubmissionPage`、`MesProProcessPoolTimelineReadMapper`、`actualEmployeeUserName` 为空、员工列显示用户编号或 `964`、只写 `mes_pro_feedback` 后组长页面无新增。
- Preflight check: 先确认页面读模型按 `mes_pro_process_pool_event.server_submit_time`、`actual_employee_id` 和生产组长责任员工集合筛选；时间线 mapper 必须按 `pool_event.actual_employee_id`、`tenant_id`、`deleted` 关联正式身份来源：正式员工按 `system_users.id/system_user_id` 读取系统用户昵称或生产人员档案姓名，临时员工按人员档案 `id` 读取档案姓名，并返回 `actualEmployeeUserName`；造数必须同时补齐正式报工、记录本 entry/event、工序池 `PRODUCTION_SUBMIT` 事件、数量片段和 `mes_pro_process_pool` 汇总，并核对员工在目标生产组长的 `EMPLOYEE` scope 内。
- Blocker: 只有 `mes_pro_feedback` 而缺工序池事件、记录本或数量片段，`actual_employee_id` 不在当前生产组长责任范围，缺 `route_process_id/process_id/work_order_id/task_id` 正式链路，正式员工或临时员工身份均无法按对应键解析 `actualEmployeeUserName`，mapper 用空值掩盖来源缺失，前端把 `actualEmployeeUserId` 当员工显示文案，或只能用 admin 登录态看到数据时必须停止，不得宣称生产组长报工管理可见。
- Verification: 用数据库只读 SQL 同时断言报工、工序池事件、记录本 entry/event、数量片段计数和两类身份解析：正式员工 `actual_employee_id -> system_users.id/system_user_id -> nickname/档案姓名`、临时员工 `actual_employee_id -> profile.id -> 档案姓名`；再使用生产组长本人登录态请求 `/admin-api/mes/pro/process-pool/team-leader/submission/page?leaderType=PRODUCTION&submitDate=<date>`，按事件 ID 或任务标识断言命中新增数据且 `actualEmployeeUserName` 非空；静态合同锁定 mapper 不得返回空姓名、前端不得退回显示员工 ID。
- Forbidden action: 禁止用 admin 页面、API-only 非组长账号、前端假行、空列表刷新、直接改工序池汇总、只改报工主表、前端硬编码姓名或显示用户 ID 替代生产组长真实时间线可见性。
- Evidence: `doc/tasks/20260806-production-leader-feedback-random-data/verification-report.md`；`doc/tasks/20260806-team-leader-employee-name/verification-report.md`。

### 一线生产正式提交必须单事务落链并按唯一组长归属可见

- Trigger: 一线生产填写页“提交”改为正式提交、重复点击、提交前预校验、无设备工序提示“当前工序缺少正式设备配置”、设备参数缺失、员工无生产组长归属或多组长归属、电子签名提示“当前登录账号必须是实际填写员工”、提交成功但对应生产组长报工列表无记录。
- Preflight check: 前端只做数量、损耗和设备参数的本地提前提示及不可逆确认，确认后只调用一次正式提交接口；后端运行态必须返回服务端解析的 `productionSubmitContext`，其中路线、路线工序、MES 工序、工作站、设备账号、实际员工和生产组长审批人来自正式运行态候选与启用生产人员档案。一线生产不需要匹配任何工单，`workOrderId`、`taskId`、`itemId`、`recordbookId`、`scheduleOrderId` 和 `scheduleOrderProcessId` 可以为空且不得作为运行态或正式提交前置条件；当前工序没有正式记录本上下文时不写 `recordbookPayload`，也不得用默认工单、默认任务、默认物料或默认记录本补齐。PQC 和其它订单级流程仍按各自门禁要求订单上下文。后端必须在写入前按启用生产人员档案确认实际员工只属于一个生产组长；正式提交授权只校验路线、路线工序、MES 工序、实际员工、签名员工和模板，请求仍携带工作站用于提交追踪，但不再用 route-start/post-binding 候选的 `deviceId` 或 `workstationId` 拦截提交；正式提交阶段不执行设备参数校验，不因 `selectedDevice` 缺失、`processPoolContext.deviceId` 与 `selectedDevice.deviceId` 不一致、设备参数缺失/重复/异常或设备参数规则不匹配而阻断。设备端登录账号只代表入口账号，正式签名主体必须是页面选择的实际填写员工：`signatureEmployeeId` 必须等于 `actualEmployeeId`，但不得要求其等于 `loginUserId`；生产提交签名服务必须显式传入该选择员工作为 actor 并验证其电子签名密码。报工、可选记录本原始条目、生产提交签名、工序池 `PRODUCTION_SUBMIT` 事件和正式响应 ID 必须处于同一事务。前端设备卡片必须使用运行态 `devices` 的全量集合，不得通过 `slice(0, 3)`、固定三列但允许设备换行后被单行定高容器裁剪，或其它展示层限制隐藏工序设备。
- Device visibility detail: 运行态 `devices` 的正式可见性由当前生产组长范围内启用的 `mes_pro_process_pool_team_device` 与启用的 `mes_pro_process_pool_team_process_device` 共同决定，参数规则只描述已绑定设备的参数，不能创建设备或替代工序绑定。预期设备缺失时必须依次只读核对班组设备、工序设备绑定、`route_process_id` 参数规则和运行态响应；任一正式层缺失都应补齐对应数据链路，不得在前端按 `deviceCode` 合成设备卡片或业务参数。
- Device parameter rule route detail: DCC 项目代码切换到专属当前路线后，设备参数规则也必须落在该当前路线工序上。若班组设备和工序设备绑定存在但参数为空，先只读核对当前 `routeProcessId` 是否缺规则；缺规则时用正式迁移把源路线或旧路线规则复制到当前路线，不得在运行态 fallback 旧 `routeProcessId` 或按同名工序即时补算。
- Preflight detail: 一线生产运行态和正式提交不得解析、匹配或要求 `productionSubmitContext.activeOrder`、生产工单、生产任务、产品物料或开启记录本；同一路线存在多个 ACTIVE 活跃订单、没有 ACTIVE 活跃订单或没有生产任务时，一线生产仍按当前候选的路线/工序/工作站/员工上下文提交。若未来需要订单级分配、PQC 或工单追溯，必须建独立订单级链路，不得恢复一线生产提交的隐式工单匹配。
- Explicit active-order allocation extension: 当页面已经由用户显式选择活跃订单并进入独立的订单初始分配链路时，放宽工序身份校验前必须先区分“纯拦截条件”和“同时提供目标身份、生产系数、目标数量的解析调用”。路线升级只造成提交 `routeProcessId` 与订单冻结 `routeProcessId` 不同时，经用户明确批准的 MVP 可以按 `activeOrderId + 唯一 processId` 读取订单已有目标快照，但分配、审计、数量片段、订单工序完成量和报工汇总仍必须使用该快照并完整执行；同一订单内 processId 缺失或重复必须失败，禁止任取一条、使用当前路线目标、删除完成量协调或用提交事件工序身份覆盖订单快照。
- Frozen active-order process source extension: 一线生产已经显式选择活跃订单时，工序候选和运行配置必须以该订单锁定的 `routeVersionId`、版本快照和订单工序目标快照为正式来源；不得按订单 `routeId` 查询当前 ACTIVE 路线工序，也不得按相同 `processId` 把新版 `routeProcessId` 套给旧订单。运行配置请求必须携带 `activeOrderId` 并先确认工序属于该订单冻结版本；旧版工作站、设备或参数正式配置缺失时明确失败，不得回退当前新版配置。`routeProcessId + processId` 必须同时存在于订单逐工序快照和锁定路线版本节点中，两边身份集合、编码或名称冲突时必须 fail-fast。展示编码和名称只能来自订单逐工序快照或锁定路线版本快照；两份冻结数据同时缺失时必须暴露坏数据，禁止查询当前工序主数据、当前 ACTIVE 路线节点或同名工序补算历史。PQC 任务的 `routeProcessId + processId` 也必须属于同一订单冻结工序快照。工艺路线升级只影响升级后新加入活跃池的订单，既有订单快照不得被读取链路自动升级或覆盖。Evidence: `doc/tasks/20260817-frontline-active-order-frozen-route-submit/verification-report.md`; `doc/tasks/20260819-route-upgrade-history-order-process-identity/verification-report.md`。
- Multi-material submission detail: 当一个工序按冻结路线产品 BOM 关联多个报工物料时，单行 `mes_pro_feedback` 不能继续冒充逐物料正式事实。运行态与服务端会话快照必须携带同一冻结物料集合；正式请求必须一次提交完整且不重复的 `materialDetails`，每项独立保存物料身份、完成数、损耗、损耗原因、设备和参数快照，任一失败整事务回滚。工序池进度和初始分配取各物料完成数量最小值，不按 BOM 比例折算、不求和；旧主报工仍必须满足 `feedbackQuantity = qualifiedQuantity + unqualifiedQuantity`，其中 qualified 为最小值进度、unqualified 为逐物料损耗合计，禁止让损耗合计从最小值直接相减后产生负合格数量。批号只按生产订单号和物料编码读取系统内同步事实，系统内没有就返回空集合，不直连 ERP、不读取库存替代源、不复制批号到报工事实表。
- Batch-record material config detail: 工序多物料报工的 MVP 配置入口是工艺路线候选版本中当前工序“批记录表单”字段明细下的批记录物料配置，快照字段为 `frontlineReportMaterialIds`。该配置属于路线版本中的路线工序，不属于产品 BOM，不展示或保存用料比例；旧产品 BOM 模块只表达路线产品消耗，不得自动替代、推导或覆盖批记录物料配置。运行态必须按订单冻结路线版本的 `routeProcessId` 精确读取该字段；字段缺失或空数组是合法的“无批记录物料”状态，应返回空集合并继续工序级报工，使用工序完成数量和损耗形成主报工与初始分配，不生成逐物料事实。配置非空时才启用完整多物料提交和逐物料事实；重复、非法或缺主档的配置仍须明确失败，不得回退产品 BOM、当前新版路线或前端静态物料。旧逐物料事实中的 BOM 比例字段在新来源下应为空，禁止伪造默认比例。
- Guard-relaxation verification: 回归必须同时证明精确 routeProcessId 查询不再阻断、分配使用订单快照自身 routeProcessId、完成量协调仍被调用、错误订单/工单仍在写入前失败，并复跑目标快照解析、FIFO和一线正式提交相邻测试。Evidence: `doc/tasks/20260817-frontline-route-process-id-mvp-compat/verification-report.md`。
- Device parameter default detail: 一线运行态设备数值参数必须优先使用正式显式默认值；显式默认值为空且上下限同时存在时，统一以 `(lowerLimit + upperLimit) / 2` 解析运行态默认值。文本标准或任一边界缺失时保持空值，前端在执行 `Number(...)` 前必须显式排除 `null`、`undefined` 和空字符串，不得把空默认值转换成 `0`。
- Signature authorization detail: 生产组长人员管理中的电子签名授权必须按人员身份分流。正式员工档案有 `system_user_id`，签名前必须命中同租户、未删除、`electronic_signature_enabled=1`、`authorization_state=ENABLED` 且未锁定的 `dcc_electronic_signature_authorization`；临时工档案没有 `system_user_id`，只能使用该启用人员档案自己的非空 `signature_password_hash`。批量开通权限前必须分别统计正式员工和临时工，核对正式员工系统用户启用且同租户、临时工签名密码已设置，并为每条系统用户授权写 `dcc_electronic_signature_authorization_audit`；不能把人员档案 ID 当系统用户 ID 写入 DCC 授权表。
- Blocker: 员工无启用组长归属、同时属于多个启用组长、签名员工与实际填写员工不一致、路线/路线工序/MES 工序/工作站/审批人/签名等正式必需上下文缺失、一线生产仍要求或匹配活跃订单/生产工单/生产任务/产品物料/记录本、前端把运行态设备集合截断为前三台或其它固定数量、设备数组已全量但 CSS 固定三列并在单行定高和 `overflow: hidden` 下裁掉第四台、客户端审批人、URL query 或预传 `signatureId` 被当作权威、当前运行 Jar 未加载本次正式链路时必须停止；前端失败后保留输入，不得显示成功或锁定状态。
- Verification: 后端回归覆盖运行态 `productionSubmitContext` 无活跃订单/无工单/无任务/无记录本仍返回生产提交上下文、正式提交可写入空 `workOrderId/taskId/itemId/recordbookId`、选择员工与登录账号不同但签名密码匹配时生成选择员工签名、无归属、多归属、唯一归属、授权工序合法但提交设备/工作站与候选设备/工作站不一致时放行、正式提交服务不调用设备参数校验器、幂等和任一步骤异常整事务回滚；前端静态合同覆盖无设备提交不再被缺设备文案阻断、有设备确认弹窗仍展示并提交设备参数、设备卡片直接展示全部 `configuredDeviceCards`、tab 列数按 `visibleDeviceCards.length` 分配且不保留“最多三台”的相反合同、正式上下文来自运行态且提交只传 `signaturePassword` 不传前端 `signatureId`，并禁止 `signatureEmployeeId === currentLoginUserId`、订单/任务/记录本必填、固定前三台设备截断和 URL query 幂等键这类拦截；同一设备连续报工还必须证明每次确认只发一次正式请求、明确成功后清空本次业务输入并轮换新幂等键、失败或响应不确定时保留原输入和原幂等键、成功结束后可切换另一实际员工和另一工序。真实 Playwright 从生产填写页确认提交时，断言正式接口只发一次、返回报工/可选记录本/签名/工序池 ID、本次正式事实不可修改且页面进入新的独立填写会话，再由唯一对应生产组长本人登录报工列表按事件或任务标识确认可见且其他组长不可见。
- Verification detail: 运行态回归必须覆盖“当前组长没有 activeOrder/workOrder/task/recordbook 时仍返回生产提交上下文”，并复跑员工切换相邻测试，防止选择员工触发运行态刷新后误报 `productionSubmitContext.activeOrder routeId=...`。
- Multi-material verification: 后端必须覆盖冻结批记录物料精确解析、产品 BOM 不参与来源、空配置返回空集合并按工序级数量正式提交且不写逐物料事实、非空请求集合缺失/重复、逐物料损耗守恒、`0` 完成数量、`5/3 -> 3` 最小进度、子项写入失败整体回滚、主报工数量守恒、同步批号单值/多值去重/空集合和其它订单物料隔离；真实页面必须证明空配置不报错且不显示物料页签，有配置时两个页签草稿隔离、页签颜色只由完成数量是否填写决定、一次正式请求携带全部明细，并在清理后证明任务数据残留为 0。
- Device visibility verification: 数据修复先用只读 SQL 分别证明目标设备已启用、只绑定目标工序且具有对应 `route_process_id` 参数，再通过运行态接口和真实页面确认设备卡片完整出现；参数规则存在但设备或绑定缺失不能作为可见性通过证据。
- Device parameter default verification: 回归必须同时覆盖显式默认值优先、完整数值范围生成精确中点、文本标准保持空值、单边范围保持空值，以及前端空值不进入数值归一化；不能只验证某一条清洗功率样本。
- Signature authorization verification: 数据授权任务必须独立证明目标启用人员总数、正式员工系统用户数、临时工人数、临时工签名密码就绪数、正式员工有效 DCC 授权数和剩余缺口；授权事务需断言授权变更数与审计新增数一致，并复跑验证幂等且不产生重复授权或重复审计。
- Forbidden action: 禁止用第二个预校验请求替代事务内权威校验，禁止默认组长、默认工单、默认任务、默认产品物料、默认记录本、公共待认领列表、前端 `approveUserId`、当前登录人替代 `signatureEmployeeId`、URL query 拼接 `taskId/recordbookId/signatureId`、旧运行 Jar、API-only、直接 SQL、恢复 activeOrder 匹配、只写 `mes_pro_feedback` 冒充正式提交闭环，或为了放宽设备/参数校验同时放宽工序、工位、人员、签名和事务校验。
- Device visibility forbidden action: 禁止只插参数规则却不建正式班组设备和工序绑定，禁止以页面硬编码、合成参数、默认设备或其它工序绑定掩盖当前工序设备缺失。
- Device parameter default forbidden action: 禁止由前端重复计算范围中点、用下限/上限单边猜测默认值、让 `Number(null)` 生成 `0`，或用固定常量覆盖显式目标值。
- Multi-material forbidden action: 禁止只改前端页签却仍覆盖同一套草稿，禁止循环调用单物料正式提交接口，禁止用首项、末项、求和或 BOM 比例替代用户确认的最小值，禁止为了容纳多物料损耗放宽主报工非负/数量守恒，禁止从当前路线、名称或库存猜测冻结物料和批号。
- Signature authorization forbidden action: 禁止给临时工伪造系统用户或把 `employee_profile.id` 插入 `dcc_electronic_signature_authorization.user_id`；禁止用电子签名菜单、角色、默认授权、空密码或前端可见状态代替正式员工 DCC 授权或临时工人员档案签名密码；禁止只写授权不写授权审计。
- Evidence: `doc/tasks/20260807-formal-frontline-production-submit/verification-report.md`；`doc/tasks/20260807-frontline-submit-optional-equipment/verification-report.md`；`doc/tasks/fix-electronic-signature-selected-employee/verification-report.md`；`doc/tasks/fix-frontline-active-order-route-id-context/verification-report.md`；`doc/tasks/fix-frontline-production-no-work-order-context/verification-report.md`；`doc/tasks/20260808-frontline-submit-relax-device-param-validation/verification-report.md`；`doc/tasks/20260809-frontline-range-midpoint-default/verification-report.md`；`doc/tasks/20260831-frontline-process-report-material-mvp/verification-report.md`。

### 活跃订单模拟完成必须写正式模拟事实

- Trigger: 生产组长活跃订单“模拟完成”、一键模拟一线生产提交、生产组长复核、一线 PQC 提交、PQC 组长复核、生产进度 100%、检验进度 100%、`active-order/simulate-completion`、`simulationRunId`、Stage 分段模拟闭环。
- Preflight check: 模拟入口只能在用户明确确认“模拟数据”后使用；后端必须先校验活跃订单属于当前生产组长且为 `ACTIVE`，订单冻结路线版本、逐工序快照、生产目标数量、PQC 任务、PQC 计划数量和 QA 检验项目身份完整。模拟完成应沿用正式提交/分配/复核/逐件明细/汇集确认链路写入可追溯事实，并用 `simulated=true`、来源标识、`stageCode`、`simulationRunId` 和复核备注区分模拟数据；生产进度和检验进度必须由正式事实重新计算。分段模拟按钮必须是闭环动作：一次触发内先按上一轮 `simulationRunId` 清理本段模拟数据，再创建干净 fixture，再复用正式模拟服务，再自动验证输出。每段自造输入 fixture 必须逐字段、状态枚举和来源语义对照上一段当前输出契约；本段输出必须逐字段对照下一段当前输入契约。涉及现有业务节点时还必须核对后端真实常量及正式门禁引用，禁止为文档方便自造聚合节点、别名节点或双契约 fallback。若验证前需要对同一活跃订单执行“重建”，重建清理必须物理删除该订单自有运行态事件，确保事件幂等唯一键可重复生成，不得用软删除事件替代；缺少 `simulationRunId` 的事实不得纳入自动清理范围。无下游副作用断言若会读取关联实体，迁移与实体字段必须一并核对并先以 schema 合同锁定；不得等到真实 E2E 才因旧表缺字段而失败。
- Material detail boundary: 当一线生产按路线工序输入、输出物料表达时，Stage1 只能从活跃订单冻结路线版本的对应 `routeProcessId` 读取两类物料；输入物料只读取系统同步批号并作为物料平衡追溯事实，不填写完成数量、不参与工序进度；输出物料才写完成数量、损耗和进度。不得从产品 BOM、领料单、旧批记录物料字段、当前路线或库存推断任一物料。点击完工、提交详情、批记录回填、放行和追溯需要物料批号时，必须先按活跃订单 `workOrderId -> workOrder.code -> productionOrderNo` 解析全部正式生产领料单，按领料单 ID 分组逐张校验并形成集合事实；一张和多张领料单走同一集合链路，禁止事前绑定、任取第一张、合并成代表单或从单条旧绑定补齐。用户要求默认设备时，只能从当前组长、当前工序的启用设备绑定中按绑定记录 ID 升序选择第一台且设备本身必须启用；无有效设备时保持空值，禁止将组长、设备账号或工作站编号伪造成选用设备。
- Blocker: 缺活跃订单、缺冻结工序、缺生产目标数量、缺 PQC 任务、PQC 任务不属于订单冻结工序、任务数量或 QA 项目身份不完整、当前登录组长无归属，重复重建触发 `mes_pro_process_pool_event` 幂等唯一键冲突、生产/PQC 任一模拟事实无法完整写入 `simulationRunId`/`stageCode`/模拟来源标识、分段模拟动作不能闭环完成清理建数执行验证、相邻阶段契约字段或状态不等价、文档节点键与后端真实常量不一致，或只能通过直接改进度字段达成 100% 时必须停止；不得部分写入后返回成功。
- Verification: 后端合同必须断言模拟入口逐工序生成生产提交、初始分配和生产组长复核，逐任务生成 PQC 逐件明细、PQC 提交和 PQC 组长确认/过程检验汇集，同时负向扫描不得写 `productionProgressPercent`、`inspectionProgressPercent` 等直接进度字段；合同测试必须断言每条生产/PQC模拟事实均带同一轮 `simulationRunId`、`stageCode` 和模拟来源标识，缺任一字段返回 blocker；文档级验证必须分别扫描上一段输出、本段输入、本段输出、下一段输入和后端真实节点常量，证明字段、状态枚举、来源字段及节点键一一对应，旧错误键只允许出现在禁止项或负向测试说明中；重建合同必须断言自有运行态事件使用物理删除清理，并覆盖同一活跃订单重复“重建 -> 模拟完成”不再触发幂等唯一键冲突；前端合同必须断言按钮在活跃订单行操作区、确认文案明确“模拟数据”、成功后展示生产/PQC 数量和双进度并刷新列表。真实写入 E2E 只有在具备任务自有活跃订单 fixture、多账号签名前置、`simulationRunId` 落表验证和清理计划时执行。
- Forbidden action: 禁止用 SQL、状态字段、直接进度覆盖、默认成功、mock 提交、空逐件明细、跳过 PQC 任务确认、跳过生产组长/PQC 组长复核、事件软删除、只写 `simulated=true` 但缺 `simulationRunId`、API-only 成功提示或吞异常来伪造双 100%。
- Evidence: `doc/tasks/20260820-active-order-simulation-complete/verification-report.md`；`doc/tasks/20260820-active-order-simulation-e2e/verification-report.md`；`doc/tasks/20260821-simulation-stage1-active-order-complete-design/verification-report.md`；`doc/tasks/20260821-simulation-stage4-dossier-upload-design/verification-report.md`。

#### 最新版本测试副本必须新建并重新冻结正式来源

- Trigger: 生产组长“复制测试单”、复制活跃订单后使用最新工艺路线/QA、`active-order/simulation/copy-latest`、`LATEST_VERSION_COPY`、模拟副本清理。
- Preflight check: 需要测试“当前最新版本”时必须创建独立模拟工单，并复用正式活跃订单新增链路重新解析当前唯一 ACTIVE 路线和最新 PUBLISHED QA 版本；工单基础字段可以复制，但生产快照、PQC 任务、报工、进度、领料绑定、异常、批记录和放行历史必须重新生成或保持为空。活跃订单、逐工序快照和 PQC 任务必须写入同一 `simulated=true + simulationStage + simulationRunId`，同一运行编号重试只返回原模拟副本。清理前必须锁定当前组长归属和完整模拟标识，并先检查批次执行、正式领料和放行申请；存在任一不可安全回滚的下游数据时明确阻断，不能部分删除。
- Verification: 后端测试必须覆盖复制基础字段、未读取来源工序/PQC快照、当前 ACTIVE 路线与最新 PUBLISHED QA 锁定、模拟标识贯穿三层、幂等重试和受控清理；前端合同必须覆盖测试标签、确认文案、不复制历史说明、写成功与列表刷新失败分层，以及只在 `LATEST_VERSION_COPY` 副本显示清理入口。
- Forbidden action: 禁止为使用最新版本直接改写原活跃订单冻结版本，禁止调用 `cloneSnapshots`/`clonePqcTasks` 复制旧身份，禁止让正式订单出现模拟清理入口，禁止清理存在批次、领料或放行副作用的模拟副本。
- Evidence: `doc/tasks/20260901-active-order-latest-simulation-copy/verification-report.md`。

## 一线 PQC DCC-QA 正式关系目标态切换门禁

- Trigger: 明确实施一线 PQC 全部活跃订单、路线-DCC 正式关系、订单 QA 版本锁定、QA 自有工序、任务只叠加，或删除产品/路线/MES 工序 QA 推算。
- Runtime boundary: 用户确认的领域边界是 QA 只对应 DCC 项目代码，QA 工序不映射 MES 工序。源码、schema、迁移和离线测试通过不等于当前页面已加载新链路；真实验收前必须检查运行 Jar 内嵌 MES 模块的方法集和目标接口，并通过登录态页面确认。运行 Jar 仍为旧方法集时必须明确标记为运行态未刷新，不能把代码完成表述为页面已经生效。
- Preflight check: 正式权威固定为路线级 `routeId -> dccProjectCodeId` 关系和 `mes_qa_inspection_regulation.dcc_project_code_id`，不在 DCC 再建第二张 DCC-QA 关系表。active order 只冻结 DCC、QA 主档和 QA 发布版本三个身份；不建 PQC context 表、BLOCKED/LOCKED 状态机或公开 lock/retry API。其它租户 ID 与当前租户不存在 ID 使用同一非法引用语义，禁止绕过租户拦截探测。
- QA authoring contract: QA 配置下拉的业务对象是 DCC 项目代码；每个 DCC 项目代码最多一个 QA 规程根，草稿和发布版本属于该根。QA 工序必须有规程内稳定身份，检验项目直接归属 QA 工序；保存草稿必须完整持久化工序、项目、适用检验类型、标准、方法、设备、抽样和排序，发布后版本不可原地修改。读取编辑态时优先返回最新草稿，否则返回当前发布版本；DCC 列表状态与精确跳转都按 `dccProjectCodeId` 批量读取，不得重新从产品、路线版本、MES 工序或规程编号推算。
- Rule identity: QA 发布规则以 `key` 为稳定身份，当前至少包含 `FIRST/PATROL_AM/PATROL_PM/FINAL`；`PATROL_AM` 与 `PATROL_PM` 共用 `inspectionType=PATROL` 项目，但必须分别生成、展示、排序和提交任务。禁止把 inspectionType 当唯一规则键或把上午/下午巡检合并。
- QA version contract: 新建或明确重建 active order 必须在执行时按 QA 规程版本表实时查询最新 `PUBLISHED` 版本（按 `publishedAt`、`id` 倒序），再把该版本锁定到 active order 和 PQC 任务；不得依赖可能失真的 `currentVersionId`、历史订单旧版本或具体版本号常量。历史页面必须使用独立锁定版本读取合同，按 activeOrder 的 DCC/QA/version 快照校验同租户和归属，并允许 DCC 后续禁用、version 为 `PUBLISHED/RETIRED` 时继续读取。管理 API 的“DCC 当前启用”校验不能复用于历史锁定读取。
- Active-order contract: `REMOVED` 订单重新激活复用原冻结版本、process snapshot 和 task 历史，不重新解析今天的路线关系或 current QA。全部有效活跃订单可见，不能由 PENDING 任务过滤；QA 工序保持独立，不建立 QA 工序到 MES 路线工序映射。
- Frozen-route display contract: 活跃订单列表的路线展示必须先校验订单锁定的 `routeVersionId` 与 `routeId`；当前路线主档可读时使用主档名称，主档缺失时只能使用同一锁定版本快照中同时匹配 `routeId` 且非空的正式 `routeName`。路线版本或快照身份缺失必须形成明确的订单完整性错误，禁止空名称、当前路线猜测、同名路线或前端隐藏错误。
- Active-order list isolation contract: 当用户明确要求列表容忍历史缺损订单时，只允许按明确白名单隔离单条订单的路线、版本、工单、产品或工序快照完整性错误，并记录订单 ID、读取阶段和业务错误码；其它有效订单必须继续返回。白名单外的 `ServiceException`、解析异常、数据库异常和系统异常仍必须 fail-fast，禁止捕获通用异常、返回空列表或用前端隐藏错误。隔离只改变列表读取，不得修改订单、任务、快照、报工或进度数据；缺损订单仍须按正式重建或删除后重新新增流程治理。
- Response contract symmetry: 当任务身份正式下沉到 `pqcTaskOptions` 时，后端响应 VO、响应映射、前端 DTO、页面读取点和合同测试必须同时删除工序顶层的 `pqcTaskId/inspectionRuleKey/taskStatus/inspectionType/businessDate/shiftCode/roundNo/plannedInspectionQuantity` 副本。只跑前端正向静态合同和类型检查可能在后端仍输出兼容字段时全部通过；完成门禁必须按冻结接口逐层比对精确字段集合，并对后端顶层字段声明、映射写入、前端顶层字段/读取点和仍断言旧字段的测试做负向扫描。任一层保留第二权威时必须阻塞，不得以“前端未读取”接受兼容输出。
- Task overlay boundary: 当前页面的任务叠加只接受活跃订单锁定 QA 版本及其正式 QA 工序。旧版本或旧工序下已经 `CANCELLED` 的历史任务可以保留审计记录，但不得阻断当前锁定版本读取，也不得进入当前工序汇总或提交选项；当前锁定版本内身份完整的 `CANCELLED` 任务仍可进入历史汇总。任何 `PENDING/SUBMITTED/CONFIRMED` 任务与锁定版本、QA 工序或订单身份不一致时必须 fail-fast。前端可以保留各状态投影视图，但员工切换和正式提交只能从 `taskStatus=PENDING` 的任务建立上下文。
- Concurrency gate: 路线-DCC version 必须在同路线全部历史上单调递增，PUT/DELETE 按 tenant/route/deleted/version 原子更新；PQC 同一 `pqcTaskId` 的正式提交必须行锁或等价串行化，PENDING->SUBMITTED CAS 影响行数必须为 1，失败后只能进入唯一正式事件的内容比较，不得继续写签名、明细或事件。
- Blocker: 规则 key 到 task `shiftCode/roundNo` 的映射和业务排序未冻结、同任务并发提交没有原子闭环、历史锁定 QA 仍依赖 DCC 当前启用状态、迁移证据不唯一，或真实测试前置缺失时必须停止实施。完整写 E2E 只能在明确恢复写入口的受控测试环境执行；生产停写窗口不能伪造写路径通过。
- Verification: BDD/TDD 必须覆盖 `PATROL_AM/PATROL_PM` 两条任务的数量、顺序、独立状态和提交，同 task 同内容/冲突内容/不同 actual employee 并发，DCC 锁定后禁用仍可读历史 QA，第三个订单和无任务订单可见，`REMOVED` 重激活保留原版本，以及路线关系解绑重绑无 ABA。独立验收必须先检查所有责任测试类真实存在，不能用 Surefire 无匹配设置掩盖缺类。
- Forbidden action: 禁止双读、双写、fallback、GET 补快照、PENDING 任务过滤订单、按产品/路线/路线版本/MES 工序反查或校验 QA、用规程编号前缀推算 DCC、合并上午下午巡检、复用管理 API 读取历史锁定版本、无锁提交同一 PQC task，或在 DCC 再建 DCC-QA 关系。
- Evidence: `docs/changes/20260811-frontline-pqc-dcc-qa-contract.md`；`doc/tasks/20260811-frontline-pqc-dcc-qa-agent-design-review/verification-report.md`；`.review-fix-loop/runs/20260811T135744Z-e156b2/review/report-round-4.md`；`doc/tasks/20260811-dcc-qa-backend-persistence/verification-report.md`；`doc/tasks/20260812-frontline-pqc-dcc-qa-int12/verification-report.md`。

## DCC 项目代码配置状态筛选跨模块聚合门禁

- Trigger: DCC 项目代码列表、基础数据项目代码筛选、工艺路线配置状态、主批记录配置状态、QA规程配置状态、`routeConfigured`、`mainBatchRecordConfigured`、`qaRegulationConfigured`。
- Preflight check: 先确认三类状态的正式来源和模块边界：DCC 列表只接收筛选参数并消费跨模块状态 API；工艺路线与主批记录来自 MES 工艺路线治理状态；QA规程来自 MES QA 规程项目状态。单独筛选某一类时，请求契约必须显式标记只需要该类状态，跨模块 API 和 MES 聚合服务不得触发未请求的其它配置链路。列表分页成功后若还会加载展示列补状态，也必须按实际展示列显式关闭未展示链路；DCC 项目代码列表只展示工艺路线和主批记录治理状态时，不得继续请求损耗单、过程检验单、参数记录表等表单槽位状态。不得在 DCC Mapper 中复制 MES 表 JOIN 或按名称、表单槽位、工序开始配置推断配置状态。
- Blocker: DCC 分页 SQL 直接查询 MES 表、三类筛选共用一个状态字段、QA规程状态从路线或批记录推断、主批记录从 `formBindings`/默认 `MAIN` 推断、未命中状态被默认当作已配置、工艺路线单独筛选仍调用主批记录/QA规程/表单槽位查询导致系统异常、分页后展示列补状态请求仍无条件调用表单槽位链路导致系统异常，或前端只做本地展示过滤时必须停止。
- Verification: 后端回归必须覆盖三类状态分别单独命中和组合筛选；跨模块聚合单测必须证明工艺路线、主批记录、QA规程三条来源互不替代；单项筛选还必须用 mock 负向断言未请求链路不被调用，例如工艺路线筛选不得触发主批记录、QA规程或表单槽位查询；前端静态合同必须锁定三个独立筛选控件、请求字段和正式分页参数，并断言列表展示列补状态请求只请求当前展示所需的治理状态。
- Forbidden action: 禁止用 DCC 项目名称模糊匹配、前端本地过滤、默认成功、空状态兜底、`formBindings`、工序开始上传人、批记录表单槽位、无条件全链路聚合或跨模块复制 SQL 代替正式配置状态 API。
- Evidence: `doc/tasks/20260820-dcc-project-code-config-filters/verification-report.md`，工艺路线已配置筛选曾因无条件触发主批记录/QA规程聚合而显示系统异常；复报后又发现分页成功后的展示列补状态请求仍会拉表单槽位状态；修复后单项筛选只请求对应状态链路，列表展示补状态请求显式关闭未展示的表单槽位链路。
- Evidence: `doc/tasks/dcc-project-code-config-filters/verification-report.md`。

## MES PQC 项目级检验快照门禁

### PQC 检验项目事实必须来自发布规程和结构化 itemResults

- Trigger: PQC 填写、PQC 组长复核、QA 检验规程、检验设备、设备编号、无设备检验项目、`equipmentRequired=false`、接收标准、检验方法、参数上下限、`itemResults`、`rawPayload.pqcPieceValues`、`pqcItemDetails`、固定 `length/appearance/seal/pressure` 字段。
- Preflight check: 修改 PQC 链路前先核对发布 QA 规程项目、`equipmentRequired`、项目级设备表、设备台账编号归属、接收标准上下限、单位和精度字段；提交契约必须以结构化 `itemResults[]` 为业务事实，后端在提交时从发布规程冻结设备、编号、方法、标准、上下限、单位、精度、实测值和判定。设备是否必填必须按单个 QA 项目判断：`equipmentRequired=true` 才强制 `selectedEquipmentId/selectedEquipmentNumber` 并校验项目设备归属；`equipmentRequired=false` 且无设备选项是正式无设备项目，应允许设备字段为空并保存空设备快照。一线弹框、卡片摘要、组长列或提交快照若展示“接收标准/检验方法”，必须使用显式 QA 工序列字段或别名（例如 `acceptanceStandard/processInspectionMethod`），不能直接让默认首检摘要、判定值、上下限合成文案或旧兼容字段成为可见来源。
- Blocker: 客户端提交可改写接收标准或检验方法、后端仍把 `rawPayload.pqcPieceValues` 当权威、组长页仍按固定四项字段展示、设备编号未按项目设备归属校验、无设备项目仍被要求选择设备、缺发布规程项目或设备必填项目缺设备主数据时默认成功，必须停止。
- Verification: 后端回归需覆盖 schema、项目设备 mapper、`itemResults` 提交、设备编号归属校验、设备必填项目明细冻结和无设备项目空设备快照；前端静态或真实路径需覆盖填写页每项目设备/编号/标准/方法入口、无设备项目显示“无需设备”、组长页读取 `pqcItemDetails/itemResults`，并复跑相邻 eDHR/PQC 布局合同和 `pnpm ts:check`。
- Forbidden action: 禁止用整单设备替代项目级设备，禁止把所有 PQC 项目统一当作设备必填，禁止用固定四项字段、前端文案、默认上下限、默认首检规则、判定值、空标准、raw payload 或 API-only 展示替代正式项目级快照。
- Evidence: `doc/tasks/20260803-pqc-equipment-standard-method-implementation/verification-report.md`；`doc/tasks/20260808-pqc-optional-equipment-items/verification-report.md`；`doc/tasks/20260808-pqc-qa-process-standard-method-source/verification-report.md`。

### QA 多工序正式发布与退役夹具唯一键必须隔离

- Trigger: QA 规程页面一次维护多个工序组、按路线工序发布、`routeProcessId`、激活路线版本快照、`owner_module=MES_QA`、退役 `CODX_QA` 夹具仍占路线工序唯一键、正式接收标准或检验方法仍显示测试夹具文案。
- Preflight check: 发布前必须读取当前唯一 ACTIVE 路线版本中的路线工序身份，按项目的明确业务工序范围逐项解析并分组，每个 `routeProcessId + processId` 单独生成规程 payload；业务标签包含复合名称时必须维护显式映射，不能按包含关系把相邻工序猜入范围。QA 业务工序与批记录表单绑定是两个独立事实：业务方明确确认某 QA 工序即使尚未识别批记录绑定也要保留时，必须使用项目级显式允许清单，只把该项目归入页面已经唯一解析的正式 QA 质检工序身份，保留原业务工序显示，并让批记录绑定摘要为空；不得据此猜测它对应其它路线工序。修复本地测试数据前必须精确核对唯一键占用行的 owner、生命周期、deleted、版本、项目、任务状态和逐件明细；若退役 `CODX_QA` 占键，只有取得明确授权后才能软删除该退役 owner 行，再新建独立 `MES_QA/PUBLISHED` 规程，旧版本、旧项目和已取消任务继续保留审计。
- Blocker: ACTIVE 路线版本缺失或不唯一、项目无法唯一映射正式路线工序且未进入业务方确认的未绑定允许清单、允许未绑定但页面无法唯一解析正式 QA 质检工序身份、使用当前编辑态路线工序 ID、把未获允许的跨工序项目混入一个 payload、正式运行态仍引用非 `MES_QA/PUBLISHED` 规程、退役夹具占键但未获处理授权、目标任务已有提交或逐件明细时必须停止。
- Verification: 前端合同必须覆盖激活版本工序解析、逐工序 payload、复合工序显式映射、显式允许的未绑定业务工序继续显示且不产生批记录绑定摘要，以及任一请求失败时整体显式失败；后端回归必须锁定正式 owner/发布态准入和项目级快照；真实 Playwright 至少确认未绑定业务工序和 QA 来源列可见、目标写请求为 0，完整发布链路还必须满足任务数据清理门禁。数据修复后核对正式规程、版本、FIRST/PATROL/FINAL 项目、待检任务及零明细，并通过真实 Playwright 页面确认标准、方法和来源均为发布 QA 规程快照且不含夹具/默认规则文案。回滚脚本必须只处理未变化的任务自有新记录并恢复被软删除的退役 owner 行。
- Forbidden action: 禁止把全部 QA 项目默认发布到单一路线工序，禁止把未识别批记录绑定扩展成全局宽松规则，禁止用字符串模糊匹配猜测工序，禁止隐藏或删除业务方明确要求显示的未绑定 QA 工序，禁止为其伪造批记录绑定摘要，禁止把当前可变路线工序 ID 当发布快照身份，禁止把 `CODX_QA` 改 owner 或重新发布成正式规程，禁止硬删除旧版本、旧项目和已取消任务，禁止用默认首检规则或夹具文案补齐缺失正式配置。
- Evidence: `doc/tasks/20260809-pqc-formal-standard-method-source/verification-report.md`。

### QA 首检数量和巡检比例按检验项目配置，末检适用性项目级统一

- Trigger: QA 规程“适用检验类型”、工序抽样方案、首件/首检数量、上午巡检、下午巡检、`AQL`、`patrolInspectionRatio`、末检开关、保存或发布规程。
- Preflight check: 首检数量和巡检比例是 QA 检验项目级正式配置；即使属于同一 QA 工序，不同检验项目也允许取不同值。保存、发布、任务生成和完整性审计必须同时保留工序身份与检验项目身份，并直接读取每个项目的 `applicableInspectionTypes`、`firstInspectionQuantity` 和 `patrolInspectionRatio`，不能提升为工序、产品或项目级全局数量/比例，也不能从抽样方案文字反向解析。PQC 任务生成的唯一身份必须按生产工序 + QA 工序 + QA 项目 + 规则键冻结，`FIRST`、`PATROL_AM`、`PATROL_PM` 和 `FINAL` 都不得只按 QA 工序合并；同一 QA 工序下多个 FINAL 项目必须生成多个 `FINAL` 任务。多道冻结生产工序共用同一发布 QA 项目时，生成器必须遍历全部冻结生产工序并把 `routeProcessId + processId` 纳入任务身份、唯一键和重复检查；禁止先校验全部工序再返回第一道工序，禁止让后续工序因为同一 QA 项目/规则键被唯一键挤掉。末检适用性是项目级统一开关，所有工序和检验项目必须服从同一份 `finalInspectionApplicable`；明确适用时各正式 QA 工序都必须具备 FINAL 项目，明确不适用时不得保留 FINAL 项目。涉及比例字段时必须同时核对上下游单位和最终计算公式：当前 `patrolInspectionRatio` 存储百分比原值，任务数量公式再执行 `plannedQuantity × ratio ÷ 100`，因此 `AQL=0.4` 应保存为 `0.4`，前端不得二次除以 100。若一线或发布版需要展示 QA 项目的“抽样方案”“检验器具及设备”等人类可读原文，必须把原文作为独立正式字段贯穿保存、发布和运行态响应；数量、比例和设备选项只承担结构化计算或选择职责，不能替代原文。运行态还必须区分“发现正式 QA 工序”和“使用检验详情”两个边界：工序列表只依赖发布规程的产品、路线、版本、工序和检验项身份，历史原文字段为空时原样返回 `null`；打开详情或正式提交时再按同名字段严格校验。QA 生成的 `plannedInspectionQuantity` 是冻结规程给出的初始建议值，不是提交时必须相等的限制；一线 `FIRST`、`PATROL_AM`、`PATROL_PM` 和适用的 `FINAL` 都允许提交正整数 `actualInspectionQuantity`，但正式逐件样本数量必须与实际数量一致，且损耗数量不得超过实际数量。
- Blocker: 任一启用首检的检验项目缺合法正整数数量、任一启用巡检的检验项目缺合法比例、页面与 payload 的项目级派生结果不一致、前后端比例单位未核对、项目级末检适用性与工序 FINAL 项目不一致，或打开检验详情、发起正式提交时历史发布项目缺展示所需正式原文字段，必须停止。同一工序或不同工序的检验项目之间首检数量、巡检比例不同都不是 blocker。不得补默认比例、默认首检数量、拼装历史原文或保留旧数组掩盖缺失。新增原文字段的迁移不得猜测回填历史数据，历史项目需通过正式 QA 保存/发布链路补齐。仅用于“选工序”的接口不得因这些非工序身份原文为空而整单失败。
- Verification: 纯函数合同覆盖同一工序不同检验项目使用不同首检数量和巡检比例、跨工序不同值、非法数量、缺比例原值，以及末检项目级开关两态；任务生成回归必须覆盖同一 QA 工序下多个 FINAL 项目按不同 `qaItemCode + FINAL` 生成独立任务；静态合同锁定页面展示、完整性检查和保存载荷按检验项目读取结构化字段，并负向扫描工序、产品或项目级全局数量和全局比例；后端公式核对或回归必须证明百分比只除以 100 一次；原文展示链路还必须逐层断言保存载荷、发布记录、运行态响应和页面直接读取同名正式字段，并覆盖“历史缺字段仍列出正式工序、详情不打开、提交不发送且后端提交边界 fail-fast”；真实页面覆盖项目级首检/巡检控件、未配置空态和末检开关两态，且验证时不产生保存/发布写请求。数量调整回归必须分别覆盖 `FIRST`、`PATROL_AM`、`PATROL_PM` 和 `FINAL` 的计划值与实际值不同仍可提交，并校验逐件样本数量跟随实际值。
- Forbidden action: 禁止把同一工序不同检验项目或跨工序不同的首检数量、巡检比例判定为冲突，禁止把检验项目级数量/比例合并成工序或项目全局值，禁止把 FINAL 任务退回按 QA 工序单条生成，禁止把 AQL 同时当百分数和小数比例，禁止根据抽样方案文字或其它旧字段猜测结构化首检/巡检配置，禁止用 `firstInspectionQuantity`、`patrolInspectionRatio`、`equipmentOptions` 或前端默认值反推/拼装“抽样方案”“检验器具及设备”原文，禁止把计划检验数量误当成实际提交数量的硬相等约束，禁止因允许调整而放宽正整数、样本数量一致性或损耗数量边界，禁止用兼容分支或静默跳过无效方案维持假成功，也禁止把详情原文完整性校验提前到只负责发现工序身份的列表边界。
- Evidence: `doc/tasks/20260809-qa-applicable-types-derived/verification-report.md`。

### QA 测试重置入口必须显式隔离正式生命周期

- Trigger: 测试阶段重复导入同版本 QA Word 模板、QA 规程已发布但需要重新覆盖测试数据、`/mes/qa/inspection-regulation/test-reset`、QA 规程配置页“测试重置”按钮。
- Preflight check: 测试重置只能作为管理员显式测试清理入口，不能改变正式导入、升版、同版本已发布拒绝和发布不可变语义；前端必须二次确认并要求 QA 更新权限，后端必须用同一权限拦截并在事务内校验所选 DCC 项目、规程根记录、版本树和下游引用。删除前必须统计活跃订单与 PQC 检验任务等正式运行引用，任一引用存在即 fail-fast，不得部分删除。无规程时只返回零清理数量，不得创建新规程或伪造成功导入。
- Blocker: 测试重置入口缺权限、缺二次确认、可删除被活跃订单或 PQC 任务引用的规程、删除顺序可能留下孤儿设备/项目/工序/版本记录、重置后页面仍显示旧发布内容、或把测试重置作为导入同版本发布规程的 fallback 时必须停止。
- Verification: 后端回归必须覆盖“无生产引用时只删除所选 DCC 的规程树并返回数量”“存在活跃订单或 PQC 任务引用时拒绝且不删除任何数据”“无效或停用 DCC 项目拒绝”；前端静态或真实路径必须覆盖按钮位置、权限可见性、二次确认、调用测试重置接口、成功后清空当前项目状态并不会调用发布接口。若运行态需要看到按钮变化，必须按本地运行态规则刷新或重启前端，不能用源码已改冒充页面已更新。
- Forbidden action: 禁止直接 SQL 删除已发布 QA 规程来绕过导入规则，禁止放宽“同版本已发布拒绝”来服务测试反复导入，禁止无引用检查硬删除正式版本，禁止 API-only 成功冒充前端入口可用，禁止用测试重置处理真实生产纠错；真实生产纠错应走正式更正/升版或经批准的数据修复流程。
- Evidence: `doc/tasks/20260818-qa-reset-regulation-test-admin/verification-report.md`。

### QA Word 升版旧快照歧义项目必须延迟判定

- Trigger: QA Word 模板升版导入、`import-word-draft`、旧发布快照同一工序存在多个末级同名检验项目、`sourceOriginalItem` 缺失完整路径、报错 `同名检验项目不唯一`、例如 `大包装工序 / 外观`。
- Preflight check: 升版继承必须优先使用“规范化工序名称 + 检验项目完整名称”精确匹配旧项目；旧快照存在无法区分的末级同名 key 时，只能登记为歧义旧键，不得在基线索引构建阶段整体阻断所有导入。新 Word 项目若带完整路径且不命中旧歧义 key，应按新项目生成草稿并保留完整 `sourceOriginalItem`；若新 Word 也只解析为同一歧义 key，则必须 fail-fast 且不保存草稿。Word 通过纵向合并表达父项目、把子检测名称写在接受标准开头时，解析层只能在同一工序下父项目确实重复，且重复组每条标准都具有互不相同的明确 `X检测/检验：` 前缀时，将其消歧为 `父项目 / 子检测`；单项项目不得从标准推断名称。
- Blocker: 旧快照歧义 key 被错误继承旧编码、设备绑定、结果类型、数值范围、关键项或失败规则，新完整路径项目因无关旧歧义被整体拒绝，歧义项目被模糊匹配到任一旧项目，或同一纵向合并父项目下的多个明确子检测仍解析成相同名称时必须停止。
- Verification: 后端回归必须同时覆盖“旧快照已有完整来源名时可精确继承”“旧快照缺完整来源名但新 Word 有完整路径时不阻断并按新项目保存”“新 Word 命中旧歧义 key 时继续拒绝且不保存”。相邻解析测试必须覆盖多级项目名称和合并单元格解析，防止完整路径再次退化为末级名称；对标准前缀消歧还必须覆盖重复父项目的多个唯一前缀、抽样/方法/器具不变，以及单项标准带前缀时不误改名。
- Forbidden action: 禁止按末级项目名模糊继承，禁止为了通过升版删除旧项目或直接重置已发布版本，禁止把旧快照歧义当作同版本覆盖入口，禁止吞掉歧义后错误继承旧正式配置，也禁止对任意接受标准冒号前文本做项目名推断。
- Evidence: `doc/tasks/20260819-qa-word-import-duplicate-item-key/verification-report.md`；`doc/tasks/20260903-qa-regulation-hierarchical-item-recognition/verification-report.md`。

### PQC 待检准入与工序选择必须分离

- Trigger: 一线 PQC 真实页面、`active-order/list`、`active-order/processes`、QA 检验项目列表“工序”列、活跃订单当前产品、生产工单产品路线绑定、订单产品代码不等于项目代码、路线产品绑定物料、DCC 项目代码 `productMasterId`、同一路线绑定多个产品、路线存在额外工序但 QA 项目未配置、只有一个工序存在 `PENDING` PQC 任务、`activeOrderId` 有值但 `routeProcessId/processId=null`。
- Preflight check: PQC 待检工单列表必须以正式 `PENDING` PQC 任务为准入条件，按最新 active order ID 过滤后再加载工单、路线和产品摘要；没有待执行任务时返回空列表，由前端显示业务空态。用户选择工单后，`active-order/processes` 必须依次校验活跃订单、生产工单及当前产品与路线的正式绑定；订单产品只用于定位当前路线，不是 DCC 项目代码。随后读取该路线全部正式 `mes_pro_route_product` 绑定中可解析的 MES 物料代码，以这些路线绑定代码精确匹配唯一启用的 DCC `projectCode`；路线中的普通业务产品物料与项目代码物料身份不同，不要求每个路线物料 ID 都等于或能转换成 DCC `productMasterId`。命中唯一 DCC 项目后，只使用其 `productMasterId` 作为 QA 产品身份，按当前 `routeId + routeVersionId + MES_QA/PUBLISHED` 过滤正式规程。候选工序只从该 QA 产品实际存在检验项目的规程中提取，按 `routeProcessId + processId` 去重；当前路线工序只能补充名称、排序和工位，不得扩展候选集合。未绑定当前路线的其它 DCC 项目、活跃订单工序快照、路线全部工序和 `PENDING` 任务都不是候选工序来源。正式 `PENDING` 任务只为已有 QA 候选工序附着 `pqcTaskId`、规程快照和检验项；历史检验项的 `inspectionTool/samplingPlanText` 为空时，列表原样返回空值但仍保留正式工序，打开详情和正式提交再严格拦截。提交链路必须携带正式 `pqcTaskId` 并校验任务、QA 工序、MES 工序、活跃订单和状态一致。非 `CANCELLED` PQC 任务的 `routeProcessId/processId` 必须是正式任务身份。
- Blocker: PQC active order 列表返回的工单没有 `PENDING` 任务、生产工单当前产品未绑定所选路线、路线产品绑定为空、可解析的路线物料代码无法唯一匹配启用 DCC 项目、DCC 项目缺 `productMasterId`、该 DCC productMasterId 未命中 QA 产品、目标路线版本没有正式 `MES_QA/PUBLISHED` 规程或规程缺检验项目、QA 规程的产品/路线/版本/工序身份不一致、MES 工序缺失或停用、非取消 PQC 任务缺正式工序身份、待检任务不属于 QA 候选工序、提交时所选工序没有正式 `PENDING` 任务时必须停止；不得返回默认成功、推断候选或伪造可提交上下文。单个无关路线物料无法解析不是项目代码匹配 blocker；`inspectionTool/samplingPlanText` 为空也不是工序列表 blocker，但必须成为详情和提交 blocker。
- Verification: 后端回归必须覆盖“订单产品代码与路线项目代码不同时，由订单产品定位路线后使用路线绑定物料代码匹配 DCC projectCode”“路线项目物料 ID 与 DCC productMasterId 不同时仍按 projectCode 命中，并只用 DCC productMasterId 查询 QA”“单个无关路线物料无法解析不阻断已存在的项目代码物料”“未绑定当前路线的其它 DCC 项目不参与”“路线有额外工序但 QA 项目只覆盖部分工序时只返回 QA 工序”“同一工序含多个 QA 项目或重复规程时按正式工序身份去重”“有任务工序可附着首检/巡检任务选项”“历史展示原文为空仍返回工序且原样保留空值”“非 `MES_QA` owner 必须 fail fast”“QA 工序身份漂移或缺失 fail fast”“无 active order 返回空列表”“active order 仅有非 PENDING 任务被过滤”“产品路线绑定不匹配 fail fast”“非取消任务缺正式工序身份 fail fast”。前端真实路径应逐项比对 `active-order/processes` 与工序卡片，并确认未配置 QA 检验项目的路线工序不可见、无正式任务的 QA 工序未获得伪造提交上下文、历史展示原文为空时详情不打开且提交请求不发送。
- Forbidden action: 禁止把订单产品物料代码直接当成 DCC 项目代码；禁止用未绑定当前路线的 DCC 项目、活跃订单工序快照、路线全部工序、当前进行状态、待检任务集合、草稿路线、`formBindings`、默认 `MAIN` 或前端补齐逻辑替代或扩展路线项目代码下的 QA 检验项目工序集合；禁止为空任务工序伪造 `pqcTaskId`、规程、检验项或提交成功；禁止在附着正式任务上下文时接受 `CODX_QA`/其它测试 owner。
- Evidence: `doc/tasks/20260808-frontline-pqc-process-cards-qa-items/verification-report.md`；`doc/tasks/20260807-pqc-leader-management-five-records/verification-report.md`；`doc/tasks/20260807-frontline-pqc-pending-order-filter/verification-report.md`；`doc/tasks/20260809-frontline-pqc-qa-project-process-source/verification-report.md`。

### PQC 历史任务修复必须同时核对冻结工序快照

- Trigger: 修复历史 PQC 任务身份、`qaProcessId` 为空、任务规程版本与活跃订单不一致、修复后待检订单可见但工序列表为空、活跃订单工序快照缺失。
- Preflight check: 任务修复前同时冻结活跃订单锁定的 QA 规程版本、路线版本、任务提交引用和活跃订单工序快照；预期工序只能来自该订单锁定路线版本的正式路线快照。任务重建完成后，必须校验当前任务的 QA 身份和活跃订单工序快照数量、顺序、路线工序身份都完整。
- Blocker: 订单缺锁定路线版本、锁定路线版本缺正式工序快照、存在提交事件/检验结果/逐件明细、工序映射不唯一、或准备从当前路线、其它活跃订单、工序名称或前端候选反推历史工序时必须停止。
- Verification: 修复验证必须分别证明旧错误任务不再可执行、当前版本任务身份和抽样规则完整、提交引用为零、冻结工序快照与订单锁定路线版本逐项一致；随后通过真实一线 PQC 页面选择精确活跃订单，确认工序列表和项目级任务均可操作。
- Forbidden action: 禁止只修任务表而忽略活跃订单工序快照；禁止复制同工单其它活跃订单的工序快照、使用当前激活路线覆盖历史订单、按名称猜工序，或放宽后端身份门禁让缺快照订单继续提交。
- Evidence: 任务 `doc/tasks/20260817-repair-active-order-30-pqc-history/verification-report.md`，任务身份修复后仍因冻结工序快照缺失无法返回工序；最终按订单锁定路线版本补齐正式快照并完成真实页面复验。

### PQC 末检适用性按显式 true 要求 FINAL

- Trigger: AC-M15、PQC 末检、末检不适用、QA 规程发布、`finalInspectionApplicable`、`finalInspectionNotApplicableReason`、`FINAL` 检验项目、PQC 任务生成、放行完整性预检。
- Preflight check: 修改末检、QA 规程发布、PQC 任务生成或放行完整性前，必须核对发布版本表、保存/发布 VO、前端 payload、生成器和放行校验是否都读取同一份 `finalInspectionApplicable` 与 `finalInspectionNotApplicableReason`；放行完整性中只有发布版本明确 `finalInspectionApplicable=true` 才要求 FINAL 任务，历史发布版本 `null` 不再作为 blocker。
- Blocker: 末检不适用但缺依据、适用却缺 FINAL 项目、不适用却仍保存 FINAL 项目、生成器因明确适用却缺 FINAL 任务默认跳过末检、或放行预检无法追溯发布版本依据时必须停止。
- Verification: 后端回归必须覆盖适用生成/要求 FINAL、不适用且有依据跳过 FINAL、历史 `finalInspectionApplicable=null` 不阻塞放行、明确 false 但缺依据阻塞；前端静态或真实路径必须覆盖末检关闭时填写正式依据、payload 提交字段、禁用检验类型不序列化为项目；schema 测试需锁定版本表字段。
- Forbidden action: 禁止把缺少 FINAL 任务、空规则列表、前端开关、历史任务状态或 API-only 说明当作明确不适用依据；禁止在明确 `finalInspectionApplicable=true` 时默认放行或吞掉 FINAL 缺失。
- Evidence: `doc/tasks/20260805-pqc-regulation-task-generation-fix/verification-report.md`。

### PQC 过程检验汇集必须形成最终确认明细

- Identity extension: 同名检验项目可能在不同QA版本使用不同编号。租户级设备配置和一线运行态必须以当前订单冻结QA版本实际返回的检验项目编号核对；配置页项目编号与当前订单项目编号不一致时，必须按配置缺失处理，不得按名称、QA版本或其它编号回退匹配。

- Trigger: AC-M21、过程检验记录汇集、一线 PQC 提交、PQC 组长复核通过、`productionSubmitEventId`、`qaProcessId`、`aggregateApprovedPqcSubmission`、`processInspectionAggregationStatus`、`mes_pqc_process_inspection_aggregate_detail`、`mes_pqc_inspection_task.task_status`。
- Preflight check: 修改一线 PQC 提交或汇集链路前先核对 `mes_pro_process_pool_event`、`mes_pro_process_pool_pqc_record`、`mes_pqc_inspection_task`、`mes_pqc_inspection_piece_detail` 和汇集明细表的租户、事件、任务、轮次、规程版本、逐件明细来源。一线 PQC 的正式绑定对象只有活跃订单、当前 PQC 任务、QA 工序/规程和结构化逐件明细；检验设备配置只来自当前租户级 `itemCode` 设备配置，QA 版本只提供检验项目、检验标准、抽样规则等任务事实；`productionSubmitEventId` 不是 PQC 提交身份，PQC 事件和记录允许为空，不能从同一订单同一工序的生产报工反查、选择或绑定唯一事件。过程检验动态 FormCenter 模板身份必须来自当前路线工序正式绑定的 `formTemplateId + lastPublishedTemplateVersionId + lastPublishedTemplateVersionNo`，并校验已发布模板版本的 `templateId` 与绑定一致；不得把“过程检验记录”或某个具体模板 ID（例如 28）写成业务判定。汇集只能读取正式 `SUBMITTED` 任务和结构化逐件明细，并在同一事务中 CAS 标记记录已汇集、确认任务为 `CONFIRMED`、写入结构化汇集明细；活跃订单检验进度来自这条 PQC 任务确认和汇集事实，不依赖生产报工事件或当前设备配置。
- Blocker: PQC 提交要求存在、唯一解析或自动绑定生产报工事件，因同工序有多条或零条生产报工拒绝 PQC 提交，PQC 幂等查询遗漏正式 `qaProcessId` 身份，设备默认回填未证明所选设备仍属于当前租户级 `itemCode` 配置，过程检验动态模板写死为某个 templateId 或只按模板名称判定，或者只能证明状态标记而没有结构化明细、仍从 raw payload 汇集、未校验租户/事件/任务一致性、未排除旧修订/未确认任务/重复汇集、任务确认与明细插入不在同一事务时必须停止。
- Verification: 后端回归必须覆盖无生产报工、同订单同工序多条生产报工时的一线 PQC 提交均不绑定生产报工事件，提交载荷 `productionSubmitEventId` 为空且不查询生产报工；还必须覆盖 PQC 事件幂等查询使用 `qaProcessId`、默认设备回填按实际检验员 + `itemCode` 且命中当前租户级启用配置、非固定 templateId 的过程检验动态模板解析、成功汇集明细字段、重复汇集 CAS、跨租户拒绝、无逐件明细拒绝、任务确认 CAS 失败回滚，并配合 schema 测试验证唯一键 `tenant_id + event_id + source_piece_detail_id + deleted`。
- Forbidden action: 禁止将生产报工存在性、唯一性、事件 ID、报工状态或报工进度作为一线 PQC 提交、PQC 组长复核或活跃订单检验进度的前置；禁止把 QA 版本设备配置作为一线设备选择、默认回填或放行设备字段的权威来源；禁止把过程检验表单模板 ID、模板名称或当前截图当作固定判定；禁止用前端展示、状态字段、默认空明细、raw payload、API-only 截图或吞唯一键异常替代正式结构化汇集事实。
- Evidence: `doc/tasks/20260805-ac-m21-process-inspection-aggregation-fix/verification-report.md`；`doc/tasks/20260820-frontline-pqc-decouple-production-submit/verification-report.md`；`doc/tasks/20260820-frontline-pqc-process-inspection-route-binding/verification-report.md`；`doc/tasks/20260820-frontline-pqc-inspection-equipment-selection/verification-report.md`。

### 项目范围设备候选必须先过滤正式项目身份

- Trigger: 租户级 PQC 检验设备配置、QA 项目切换、`dccProjectCodeId`、同名检验项目、历史 QA 数据异常、`item-equipment/items`。
- Preflight check: 项目范围查询必须先按正式 `dccProjectCodeId` 过滤 QA 检验项目关系，再对命中当前项目的数据执行版本、规程、DCC 项目和显示字段的严格校验；租户内其它项目的历史脏数据不得参与当前项目解析。
- Blocker: 当前项目命中的规程版本、规程、DCC 项目或检验项目字段缺失，或当前项目内同一 `itemCode` 的正式名称/项目身份冲突时必须 fail-fast；不得用跳过错误、空列表或其它项目数据掩盖当前项目异常。
- Verification: 回归必须覆盖“其它项目缺少 DCC 项目归属但当前项目候选仍正常返回”“当前项目自身缺少正式项目归属仍明确报错”“同名项目只展示一条但保留全部 `itemCode`”；真实 QA 页面必须核对请求携带当前项目 ID、批量配置携带全部编号，一线 PQC 必须按当前订单冻结 QA 项目的实际编号读取租户级设备选项。
- Forbidden action: 禁止先全租户严格解析再按项目过滤，禁止按项目名称猜测身份，禁止从 QA 版本回退到设备配置，禁止在历史脏数据影响当前项目时返回默认成功或空配置。
- Evidence: `doc/tasks/20260824-qa-inspection-equipment-tab/verification-report.md`。

### QA 规程配置状态必须来自产品级规程记录

- Trigger: QA 规程配置页、DCC 项目代码对应产品、`已配置 QA 规程`、`待配置 QA 规程`、产品级检验规则草稿、`qaInspectionTypeRules`、`qaProductRuleDrafts`、`project-statuses`、`mes_qa_inspection_regulation.product_id`、前端硬编码 `IDI` 或压力泵模板判断产品状态。
- Preflight check: 修改 QA 规程配置状态或检验规则前，先核对 DCC 项目代码的 `productMasterId` 与 QA 规程表 `product_id` 的正式关系；配置状态必须由后端按产品 ID 查询 QA 规程记录并返回。页面内尚未保存的规程字段、检验规则和检验项目也必须以 `productMasterId` 为唯一状态 key，切换产品前保存当前产品草稿、切换后恢复目标产品草稿；同一产品的不同 DCC 入口必须复用同一状态，缺产品绑定时清空并阻塞。
- Blocker: 页面把压力泵 `IDI`、产品名称、前端常量集合、空状态、模板初始化数据或查询失败当作配置状态来源，直接以项目代码选择当前规则，多个产品共享同一个可变规则数组，切换产品不重置/恢复规则，状态接口失败时静默把项目归入待配置，或只加载第一页/局部 DCC 候选后就执行已配置排序，必须停止并补齐正式产品状态链路和完整候选输入。
- Verification: 后端回归必须覆盖已配置与未配置产品按请求顺序返回；前端静态契约必须断言调用正式 `project-statuses` API、产品草稿 Map 以正式产品 ID 为 key、切换前保存和切换后恢复、同产品跨项目入口复用、缺产品绑定清空、默认下拉完整加载候选后再排序，并禁止项目代码直接选择当前规则；真实页面回归需覆盖目标已配置产品位于默认第一页之外时仍进入已配置优先组；同时运行相邻 QA 合同和 `pnpm ts:check`。
- Forbidden action: 禁止用前端文案、默认项目、产品名称、项目代码、压力泵样例模板、共享页面单例、API-only 展示或吞掉状态接口错误替代产品级 QA 规程和检验规则事实；样例规则如需保留，只能先通过正式 DCC `productMasterId` 登记产品归属。
- Evidence: `doc/tasks/20260804-qa-regulation-dcc-project-code/verification-report.md`；`doc/tasks/20260805-qa-regulation-product-specific-rules/verification-report.md`。

## MES 工艺路线产品绑定状态门禁

### eDHR 路线主档与产品启用绑定必须分开报错

- Trigger: 创建 eDHR 批次、选择生产工单后路线下拉为空、路线主档明明存在但提示“eDHR 批次执行对应工艺路线不存在”、`mes_pro_route_product` 缺失或只关联停用/已删除路线。
- Preflight check: 先区分路线主档是否存在，以及当前工单产品是否通过正式 `mes_pro_route_product` 绑定至少一条启用且未删除的路线。产品 ID 缺失、无绑定或绑定全部不可用时，应提示先完成产品与工艺路线绑定；只有已经解析到正式路线标识后路线主档真实缺失时，才提示对应工艺路线不存在。
- Blocker: 不得因为同名路线主档存在就认定工单产品已绑定路线，也不得把缺产品绑定、停用绑定、已删除绑定和路线主档缺失归为同一错误语义。
- Verification: 后端回归必须覆盖正常启用绑定、无产品绑定、只绑定停用路线和创建入口；同时确认产品路线解析失败不会按名称猜测、选择默认路线或从其它配置链路补齐。
- Forbidden action: 禁止用产品名称、路线名称、批记录表单、`formBindings`、默认路线或前端文案替代正式产品路线绑定。
- Evidence: `doc/tasks/20260813-edhr-route-binding-prompt/verification-report.md`。

### 产品侧路线选择必须匹配后端可维护状态

- Trigger: MES 物料产品选择工艺路线、产品侧路线下拉、`getRouteSimpleList`、`item-binding-list`、`saveRouteProductByItem`、`validateRouteNotEnable`、已启用路线不可维护。
- Preflight check: 修改产品侧路线选择或 route-product 保存前，先核对下拉数据源返回的路线状态集合和后端维护校验是否一致；若后端禁止维护已启用路线，前端不能使用只返回已启用路线的精简列表作为可选项，必须禁用不可维护路线并调用 `saveRouteProductByItem` 后重新读取 `getRouteProductByItem`。
- Blocker: 产品侧下拉只提供已启用路线但保存接口会因 `PRO_ROUTE_IS_ENABLE` 失败、已启用当前绑定允许清空或改选、产品侧新增第二套路由字段、保存后未重读正式当前绑定、或用前端隐藏错误替代后端 fail-fast 时必须停止。
- Verification: 前端静态契约必须断言产品侧使用专用路线选择接口、禁用已启用路线选项、不调用只返回已启用路线的 `simple-list`；后端回归必须覆盖创建、迁移、解除绑定和旧路线产品 BOM 清理。
- Forbidden action: 禁止为了让产品维护页能选择路线而放宽 `validateRouteNotEnable`、禁用后端校验、使用 `MdItemApi.routeId` 第二关系源、默认成功、吞掉保存错误或混入表单槽位/批记录表单链路。
- Evidence: `doc/tasks/20260804-mes-item-route-selection/verification-report.md`。

### DCC MDM 产品身份不得跨域当作 MES 路线物料身份

- Trigger: eDHR Word 只导入工艺路线、路线预检/创建/升版、`dcc_project_code.product_master_id` 非空、`DCC项目绑定产品主数据不存在`、DCC 项目对应路线产品绑定。
- Preflight check: `product_master_id` 只表示 MDM 产品主数据身份，不是 `mes_md_item.id`；MES 路线产品身份必须按启用 DCC 项目的 `project_code` 精确匹配 `mes_md_item.code`，再通过 `mes_pro_route_product` 解析路线。预检只读取现有项目代码物料；创建或升版可在同一导入事务中按项目代码创建缺失 MES 项目物料并绑定路线。既有 MES 项目物料可使用独立显示名称，代码才是跨 DCC/MES 的稳定身份；仍须校验物料启用且允许批次绑定。
- Blocker: 把 `product_master_id` 直接传给 `mes_md_item` 主键查询、按同名 MES 产品猜测绑定、用 MDM 产品编码替代 DCC 项目代码、预检阶段写入物料、项目代码为空，或既有项目代码物料未启用批次绑定时必须停止。
- Verification: 后端回归至少覆盖“DCC 已绑定非 MES 的 MDM ID，但只导入路线仍按项目代码创建 MES 物料并绑定”和“DCC 项目名、MES 产品/路线显示名不同，但项目代码一致时能预检并升版”；真实页面使用原始 Word，只勾选工艺路线，确认批记录版本为空、路线工序数大于零、产品名称/代码绑定计数正确，并通过路线正式读接口回读。
- Forbidden action: 禁止以 ID 数值恰好相同为依据跨 MDM/MES 关联；禁止用产品名称、`formBindings`、默认 `MAIN`、批记录表单或静默跳过产品绑定替代 DCC 项目代码路线身份。
- Evidence: `doc/tasks/20260811-word-route-only-upgrade-create-verify/verification-report.md`。

### Word 导入路线新建或升版必须以所选 DCC 身份和正式产品关系为准

- Trigger: eDHR Word 导入弹窗选择产品名称、导入预检、路线新建/升版、同名 DCC 项目代码、路线产品物料代码与 DCC 项目代码相同。
- Preflight check: 产品下拉可以显示 DCC 产品名称和项目代码，但选中值、预检和提交必须携带唯一 `dccProjectCodeId`；后端先校验该 DCC 记录启用且产品名称与导入批记录名称一致。目标路线按固定优先级解析：先读取 `mes_pro_route_dcc_project_binding.dcc_project_code_id` 的正式路线关系；仅当该关系为零条时，才将已选 DCC 的 `project_code` 精确映射到唯一启用且允许批次管理的 `mes_md_item.code`，再读取 `mes_pro_route_product`。产品未绑定路线时新建；只绑定一条可见路线时升级原路线，并在同一导入事务补齐正式 DCC 绑定；绑定多条路线时必须阻塞。草稿重传等非弹窗入口必须从既有批记录版本的正式路线反查唯一 DCC 绑定后再调用导入，不得缺省 DCC 身份。
- Blocker: 请求缺 `dccProjectCodeId`、DCC 记录不存在或停用、所选 DCC 产品名称与批记录名称不一致、已存在的项目代码产品停用或未启用批次管理、产品绑定指向不存在/已删除路线、唯一目标路线缺当前 ACTIVE 版本、路线正式绑定在预检和提交之间漂移、同一 DCC 项目或项目代码产品命中多条当前路线、产品唯一路线已正式绑定其它 DCC 项目、既有路线与所选 DCC 绑定不一致时必须 fail-fast。项目代码产品确实不存在时仍属于新建路线场景，由导入事务按正式创建链路建产品和路线。
- Verification: 前端静态契约覆盖下拉值为 DCC ID、预检和提交均传 ID、新建/升版提示基于正式关系；后端契约与数据库回归覆盖正式 DCC 绑定优先、无 DCC 绑定但产品唯一绑定路线时原路线升版并补齐 DCC 绑定、产品多路线阻塞、跨 DCC 绑定阻塞、不同路线显示名不影响目标识别，以及 DRAFT 原 ID 复用且不创建 V3、PENDING_APPROVAL/READY_TO_PUBLISH 阻塞；还必须覆盖停用/未启用批次管理产品、孤立 route-product 绑定和唯一目标缺 ACTIVE 版本均阻止，不得回落为新建。
- Forbidden action: 禁止把产品名称字符串作为 DCC 唯一身份，禁止按路线名称匹配或猜测路线，禁止枚举同名 DCC 或多条产品路线后任取一条，禁止用 MDM `productMasterId` 当 MES 物料 ID。DCC `project_code -> mes_md_item.code -> mes_pro_route_product` 只允许在用户已选定唯一 DCC 身份且该 DCC 尚无正式路线绑定时正向定位已有路线；不得反向推断 DCC 身份，也不得在正式 DCC 绑定已存在时覆盖其优先级。
- Evidence: `doc/tasks/20260813-batch-record-import-dcc-binding/verification-report.md`；`doc/tasks/20260812-word-route-disabled-restore-dedup/verification-report.md`。

### Word 工艺路线导入必须锁定唯一未结束候选

- Trigger: eDHR Word 只导入工艺路线、当前 ACTIVE 路线存在 `DRAFT`、`PENDING_APPROVAL` 或 `READY_TO_PUBLISH` 候选、重复上传同一 Word、候选版本并发状态变化。
- Preflight check: 预检必须返回唯一未结束候选的 ID、版本号和生命周期状态；同源 `DRAFT` 只能经用户明确确认后更新原候选，同一提交必须携带预检冻结的候选 ID。服务端写入前必须再次加锁核对候选 ID、状态和来源 ACTIVE 版本，不能只信前端预检。路线首次创建、Word 导入创建 ACTIVE 版本、候选创建和候选取消还必须同步维护 controlled content 的 master、ACTIVE version ref 与 candidate version ref；既有数据缺引用时应通过正式迁移或可审计修复补齐，不能留到取消草稿时临时猜测。
- Blocker: `PENDING_APPROVAL` 或 `READY_TO_PUBLISH` 存在、草稿来源版本与当前 ACTIVE 不一致、预检候选 ID 与提交时不一致、出现多个未结束候选、候选在预检后改变状态，或删除草稿时报 `controlled content active ref does not exist for route` 时必须 fail-fast；提示用户先撤回、取消或完成发布，或先完成正式引用修复。缺 ACTIVE ref 时不得继续删除候选或宣称无草稿场景可验。
- Verification: 后端数据库测试至少覆盖同源 V2 DRAFT 原 ID 更新且不插入 V3、两个锁定状态不修改且不插入下一版本、来源版本漂移和预检/提交候选 ID 漂移；还要覆盖 Word 新建路线后 ACTIVE ref 存在、候选 ref 存在、取消候选成功，以及取消后再次仅路线导入从 V1 创建 V2 而不创建 V3。真实页面连续导入两次后，应在版本工作区只看到原 ACTIVE 与同一个 DRAFT 候选；无草稿场景必须从真实页面删除草稿成功并确认只剩 ACTIVE 后再执行创建验证。
- Forbidden action: 禁止为重复导入创建 V3、覆盖待审批/待发布候选、仅依赖版本号唯一约束、预检后不做最终校验、在 Word 导入时隐式提交或发布草稿；禁止用直接 SQL 改候选状态、删除引用、吞掉 controlled content 异常或 API-only 结果伪造“无草稿”。
- Evidence: `doc/tasks/20260811-word-route-existing-candidate-governance/verification-report.md`。

### Word 候选快照重复结构必须独立序列化

- Trigger: Word 导入创建或更新路线候选、同一工序集合同时写入顶层 `processes` 与 `configSnapshots.flowGraph.nodes`、候选编辑提示“工艺路线候选版本快照不完整”、快照出现 Fastjson `$ref`。
- Preflight check: 同一业务集合写入候选快照多个位置时，每个位置必须使用独立容器和独立元素对象；`flowGraph.nodes` 每项还必须保存正式 `processId`，并保存已有 `routeProcessId` 或唯一负数 `clientRouteProcessId`。序列化后应立即按读取契约重解析，确认 `nodes` 为非空数组且不存在 `$ref`；还必须成套核对普通连线与 `START/END` 边界关系。前端加载候选后必须冻结全部已加载工序身份（包括负数 `clientRouteProcessId`），只有加载基线建立后新分配的负数身份才能进入新增列表，新编号必须小于当前最小负数以避免碰撞。
- Blocker: `nodes` 是对象、出现 `$ref`、节点缺 `processId`、同时缺 `routeProcessId/clientRouteProcessId`、非空节点图缺 START/END 边界、候选与 ACTIVE 工序集合不同却被覆盖，或直接保存把已加载负数身份再次放入新增列表时必须 fail-fast；不得复制 ACTIVE 节点覆盖候选差异。
- Verification: 静态回归先证明直接复用对象会失败，再断言候选节点使用独立副本；数据库集成回归必须读取最终 `routeSnapshotJson`，断言无 `$ref`、`nodes` 为数组、节点身份完整、普通连线和 START/END 边界完整。真实 E2E 从路线列表点击“编辑”，按候选版本 ID 验证接口节点数和页面渲染节点数一致；不改节点直接保存时还应断言已加载负数节点数大于零、`routeProcessCreates=0` 且保存前校验 `valid=true`。
- Forbidden action: 禁止启用 Fastjson 循环引用作为快照压缩手段；禁止读取时解析 `$ref`、回退 ACTIVE、返回空图或仅隐藏错误提示；禁止仅凭编号小于零就把候选已存在工序判作本次新增，也禁止只恢复节点和普通连线而遗漏 START/END 边界。
- Evidence: `doc/tasks/20260813-route-candidate-snapshot-incomplete-edit/verification-report.md`；`doc/tasks/20260813-route-candidate-negative-process-resave/verification-report.md`。

### Word 升版候选必须保留正式批记录绑定身份

- Trigger: eDHR Word 导入重建已有路线、按 Word 顺序重排工序、候选 `batchUseConfigs.batchRecordReports`、候选新增 `clientRouteProcessId` 工序、候选发布投影。
- Preflight check: 先把逐工序正式批记录表单 `batchRecordReports`、表单槽位 `formBindings` 和工序开始配置作为三条独立来源冻结。旧工序必须按 `processId + occurrence` 唯一映射，并只更新候选节点引用；既有正式绑定的 `permissionScopeId`、`recordCategorySnapshotHash`、`slotConfigSnapshotHash` 必须原样保留。Word 新增工序只能在候选中使用 `clientRouteProcessId`，发布时先创建正式 `routeProcessId`，再以正式工序身份创建权限范围和两类冻结 hash。
- Blocker: 旧配置无法唯一映射、候选缺少独立 `batchRecordReports/formBindings` 数组、缺少明确的工序开始配置数组、旧绑定权限或冻结 hash 被临时候选身份覆盖、或新增工序准备把 `clientRouteProcessId` 发布为 `permissionScopeId` 时必须在修改 ACTIVE 前 fail-fast。
- Verification: 数据库回归必须让同一 `processId` 出现多次并验证按 occurrence 保留各自正式绑定、权限范围和两类 hash；发布回归必须分别覆盖旧工序原值保留、新工序正式权限范围建立、正式批记录与 `formBindings` 独立投影、START 配置保留、无 END 业务绑定，以及缺快照时所有正式路线写入均未发生。
- Forbidden action: 禁止用 `formBindings`、默认 `MAIN`、工序开始配置或 Word 新报表替代旧工序正式批记录绑定；禁止重算或覆写旧绑定权限范围和冻结 hash；禁止发布负数 `clientRouteProcessId`、从 ACTIVE 回填缺失候选快照、吞掉映射或权限创建失败。
- Evidence: `doc/tasks/task-6586818a22-20260814T121328/verification-report.md`。

### 工艺路线候选产品快照必须保存正式产品身份

- Trigger: 编辑工艺路线候选版本的“关联产品”、Word 导入创建或更新路线候选、`configSnapshots.products`、关联确认提示“系统异常”或 Fastjson 解析产品名称失败。
- Preflight check: `configSnapshots.products` 的每一项必须是包含正式 `itemId` 的结构化对象；候选创建、导入和后续增删改必须复用同一快照合同。若历史候选仍保存产品名称字符串，只允许从当前路线已有的正式产品绑定重建对应结构化对象，并保留候选中已经存在的结构化配置；不能按产品名称查物料或覆盖候选已有值。
- Blocker: 产品项不是结构化对象、缺 `itemId`、历史名称快照无法从当前路线正式绑定重建、或正式绑定本身缺产品身份时必须 fail-fast；不得把名称当 JSON 解析、按名称猜 `itemId`、忽略错误或返回成功。
- Verification: 后端回归必须覆盖名称字符串复现失败、从正式路线产品绑定重建后按 `itemId` 保存、候选已有结构化配置不被正式投影覆盖；Word 导入静态或集成合同必须断言产品快照来自正式路线产品绑定而不是产品名称列表。真实页面应在指定 DRAFT 候选中按产品编号确认关联，断言写接口业务码为 `0`、页面无“系统异常”，并只读核对最终候选快照的产品项全部为含 `itemId` 的对象。
- Forbidden action: 禁止在保存端用产品名称查主数据补身份，禁止把异常名称字符串当空列表，禁止回退 ACTIVE 快照覆盖 DRAFT 差异，禁止只凭前端成功提示或只看接口 HTTP 200 放行。
- Evidence: `doc/tasks/20260813-route-product-confirm-system-error/verification-report.md`。

### 删除工艺路线前必须结束开放候选

- Trigger: 删除停用工艺路线、路线下仍有 `DRAFT`、`READY_TO_PUBLISH` 或 `PENDING_APPROVAL` 候选、只读巡检发现父路线已删除但候选仍开放。
- Preflight check: 路线删除事务必须先锁定并读取唯一未结束候选；`DRAFT` 和 `READY_TO_PUBLISH` 必须先通过正式候选生命周期服务进入 `CANCELLED`，同步取消 controlled content candidate ref 并写取消审计，再删除父路线及其配置；`PENDING_APPROVAL` 必须阻止删除。候选生命周期处理失败时，整笔路线删除必须回滚。
- Blocker: 出现多个未结束候选、审批中的候选、候选状态并发变化、原生候选与 controlled content 引用状态不一致、或取消审计无法写入时必须 fail-fast。历史孤立候选缺 controlled content 引用时应作为数据异常单独审计和精确修复，不得补造无法证明的引用。
- Verification: 后端回归必须断言候选取消发生在父路线删除之前，覆盖无候选、草稿、待发布和待审批状态；数据库巡检必须断言已删除路线下未结束候选数为零，并分别核对原生生命周期、controlled content 引用和取消审计。涉及存量修复时必须按版本 ID、租户、路线、状态、快照哈希和预期影响行数保护事务。
- Forbidden action: 禁止只软删除父路线而遗留开放候选，禁止物理删除候选、直接改 `deleted` 隐藏问题、吞掉生命周期异常、把审批中候选默认取消，或为历史缺失记录编造 controlled content 引用。
- Evidence: `doc/tasks/20260813-route-candidate-snapshot-batch-repair/verification-report.md`。

### 禁用路线恢复前必须同时校验 ACTIVE 关系图与候选快照

- Trigger: Word 仅导入工艺路线需要恢复唯一禁用路线、路线已存在 `ACTIVE + DRAFT`、启用校验报告关系图无效、或候选关系图读取报告快照不完整。
- Preflight check: 在执行恢复启用或真实导入前，必须分别只读校验当前 ACTIVE 关系图和未结束候选快照；ACTIVE 至少应有完整工序链、`START -> 首工序` 与 `末工序 -> END` 边界关系并通过正式关系图校验，DRAFT 必须能按候选版本 ID 读取完整快照。已发布 ACTIVE 继续保持只读，关系图修改只允许写入 DRAFT 候选。
- Blocker: ACTIVE 关系图缺少起止边、DRAFT `routeSnapshotJson` 不完整、正式页面只能读取 ACTIVE 且只能写 DRAFT，或候选状态不允许写入时必须停止恢复和导入；这是数据治理闭环阻塞，不能用“关键工序可选”或单元测试通过替代真实数据修复。
- Verification: 后端回归分别覆盖禁用路线通过正式状态服务恢复、关系图无效时恢复失败且不写候选、同源 DRAFT 只更新原 ID；真实 E2E 先从页面确认 ACTIVE/DRAFT 数量，再通过正式页面完成可写候选修复、发布或其它已批准治理闭环，最后验证启用和 Word 连续导入。若没有正式页面入口，报告具体缺失边和候选快照错误，不得宣称恢复验证通过。
- Forbidden action: 禁止直接 SQL 补边、改候选快照/生命周期/引用状态，禁止放宽 ACTIVE 只读守卫，禁止调用底层保存接口绕过候选版本治理，禁止把 API-only 修复冒充真实页面 E2E。
- Evidence: `doc/tasks/20260812-word-route-disabled-restore-dedup/verification-report.md`。

### QA 规程手动绑定必须允许已发布路线

- Trigger: QA 规程适用范围手动绑定工艺路线、`data-qa-regulation-manual-route-bind`、`saveQaRegulationRouteProductByItem`、`save-qa-regulation-route-by-item`、已发布路线不能选择、`已启用，仅回显`。
- Preflight check: QA 规程只允许手动绑定“工艺路线”这一正式产品路线关系；路线版本、质检工序、SOP、生产系数和批记录绑定仍必须从已发布路线自动解析。QA 适用工序解析顺序必须显式、可追溯：唯一 `checkFlag=true`、单一正式工序、唯一启用 BATCH `batchRecordReports`、唯一发布投影 `batchRecordReportId/code/name`、唯一路线 `keyFlag=true`；任一候选出现多个都必须 fail-fast，不得猜测。QA 下拉可复用 `getRouteItemBindingList` 候选，但不得按 `CommonStatusEnum.ENABLE` 禁用已发布路线；选择 DCC 项目时必须用 `getRouteProductByItem` 读取到的正式 `routeProduct.routeId` 回填手动绑定下拉默认值；保存必须调用 QA 专用 `saveQaRegulationRouteProductByItem`，后端校验路线存在且有 ACTIVE 版本，不调用 `validateRouteNotEnable`，保存后必须重新读取 `getRouteProductByItem` 并以重读结果作为默认绑定。
- Blocker: QA 下拉把已发布/已启用路线置灰、选择 DCC 项目后不回显已有正式绑定、仍调用 `saveRouteProductByItem` 导致 `PRO_ROUTE_IS_ENABLE`、后端 QA 方法缺少 ACTIVE 版本 fail-fast、绑定后只用本地选择值展示、缺 `checkFlag` 时未按正式批记录或唯一 `keyFlag` 链路解析、多个关键工序仍继续猜测、或把黄框字段重新开放为手工输入时必须停止。
- Verification: 前端静态契约必须断言 QA 页面不再禁用 `CommonStatusEnum.ENABLE`、不显示“已启用，仅回显”、选择 DCC 项目会把正式 `routeProduct.routeId` 赋给 `manualQaRouteBinding.routeId`、调用 `saveQaRegulationRouteProductByItem` 并重读当前绑定、无 `checkFlag` 路线按正式批记录/发布投影/唯一 `keyFlag` 顺序解析且不使用 `formBindings`；后端回归必须覆盖 QA 新建绑定、修正既有绑定、缺 ACTIVE 版本失败、Controller QA endpoint 和不调用 `validateRouteNotEnable`。
- Forbidden action: 禁止放宽产品维护页 `validateRouteNotEnable` 来满足 QA；禁止用前端本地值、默认路线、`formBindings`、批记录表单、空成功或吞异常冒充 QA 绑定成功。
- Evidence: `doc/tasks/20260805-qa-regulation-publish-fix/verification-report.md`。

### 零排产活跃订单必须使用发布态正式路线

- Trigger: 生产组长活跃订单候选/新增、已确认生产工单没有有效排产工单、`MesTeamLeaderActiveOrderServiceImpl`、`mes_pro_route_product`、`mes_pro_route_version.route_snapshot_json`。
- Preflight check: 先按生产工单产品读取唯一未删除的 `mes_pro_route_product` 正式绑定，再读取该路线唯一 `active=1 AND lifecycle_status=ACTIVE` 版本；若未删除绑定指向已删除路线，必须先把它当作孤儿正式关系只读暴露并精确修复，不能让它参与“唯一绑定”判断。工单产品 ID 与 QA 产品 ID 不同时，必须读取该正式路线的全部产品绑定和 MES 物料编码，以物料编码与启用 DCC 项目代码做精确等值匹配；只接受 `productMasterId` 非空且唯一的 DCC 项目，再把路线产品 ID 与该 DCC `productMasterId` 组成 QA 查询范围，并只保留精确命中当前 ACTIVE 路线/版本、`PUBLISHED` 且有当前版本的规程。最终只允许一个 QA 产品上下文，取消工单必须在加载路线、DCC 和 QA 前先行阻断。运行工序、顺序和数量系数必须从发布快照 `configSnapshots.flowGraph.nodes` 与 `scheduleUseConfigs` 逐项匹配，ERP 数量必须来自生产工单正式字段；QA 规程只提供 DCC 项目、QA 工序、检验项目和检验规则，不要求 QA 主规程绑定 MES 生产工序；PQC 任务需要的生产工序身份必须来自活跃订单冻结路线快照，不能用当前 `mes_pro_route_process` 重建后的新 ID 代替。零排产不得以 ERP 计划开工时间为空或 PQC 业务日期为由拒绝候选/新增；PQC 非空记录日期使用已落库活跃订单的 `joinedAt` 日期。有一条有效排产时优先使用工序 `planDate`，`planDate` 为空时不得阻断候选/新增，PQC 业务日期使用已落库活跃订单的 `joinedAt` 日期。候选资格和新增写入必须复用同一个路线来源解析契约。宽关键词可能命中大量工单时，候选数量上限只能在全部匹配项完成正式路线、唯一 ACTIVE 版本、DCC 项目和已发布 QA 资格解析，并按资格优先排序后应用；数据库状态排序不能替代正式资格排序。
- Blocker: 产品无绑定/多绑定、绑定只指向已删除路线、ACTIVE 版本缺失/不唯一、路线无法精确匹配唯一启用且已绑定产品主数据的 DCC 项目、当前 ACTIVE 路线版本的已发布 QA 产品上下文缺失/不唯一、快照节点与 SCHEDULE 配置集合不一致、PQC 规程未按发布快照 routeProcessId 建档、明确适用末检却缺 FINAL 项目、同一 QA 工序内部 FIRST 数量不唯一或 PATROL 比例不唯一、工序重复、没有启用工序、数量系数非正数、ERP 数量非正数或正式 PQC 规程缺失时必须 fail fast。不同 QA 工序之间 FIRST 数量或 PATROL 比例不同不是 blocker。ERP 计划开工时间缺失不是零排产 blocker；历史 PQC 发布版本 `finalInspectionApplicable=null` 不再单独阻塞。有效排产工单为 1 条时继续使用排产路线/版本/工序计划；大于 1 条时仍按冲突阻塞。
- Verification: 后端测试至少覆盖零排产成功、工单产品与 QA 产品 ID 不同但经正式路线和唯一 DCC 项目解析成功、旧路线版本 QA 排除、DCC 缺失/歧义、QA 产品上下文歧义、取消工单先行阻断、孤儿路线绑定不参与有效路线唯一性、缺绑定、缺 ACTIVE 版本、快照不完整、ERP 计划开工时间为空仍可加入且 PQC 日期等于活跃订单 `joinedAt` 日期、单排产 `planDate` 有值继续使用工序 `planDate`、单排产 `planDate` 为空时使用活跃订单 `joinedAt` 日期和多排产冲突；宽关键词回归必须构造超过候选上限的匹配项，并让符合资格的目标工单位于原始数据库排序上限之外，证明最终仍按资格排序进入返回结果。真实 E2E 必须通过页面按工单号和宽关键词分别搜索并确认候选资格，写入型验收还必须使用任务自有工单，并只读核验工序快照数量系数/计划数量、PQC 任务生成、PQC 任务 routeProcessId 来自发布快照、FIRST/PATROL/FINAL 任务数量和业务日期，最后精确清理任务数据。
- Forbidden action: 禁止把零排产或跨产品 QA 解析实现为默认路线、任取第一条绑定/版本/DCC 项目、产品名称匹配、项目名称匹配、前缀或模糊匹配、跨路线 QA、读取草稿当前配置、用当前路线工序表 ID 替代发布快照 routeProcessId、默认数量系数、默认 QA 规程、用需求日期或未落库的临时时间猜测 PQC 日期、继续要求 ERP 计划开工时间、空工序成功、前端文案放宽或 API-only 成功；禁止在 mapper 或其它资格解析之前按固定条数截断宽关键词结果，也禁止仅把“已确认”状态提前排序后继续资格前截断。
- Evidence: `doc/tasks/20260807-active-order-without-schedule-order/verification-report.md`；`doc/tasks/20260808-pressure-pump-active-orders/verification-report.md`；`doc/tasks/20260809-active-order-route-dcc-qa-resolution/verification-report.md`。

### FIFO 自动分配当前工序快照边界

- Trigger: 生产组长报工管理、`FIFO 自动分配`、活跃订单提示“缺少当前工序生产系数和目标数量快照”、同一基础工序 `processId` 但不同 `routeProcessId`、`MesTeamLeaderFifoAllocationService`、`MesTeamLeaderOrderProcessTargetService`。
- Preflight check: 修改 FIFO 预览或报工确认前，先区分自动候选和指定确认：FIFO 预览只能消费含当前 `routeProcessId + processId` 快照的活跃订单，缺当前 routeProcess 快照的较早活跃订单应视为当前预览不可分配并继续后续候选；手工/最终确认指定的活跃订单仍必须用 `requireTarget` fail-fast。生产系数未显式设置时按业务默认 `1` 归一，目标数量缺省按 ERP 数量乘以生产系数派生；非正 ERP 数量、非正系数或非正目标数量仍必须失败。
- Blocker: FIFO 预览因为非当前 routeProcess 的活跃订单直接阻塞当前工序、手工确认把缺当前快照的指定订单静默成功、生产系数缺省被当成非法空值、非正数量被默认成功、或预览和最终确认共用同一宽松路径时必须停止并补回归。
- Verification: 后端回归必须同时覆盖“FIFO 预览跳过不含当前 `routeProcessId + processId` 快照的活跃订单并继续分配”“缺省生产系数按 `1` 派生目标数量”“非正系数仍失败”“最终确认/手工指定仍 fail-fast”，并复跑 FIFO 闭环和 PQC 分配相邻测试。
- Forbidden action: 禁止为了消除 FIFO 报错而全局默认目标数量、吞掉 `requireTarget` 错误、用基础 `processId` 替代 routeProcess 身份、把缺正式快照的指定订单当成功、改前端隐藏错误或用 API-only 说明替代服务回归。
- 真实页面验收门禁：点击活跃订单模拟按钮前，必须从生产组长活跃订单池真实搜索并确认目标产品候选存在有效的当前工序生产系数和目标数量快照；若页面提示快照缺失或没有候选，必须在零业务写请求前记录 BLOCKED，不能用旧活跃订单、API-only、JSON fixture 或直接数据库补值代替正式来源。
- Evidence: `doc/tasks/20260808-fifo-active-order-process-target/verification-report.md`。

### 工序共享分配池与旧报工终结链路边界

- Trigger: 生产组长报工分配、共享数量池、`allocation/confirm`、旧报工确认、PQC 质量门禁、正式批记录回填、跨订单目标工序上下文。
- Preflight check: 先区分“共享分配保存”和“旧报工确认终结”两个写入职责。共享分配只读取生产报工的正式输出数量，按目标活跃订单自身 `routeProcessId + processId` 保存版本化分配、完成量、数量碎片和调整审计；来源报工只提供数量/字段值，不能把来源工序 ID 当成目标订单工序 ID。最终确认的行数量是期望分配量，实际写入量必须按 `min(本行期望量, 目标订单当前工序剩余量, 报工池逐行消费后的当前可用量)` 计算；按请求/FIFO 顺序逐行消费池余量，未消费数量继续保留在来源报工池，不得因为任一行期望量大于订单剩余量或池余量而拒绝整次确认。共享分配不得隐式调用旧 PQC 或批记录回填门禁；旧确认接口仍独立执行既有质量和批记录规则。调整审计字段为非空时，复核说明为空必须由服务按分配模式写入明确系统原因，不能让数据库默认值或前端必填假设决定事务成败。
- Blocker: 共享分配因期望分配量超过目标订单当前工序剩余量或报工池当前可用量而整次拒绝、未按请求/FIFO 顺序消费池余量、把未消费数量从报工池扣除、因缺旧 PQC/批记录配置、已退出排产池的订单、来源与目标 routeProcess 不同而错误拒绝，或为绕过拒绝而放宽旧确认质量门禁、静默跳过目标上下文、吞异常、写入默认成功时必须停止并补回归。旧确认质量/批记录规则被共享分配改弱也必须停止。
- Verification: 后端回归必须分别覆盖期望分配量超过订单当前工序剩余量时按剩余量写入且报工池保留余量、订单剩余量充足但池余量不足时只写池余量且下游只消费实际量、共享分配无 PQC/批记录配置仍可提交、目标订单无排产记录仍按活跃订单快照完成、来源与目标 routeProcess 不同的目标上下文、旧确认继续执行 PQC/批记录门禁，以及空复核说明的审计原因落库；真实 Playwright 必须验证 FIFO 保存、未放行手动调整、余量留存、报工管理分配订单列和历史投影。
- Forbidden action: 禁止把旧确认终结服务作为共享分配的隐式前置条件，禁止用 `formBindings`、默认批记录槽位、来源事件 routeProcess、前端空说明或数据库默认值替代正式目标上下文和审计原因，禁止用 API-only、SQL 或 mock 冒充真实页面成功。
- Evidence: `doc/tasks/20260809-process-report-shared-allocation-pool/verification-report.md`。

## MES 生产人员档案正式工重复关联门禁

### 同一组长正式工关联必须先业务拒绝再写库

- Trigger: 生产人员档案、班组员工、一线生产员工弹窗、`getFrontlineRuntimeConfig().employees`、正式工搜索关联、全量用户下拉、跨部门正式工、临时工/正式工统一候选、`getUserListBySubordinate`、`getUserListByNickname`、`mes_pro_process_pool_team_employee_profile`、`system_user_id`、`employee_code=USER-<id>`、DuplicateKeyException、重复关联返回 500。
- Preflight check: 先明确正式工候选范围是“组长下属”还是“全量系统用户”；候选查询与提交关联校验必须使用同一范围，不能只放开下拉。一线生产员工弹窗、运行配置员工和切换员工校验必须同源于当前负责生产组长启用的生产人员档案。生产员工账号请求一线工序时，必须先按启用人员档案 `systemUserId -> 唯一 leaderUserId` 解析负责生产组长，再读取该组长在正式“工序开始”配置中负责路线下的全部工序；禁用档案、多组长归属或组长无正式负责工序必须显式失败，不得回退本人岗位、设备账号路线或历史工序员工绑定。即使设备/工序 scope leader 与当前负责组长不同，员工弹窗也不得切到设备 scope leader，不得用工序员工绑定或设备账号候选替代人员管理列表。新增正式工关联前还必须按当前 `leaderUserId + systemUserId` 查询现有未删除生产人员档案，并区分“已禁用可启用既有档案”和“从未关联可新增”；显示名唯一校验不能替代正式用户唯一关联校验。新增正式工/临时工档案只建立当前组长名下档案，不得要求该员工已在负责员工范围内。生产组长员工 scope 必须同步写入与正式提交一致的 `actualEmployeeId`：正式工使用 `systemUserId`，临时工使用人员档案 `id`，测试不得只断言其中一种身份。
- Historical data gate: 当运行时代码已保证新增/启停人员同步 `PRODUCTION/EMPLOYEE` scope，但旧人员档案仍缺范围时，必须用正式幂等数据迁移按 `tenant_id + leader_user_id + COALESCE(system_user_id, profile.id)` 补齐；迁移前应阻塞重复派生身份、重复有效范围和档案/范围启用状态错位，迁移后验证全量覆盖并复跑幂等。组长报工读取仍只消费正式 scope，禁止改成读取人员档案推断、管理员全量可见或查询时自愈写入。
- Blocker: 全量候选可见但提交仍按 `getUserListBySubordinate` 拒绝、候选只靠前端过滤、生产员工仍需逐工序绑定才能看到工序、员工档案存在却进入设备/岗位路线来源、一线生产弹窗和生产组长人员管理列表不一致、弹窗能选但 `switchActualEmployee` 按另一来源拒绝、重复正式工关联落到数据库唯一键异常、接口返回 500、禁用旧档案后再次新增同一系统用户、创建人员阶段触发 `assertCanAccessEmployee` / `assertCanMaintainProcess`、范围拒绝仍返回“员工或工序”混合文案，或只靠前端禁用按钮阻止重复时必须停止并补后端 RED/GREEN。
- Verification: 后端回归必须覆盖生产员工继承唯一负责组长的全部正式路线工序、禁用/多组长/无负责工序失败且不触发设备路线来源、候选数据源、运行配置员工来源、切换员工校验同源、空关键字不触发无条件全量扫描、重复正式工在 `employeeProfileMapper.insert` 前抛业务错误，且成功正式工路径仍不保存签名密码；前后端合同必须断言员工工序绑定写入口和运行时读取均已移除。新增人员成功路径必须断言不调用负责范围校验；跨模块新增 `AdminUserApi` 方法时必须用 `-am` 编译所有上游测试手写实现。真实 E2E 重跑时使用新的任务自有正式工候选或先明确启用既有档案。
- Forbidden action: 禁止只改候选接口不改关联校验，禁止让前端加载全系统用户后本地过滤，禁止保留员工工序绑定入口或用工序员工绑定冒充人员管理列表和工序授权，禁止生产员工归属异常时回退岗位/设备路线，禁止为接口扩展增加默认空列表兼容 fallback，禁止 catch DuplicateKeyException 后返回默认成功，禁止创建重复正式工档案，禁止把正式工重复关联伪装成显示名重名，禁止用“员工或工序”混合错误掩盖实际越权目标。
- Evidence: `doc/tasks/20260805-production-personnel-management/verification-report.md`、`doc/tasks/20260805-production-personnel-full-user-dropdown/verification-report.md`、`doc/tasks/20260806-production-employee-create-scope-fix/verification-report.md`、`doc/tasks/20260806-frontline-production-employee-options-match-leader-personnel/verification-report.md`、`doc/tasks/20260807-production-employee-inherits-leader-processes/verification-report.md`；目标测试 `MesTeamLeaderRuntimeConfigServiceTest#shouldRejectDuplicateFormalUserBeforeDatabaseInsert`、`MesTeamLeaderRuntimeConfigServiceTest#shouldSearchFormalCandidatesFromAllSystemUsers`、`MesTeamLeaderRuntimeConfigServiceTest#shouldLinkFormalUserWithoutStoringSignaturePassword`、`MesTeamLeaderRuntimeConfigServiceTest#shouldCreateTemporaryProductionPersonWithSignaturePasswordHashAndAudit`、`MesTeamLeaderScopeServiceTest#shouldRejectOutOfScopeEmployeeAccess`、`MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_returnsEnabledLeaderPersonnelProfilesInsteadOfOnlyProcessBindings`、`MesFrontlineProductionEmployeeLeaderProcessScopeTest`。
- Historical evidence: `doc/tasks/20260809-frontline-submit-leader-visibility/verification-report.md`，历史范围迁移后由真实生产组长页面证明正式提交事件可见，未复核记录不进入报工历史。

## MES PQC组长人员范围与管理数据可见性门禁

### PQC管理必须按唯一启用人员范围读取

- Trigger: PQC组长人员管理、PQC管理列表、指定租户管理员看不到一线 PQC 提交、`pqc_permission`、`mes_pro_process_pool_team_leader_scope`、`leader_type=PQC`、`scope_type=EMPLOYEE`。
- Preflight check: 先确认目标提交、实际检验员和查看账号属于同一租户，再核对实际检验员是否具有启用的 `pqc_permission` 角色，以及是否只存在一个启用的 PQC 组长人员范围。PQC管理读模型只按当前登录组长的启用人员范围读取，不因账号是租户管理员或超级管理员而自动全量可见。把检验员调整到另一组长时，必须先通过正式人员管理禁用原范围，再由目标组长通过正式候选搜索和关联页面创建新启用范围。
- Preflight detail: 若 `PQC管理` 页面显示 `No Data` 但怀疑测试数据仍存在，先比较页面请求的 `submitDate` 与 `mes_pro_process_pool_event.server_submit_time`；列表按正式提交事件时间过滤，不按 PQC 任务 `business_date` 推断。恢复任务自有测试 fixture 时，只能按明确任务标识、事件 ID 和记录 ID 精确更新 PQC 事件与对应 PQC 记录的提交时间，不得改实际检验员、人员范围、角色或任务状态来制造可见性。
- Blocker: 目标数据属于正确租户但当前查看账号范围未包含实际检验员、检验员缺 `pqc_permission` 导致正式候选为空、检验员仍被其他 PQC 组长启用占用，或同一检验员存在多个启用组长范围时必须停止；不得把租户一致误判为列表可见。
- Verification: 使用指定租户/账号真实登录，先在人员管理确认目标检验员为已启用，再进入 `PQC管理` 断言目标业务行和分页响应；只读数据库同时核对角色、唯一启用范围、提交事件实际检验员和 tenant_id。角色和人员范围写入必须走真实页面，API/数据库只用于最终只读复核。
- Forbidden action: 禁止给管理员增加全量可见 fallback，禁止直接改事件实际检验员、直接插入人员范围或角色关系，禁止保留多组长同时启用，禁止用另一组长页面或仅租户 ID 证明指定账号可见。
- Evidence: `doc/tasks/20260807-pqc-leader-management-five-records/verification-report.md`；`doc/tasks/20260808-restore-pqc-management-test-data/verification-report.md`。

## 禁止做法

- 禁止跨模块复制业务逻辑来绕过现有服务边界。
- 禁止未核对 schema 就写运行 SQL。
- 禁止捕获异常后静默返回成功、空数据或默认数据。
- 禁止缺少依赖或测试数据时跳过验证并宣称完成。

## 2026-07-25 子表集合替换软删除唯一键门禁

- Trigger: 后端更新父表时先删除再重建子表集合，且子表存在 `case_id + sort`、`parent_id + code`、`tenant_id + key` 等唯一约束，并启用了 MyBatis Plus 逻辑删除。
- Preflight check: 先核对 mapper 删除方式、唯一索引字段、逻辑删除字段是否参与唯一索引；集合替换语义若要求同一唯一键可重建，删除必须释放真实唯一键占用。
- Blocker: 逻辑删除记录仍占用唯一键且后续插入使用相同 key 时，不得用 catch、重试、跳过插入、修改 sort 或前端规避来绕过。
- Verification: 新增或更新后端回归测试，覆盖同一父记录连续两次替换子表集合且第二次使用相同排序或业务键；目标 Maven 测试必须 PASS。
- Forbidden action: 禁止把集合替换失败归因于前端重复提交；禁止为了避开唯一键冲突引入随机排序、默认成功或软失败。
- Evidence: `doc/tasks/20260725-codex-test-method-target-table-rows/verification-report.md`，`CodexTestCaseServiceImplTest#updateCase_allowsRepeatedCheckpointReplacement`。

## 2026-07-27 测试项固定名称删除唯一键门禁

- Trigger: `系统管理 > 测试管理` 使用固定名称反复创建、删除测试项，或表唯一键包含 `tenant_id + name + deleted` 且删除语义需要释放同名占用。
- Preflight check: 修改删除逻辑前先核对唯一索引、逻辑删除字段、运行中 execution 保护和子表清理顺序；若业务要求固定名称可重复闭环，删除必须真实释放同名唯一键占用。
- Blocker: 第二次创建/删除同名测试项触发 `DuplicateKeyException`、删除后仍占用 `deleted=1` 唯一键，或物理删除会绕过运行中执行保护时必须停止。
- Verification: 新增后端回归覆盖同一固定测试项名称连续创建、删除两轮，并复跑测试项管理、执行创建和 Runner 相邻测试。
- Forbidden action: 禁止改成随机名称、吞唯一键异常、前端隐藏删除失败、跳过运行中 execution 校验，或只清子表不释放测试项主表唯一键。
- Evidence: `doc/tasks/20260727-codex-test-node-chain/bug-regression-evidence.md`，`CodexTestCaseServiceImplTest#deleteCase_allowsRepeatedCreateAndDeleteWithSameName`。

## 2026-07-25 Maven Reactor 兄弟模块验证门禁

- Trigger: 多模块 Maven 项目中当前模块依赖兄弟模块，出现缺方法、缺字段、DO/DTO builder 不一致、或测试编译引用 sibling module 新接口时。
- Preflight check: 先确认失败符号所属模块；若符号来自同 reactor 兄弟模块，必须用 `mvn -pl <module> -am ...` 重跑，让 Maven 同时构建依赖模块。
- Blocker: `mvn -pl <module> ...` 因未构建兄弟模块而失败时，不得直接判定为产品代码阻塞；必须复验 `-am` 后再给结论。
- Verification: 任务日志同时记录窄范围失败、`-am` 复验命令、PASS/FAIL 结果和影响模块。
- Forbidden action: 禁止用旧本地产物、跳过编译、API-only、或改 unrelated sibling 代码来掩盖 reactor 构建边界问题。

## 2026-07-27 Windows Maven 增量输出删除卡住门禁

- Trigger: Windows 上目标 Maven 命令长时间无输出，`jcmd <pid> Thread.print` 显示主线程停在 `IncrementalBuildHelper.beforeRebuildExecution` 和 `WinNTFileSystem.delete0`。
- Preflight check: 先确认 Maven PID、父进程、启动命令和是否属于当前任务；检查同仓并发 Maven，但不得停止其他任务进程。
- Blocker: 目标 Maven 超时且未生成 surefire 报告时，不得宣称测试通过；只允许停止当前任务启动的 Maven PID，并记录命令、PID 和诊断栈。
- Verification: 保持项目标准 Maven 参数重新运行目标测试，必须得到明确 `BUILD SUCCESS` 和测试计数；一次关闭增量编译后的全量编译失败不能替代标准参数复验。
- Forbidden action: 禁止强杀所有 Java/Maven 进程、删除其他任务构建产物、用静态检查冒充 JUnit 通过，或把 `-Dmaven.compiler.useIncrementalCompilation=false` 固化为产品构建 fallback。
- Evidence: `doc/tasks/20260727-remove-lfs-assets/verification-report.md`。

## 2026-09-03 Maven 目标单测外部源文件编译阻塞门禁

- Trigger: 目标 Maven 单测在进入测试前因同模块 Java 编译错误失败，且报错文件不属于当前任务已授权修改范围。
- Preflight check: 先用 `git status --short` 和 `rg --files` 确认报错文件归属、是否未跟踪、缺失符号是否真实存在；只把任务自有文件纳入修复范围。
- Blocker: 无关未跟踪或并行任务源文件参与同模块编译并导致失败时，必须记录具体文件、缺失符号和 Maven 命令；不得宣称目标单测通过。
- Verification: 外部编译阻塞解除后，使用原目标 Maven 命令复跑并取得明确 `BUILD SUCCESS` 与测试计数，才可把该测试标为 GREEN。
- Forbidden action: 禁止用 Maven excludes、跳过编译、删除/改写无关未跟踪文件、或把单个已通过测试结果冒充整个目标服务测试通过。

## 业务修订审计身份服务端归属门禁

- Trigger: 新增或修改原始记录补正、报工修改、数据修订、重新签名、字段差异日志或其它需要审计身份的业务接口。
- Preflight check: 接口请求只接收业务字段、修改原因和当前用户签名凭据；当前登录人必须由控制器或安全上下文写入内部命令，服务端负责校验业务范围、签名密码、签名 actor、字段差异、修改后 payload 和字段级业务锁。历史业务对象必须按提交时的版本/路线/工序快照修订，不能改绑最新发布版本。生产报工补正不得把最新复核状态 `APPROVED` 当作绝对修改锁；确认通过后仍可按正式补正链路记录差异和签名，但影响已 FIFO 分配数量的字段仍必须由数量片段锁拒绝。
- Optional equipment context: 生产报工修改、确认和分配不得把设备、设备状态、设备参数规则、`rawPayload.equipmentParameters`、`fieldValues.DEVICE_PARAMETERS`、`deviceParameterReadings`、`deviceParameterReadings.value` 或其它设备参数副本当作硬性前置条件。工序无设备、后续新增设备、设备停机维护、历史审计副本缺失或历史参数值为空时，仍应按完成数量、损耗、权限、签名和正式分配规则处理；前端不得用 `Number(null)` 把空参数伪造成 0，提交其它修改时只发送有正式数值的参数；已有参数发生修改时，只需以正式设备参数明细生成字段差异日志，并在副本缺失时补充审计副本，副本格式异常时不得覆盖原内容或阻断主业务修改。
- Nullable context propagation: 当正式业务主链允许 `workOrderId` 等上下文为空时，所有复制该上下文的事件、数量、修订、审计和差异持久化表必须同步核对可空合同；新增或放宽主链字段时，应盘点同事务写入及后续补正链路，不能只修改入口表。
- Blocker: 客户端可以提交或覆盖 `modifiedByUserId`、签名用户、签名 ID、签名快照、`afterPayload` 或 `changedFields`，服务端未校验当前用户业务范围，非生产报工补正的已生效审批记录缺少正式状态策略，生产报工补正绕过 FIFO 数量片段锁，允许空上下文在修订/审计表仍为 NOT NULL，或历史记录跟随最新配置漂移时必须停止。
- Verification: 后端合同测试负向断言请求 VO 不含审计身份和派生字段；服务测试覆盖当前用户范围、密码签名、actor 一致性、无变化拒绝、字段级锁定、字段差异、快照绑定和受影响业务片段同步；生产报工修改测试还必须覆盖确认通过后仍允许正式补正、无设备、设备参数副本缺失、历史参数值为空时只修改其它字段、历史空值被明确填入时的修改日志，以及已有参数修改日志；主链允许无工单时，迁移合同还必须覆盖修订审计表可空、幂等和缺表列 fail-fast；前端合同必须锁定空值不转零且提交时过滤无正式数值参数，真实路径只展示业务字段。
- Forbidden action: 禁止把前端隐藏内部输入框当作服务端安全边界，禁止信任客户端生成的审计身份或 JSON，禁止用最新配置覆盖历史快照，也禁止为兼容旧页面保留双写身份字段；禁止因设备、设备参数副本缺失或生产报工已确认通过而拒绝生产组长修改或分配；禁止为通过修订表 NOT NULL 约束而默认、推断或伪造工单 ID。
- Evidence: `doc/tasks/20260812-production-correction-equipment-nonblocking/verification-report.md`。

## 持久化列表相邻手动排序门禁

- Trigger: 业务列表增加上移、下移、置顶、置底或拖拽排序，并要求刷新、重新登录或分页后顺序保持。
- Preflight check: 先冻结正式排序范围和身份边界，例如 `tenantId + ownerUserId`；数据库增加非空正式排序字段并按旧列表的确定性顺序迁移历史数据。写接口只能从安全上下文取得当前操作者/负责人，在事务内锁定当前范围的正式列表，只交换目标记录与相邻记录的排序值；列表读取必须以正式排序字段为第一排序键。前端边界禁用应按完整正式列表判断，写成功后重新读取列表，不能在本地数组中伪造持久化结果。
- Blocker: 正式排序字段缺失、为空或重复，目标记录不属于当前范围或已失效，没有相邻记录，条件更新行数不符合预期，或运行库尚未应用正式迁移时必须明确失败；不得按加入时间、ID、前端数组下标或客户端提交的负责人身份继续写入。
- Verification: BDD/TDD 至少覆盖上移、下移、首末边界、越权/失效记录、并发条件更新失败、新增/重新激活记录进入序列末尾、历史顺序确定性迁移；前端运行聚焦合同和类型检查，真实写入 E2E 仅使用任务自有测试数据并从可见业务行执行操作。
- Forbidden action: 禁止只用 `splice`/`sort` 做前端临时排序，禁止改写加入时间或业务 ID 冒充顺序，禁止边界请求返回默认成功，禁止吞掉并发冲突，禁止在缺迁移时增加兼容旧 schema 的 fallback。
- Evidence: `doc/tasks/20260809-active-order-manual-sorting/verification-report.md`。

## 跨模块版本对象生命周期引用登记门禁

- Trigger: DCC 文件、工艺路线、批记录、QA 规程或其它版本对象被跨模块发布、冻结、作废、停用、解绑或删除。
- Preflight check: 先盘点消费者已经保存的不可变 ID、版本字段、发布快照、任务快照和现有引用表，再判断是否真的缺少关系。消费者必须先在自己的候选/版本快照中保存精确来源，之后才能登记依赖；没有精确来源时先补正式选择与冻结链路，不能先建通用登记平台再猜关系。
- Historical migration rule: 旧版本号格式合法或可以排序，只能证明“版本文本可解析”，不能证明版本链可以自动迁移。AUTO_MAP 必须同时满足稳定业务身份唯一、源文件 ownership/hash 证据完整、正式指针一致、检出状态无歧义及平台生命周期引用对齐；任一证据缺失时整条 Master 进入 blocker，先治理正式证据再重跑只读盘点，禁止按 `source_file_id` 存在、当前文件名、最新版本或共享源文件反推并补齐历史事实。
- Existing-core rule: 优先扩展现有 `ControlledContentLifecycleCoreService`、`controlled_content_version_ref`、转换审计和模块 adapter。需要版本依赖时，dependency 的 provider/consumer 都必须是现有核心中的精确 version ref；缺少某类 consumer ref 时先为该类型补 native master/version/ref 生命周期。禁止另建平行 usage registry、生命周期状态机或运行态生命周期表。
- Entry-point completeness rule: 所有能创建、导入、复制或恢复版本对象的入口，只要会生成初始 `ACTIVE` 版本或开放候选，就必须复用同一领域服务或模块 adapter 同步登记生效引用与候选引用；不得由普通页面入口登记、Word/Excel 导入入口直接 mapper 插入。发现历史对象缺引用时必须显式迁移或修复并保留审计，禁止在提交发布时按当前版本猜测、自愈或静默补写。
- Impact rule: system core 通过明确的模块 impact-provider 契约汇总当前配置、在制和历史影响；provider 缺失、异常、跨租户或返回不完整时必须 fail fast，不能显示“无影响”。运行对象 OPEN/CLOSED 从消费者现有正式状态实时推导，不复制第二套状态。
- Transition rule: 提交变更前做只读影响预检，最终不可逆转换必须在事务锁内重新检查；预检后新增依赖或在制引用时最终转换回滚。当前配置或 OPEN 运行引用是硬阻塞，只有历史 consumer 且运行 CLOSED 时才可沿现有正式转换继续；不设计通用 disposition-plan 或强制绕过。
- Verification: BDD/TDD 至少覆盖精确来源先于登记、所有创建/导入/复制入口的初始生效引用与候选引用、活动引用阻断、旧新版本并存、登记失败回滚、provider 不可用 fail-fast、租户隔离、预检后新增引用的最终复核竞态、历史精确读取和多义遗留只报告不自动修复；真实 E2E 除从提供方变更页面进入影响明细外，还要覆盖导入新建 V1、再次导入生成 V2 候选、提交发布和失败回滚，不能只证明候选快照已生成。
- Forbidden action: 禁止通过解除当前绑定证明历史无引用，禁止用名称、编码、排序、当前版本或“最新一条”补写引用，禁止从当前投影补算历史，禁止把在制对象静默切到新版本，禁止以 `force` 参数、前端隐藏按钮、删除消费者记录或 provider 失败返回空结果绕过生命周期守卫。
- Evidence: `doc/tasks/20260811-p0-business-issues-bdd-tdd-design/remediation/002-route-version-process-identity.md`；`doc/tasks/20260811-p0-business-issues-bdd-tdd-design/remediation/013-dcc-mes-lifecycle-linkage.md`。

## 需求追踪必须校验语义而不是只校验编号门禁

- Trigger: 需求、PRD、AC、BDD、API 合同、子项目和 TDD 计划使用稳定编号交接，或结构校验只统计编号数量、唯一性和覆盖率。
- Preflight check: 先从原始需求逐条建立唯一语义，再为每个 `RQ -> AC -> Given/When/Then -> API -> SP -> 测试标题/方法` 做双向核对；测试编号的权威语义必须来自同一份测试目录或标题清单，辅助 BDD 和接口矩阵只能引用，不能各自重新解释。
- Blocker: 编号数量完整但映射到无关行为、辅助 BDD/API 引用错误测试编号、历史对话只保留“其它都对”却缺被接受的具体建议文本、或开放问题会改变字段/状态/权限/迁移结果时必须判定 FAIL。无法复现来源的扩展规则应转为稳定决策项并保持 blocked，不能写成确定 AC。
- Verification: 除数量/唯一性校验外，增加逐行语义交叉检查；独立 reviewer 必须先读原始需求和当前代码，再读修订文档。计划测试不存在时明确标记 `planned/not-created`，并要求未来 RED 先证明目标测试实际被发现且测试数大于 0。
- Forbidden action: 禁止用“49 个 AC 均有 T 编号”证明追踪正确，禁止用结构 validator PASS 替代业务终审，禁止把零测试、class not found、环境失败或未获确认的默认值当作有效 RED/GREEN。
- Evidence: `doc/tasks/20260814-domestic-registration-certificate-review-closure/final-review.md`。

## 受监管业务文件全出口授权门禁

- Trigger: 受监管业务文件、旧版/失效文件申请查看、全部下载审批、`infra_file` 公共直链、通用非受控文件预览、裸 `fileId`、OnlyOffice 或其它按文件 ID 读取内容的入口。
- Preflight check: 先盘点上传响应、业务预览、公共直链、通用预览、OnlyOffice/分片读取、下载、打印和缓存的全部服务端出口；建立统一业务文件归属查询和访问 Guard，并让每个出口按租户、公司、对象状态、对象级授权和访问模式复核。全局文件表中的裸 ID 不能证明租户或业务归属。
- Blocker: 任一公共/通用入口可绕过业务申请读取文件、上传接口返回永久可访问 URL、跨租户可凭裸 `fileId` 取内容、旧版详情只在前端隐藏、或预览失败静默降级为下载时必须停止发布。
- Verification: 后端安全回归必须覆盖未授权旧版详情/文件、当前版受控预览、所有下载申请、公共直链、通用预览、跨租户裸 ID、授权过期和 OnlyOffice 分片复核；真实浏览器同时检查网络响应、打印/下载按钮、地址复制和缓存，不得只断言按钮隐藏。
- Forbidden action: 禁止只补一个下载控制器、只修改前端、依赖不可猜 ID、把文件 URL 当业务引用、或在在线预览不可用时提供无审批下载作为 fallback。
- Evidence: `doc/tasks/20260814-domestic-registration-certificate-lifecycle-design/verification-report.md`。

## 多租户每日任务失败传播与跨日补扫门禁

- Trigger: 全系统一个 Quartz 任务逐租户扫描、`@TenantJob` 汇总结果、到期/阈值提醒、首次启用、停机跨日恢复、同日重试和幂等通知。
- Preflight check: 区分全局调度配置与租户运行明细；逐租户保存业务日期、状态和错误，任一租户失败必须让顶层 Job 失败或由等价的可观测失败合同承载。扫描条件应查询“阈值日已到且尚无 occurrence”，恢复时只处理当前最高级别并显式记录被抑制的低级别。
- Blocker: 框架吞掉租户异常后仍把 Job 记成功、正常算法只匹配阈值当天导致停机后永久漏发、失败租户无法定向重试、或并发扫描没有数据库业务唯一键时必须停止。
- Verification: 固定时钟测试覆盖月末/闰年、首次启用、停机跨多个阈值、同日失败重试和后续更高阈值；运行态测试证明一个租户失败时租户明细与顶层 Job 均失败，成功租户不重复产生业务事件。
- Forbidden action: 禁止把“部分失败”文本当 Job 成功、用固定天数近似月份、只靠内存锁去重、补发全部历史阈值或要求必须在阈值当天运行。
- Evidence: `doc/tasks/20260814-domestic-registration-certificate-lifecycle-design/verification-report.md`。
- Configured-recipient extension: 当业务允许管理员按阈值或事件配置具体通知人时，接收人名单必须成为租户级正式配置，任务按事件实际命中的阈值读取对应名单；不得继续从 Quartz `handler_param` 中的固定角色、账号或公司范围推断。配置要求接收人具备业务查看权限时，应通过可审计、可同步撤销的动态权益来源维护名单并集；从全部规则移除的用户只撤销该配置来源产生的权益，不得删除其其它角色或授权。配置保存、接收人校验和权益同步必须处于同一事务失败边界，多选投递需按事件与用户唯一键逐人幂等。

## 站内信领域幂等必须延伸到平台消息门禁

- Trigger: 业务 outbox 调用站内信 API、并发重试、消息已写入但领域 ACK 前进程中断、模板禁用返回空消息 ID、要求同一业务事件每个接收人最多一封。
- Preflight check: 为平台消息提供租户内唯一的稳定业务键，幂等发送 API 必须在消息表唯一约束下“新建或返回同一 message ID”；领域 delivery 在调用前保存该键，并校验重放时接收人和模板一致。
- Blocker: 只有领域 delivery 唯一键、平台 API 不接收业务键、发送成功后才标本地 `SENT`、空 message ID 被视为成功、或重放可能再次插入消息时必须阻塞上线并先改平台契约。
- Verification: 注入“平台消息落库后、领域 ACK 前崩溃”，重启重放后断言平台消息数仍为 1且返回同一 message ID；并覆盖并发、模板禁用、同键不同接收人/模板冲突和跨租户同键隔离。
- Forbidden action: 禁止用延时、查消息文案、内存标记、先发后标记或吞掉空 ID 代替平台数据库唯一约束。
- Evidence: `doc/tasks/20260814-domestic-registration-certificate-lifecycle-design/verification-report.md`。

## 候选快照全局配置组完整性门禁

- Trigger: 候选版本中的路线级配置允许一个逻辑配置组覆盖全部普通工序，客户端仍通过分工序批量保存或局部保存更新候选快照。
- Preflight check: 先合并请求与既有候选数据形成完整候选快照，再按显式组身份校验覆盖范围、每工序唯一成员和可编辑配置一致性；服务端生成的绑定身份、模板发布版本、快照哈希和版本号不得参与配置一致性比较。校验必须发生在事务内且先于正式快照写入。
- Blocker: 只校验本次请求片段、缺失工序仍部分落库、同一工序存在重复组成员、组内模板/槽位/填写人/权限/归档/排序/备注不一致，或用模板 ID、绑定 ID 推断组身份时必须停止。
- Verification: 后端单测覆盖完整一致组、缺失成员、重复成员、配置不一致、局部请求篡改既有组且快照保存未被调用；发布投影和路线快照测试证明显式组身份不丢失，迁移合同证明组字段可空且有路线级索引。
- Forbidden action: 禁止自动补齐缺失组成员、历史数据推断回填、吞掉校验异常、部分保存，或让多个工序复用同一正式绑定身份。
- Evidence: `doc/tasks/20260817-route-form-global-sync/verification-report.md`。

## MyBatis Plus 空值更新门禁

- Trigger: 删除、撤销、恢复、解绑或重新计算流程需要把已有外键、状态明细、错误码、错误文本、业务引用字段清空为 `NULL`。
- Preflight check: 先确认当前 Mapper 更新语句是否会跳过 `NULL` 字段；凡是业务结果依赖字段被清空的路径，必须使用显式 SQL、`LambdaUpdateWrapper#set(..., null)` 或已验证的字段策略，并在测试中断言数据库真实为 `NULL`。
- Blocker: 页面或返回值显示已删除/已解绑，但数据库仍保留旧引用；单元测试只校验对象内存值，未校验 Mapper 被调用的实际清空语义；或删除后下一轮统计依然被旧关联排除时必须停止。
- Verification: 后端单测必须覆盖清空字段的 Mapper 调用；真实 E2E 或数据库验证必须证明有效业务记录不再被旧外键、旧任务号或旧状态影响。
- Forbidden action: 禁止用 `updateById` 设置对象字段为 `null` 后直接宣称数据库已清空，禁止靠前端状态文案掩盖旧外键残留，禁止吞掉清空失败。
- Evidence: `doc/tasks/20260830-nas-original-path-sync/verification-report.md`。

## 2026-09-02 JDBC GeneratedKeyHolder 自增主键读取门禁

- Trigger: 后端服务用 `GeneratedKeyHolder` / `Statement.RETURN_GENERATED_KEYS` 读取 INSERT 自增主键，尤其同一代码需要同时跑 H2 单元测试和 MySQL 运行态。
- Preflight check: 不得假设生成键 Map 固定包含 `id` 或固定只返回一列；实现必须同时覆盖 MySQL 可能返回单列驱动键名（如 `GENERATED_KEY`）和 H2 可能返回 `id + create_time/update_time` 等多列键的形态。
- Blocker: 成功 INSERT 后因取不到主键而转成业务冲突、用 `KeyHolder.getKey()` 在 H2 多列键下抛 `InvalidDataAccessApiUsageException`、或把驱动差异包装成“历史冲突/数据冲突”时必须停止并补回归测试。
- Verification: 后端回归必须直接覆盖单列非 `id` generated key 与多列含 `id` generated key；相邻服务测试覆盖真实提交、审批、履历/文件绑定或对应业务主链。
- Forbidden action: 禁止通过改测试库生成键行为、吞异常、重查最新一行、默认返回 0/空 ID、或放宽业务冲突校验来掩盖 generated key 读取错误。
- Evidence: `doc/tasks/20260902-registration-change-history-conflict/verification-report.md`。
