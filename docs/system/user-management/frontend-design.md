# 用户管理前端设计

## Purpose and Scope

采用 [架构](architecture.md) 的 G1—G4。第一阶段 UM-03 只调整用户列表的数据获取和查询部门树，消除展示对管理目录及逐人角色接口的依赖。现有 Vue 3 / Vite / TypeScript、Element Plus、UnifiedListTemplate 与列配置机制继续使用。本轮设计没有修改页面或执行 E2E。

## Evidence Reviewed

已读 E01 `getList` / `loadUserDisplayMetadata` / `loadRoleNamesByUserId`、E02 API、E06 `UserForm.open` / `submitForm`、E09 角色分配、E14 通用部门树、`src/views/system/user/utils.ts.resolveDisplayNames` / `sortUsersByDeptLeader`。现有 utils 对找不到名称的 ID 会跳过，目标查询不能沿用这个行为掩盖失效关联。已追踪 E03—E05 的后端分页排序以及 E10 的角色读取权限。

## Pages and Routes

| 页面/入口（现有） | 第一阶段行为（proposed） | 范围 |
| --- | --- | --- |
| `src/views/system/user/index.vue`，菜单路由动态配置、组件名 SystemUser | page 返回完整摘要；只依赖 user:query 读取 | 必做，不猜测部署菜单 URL |
| 左侧部门树 | 用户查询专用部门森林，选中/取消/搜索/展开折叠 | 必做，来自新 Q2，非组织管理入口 |
| 路由 query.userId 定位 | 用现有getUser获取可见对象；不可见/不存在拒绝；所在部门停用时明确拒绝定位并提示，保留失败筛选，不静默清除deptId | D1定向回归，避免用忽略范围的AdminUserApi |
| 新增/编辑 UserForm | 正式既有 assignedPosts 与启用 candidates 分开；完整目标保存 | 保留 UM-04，不改数据入口 |
| UserAssignRoleForm | 独立权限、原因、幂等键 | 不改授权流程，只有点击授权入口才调用分配接口 |
| UserImportForm / UserDingTalkImportForm | 现有导入入口 | 不改导入生命周期；UM-02 暂缓 |
| 重置密码、状态开关、生命周期、解锁 | 现有明确操作 | 保留安全回归，不顺带修 UM-06/UM-10 |

开发前核对实际菜单路由和 `system:user:query` 的进入权限；不能仅将组件路由写为 /system/user 就认为菜单授权已落地。

## Components

proposed `UserQueryDeptTree` 限于用户列表，接收 Q2 的最小字段；其他页面使用的 `DeptTreeSelect` 继续现有合同，不能把用户范围过滤强加给组织管理等调用者。可以把纯展示树抽成无请求组件，再由两种用例适配器供数据；是否值得抽取由开发时确认，避免为本任务重构所有树。

proposed `useUserListQuery` 只协调 page、部门树和请求生命周期。角色/岗位单元格直接消费 `roles` / `posts`，只做稳定格式拼接和溢出提示；使用 `isDeptLeader` 决定负责人样式，不维护全量部门负责人 Map。

删除本页展示链的 loadPagedLookupItems、loadUserDisplayMetadata、loadRoleNamesByUserId、buildUserTableRows 目录反查及本页排序调用；utils 的其他使用者先搜索再决定保留，不全局删除共用代码。保留用于“删除组织”等明确组织管理操作的 DeptApi 引用，不把禁止展示依赖误解为本页永远不得操作组织。

## State and Data Flow

1. 页面进入后读取部门查询树与 page，两者独立记录 loading/error，不通过成功的 page 掩盖树错误。树失败时明确显示错误且禁用部门选择，其他无部门过滤的列表可继续显示其真实成功结果；这是两项独立查询各自可见状态，不采用旧目录作为备用树，也不声称页面全部加载成功。
2. 翻页、现有快速筛选、部门选择只发一个 page 请求。返回 list 与 total 在同一次有效请求中替换；角色/岗位无需额外 HTTP 请求。部门树仅初始化或显式组织操作成功后刷新。
3. 请求序号和取消/离页状态确保旧响应不能覆盖新筛选；即使取消失败也用序号判定是否允许应用结果。最终 server order 原样进入表格，移除当前页负责人重排。
4. list 数据校验：list 数组、total 非负、摘要数组/状态/名称/精确 ID 必须满足合同。缺字段是协议错误，禁止把未返回 roles 转成 [] 并显示“无角色”。确实空集合可展示“—”，这是已确认无关系，不是失败占位。
5. 当前username/mobile/status/createTime/deptId与分页保持；角色筛选后端已有但当前页没有，不新增对应UI。第一阶段不新增nickname/postId/有效锁定筛选或候选页。路由定位用户所在部门停用时明确展示定位失败原因且不调用无部门过滤的备用page；用户须显式退出定位再发起自己的正常查询。
6. 操作成功后按当前查询条件刷新列表；按钮权限与原后端写权限保持一致。查询摘要里出现某角色不意味着用户可以分配该角色，也不能由名字推算权限。

与页外目标的界限：当前默认列未配置 sortable=true，useUserTableColumns 不新增该属性，UnifiedListTemplate 的 DEFAULT_COLUMN_SORTABLE=false / normalizeSortableColumn 默认禁用，页面未监听模板 sort-change 发查询。因此当前没有表头自定义排序，本阶段保持禁用，排序仅由后端合同决定。列表锁标记仍按现有字段展示，不在 UM-03 顺带改变自动解锁体验或筛选语义。

### 任职编辑必须保留的交互合同

保留 UserForm.open 的并行读取和一次激活：详情、部门候选、启用岗位候选全部校验后才允许提交。既有停用岗位有明确停用标记，允许保留或明确移除；未绑定停用岗位不可新增。详情 ID 必须等于当前对象；晚到的另一用户响应不能写入当前表单。

提交 postIds 是用户确认的完整目标集合，缺失/null 是错误，[] 才是清空。不能将列表页 posts/启用候选转成编辑正式关系，也不能把加载失败当空集合。普通资料与岗位继续一个正式保存动作和一个事务；第一阶段不拆成前端两个请求。

### 长期交互合同（非第一阶段）

未来资料、任职、授权、启停、改密、解锁按用例区分表单与 DTO；需要整体保存时由一个服务端编排入口执行。导入预览展示创建/更新/停用/组织变化，执行结果按实际提交事实展示；不把行失败集合为空等同于整批业务规则全部通过。历史姓名来自业务冻结证据，当前姓名如需额外展示须明确标为“当前资料”，不可替换当时姓名。

## Error States

| 场景 | 页面结果 |
| --- | --- |
| 无 user:query | 拒绝进入/查询，显示权限错误；不改用 simple-list 绕过 |
| 无 role:query/post:query/assign-user-role，仅有 user:query | 列表及限定部门树可完整显示；授权等按钮仍按原权限隐藏/禁用 |
| 合法筛选零用户 | 真正空列表和 total=0，分页不残留上页人员 |
| 某个关联缺正式主数据或 JSON 不一致 | 展示后端完整性错误；不能偷偷删掉该角色/岗位标签 |
| page 请求失败或响应协议错误 | 显示错误并避免把旧数据呈现为当前筛选结果；允许用户显式重试同一正式接口 |
| Q2 失败 | 单独标记树失败与不可选择；不切换全租户目录或返回默认树 |
| 401/会话失效 | 使用现有真实认证流程；不保留可执行写按钮或 mock 登录成功 |
| 页面切换、并发筛选 | 过期响应不改变当前列表、total、错误状态或 loading |

## Accessibility and Responsive Behavior

复用既有窄屏部门树/列表上下排列；表格横向滚动与列配置保持。树节点、搜索、展开折叠可键盘操作，焦点与选中明确；森林根来自正式允许范围，不能伪造父部门名称。岗位/角色长名称有可读溢出提示；停用标签不只依赖颜色。错误区域可被读屏感知，重试按钮有明确名称；loading 状态避免重复触发业务操作。

## Open Questions

- OQ-Q2：用户查询部门森林的精确范围已在后端Q2选定，开发前验证菜单与真实权限聚合/SQL行为；SELF节点不授予同部门其他人员可见性。
- OQ-F2：实际菜单路由与受限测试账号能否进入用户页，由未来开发时真实前端确认；当前没有运行态证明。

## Design Blockers

第一阶段 page/Q2 合同、现有菜单入口与真实受限用户数据须齐备。E2E 仅在未来用户当轮明确要求时执行，全部验收动作由 Playwright 真实页面完成，API/DB只读核验。缺入口、权限配置或真实环境时阻断，不能用直接 API 代替操作。未来设计的后续表单没有实现，不作为 UM-03 验收入口。
