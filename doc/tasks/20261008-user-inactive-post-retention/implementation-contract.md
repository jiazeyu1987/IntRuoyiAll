# UM-04 实施合同（等待主 Agent 评审）

日期：2026-10-08。来源基线：主 Agent 提供的 `a3b8bedfeece394f286e23a9d5144361b951e8ac`；本文直接只读核对 `E:/IntRuoyi` 当前源码，未执行 Git、测试、构建、服务、数据库或 E2E。本文不是实现完成或放行证据。作者仅写本文和 `contract-author-result.md`，主 Agent 独占 task/task-state/execution-log。

## 1. 目标、边界与优先规则

只实施已评审方案的 UM-04 / AC-04-01、AC-04-02：普通资料编辑保留既有停用但未删除岗位；完整提交集合明确移除才删绑定；新增岗位必须启用。用户 JSON `post_ids` 和有效 `system_user_post` 两份存储一致，完整性错误拒绝，不补齐、不猜名称、不丢 ID。

已核对来源：`doc/tasks/20261006-user-management-remediation-plan/remediation-plan.md:210–219`、`test-plan.md:57–58` 与真实生产 handler 载体章节、`review-report.md:29–33`；旧方案文档放行不等于本次实现放行。已识别本次 `task.md`。

最近规则为根 `AGENTS.md`（子源码目录未发现更近 AGENTS）：明确验收与对应测试，BDD/TDD 非默认强制；仅当轮已授权真实 E2E；完整链条静态审查；仅任务资产；禁止 fallback。核对了 `docs/backend-development.md`、`docs/frontend-development.md` 的通用实施规则及本次适用条款，`docs/task-closeout-rules.md` 的记录/清理规则，`docs/database-rules.md` 的真实数据/租户规则，以及 E2E/端口/worktree 文档的适用入口。上位兼容性之外，以最近 AGENTS 和本次 task 授权为准，不执行关联文档中的全 dirty 基线提交或默认推送。

不实施 UM-02 导入状态治理、UM-03 列表查询权限投影、角色授权、密码/会话/生命周期、新 schema、岗位停用/删除政策。通用 getUser/page/list/export 与其他模块的用户查询不接入岗位完整性门禁。正式数据不一致只阻断并报告治理需要，不做数据修复。

## 2. 当前逐节点证据

以下路径前缀：`SYS=IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system`；`UT=IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system`；`FW=IntRuoyiBackend/yudao-framework`。行号为本轮读取的当前源码，实施后须重新核对。

| 业务节点 | 当前依据 | 结论及实施边界 |
| --- | --- | --- |
| 用户列表修改入口 | `IntRuoyiFronted/src/views/system/user/index.vue:272–280,340,591–595` | 修改按钮使用 `system:user:update`，调用真实 UserForm.open；保留入口、列表与权限 |
| 编辑加载及提交 | `IntRuoyiFronted/src/views/system/user/UserForm.vue:157–190,196–217` | normalizer 非数组→空，按启用 simple 过滤已有 IDs；失败后 formLoading 已结束而空/半成品表单仍可提交。取消只是关闭弹窗，不应写入 |
| 编辑请求包装 | `IntRuoyiFronted/src/api/system/user/index.ts:3–28,57–70` | get 使用通用 `/system/user/get`；update PUT 完整 formData；UserVO.postIds 为 string[]，Form 初值却为 number[] |
| HTTP 权限及输入 | `SYS/controller/admin/user/UserController.java:55–69,168–180`；`vo/user/UserSaveReqVO.java:18–45,71–76` | create/update 共用 SaveReqVO；postIds 可缺失；HTTP get 只查询用户 JSON；不能给共享 VO 加全局 @NotNull |
| 普通编辑服务 | `SYS/service/user/AdminUserServiceImpl.java:179–216,678–709` | 原 update 无用户行锁，全量 validatePostList；先更新 JSON，再以普通 join 查询求差集，null→空会删除全部。已有事务回滚注解须保留 |
| 岗位候选与状态 | `SYS/controller/admin/dept/PostController.java:88–95`；`SYS/service/dept/PostServiceImpl.java:116–156` | simple 只 ENABLE 且 VO 只有 id/name；validatePostList 拒全部停用；不能全局放宽此方法，不能把 simple 当已有绑定来源 |
| 两份落库及下游 | `SYS/dal/dataobject/user/AdminUserDO.java:22–29,70–74`；`dal/dataobject/dept/UserPostDO.java:16–38`；`dal/mysql/dept/UserPostMapper.java:15–31`；`service/user/AdminUserServiceImpl.java:557–566` | JSON 使用 JacksonTypeHandler；有效 join 是正式绑定，下游 getUserListByPostIds 直接消费 join，因此两份均须核验 |
| 当前用户锁 | `SYS/service/user/AdminUserServiceImpl.java:438–454`；`SYS/dal/mysql/user/AdminUserMapper.java:25–28` | lockUserForSessionMutation 要真实可写 Spring 事务、匹配 explicit tenant、正用户 ID；实际 selectPermissionSubjectForUpdate 显式 tenant/deleted 且 FOR UPDATE，适合复用，不改公共锁合同 |
| 通用读取调用方 | `SYS/service/user/AdminUserServiceImpl.java:544–547`；`api/user/AdminUserApiImpl.java:40–44`；`controller/admin/user/UserProfileController.java:56`；auth/mail/sms/oauth2/permission 现有调用 | getUser 被认证、令牌、个人资料、权限及通知等消费；UM-04 完整性错误不应阻断这些无关路径 |
| 原数据权限边界 | `SYS/framework/datapermission/config/DataPermissionConfiguration.java:17–25`；`service/user/AdminUserServiceImpl.java:680–697` | 正式用户/部门有数据范围；旧校验只在唯一性查验范围 executeIgnore，不能把新增编辑锁/绑定查询全部包进 ignore |
| 正式租户链 | `FW/yudao-spring-boot-starter-biz-tenant/src/main/java/cn/iocoder/yudao/framework/tenant/core/db/TenantDatabaseInterceptor.java:40–80`；对应 `config/YudaoTenantAutoConfiguration.java:73–80`；`yudao-server/src/main/resources/application.yaml:345–353` | PostDO/UserPostDO 虽继承 BaseDO，已注册实体默认仍拦截；只有 @TenantIgnore/配置 ignore 才忽略。生产配置只忽略 dcc_product_catalog，没有这三表 |
| 正式结构与逻辑删除 | `IntRuoyiBackend/sql/mysql/ruoyi-vue-pro.sql:3511–3525,4854–4865`；system `src/test/resources/sql/create_tables.sql:297–324,420–450`；`FW/yudao-spring-boot-starter-mybatis/.../BaseDO.java:50–54` | 正式岗位和 join 都有 tenant_id/deleted，正式表只有 id 主键，不能假设 join 唯一约束自动拒重复。新增 Mapper 必须复用正式租户/逻辑删除链 |
| 长 ID 协议 | `FW/yudao-common/.../util/json/databind/NumberSerializer.java:19–40`；`FW/yudao-spring-boot-starter-web/.../jackson/config/YudaoJacksonAutoConfiguration.java:30–36,49–55` | Long 安全边界外返回十进制字符串；编辑中不能 Number() 强转全部岗位 ID 导致精度丢失 |

## 3. 允许修改的路径

生产路径限定如下；新增文件也是计划，当前尚未建立：

- `SYS/controller/admin/user/UserController.java`：只新增编辑专用 GET；update 参数必填合同按第4节实施。
- `SYS/controller/admin/user/vo/user/UserEditRespVO.java`（新增）：继承 UserRespVO，仅编辑响应增加 assignedPosts 的 id/name/status；可用内嵌静态 PostItem，避免复用只含 id/name 的 PostSimpleRespVO。
- `SYS/controller/admin/user/vo/user/UserSaveReqVO.java`：仅为 postIds 的有效集合/严格 JSON 解析加边界约束，绝不全局必填；若需解析器，新增同目录 `UserPostIdsDeserializer.java`，不改全局 ObjectMapper。
- `SYS/service/user/AdminUserService.java`、`AdminUserServiceImpl.java`：新增 getUserForUpdate、编辑快照校验；updateUser 锁内集合差集；保留 create/register/import 现有可选岗位合同。
- `SYS/dal/mysql/dept/UserPostMapper.java`、`PostMapper.java`：仅增加当前锁定读；必要时关联删除方法返回实际影响数，以便拒绝持久化异常。
- `SYS/convert/user/UserConvert.java`：只新增编辑响应映射，保留原分页/导出/get 映射。
- `SYS/enums/ErrorCodeConstants.java`：增加范围内业务错误（完整集合必填/非法、既有绑定完整性、异常落库）；正式停用新增仍沿 POST_NOT_ENABLE，缺失新增沿 POST_NOT_FOUND，不泄露外租户岗位名称。
- `IntRuoyiFronted/src/api/system/user/index.ts`：增加编辑响应类型/getUserForUpdate，postIds 对齐实际 number|string 传输；现有 getUser/create/update 路径和无关方法保持。
- `IntRuoyiFronted/src/views/system/user/UserForm.vue`：只实施编辑加载完整性、停用展示/移除、提交门禁与候选集合；新增岗位 ID 严格处理 helper 时限同目录且必须由真实 SFC 消费。

测试路径限定：`UT/service/user/AdminUserServiceImplTest.java` 的受影响 fixture/断言；新增 `UT/service/user/AdminUserPostRetentionTransactionTest.java`（真实 Spring/H2/PostService/租户 Mapper）；必要新增同路径独立 HTTP 合同类 `UserPostEditHttpContractTest.java`；`IntRuoyiFronted/tests/e2e/system-user-post-stale-filter-static.spec.js` 迁移过时断言；新增 `IntRuoyiFronted/scripts/system-user-post-retention.test.mjs` 实际生产 SFC/handler 合同。现有 PostServiceImplTest 只运行回归，通常不需修改。主 Agent 管理任务文档与真实 E2E runner/证据。

不改 BaseDbUnitTest、全局租户配置、全局 Jackson/NumberSerializer、Password/OAuth 公共实现、SQL/schema、其他任务文件。超出白名单须先由主 Agent评估是否仍属 UM-04。

## 4. API 与正式数据合同

### 4.1 编辑专用查询

选定 `GET /system/user/get-for-update?id=<id>`，仍使用现有 `system:user:query`，UserForm 的 update 模式只调用新 API。通用 `/get` 保持原行为；index.vue 的 route.userId 定位仍调用通用 get，避免 UM-04 扩散到查询链。

选定 `AdminUserService.getUserForUpdate(Long id)` 返回编辑响应 `UserEditRespVO`：在真实 `@Transactional(rollbackFor=Exception.class)` 内复用用户锁、读取当前 join、锁定正式岗位、验证两份集合，再映射 existing UserResp 字段和 assignedPosts。GET 不执行 INSERT/UPDATE/DELETE/恢复/治理/审计业务写，仅锁定读；因现有锁拒绝 readOnly 事务，不能标 `readOnly=true` 后 mock 绕过。只有该编辑详情承担局部锁成本。

`assignedPosts` 恰好对应原 B，每项只含正式 `id,name,status`，包含启用和停用未删除岗位。无绑定返回 `postIds:[],assignedPosts:[]`；不存在/删除/跨租户用户准确拒绝；不是 success(null) 后允许保存空表单。不得向该响应添加岗位 code/permissions 等管理字段。

原 `system_users.post_ids=null` 且有效 join 空，是现有 CREATE 不传岗位/注册的合法未绑定表示，领域集合为 B=空，编辑响应明确[]。这只解释已有合法存储表示，不是请求字段 fallback。null JSON 与非空 join、非空 JSON 与空/不同 join 都属于不一致，必须拒绝。不得把 DB 查询异常或岗位缺失变成空。

### 4.2 更新请求与直接服务入口

PUT /update 的 postIds 是完整目标 T；缺失或 JSON null 均拒绝，[] 明确删除全部。id 必须为正且明确指向存在的同租户未删除用户。直接调用 updateUser 也必须验证 id/postIds/元素，不能只依靠 Controller Bean Validation。

合法集合只由正 Java Long 身份组成；JSON 顶层必须数组，元素允许正整数 token 或精确十进制字符串（正式 Long 协议），拒 null/布尔/对象/小数/越界/非规范身份。不得经 Jackson 默认浮点→Long截断或前端 Number() 精度损失接受非法身份。如新增局部反序列化器，null 输入保留 null，让 create 可选、update 服务必填拒绝；不能改全局 ObjectMapper。重复请求 ID 可按 Set 语义归一化，但这不允许去重正式重复 active join。测试必须明确该区分。

CREATE 的 postIds 仍可缺失/null/[]，实际提供的所有岗位都是新增并且必须存在启用。不把更新必填加到共享 VO 全局 @NotNull。注册、Excel/钉钉仍走其现有专用实现；importUserList:870–881 复用 SaveReqVO 且岗位 null，不能因此增加岗位必填，覆盖导入继续不更新原 JSON/join。

### 4.3 两份原集合与完整性拒绝

取得用户锁后，其 JSON 定义 J；用当前锁定读取得有效 join 列表 L。在转 Set 前验证每条 userId/正 postId，并按 postId 计数。重复有效 join 明确完整性错误；软删历史 join 不计入 B。只有 J 与 L 的 ID 集合相等，且 L 无重复，才得到 B。

对 B 中全部岗位做正式同租户、未删除解析，包括将被显式移除的 D；不存在/已删/跨租户不是历史停用，整次编辑拒绝，即使 T 把异常 ID 去掉也不能借编辑完成隐式治理。非法/空白正式名称、非法状态同样报告资料完整性错误，不能展示自造名称或当启用。

状态规则：`I=B∩T` 允许 ENABLE/DISABLE；`A=T−B` 只允许 ENABLE；`D=B−T` 删除对应有效 join。不能全量 validatePostList(T)，不能全量放宽 PostService.validatePostList。I 的关联 id/create_time/creator 等保持，禁止全删重插。新启用 A 使用锁内当前状态，不相信编辑加载时的状态或客户端 assignedPosts/status。

## 5. 事务、锁序与实际 Mapper 方法

统一锁序：同租户正 userId 的用户行 → 该用户有效 join 按 `(post_id,id)` → `B∪T` 的正式岗位按 id 升序。编辑查询只取 B，更新取 B∪T。所有锁持有到同一事务提交/回滚。

1. updateUser 保留真实 Spring事务代理，入口先校验完整请求；调用 `lockUserForSessionMutation(TenantContextHolder.getRequiredTenantId(),id)`。不得改此公共方法，不调用 raw `selectByIdForUpdate`（该原注解 SQL 本身没有显式 deleted 条件），不得先 executeIgnore 锁用户，不得 mock 锁成功。
2. userPostMapper 增加 `selectListByUserIdForUpdate(userId)`：LambdaQueryWrapper 精确 user_id，自动租户与逻辑删除，orderBy postId/id，last FOR UPDATE。B 必须来自当前读，不能锁后继续调用原普通 selectListByUserId 再求差集。
3. postMapper 增加 `selectListByIdsForUpdate(ids)`：空集合直接返回真空且不发全表 SQL；非空按精确 ID 集合查询，自动 tenant/deleted，按 id 排序，FOR UPDATE，不过滤 ENABLE。按请求 ID 集合逐项核对返回项，没命中的外租户/删除/缺失统一准确拒绝且不查询别的租户名称。
4. JSON 已来自锁定用户；join/岗位也用当前读。即使外层 RR 事务在等待前已有旧快照，不能将新 JSON 与普通查询的旧 join/岗位混用。锁后的唯一性/部门校验可沿现有专用规则执行，但它返回的普通 oldUser 不能覆盖已锁用户快照；修改验证 helper 时保留 create/register/import 的原合同。
5. 所有预校验完成后更新用户 JSON=T 与普通资料，显式 password=null，保留 UM-05 及生命周期字段现有边界。更新影响数不是1时准确拒绝。按 A 插入、按 D 删除，I 零关系写；新增批写 false 或差集删除数不符合 D 时拒绝，不能返回成功。用户/关联持久化错误传播并整事务回滚；LogRecord成功上下文只在成功链设置。

生产单岗位 update/delete 已使用真实 postMapper SQL（PostServiceImpl:48–67），会等待相同岗位行锁，因此新增校验与停用/删除不会在本事务中穿透。跨用户编辑岗位集合按升序锁，避免输入逆序带来的锁反序。本修复不宣称覆盖配置包或旧 user-delete 等全部现有 writer 的并发治理；它们的现有行为不改，不把其风险扩成 UM-04 自动重构。

错误证据只包含当前租户用户/岗位必要 ID及原因，不输出密码、token、Cookie、完整 UserDO。正式不一致拒绝的错误须能与“新增岗位停用/不存在”区分，提示需处理绑定数据完整性，不自动治理。

## 6. 前端实际交互合同

update 加载一个完整阶段：formLoading=true，postSelectionReady=false；取得编辑响应、部门树、启用 simple 候选并全部验证后一次提交表单状态，ready=true。整个加载失败时显示正式错误，保存仍禁用/handler拒绝；finally只能复位loading，不能将失败变成ready。取消关闭零update/create请求；取消后旧异步 open 结果不能重新激活已取消表单或污染下一用户打开，可用任务内请求序号辨认过期加载，不吞新请求错误。

编辑 formData.postIds 完整保留 B。assignedPosts 与 postIds 必须精确一致且不重复，id/name/status合法；正式候选不是已有绑定的替代来源，不能过滤 B 到 simple。候选 simple 本身只有 id/name，其“启用”语义来自正式 endpoint；不得假装拥有未返回的status。同ID两来源名称/状态冲突（例如加载期间变化）不能自行覆盖或混成默认成功；应停止加载提示重新打开，保存仍按锁内正式状态复核。

岗位 UI 展示正式名称；既有停用项标“已停用”，保留选中。停用选中项必须能点击真实 tag 移除；不能把整个选项 disabled 导致 Element Plus tag 不可删除。选中时用选项点击/标签移除或对应明确删除动作；移除后该停用项从候选中去掉或禁止选择并保持可见标记，一次弹窗内不能重选。需用真实 Element Plus页面验证实际 tag 行为，不凭模板的 disabled 字符串放行。所有新候选仅启用 simple；create 无 assignedPosts，也不展示停用项。

number|string ID 在编辑内部用规范十进制字符串作精确身份，安全正整数 number可转 String，超安全 number/小数/空白等拒绝，不 Number()转换已是大整数的字符串。提交携带同一精确集合；后端 Long接受该正式协议。UserVO.postIds 可最小改为 Array<number|string>；当前列表 `utils.ts:64–66` 已接受该集合类型，WorkshopForm.vue:172 已按元素Number处理旧业务（不在本任务改它）。不改无关 UserVO.id、PostApi公共候选类型或列表lookup算法。

submitForm 先校验ready、完整数组及编辑冻结集合规则，再执行原真实wrapper；仅服务成功后 success/close/emit。业务/网络失败保持表单且无success/emit，不吞异常伪成功、不追加读取旧数据判断成功。取消未保存移除不影响正式 B；重新打开重新读取正式 B允许正常保留原停用项。

## 7. 既有测试冲突与安全 fixture 迁移

| 既有载体 | 当前问题 | 正确迁移 |
| --- | --- | --- |
| `IntRuoyiFronted/tests/e2e/system-user-post-stale-filter-static.spec.js:9–26` | 三项字符串断言强制 normalizer存在、启用simple过滤、删除不可用ID；与新需求直接冲突 | 同路径迁移为“不静默丢岗位/完整编辑来源”静态约束或调度实际handler回归；记录旧断言退役原因。不能只删断言后console PASS，不能将静态PASS写成业务PASS |
| `UT/service/user/AdminUserServiceImplTest.java:225–262 testUpdateUser_success` | B={1,2}、T={2,3}基本差集正确；PostService是mock、stub的getPostList(two args)不是原真实validatePostList消费入口；没有真实岗位行/tenant验证 | 只为受影响路径补正式岗位fixtures或准确边界stub，保留资料/密码/差集原断言；新增断言保留join2原id/create_time。完整校验使用新专用真实测试类，不能以这里mock无异常证明停用判断 |
| 同类 `:264–276 testUpdateUser_genericAccountForbidden` | 请求 postIds=null，新合同会先被必填阻断，原错误验证不可达；随机用户postIds可能与无join不一致 | 将原用户postIds和有效join都设明确空，提交显式[]，其余前提保持；仍断言USER_GENERIC_ACCOUNT_FORBIDDEN与用户名未变，不降低/删除原断言 |
| 同类 `:125–222 CREATE测试` | CREATE成功mock未跑实际PostService；null在generic/weakPassword测试中是合法可选输入 | 保持null可选fixture，不把新编辑必填强塞进create。新真实测试覆盖create不传岗位成功/停用新增拒绝 |
| 同类 `:752–802` 用户锁测试 | 已用真实事务测试无事务/只读/tenant错误；不是无条件mock锁 | 原样回归，不能改公共锁以便编辑测试通过 |
| 同类 `:1199–1368` Excel及`:1371+`钉钉 | import没有postIds，覆盖直接updateById，未调用updateUser | 只证明本次共享VO变更没有误加必填；增加覆盖导入原JSON/join保持fixture时仍走现有服务，不顺手修UM-02/07 |
| `UT/service/dept/PostServiceImplTest.java:207–237` | 已有validatePostList启用/缺失/停用断言正确 | 保留POST_NOT_ENABLE，运行既有回归，不为保留I而放宽全局校验 |

BaseDbUnitTest的 Application imports（`:61–75`）没有正式tenant配置；before只设TenantContextHolder，原 AdminUserServiceImplTest 对 PostService mock，不能证明两张岗位表跨租户隔离。新增专用上下文必须局部安装正式 `TenantLineInnerInterceptor(new TenantDatabaseInterceptor(TenantProperties))` 到实际 MybatisPlusInterceptor首位（生产YudaoTenantAutoConfiguration:73–80的模式），保留自动tenant INSERT及SELECT/UPDATE/DELETE，不以手工模拟过滤结果证明生产链。

新类使用真实 AdminUserServiceImpl、PostServiceImpl、User/UserPost/Post Mapper与Spring事务；仅Dept/tenant额度/password encoder/oauth2等不被验收外部边界显式mock，不能mock岗位查询或用户锁。每例task独立H2数据，多租户fixture通过测试JdbcTemplate显式tenant列准备，正式运行库不写。默认clean.sql已包含三表清理。新增上下文用 `@DirtiesContext(AFTER_CLASS)` 释放自身，避免MyBatis Plus静态TableInfo/批写factory污染旧context（docs/backend-development.md:1446+）；不得改全局BaseDbUnitTest、清全局实体缓存或用独立forkPASS替代默认组合回归。

## 8. 定向验收与测试载体（计划，未执行）

| 必验行为 | 载体与关键断言 |
| --- | --- |
| 已有启用+停用，修改联系方式保留 | 真实事务测试锁内B，两份postIDs相等；停用有效join原id/create_time保持；资料已变，密码/角色/生命周期/令牌未调用变更 |
| 明确移除一个/全部 | T差集精确、I未重插、[]两份清空；即使B停用合法也可移除 |
| 新增启用，新增停用/删除/跨租户拒绝 | 真实PostService/Mapper与tenant interceptor；锁内状态校验；每个负例拒绝后事务外重读资料/JSON/join全不变，无成功结果 |
| 旧JSON/join不一致、重复active join、既有岗位删除/跨租户 | 同一异常fixture分别调用get-for-update及update；both拒绝；T去掉错误ID也不得治理；deleted join历史不被误算active重复 |
| CREATE/注册既有合法无绑定 | CREATE缺post/null/[]可成功、编辑返回[]，编辑缺post/null拒绝；注册/Excel不加必填，覆盖导入原绑定不变 |
| 编辑HTTP参数与状态码 | 正式ObjectMapper绑定+真实Controller/服务代理：缺字段、null、非数组、小数、负数、空元素、溢出拒绝；[]允许；精确大Long字符串往返。standalone MockMvc若未加载生产安全filter，只算绑定/Controller合同，不声称HTTP权限或全链已通过 |
| 数据范围/tenant | 新锁不executeIgnore；专用真实tenant测试同时核对另tenant用户、岗位、join零泄漏/零变更；完整生产数据权限链若没有加载必须标明证据边界，静态权限链由主 Agent逐节点核验 |
| JSON更新后关联写失败 | 到达真实故障点（Spy当前任务Mapper或明确测试MyBatis故障注入），实际Spring代理回滚；事务外重读两份/资料与before相同。缺fixture/缺表/先被其他门禁挡住不算回滚PASS |
| 关联false/删除影响数异常 | 确认服务拒绝及整事务回滚，不接受默认成功 |
| 外层RR旧快照 | 代码/Mapper合同必须确认三类当前锁定读；受控latch验证外层先普通读、另一事务提交新B、待锁后取current B。若只有H2/mock证据，明确不等同MySQL InnoDB隔离验证，禁止写正式RR并发PASS |
| 实际前端handler | 新Node载体完整读取实际UserForm.vue，compiler-sfc parse/compileScript/compileTemplate，再TypeScript转译及Vue真实createRenderer执行setup/open/submit；实际user API/request wrapper加载，只替换底层I/O。不得正则抽取/复制handler、Proxy默认noop、把源码字符串当功能PASS |
| 前端正反路径 | assigned停用完整显示/保持、字段修改PUT完整集合、单项/[]移除、新启用加入、移除后停用不可重选、missing/坏响应加载阻断、取消/打开失败/提交失败无success或写请求、create不展示停用、连续打开/取消不串数据、大ID无精度损失 |
| 真实Playwright | 通过岗位和用户页面建立task自有启用岗位/用户→停用已绑定岗位→打开编辑可见已停用且选中→只改资料保存再打开保持→真实tag移除且本弹窗不可重选→保存再打开确认删除→创建/编辑候选不能新增停用。所有动作走页面；收集自然响应及最终两份只读核验；页面清理自有用户/岗位 |

前端已有设施依据：`IntRuoyiFronted/scripts/astra-c-reverse-drawer-size-1004.test.mjs` 确实使用真实SFC compiler/TS/createRenderer；新版 UM-05 harness可由主 Agent提供作为复用参考，但不能虚称某未读取/未建立载体已经存在。本合同Node harness不验证真实DOM和Axios拦截器，Element tag可删/不可重选必须由真实E2E关闭。

计划命令，执行前由主 Agent核对可用依赖、路径、实际新类名；不是已运行结果：

```powershell
# cwd: <managed worktree>/IntRuoyiBackend
mvn -pl yudao-module-system -am test '-Dtest=AdminUserServiceImplTest,AdminUserPostRetentionTransactionTest,UserPostEditHttpContractTest,PostServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false'
# cwd: <managed worktree>/IntRuoyiFronted
node --test scripts/system-user-post-retention.test.mjs
node tests/e2e/system-user-post-stale-filter-static.spec.js
pnpm exec eslint src/views/system/user/UserForm.vue src/api/system/user/index.ts
```

upstream的failIfNoSpecifiedTests=false不允许system零目标类；核对当前Surefire报告类名/时间/用例数/failure/error/skip与源码指纹。新增类若合并为一个载体，主 Agent须同步精确命令并证明覆盖，不运行虚构文件。组合测试必须保留原类，不把单独fork作为完成PASS。全仓未执行/既有无关失败分列；不给未授权无关缺陷补实现。

## 9. 前置条件与交接门禁

主 Agent 最新交接已实际核验：Java17 `C:/Users/BJB110/.jdks/jdk-17.0.20+8`、Maven3.9.16、Node24.12.0、pnpm10.22.0可用；执行Java/Maven需 `-Dfile.encoding=UTF-8`。真实MySQL `127.0.0.2:23306/ruoyi-vue-pro`、Redis `127.0.0.2:26379`已由主 Agent只读确认可用。作者未重跑这些核验，不能把环境可用转写成测试PASS。AdminUserServiceImplTest的BaseDb设施不依赖unit Redis16379，不应强加无关Redis前置。

仍未核验：worktree frozen依赖安装、H2/Surefire/专用tenant上下文是否可启动、实际handler harness可执行、正式运行MySQL三表与任务数据状态、真实用户/岗位菜单和页面入口、任务runtime及浏览器。这些不是本文PASS。

managed worktree创建由主 Agent管理，operation `50a967c3-8671-494f-a99e-06d246d57a5d` 已正式完成；主 Agent已核实clean/head基线与分支 `codex/user-inactive-post-retention`，路径 `E:/IntRuoyiWorktree/user-inactive-posts/IntRuoyi`，official slot7，前端8088/后端48088。作者继续仅写主仓两份文档，不在worktree改文件；主 Agent复制合同并维护worker任务文档。worker只在该最终managed路径与批准白名单实施。运行服务前重新核对profile/port/PID/归属，禁止占用48081/启停主int_main；不碰并行runtime。

主 Agent须先评审本文并签署待实施任务单，再派worker代码实施；本文作者不自行放行。实施与独立评审完成后仍需主 Agent逐节点核验与本次授权真实E2E。任何缺依赖/正式数据/页面入口准确阻断，不安装其他框架或API造数作为替代，不将文档结构/静态字符串/历史测试结果写成本轮实际PASS。

## Current Status

awaiting_main_review

本文仅形成可审查的实现合同与测试迁移清单。生产/测试代码零修改，测试/构建/服务/正式数据库/E2E零运行。

## UM-04 局部原始 JSON 完整性补充（主 Agent 批准，2026-10-08）

现有 JacksonTypeHandler 会在构造 Set<Long> 时丢失原始 JSON 的小数/重复 token 信息。UM-04 的正式事实完整性校验须读取并验证原始 post_ids，不可把已截断或去重的 DO 集合当作原事实有效的证明。

最小白名单扩充：AdminUserMapper.java 可增加一个局部 raw post_ids current read，显式约束用户 ID、当前 tenantId、deleted=false、FOR UPDATE。只能在编辑 get-for-update/update 链条内、原有正式用户锁成功后使用，同事务与 active join 集合核对；不修改既有锁方法、AdminUserDO、全局 JSON handler、CREATE/导入或 schema。

原始 null 且 active join 为空仍合法。非 null 必须是 JSON 数组，元素只能为正 Long 整数或规范正十进制 Long 字符串；小数、指数、越界、非规范字符串、null/boolean/object/嵌套元素及重复身份必须以 USER_POST_BINDING_INCONSISTENT 拒绝。不得在写入前规范化或猜测修复既有脏数据。合法精确大 Long 字符串须保留。

验证：真实 H2/Jackson/Mapper/事务负例至少覆盖 [10.5]、[10,10]、["01"]、非法 token；失败后只读重查用户与关联表，确认零写入。保留 null+空关联、规范大Long字符串正例。最终组合测试必须来自修改后稳定源码；中途旧源码结果只作诊断，不充当最终 PASS。

UM-04 编辑链可在局部私有锁包装器中将确证的 post_ids 列映射失败（ResultMapException 且 cause 链含 JsonProcessingException）归类为 USER_POST_BINDING_INCONSISTENT，必须保留原 cause 并终止。其他 Mapper/DB/权限/事务异常原样传播；禁止绕过原正式用户锁、继续流程、空集合或模拟成功。真实 H2 负例须触达该分支并验证零写入。既有公共锁方法及全局 handler 不改。主 Agent 2026-10-08 授权，待独立评审复核判据。

## Primary Approval — UM-04 Visible Error Ownership (2026-10-08)

Independent static audit found the UM-04 form sets a visible formal error then rethrows to unconsumed open/native click promises. docs/frontend-development.md command error boundary and duplicate-error ownership gates apply. The approved local boundary is: show the actual failure reason, keep editor and failure gate, terminate the command without further write/success/close, and consume that already-presented handler failure. Formal API wrappers must still reject; no global error disabling, empty catch, default data/success, or continuation after failure.

Frontend whitelist additionally permits only src/api/system/post/index.ts getSimplePostList and src/api/system/dept/index.ts getSimpleDeptList: a typed optional request-error ownership option with current global default preserved. UM-04 callers explicitly own load/write errors (ignoreErrorMessage:true); other callers retain existing global ownership. User create/update wrappers may accept the same optional ownership option; new get-for-update always uses the form-owned option. No other methods/routes/options or shared interceptor changes.

Corresponding actual-SFC tests must prove failed commands return at the visible failure state, preserve exact reasons, issue zero extra writes/success/close, and wrappers still reject; verify ownership options through real wrappers. Real Playwright must also exercise an actual server rejection via approved UI/task data and verify local formal text, no extra success/write, no unhandled page/native event exception. Root remains final release owner.
