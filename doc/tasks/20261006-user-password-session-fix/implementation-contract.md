# UM-05 实施约束

## 已批准范围

只实现密码重置/自助改密的秘密保护、事务撤销与本地退出，以及 S-01 为防止旧会话复活所需的共同锁和校验。其他 UM 不实施；不改表结构、签名内核、公共 OAuth/ CommonApi 响应协议，不增加降级或兼容路径。

## 本轮设计选择

用户本轮表示“不懂代码，你认为哪种合适”，将此项技术取舍交由主 Agent 判断。选择共用入口先读取正式 access，未命中才读取正式 refresh，缺正式行拒绝；仅正 ID ADMIN 再读取当前账号。数据库为有效性依据，缓存不能独立授权。MEMBER 的登录、密码、响应、刷新协议保持；共同校验增加正式令牌读取并拒绝其孤立缓存，这是本轮明确告知的取舍。只用于UM-05/S-01，不引入其它用户管理修复。

正式校验不得继承外部事务的旧RR普通读快照。两个公共get/check入口均暂停外部事务并用现有DataPermission注解关闭调用者部门数据范围，执行明确归属的只读正式查询，查询使用已提交状态；每次调用的access/refresh/user预算仍为最多三次，不持跨查询人员/token写锁。缺正式tenant/user/type、查询及Redis异常须明确失败；不打印token。Redis只接受按正式行生成的副本，残留或并发回填不影响授权判定。

## 最小安全合同

- 令牌校验以存储 tenant/user/type/client/scopes/expiry 为准，ADMIN 最多 access/refresh/user 三次正式读取；不信缓存的人员身份。正 ID ADMIN 账号必须存在、启用且无有效锁；INITIAL/RESET_REQUIRED 仍可登录改密。userId=0 ADMIN 仅为已有机器协议，不查询假人员。正式 tenant 缺失明确失败。
- 外部 Spring 事务持目标用户显式 tenant/id/deleted=false 行锁，再操作 token。密码两入口锁内验证旧密码/强度/原复用政策，历史、密码/更新时间/凭据状态与全部 DB access+refresh 撤销同事务。保留角色、岗位、组织、生命周期与锁状态。
- access 和 refresh 分别按明确 tenant/user/type 全量枚举并删除，覆盖所有 client 和孤立 refresh。Redis 同步删除失败抛错，回滚 DB；已删缓存不恢复，额外退出副作用如实报告。
- 密码 login/password grant 把认证时的 tenant/id/hash/time/credentialStatus 快照交给锁内签发，变化拒绝重新认证；快照仅内存且不生成含秘密的 toString。成功登录清失败计数在锁内校验通过后执行，不能预先清新锁。
- 刷新先正式定位，再存储租户内锁用户，再显式当前读 refresh；不用锁前对象/旧 RR 快照签发。创建、状态停用、到期停用和撤销采用 user -> token 同一锁序。MEMBER 写协议保留，不加 ADMIN 人员锁。
- TenantUtils Runnable 作用域恢复 tenant/ignore 且保持业务异常，不使用会包装 ServiceException 的 Callable 造成401变500。HTTP 不一致租户由既有 filter 拒绝，不改全局框架。
- 登录/短信/社交/注册/刷新从签发时正式凭据状态生成现有改密标记；UI 标记优先于原 redirect/SSO。开放 OAuth/CommonApi 响应不扩字段，一次性凭据消费副作用不虚称回滚。
- 管理重置仅记录 targetId/targetNickname 和操作事实，不放完整 user/新旧密码或哈希到 LogRecord。两密码API保留sanitizeKeys并明确requestEnable=false，禁止请求参数进入访问/控制台/错误日志；提前提供两真实密码Controller的隐私元数据，覆盖Security拒绝和错误HTTP方法。受保护日志不读取body/query，不记录异常消息、cause/suppressed文本或原Throwable，但保留异常类型、安全栈帧及失败code；操作者、URL/方法、操作名、结果和耗时保持，目标id/昵称由原安全LogRecord记录。控制台拦截器只尊重现有requestEnable开关，不重写全局日志或改变默认记录接口。
- 两密码接口的日志URL使用真实映射派生的固定路径，不能记录可能含matrix参数的原始URI；固定路径元数据缺失明确失败，不退回原URI。真实认证、权限和租户拒绝沿用原CommonResult响应与code，只将受保护请求的结果交给现有访问审计；框架直接HTTP拒绝以原状态码记录失败，不解析响应正文。普通接口保持原有日志行为。
- prompt inputType=password、共享强度规则、取消不请求；成功消息不含秘密。仅密码请求 ignoreErrorMessage=true，生产 handler 局部脱敏报错一次。成功后列表刷新失败单独说明，不重复重置。
- 重置本人/自助成功直接明确本地清理 removeToken/deleteUserCache/resetState 再导航登录，不请求已失效 logout；失败保留表单。清理后导航失败显示需重新登录，不恢复旧 token。

## 验证矩阵

- 后端定向：孤立 refresh、多 client、refresh-as-bearer、缓存污染/回填、账号禁用/有效锁/待改密、机器与 MEMBER 边界、tenant 恢复与查询预算、入口快照传递。
- 真实 Spring/Mapper 联合事务：密码/历史/token 回滚，Redis 第 N 次删除故障，锁后重读，认证/重置及刷新/重置两种交错；不能仅 mock verify 证明事务。
- 前端：实际 SFC handler/API wrapper/Pinia action 行为，遮蔽、取消、弱密码、局部错误、重置他人列表失败、本人/自助清理与导航失败、标记优先跳转；SFC 编译/ESLint/相关 TypeScript 验证。
- 真实 E2E：仅 Playwright 操作页面创建任务账号、首次改密、建立两个真实会话、管理员重置、旧会话退出、重置后登录强制改密、自助改密后重新登录及账号清理。依据 docs/e2e-rules.md 的 frontend-only check，业务断言取页面 DOM、输入、弹窗及导航；页面DOM、输入、弹窗及导航为业务动作验收依据；自然页面响应仅附带路径/方法/HTTP状态/业务code及解析错误类型，按docs/e2e-rules.md核对请求结果，不用接口JSON替代页面行为，不读取或记录其它响应字段，不主动发API请求或API/DB写请求，不改共享 admin 凭据或权限。
- 静态/定向测试/真实 E2E 全过且主 Agent 复核才可融合。缺前置或有当前范围失败须记录 BLOCKED/FAIL，不以静态 PASS 替代 E2E。

## 当前阶段

M2全部门禁实际通过，M3融合收尾待执行。Java17完整31模块package、包身份、后端257、前端50及完整规则定向ESLint、真实Playwright业务与实际MySQL旧RR生产service核验均PASS。首次页面双提交局部修复仅ResetPwd及已有回归；初次wiring失败与H2隔离诊断保留，未降低断言。测试账号已UI删除且只读活动令牌为零，不改共享admin及主48081。
