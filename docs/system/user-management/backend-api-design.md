# 用户管理后端与接口设计

## Purpose and Scope

以 [架构](architecture.md) 的基线和 G1—G4 为准。以下明确标记“现有”和“proposed”；本轮没有新增接口或运行验收。第一阶段只修 UM-03，不改导入、密码、生命周期、岗位写入或 MES。

## Evidence Reviewed

已沿架构证据 E01—E05、E09—E19 读取列表、写入、授权、数据权限、令牌与内部 API。角色管理分页要求 role:query，岗位管理分页要求 post:query，E10 的单用户角色读取要求 assign-user-role；现有用户 page 要求 user:query。E13 使用户查询具有部门/本人范围；E19 的拼接用途 API 忽略数据权限，禁止直接用作公开页面查询。

## Modules

proposed `UserQueryService` 负责已授权用户投影；`UserApplicationService` 负责明确写用例与事务协调。身份、任职、生命周期、凭据、授权、会话各自拥有仓储及规则；Controller 只做协议校验与交给相应用例。不要把 AdminUserDO 传到模块外或把 HTTP Request VO 当域模型。

proposed `UserSummaryReadPort`、`AssignmentSummaryReadPort`、`AuthorizationSummaryReadPort` 只读取给定的已授权用户 ID；组织提供给定部门/岗位的名称与状态。它们不是通用管理列表权限，也不返回全量角色库。第一阶段允许在 system 模块实现批量 Mapper 和查询投影，不为分层而建立无行为的额外框架。

## API Contracts

### Q1：用户分页（现有路径，proposed 查询实现及增量响应）

`GET /system/user/page`，权限保持 `system:user:query`。继续 CommonResult<PageResult<...>>。现有 `UserPageReqVO`：pageNo ≥ 1、pageSize 1..200、username/mobile 模糊条件、status、createTime 闭区间、deptId 含自身和子部门、roleId 永久角色关联条件。默认页码 1、大小 10，公开分页拒绝 -1；导出内部 PAGE_SIZE_NONE 使用现有独立 export 权限，不经这个公开查询入口绕过上限。

第一阶段不新增 nickname/postId/锁定状态筛选，不添加“有效权限角色”筛选。roleId 当前已支持，保持永久正式关联语义；临时授权与实际有效菜单权限不混入角色显示数组。

| 字段 | 合同 |
| --- | --- |
| 现有 UserRespVO 字段 | 保留既有公开字段与类型，显式白名单转换，绝不加入 password/hash/history/认证快照；本次仅新增摘要，不全局迁移 ID 协议 |
| `roles`（proposed） | 当前页用户永久正式角色摘要 `{id: decimal string, name: string, status: 0\|1}` 数组；停用角色仍展示关联事实，不表示可授权或权限生效 |
| `posts`（proposed） | `{id: decimal string, name: string, status: 0\|1}` 正式既有岗位摘要，停用既有岗位保留 |
| `isDeptLeader`（proposed） | 是否等于当前部门正式 leaderUserId；只给布尔，不向页面发放完整负责人目录 |
| `postIds`（现有） | 第一阶段保留字段；page SQL 的内部投影同时读取原始 post_ids JSON 字符串，在同一只读一致快照中与当前页批量正式关联精确对账；差异拒绝，不自行修复；唯一源切换属于后续阶段 |
| `deptName`（现有） | 用户有 deptId 时读取该正式部门名称；无部门可以空，已有部门引用缺失/名称为空是完整性错误，不能默认为“未知部门” |

摘要按数字 ID 升序稳定排序；ID 使用精确十进制字符串，前端不能 Number 强转。页面拼接“、”仅负责格式，不反查名称。当前主用户 ID 协议未全局改成字符串，前端遇到超安全整数明确失败；整体 ID 合同迁移是后续独立兼容性工作，不能静默截断。

原始JSON只在内部分页投影中保留为字符串，不能先通过JacksonTypeHandler转换成Set<Long>后才对账；该过程可能已经吞掉重复token或小数表示。提取/复用E07.parseStoredPostIds的纯解析规则：数组、正整数/精确十进制字符串、Long范围、重复token、尾随内容分别校验；NULL仅在与确实空有效关联一致时是现行合法表示。原始JSON不得进入公开DTO或日志，解析失败明确错误，不返回[]。不用逐用户JSON查询，不调用getUserForUpdate/lockAndValidateUserPosts的编辑锁链，7+H预算中的page SQL一次携带当前页原文。

空集合仅表示确实没有正式关联。已有岗位/角色关联无法找到当前租户未删主数据、重复关联、非法 ID 或名称时返回完整性错误；不能跳过失效关联后显示“无”。这是显式目标失败合同，第一阶段测试须覆盖，不能被现有前端静默过滤行为掩盖。

### Q1 排序、分页与查询预算

无 deptId 时，保持 `id DESC` 全局排序再数据库分页。有 deptId 时，严格保留当前 E04 算法的结果：先取得通过全部过滤和数据范围的匹配用户集合；所选部门最先；其余部门按该匹配集合中 `MAX(user.id) DESC` 排序；每组部门负责人若在匹配集合中则先于成员；成员 `id DESC`；无部门组最后。不是按 deptId 排序，也不让未匹配或未授权的负责人插入列表。

proposed SQL 采用授权过滤后的派生匹配集合、按部门聚合最大 ID、负责人关联，再对排序结果 LIMIT/OFFSET；不依赖尚未确认的数据库窗口函数或 CTE。count、组最大值、page 三处必须应用完全相同的租户、逻辑删除、数据范围和筛选条件。若当前 MyBatis 数据权限插件不能正确保护派生表，必须显式编译相同已验证范围条件并用真实数据库验证，不能临时关闭 @DataPermission。

逻辑参考键：`selectedDeptFirst ASC, nullDeptLast ASC, matchedDeptMaxId DESC, isDeptLeader DESC, userId DESC`。前两个键只作用于有部门条件的分支。DAO 返回最终顺序；前端取消二次重排。无负责人/负责人已过滤/同一部门跨两页/所选部门无匹配用户均按该规则处理。当前默认列没有 sortable=true，UnifiedListTemplate.DEFAULT_COLUMN_SORTABLE=false，页面也未监听模板 sort-change 发查询；第一阶段保持表头自定义排序禁用，不增加排序参数。若希望跨页自定义排序，需另一项明确产品需求。

预算按请求分别量化：

- 一次正常翻页/过滤：1 个 page HTTP 请求；初始化额外 1 个部门树请求；路由 userId 定位可额外使用现有 get 请求，不计为逐行查询。列配置、字典与会话请求独立记录，不能用排除这些掩盖逐行角色请求。
- page基本数据SQL：count + page 2次、部门上下文/标签最多1次、当前页任职关联1次、岗位元数据1次、永久角色关联1次、角色元数据1次，合计最多7次。无deptId时部门标签可以页内批量读取；有deptId时此1次用于验证选定启用部门与范围，page SQL已有部门负责人关联时同时取得本页必要部门名称/状态，不能再重复批量读取。另一种实现是所有分支均在page SQL左关联同租户有效部门最小字段，则无deptId总数最多6次。有deptId的空页还需这1次上下文校验；无deptId空页仅count/page。角色筛选用同租户EXISTS，不另外查全体角色用户ID。
- deptId 子部门遍历保留既有按层方式：额外 H 次，H 为实际遍历层数含终止查询；不随本页人数 N 增长。鉴权/数据范围基础设施 SQL另计并完整记录。第一阶段不改组织树遍历算法或新增组织闭包表。
- 派生表/聚合在数据库处理匹配集合，应用层不得加载全部匹配用户后分页；只向应用返回 pageSize 条用户及其关联摘要。关系数量为真实关联数量，不伪造固定长度。SQL 总数随 N=1/10/200 不增长；复杂度、执行计划和耗时另测，不把“7 次查询”误写成性能已经达标。

阶段开发前须确认实际数据库方言、数据权限拦截效果和派生排序执行计划；等价性或预算无法证明则阻断，不切换回全量前端拼接作为成功路径。

### Q2：用户页部门过滤树（proposed）

`GET /system/user/query-dept-tree`，要求 `system:user:query`，返回 `{id: decimal string, parentId: decimal string|null, name: string, status:0|1}`。不返回负责人、手机、邮箱、人数或未授权用户；只纳入此操作者在当前租户可查询范围内的部门，返回节点仅启用部门。停用部门不出现在树中，但可见用户已关联的停用部门名称仍可在列表作为正式现有关系展示。部门数据范围由服务端既有权限聚合规则产生，不由前端提交。

范围映射须和 `PermissionServiceImpl.getDeptDataPermission` / `DataScopeEnum` 保持一致；以下是选定合同，不是未决政策：

| 既有范围 | Q1人员范围 | Q2部门上下文 |
| --- | --- | --- |
| ALL | 当前租户全部未逻辑删除用户；启用与停用都可查询，是否纳入由status筛选决定，仍受其他请求筛选 | 当前租户全部启用部门 |
| DEPT_CUSTOM | 配置部门集合∪本人部门内用户；当前实现会加入本人部门，不自动扩子部门 | 该精确集合的启用部门，不自动扩层级 |
| DEPT_ONLY | 仅本人所在部门内用户 | 本人部门若启用则一个节点，不扩子部门 |
| DEPT_AND_CHILD | 本人部门∪其正式子部门内用户 | 该精确集合的启用部门 |
| SELF | 仅本人；部门选择只是附加过滤 | 本人部门若启用则最小上下文节点；绝不授予同部门他人可见性 |
| 无启用角色 | 保留现行显式业务规则：仅本人 | 同SELF；不是配置错误时的默认兜底 |
| 混合有效角色 | 部门集合取并集，self标志取或；ALL覆盖，其余仍受租户/筛选 | 相同部门并集，self时另包含本人部门作为上下文；不把上下文节点加入Q1授权部门集合 |

正式临时角色是否有效继续由当前授权模块计算，查询不得自行改变其时效。当前 null dataScope 会跳过、未知值只记录日志属于旧实现；目标查询适配器遇到被纳入此次范围计算的 null/未知范围明确返回查询权限配置错误，不升级为ALL或默认SELF，也不在本阶段全局修改 PermissionServiceImpl。开发时必须验证范围聚合返回值及所有有效角色配置，避免借“复用旧服务”隐式放行不完整范围。

父节点不在允许返回范围时，将已授权节点作为森林根（parentId=null）展示；不额外披露范围外父部门名称。不把“没有可见用户”误认为“没有部门查询范围”；未分配部门的本人返回空树但列表仍可查询本人。分页deptId只接收该操作者当前允许选择的启用且未删部门ID，非法/跨租户/越范围统一拒绝，避免借错误区分其他租户是否存在该节点。授权启用部门内没有匹配用户是合法空页。

路由userId定位的目标用户可见但其部门已停用时，现有syncUserQueryFromRoute仍会带入deptId；目标行为必须明确拒绝定位并提示“目标用户所在部门已停用，无法按部门定位”，保留该筛选/定位失败状态，不静默清除deptId再扩大查询。非法/越范围部门用统一权限错误。用户显式退出定位后可按另一次正常查询操作查看授权列表；不自动转为宽范围成功。此为D1明确的路由边界变化，须覆盖现有路由调用者和UI定向回归。

该树只用于用户列表；新增/编辑 UserForm 的部门候选、组织管理删除、其他 DeptTreeSelect 调用者不改。新用户页树提供刷新后同节点取消选择、搜索、展开折叠。返回 1 次部门批量 SQL 与权限范围解析成本；不逐节点拉取人员。专用最小投影依据上述显式允许ID/ALL集合查询，不直接调用关闭数据权限的 getSimpleDeptList；SELF上下文不依赖广义部门管理权限。相同原则用于Q1：页内已授权用户的必要部门标签由限定投影读取，不能因 DeptDO 的广义管理范围挡住本人标签而改用全目录。

### 目标写用例（proposed，第一阶段不实施）

| 操作 | 允许字段和一致性 |
| --- | --- |
| CreateAccount | 资料、初始凭据、明确任职；租户配额及账号唯一性，保持凭据 RESET_REQUIRED |
| UpdateProfile | 明确资料白名单；不接受 password/status/roleIds 或隐式任职 |
| SaveUserAndAssignments | 当前整页保存的编排入口；资料与完整 targetPostIds 同一事务，保留 UM-04；未来拆 UI 需另授权 |
| ReplaceAssignments | 明确目标集合；缺失与 null 拒绝，[] 表示清空，保留停用既有关系，新增只允许启用 |
| AssignRoles | 继续正式授权模块、原因/幂等键/GxP审计与受限角色规则；岗位不自动授权 |
| ChangeAccountStatus / RecordLifecycleDeactivation | 合法状态迁移与会话撤销原子完成，单据已生效后不得普通手工启用 |
| ResetPassword / ChangeOwnPassword | 政策校验、密码历史、凭据状态、正式令牌撤销；真实调用者和原因合同另行确认 |
| UnlockAccount | userId 与非空原因，审计；不能把普通资料保存当解锁 |

角色分配现有审计协议与接口暂不替换；通用 UserSaveReqVO 的缩窄/独立 DTO 是未来阶段，不在 UM-03 顺带清理。

### 模块间最小合同（proposed，渐进演进 AdminUserApi）

1. `IdentityLookup.lookupCurrentIdentities(ids)`：当前调用租户内批量当前身份 `{userId, username, displayName}`，ID 精确字符串。受控服务端用途，不作为任意公开查询；不存在明确列为 missing，不返回默认姓名。需要组织关系的调用另用 AssignmentReadPort，不把岗位塞回所有身份 DTO。历史查看不能用此接口。
2. `AccountEligibility.requireEligible(userId, purpose)`：当前租户内校验已存在、启用及用途规定的状态，失败抛明确业务异常；purpose 是枚举，不能传任意字符串隐式扩展。只读结果不代表未来写入获授权；签名/发令牌必须在实际写事务中重新锁定校验。业务对象授权仍由调用模块负责。
3. `SystemUserSignatureReauth.reauthenticate(request)`：服务端可信签名上下文明确 tenant、operator、signer、动作以及模块已核验的合法签名选择；验证锁定/凭据过期/RESET_REQUIRED/密码，返回 `{tenantId, identityDomain:SYSTEM_USER, signerId, operatorId, displayName, authenticatedAt}`。不返回 passwordHash、AdminUserDO，不把这个结果充当可复用登录令牌。选定他人签名只有现有业务明确允许时才接受，不凭请求 userId 授权。

MES_EMPLOYEE_PROFILE 继续自身认证端口与身份合同；SYSTEM_USER 和员工档案的 ID 不能互换。正式签名必须由签名模块绑定对象版本、动作、原因和幂等身份，把认证身份冻结进签名证据。旧 AdminUserApi 调用点在全量枚举和逐条迁移完成前不能删除；不得用空实现或转查现时昵称掩盖缺失。

## Error Model

现有 `CommonResult.code/msg/data` 与正式错误常量保留。已存在的 USER_POST_IDS_REQUIRED=1002003040、USER_POST_IDS_INVALID=1002003041、USER_POST_BINDING_INCONSISTENT=1002003042、USER_POST_WRITE_FAILED=1002003043 只用于相应现行场景。

proposed `USER_QUERY_REFERENCE_INCONSISTENT`、`USER_QUERY_SCOPE_FORBIDDEN`、`USER_QUERY_PERMISSION_CONFIGURATION_INVALID` 为名称合同，尚未分配数字，开发前检查错误码登记避免冲突；分页参数错误复用现有验证错误。当前无排序参数，不新增排序错误码或让任意请求字段进入SQL。缺权限拒绝；范围外对象与不存在对公开ID查询给等价拒绝，避免存在性泄漏。空查询是成功空页，依赖服务/数据库不可用是失败。前端收到失败不能显示已加载完整名称或默认成功。

## Transactions and Idempotency

page 与其摘要在一个真实只读一致事务快照内完成，明确当前数据库隔离级别与授权插件行为；只读对账不取编辑锁，不写修复。若隔离级别不能保证一致视图，先确定经验证的查询实现，不能把并发差异归类为永久数据错误而无证据。

写用例拥有真实 Spring 可写事务；不通过同类自调用丢失事务代理。主体、岗位、令牌、授权审计的锁序见安全设计，迁移先建立调用链完整锁图。认证快照在发令牌锁内再次比对，防止重置后的旧认证发出新令牌。安全撤销继续同步执行；Redis 失败传播并回滚 DB，已有缓存删除无需补建；DB 的正式验证使残留缓存不能独立授权。

有既有幂等合同的授权与签名按租户/操作/对象/键/载荷核对，精确重放只返回已提交结果，异载荷拒绝。第一阶段没有写入，不新增幂等基础设施。后续普遍乐观版本字段不能无依据假设已经存在；资料并发覆盖策略为开发前问题，岗位保持现行锁后规则。

## Open Questions

- OQ-Q1：现场数据库版本、隔离级别、SQL 方言与数据权限插件对派生查询效果，影响 UM-03 开发与真实查询验证。
- OQ-Q2：已选定上述精确范围/森林/启用节点合同；开发前须验证现有权限聚合、菜单及专用SQL能实现它，而非重新默认扩展CUSTOM/ONLY。若业务要求额外祖先展示，需另一项明确需求与授权。
- OQ-ID1：全系统 Long/string 协议调用清单，影响后续稳定身份 API 迁移；第一阶段不全局改现有字段。
- OQ-S1：跨签名外层事务的失败计数保留策略、是否需要改密/重置原因和资料乐观版本，影响后续安全职责抽取，不作为 UM-03 前置。

## Design Blockers

UM-03 开发前须完成 OQ-Q1 的读取确认、Q2 权限合同评审和真实受限账号/跨租户数据准备授权；缺任一条件不得宣称功能可验收。后续 API 迁移先全量枚举既有调用者；缺失调用清单不能删除 AdminUserApi 方法。UM-02 保持暂缓，不成为本阶段阻塞。
