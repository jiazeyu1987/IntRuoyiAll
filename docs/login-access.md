# IntRuoyi 登录访问说明

## 当前本机入口

- 前端工程：`E:\IntRuoyi\IntRuoyiFronted`
- 后端工程：`E:\IntRuoyi\IntRuoyiBackend`
- 本机前端登录页：`http://localhost:8081/login?redirect=/index` 或 `http://127.0.0.1:8081/login?redirect=/index`
- 本机后端健康检查：`http://127.0.0.1:48081/actuator/health`
- 本机 API 前缀：`/admin-api`

## 本机登录来源

- 前端本机模式使用 `IntRuoyiFronted\.env.local`：`VITE_PORT=8081`，`VITE_BASE_URL=http://127.0.0.1:48081`，`VITE_PROXY_TARGET=http://127.0.0.1:48081`，本机验证码关闭。
- 登录表单只允许从 `IntRuoyiFronted\.env` 的 `VITE_APP_DEFAULT_LOGIN_TENANT` 读取默认租户；用户名和密码不得通过环境变量或源码默认预填。
- 需要描述默认本机 E2E 身份时，使用 `芋道源码/admin` 作为身份标签；它只表示本机默认租户和用户名，不代表正式环境地址或正式环境授权。
- 用户要求修改截图或当前页面中的本机数据但未另行指定租户时，写入前必须通过真实登录确认截图对应的租户/账号，并同时核对页面可见业务范围标识（例如负责路线）和目标列表数量；不得因为另一“测试租户”更便于操作就静默切换。无法从页面证据唯一确认目标租户时必须先阻塞询问。

### 登录页默认凭据禁止门禁

- Trigger: 登录页默认出现 `admin`、无浏览记录浏览器出现默认用户名、构建环境或源码出现 `VITE_APP_DEFAULT_LOGIN_USERNAME` / `VITE_APP_DEFAULT_LOGIN_PASSWORD`，或有人要求恢复默认账号密码。
- Preflight check: 修改登录页、登录缓存、`.env`、发布包或 E2E 登录脚本前，运行 `node IntRuoyiFronted/tests/e2e/login-default-credentials-static.spec.mjs`，并用 `rg -n "VITE_APP_DEFAULT_LOGIN_USERNAME|VITE_APP_DEFAULT_LOGIN_PASSWORD" IntRuoyiFronted/.env IntRuoyiFronted/src IntRuoyiFronted/types` 确认默认用户名/密码未进入源码和环境配置。
- Blocker: 登录组件读取默认用户名/密码环境变量、`.env` 设置默认用户名或密码、空浏览器登录页预填 `admin` 或默认密码、旧缓存只按单一租户清理默认凭据，必须停止并修复。
- Verification: 静态合同通过；空缓存登录页用户名和密码为空；用户主动保存的非默认登录历史仍可回填；旧版默认 `admin` / `admin123` 缓存会被清理。
- Forbidden action: 禁止用隐藏输入框、仅清本机浏览器缓存、只改测试脚本、只改发布环境变量、继续保留默认密码变量、或把“仅测试环境方便登录”作为默认管理员凭据保留理由。
- Evidence: `doc/tasks/20260901-remove-login-default-admin/execution-log.md`。

## 环境门禁

- 默认只允许使用本机 `localhost:8081` / `127.0.0.1:48081` 做开发、调试、联调和 E2E。
- 访问测试服务器、正式服务器或备用服务器前，必须先取得当前任务中的明确授权，并核对 `docs/server-access.md`。
- 未经当前任务明确授权，不得登录、联调、E2E、发布、重启、排障或验证远端环境。
- 正式服务器与备用服务器按生产等级处理；即使只读登录或 HTTP 探测，也必须有当前任务授权。

### 远端单账号菜单授权门禁

- 触发场景：用户要求在测试服务器或其它远端环境给某个账号增加菜单页签可见权限。
- 排查顺序：先确认目标环境授权，再按账号真实所在租户核对账号唯一性、目标角色唯一性、目标菜单链和角色菜单绑定；“测试服务器”不等于“测试租户”，不得因为角色或账号在另一个租户更方便就静默切换。
- 经验规则：只补齐目标账号到正式权限角色的绑定，不修改目标角色菜单、不扩大其它角色权限、不重置或猜测目标账号密码；如需页面验证，必须使用目标账号真实凭据重新登录。
- 验证方式：授权前记录缺失绑定作为 RED；授权后核对 active 用户角色绑定数、目标菜单链授权数、非目标角色未新增该菜单，并精确清理该用户权限缓存；缺少目标账号密码时，菜单本人登录 E2E 应标记为阻塞项而不是 API-only 冒充通过。
- 禁止做法：禁止把同名角色跨租户复制、给管理员或宽权限角色绕过、用前端隐藏代替权限授权、输出或记录密码/哈希/Cookie、为了验收重置业务用户密码。

### 审批中心入口角色菜单隔离门禁

- Trigger: 账号只应拥有审批中心入口，但登录后看到 MES、ERP、DCC、智能排产、报工或其它业务模块父级菜单。
- Preflight check: 先按账号真实角色枚举 `审批中心入口` 的全部角色菜单绑定，并沿每个菜单的父级链核对是否落入非审批业务模块；按钮权限也必须检查，因为子按钮会带出父级菜单。
- Blocker: `审批中心入口` 绑定任何非审批中心菜单、业务模块按钮、业务页面或会带出业务父级的子权限时必须停止并修正角色菜单，不得把问题归因于前端缓存或单个账号。
- Verification: 修正后核对该角色到目标业务菜单的 active 绑定数为 0；目标账号在相关业务菜单树下的静态和动态授权均为 0；与当前任务无关的正式权限角色仍保持原菜单链。
- Forbidden action: 禁止用隐藏菜单、只改单个用户角色、清空全库 Redis、删除菜单本身或扩大其它角色权限来掩盖审批入口角色污染。
- Evidence: `doc/tasks/20260830-test-approval-center-pressure-pump-permission-removal/verification-report.md`。

## E2E 与数据约定

- 默认 E2E 使用本机前端入口：`http://localhost:8081`。
- 写入型 E2E 必须使用已确认的测试租户和账号，并创建可追踪的任务自有测试数据。
- 从现有用户列表选择多角色 E2E 账号时，必须先从业务合同取得精确角色标识、权限标识和固定账号约束，再通过真实用户管理页、权限角色页与菜单页做零写入核对；角色不存在、目标账号未绑定该精确角色、必需权限菜单不存在或只能找到名称相似角色/权限时，必须在创建 fixture 前停止。创建角色、分配角色、修改账号、创建菜单权限是不同的数据基线变更，必须分别取得当前任务明确授权，不能把“可使用现有账号”或“可创建角色”解释为可修改其它权限基线。
- 创建本机任务自有 E2E 用户前，必须核对登录接口的用户名格式校验；当前账号密码登录用户名只允许数字和字母，任务账号不得包含下划线、空格或中文。账号需带可追踪任务标识并记录租户、角色、业务范围和回滚方式，但不得记录密码。
- 如果任务需要使用远端测试服务器做真实路径验证，必须先记录授权范围、目标主机、目标租户、账号来源和数据清理方式。
- 最终 E2E 发现问题时，回到已授权的开发或测试租户修复与复测，不得静默切换到其他租户、账号或环境。

### 后台用户密码复杂度变更门禁

- 触发场景：修改注册、忘记密码、个人中心改密、管理员新增用户或重置密码的后台用户密码强度规则。
- 经验规则：密码强度必须以后端正式策略为准，前端只能共享同一校验入口和用户可见文案；登录页不得套用新密码复杂度校验，以免阻断存量弱密码、过期改密或后端认证链路。
- 排查顺序：先补后端策略 RED，再补前端共享规则静态合同 RED；实现时同步更新后端错误码文案、前端共享规则、个人中心提示文案和相关服务测试里的强密码样例。
- 验证方式：后端至少覆盖长度不足、缺大写、缺小写、缺数字、缺特殊字符和强密码通过；前端静态合同必须证明所有新密码入口复用共享规则，且登录页仍只做必填校验。运行态复验必须额外覆盖“不含特殊字符但满足长度、大小写和数字”的探针；若源码/测试已通过但运行态仍接受该探针，应先判定运行 Jar 未刷新，重新确认 `48081` PID、运行 Jar 和健康检查后再复验。
- 禁止做法：禁止只改前端提示、只改某一个页面、保留旧弱密码兼容路径、用登录页校验证明改密规则，或在任务日志中记录明文密码、哈希、Cookie。

## 失败处理

- 登录失败时，必须直接报告实际失败位置、目标入口、租户/用户标签、接口响应和影响。
- 租户识别失败时，先记录登录页可见错误和是否发出登录请求；不得把未发请求的前端租户校验、登录接口失败和登录后权限信息请求超时混为同一原因，也不得静默切换租户或账号。租户恢复后必须用同一业务账号重新走真实登录页，再继续权限和业务路径核验。
- 不得静默切换账号、租户、后端地址、验证码模式或远端环境。
- 不得在文档、任务日志、提交信息或命令记录中写入密码、令牌、私钥、连接串密钥或其他凭据。

## 登录图形验证码链路门禁

- 修改账号密码登录的图形验证码时，必须同时核对前端登录页触发点、登录 API payload 和后端 `AdminAuthServiceImpl.login` 校验链路；不得只改 `.env.local` 或隐藏弹窗。
- 若范围仅为账号密码登录，不得顺手关闭注册、忘记密码、短信验证码等其它验证码链路。
- 验证至少覆盖一个前端静态合同、一个后端登录服务单测和前端类型检查；如果本机运行态可用，再通过真实登录页确认不出现滑块/点选/图形验证码。

## ERP 外部助手短期票据授权门禁

- Trigger: ERP 菜单接入独立外部工具、iframe 打印助手、发票凭证打印助手，或用户要求“知道助手地址也不能直接访问”。
- Preflight check: 先确认 ERP 角色、菜单权限和入口路径，再确认前端不会直接把 iframe 指向助手首页；前端必须先向 ERP 后端申请短期票据，助手只能通过 `/auth/callback` 校验票据并换取助手会话，首页和业务 API 直连必须拒绝。
- Runtime readiness: 外部助手由 ERP 页面承载时，进入页签先通过 ERP 后端探测助手是否在线；在线才申请票据并加载 iframe，未在线且配置可启动时显示明确的“启动助手”动作，由 ERP 后端启动配置的助手程序并等待健康探测成功后再进入，不能把连接拒绝页直接展示给业务用户。状态查询和启动接口必须继续使用同一业务权限保护。
- Port contract: 发票凭证打印助手使用独立固定端口 `18733`，不得占用 ERP 前端 `8081` 或通过环境变量静默改成其它端口。测试服务器和正式服务器是不同机器时可以复用 `18733`；助手和 ERP 后端都必须在启动阶段拒绝非 `18733` 配置。
- ERP config bridge: 发票凭证打印助手的金蝶连接信息必须由 ERP 后端在短期票据校验成功后返回当前生效配置快照，字段至少覆盖基础地址、账套 ID、用户名、密码、应用 ID、SimPas 签名数据、SimPas 签名时间戳和 LCID；助手只能把该快照写入授权会话专用配置文件并传给查询/生成/上传脚本，不得依赖独立部署的全局 `.env.kingdee` 或 `KINGDEE_ENV_PATH`。SimPas 的 `signeddata` 只能作为签名数据使用，禁止当作 `appSecret`。容器只保留 `KINGDEE_RUNTIME_DIR` 作为会话配置文件目录。若测试服仍报 `ERP配置文件不存在：/opt/invoice-voucher-print-assistant/runtime/.env.kingdee`，先判定远端助手包或运行进程仍是旧版本，必须重新部署并重启新版助手，不能通过补一个全局 `.env.kingdee` 掩盖链路未刷新。
- 配置修复边界：正式账套配置页读取必须允许展示缺失授权字段的已保存连接，供管理员补齐并保存；ERP 实际登录、同步和助手配置快照仍必须对缺少 `appId/signedData/timestamp` 明确失败。禁止让配置读取直接复用运行态严格校验，导致管理员无法修复坏配置。
- Blocker: 助手缺少 ERP 票据校验地址、票据不是短期有效或一次性消费、无权限用户可拿到票据、iframe 直接打开助手首页、助手首页/API 直连仍展示业务功能、只隐藏菜单但独立助手仍可直连，或短期票据校验成功后缺少当前 ERP 金蝶配置快照时必须停止。
- Verification: 后端单测覆盖有权限签票、无权限拒绝、票据校验和消费、当前金蝶配置快照返回；前端静态合同覆盖票据入口与 `/auth/callback` 以及 ERP 配置页可维护助手所需字段；助手静态合同覆盖会话拦截、会话级配置文件生成和业务脚本使用会话配置；真实 Playwright 必须证明 admin 从 ERP 菜单可打开助手、无权限账号看不到菜单、直接访问助手返回 403。
- Forbidden action: 禁止把菜单隐藏当成直连防护，禁止把 ERP 登录态 token 直接暴露给外部助手，禁止用 mock 校验、默认放行、配置缺失时开放访问、让助手回退读取全局 `.env.kingdee`，或 API-only 冒充页面验收。
- Evidence: `doc/tasks/20260829-invoice-voucher-print-assistant-auth-gate/verification-report.md`。

## ERP 金蝶账套登录连通性门禁

- Trigger: 更换金蝶账套、验证 `acctId`、复用当前 ERP `baseUrl`、调用 `ValidateUser`、判断金蝶 WebAPI 是否连接成功。
- Preflight check: 从当前生效配置读取基础地址、集成用户、密码和 `lcid`，但不得输出敏感值；当前生效配置必须按 `ErpKingdeeConfigService.getEffectiveProperties()` 的正式解析顺序确定，先读取 `yudao.erp.kingdee.connection.active`，再读取对应的测试或正式账套保存配置。环境变量为空不等于当前运行配置缺失，只有正式解析入口也缺少必需字段时才可判定 blocker；不得跳过已保存配置直接用环境变量存在性下结论。通过 MySQL CLI、PowerShell 或其它中间工具读取中文用户名时必须显式使用 UTF-8/`utf8mb4`，并在请求前验证用户名未变成问号或乱码；登录 URL 必须与生产客户端使用同一归一化规则，基础地址未以 `/K3Cloud` 结尾时先补齐；先以当前已配置账套做控制组，再以目标账套做目标组，且两组只能改变 `acctId`。
- Blocker: `yudao.erp.kingdee.connection.active` 缺失或为空、HTTP 非 200、响应无法解析、`LoginResultType` 不为 `1` 且 `IsSuccessByAPI` 不为 `true`、或缺会话 Cookie 时必须判定登录失败；控制组也失败时，当前凭据或认证基线无效，不能把失败归因于目标账套；目标组单独失败时，阻塞于目标账套账号、密码、授权或登录方式。
- Verification: 仅调用 `AuthService.ValidateUser.common.kdsvc`，记录脱敏的接口来源、路径、HTTP 状态、业务登录状态、Cookie 是否存在和安全错误摘要；登录成功必须同时满足 HTTP、金蝶业务状态和会话 Cookie 条件。若用户名包含中文，验证探针还必须证明 UTF-8 编码链路完整，乱码请求的失败结果不得归因于密码或账套。
- Read permission check: 登录成功只证明会话可建立，不证明具体业务对象可读；验证生产订单、采购订单等对象时，必须在同一会话中调用目标表单的正式只读接口，并限制到最小字段和少量行。`ExecuteBillQuery` 返回合法数组才可判定读取权限通过；HTTP 200、Cookie、错误对象或含错误信息的数组都不能代替业务对象读取成功。
- Credential refresh check: 正式账套用户名、密码或账套编号变更后，必须先读回当前保存配置，再用同一组正式凭据做至少一条真实表单查询；只验证 `ValidateUser`、只验证 Cookie、或只看配置写入成功都不算正式读取通过。
- Test config mirror check: 需要把测试保存配置同步为正式访问方式时，必须先比对 `yudao.erp.kingdee.config` 与 `yudao.erp.kingdee.connection.production` 的连接字段，再用更新后的测试保存配置做真实 `ValidateUser` 和至少一条 `ExecuteBillQuery` 验证；不能把正式连接成功误当成测试保存配置已更新。
- Field contract check: 金蝶不同账套、补丁版本或业务表单的字段标识可能不同；新增同步对象时必须先在目标账套逐步探测正式字段合同，并把不可读字段视为阻塞或从正式模型中移除，不得按页面列名猜字段、用旧表单字段替代或把错误数组当作数据数组。日期格式也必须以目标账套真实返回为准并纳入解析测试。
- Sync timeout check: 执行金蝶全量同步前，必须确认客户端等待时间和服务端 ERP HTTP 客户端超时都明确可观测；客户端超时只证明调用方放弃等待，不等于服务端线程已停止或同步失败记录已落库。全量入口必须先提交后台运行记录，再执行 `ExecuteBillQuery` / `RestTemplate.postForEntity` 等远程请求；开始记录不能与远程 ERP 查询共用一个长事务，否则其他页面会持续显示未执行。若线程栈停在 ERP 响应路径，必须先记录运行表、访问日志和线程栈，再判定为外部 ERP 响应阻塞；不得启动第二轮全量同步、按单据号分批替代全量、切换连接或手工写表冒充完成。
- Connection switch: 多账套切换必须把各账套连接配置与当前连接类型分别保存在后端；查询接口只返回类型、名称和固定选项，不得返回连接凭据。前端选择态只能标记“待保存”，不得直接改变实际连接；保存时必须先校验目标连接配置完整有效，再持久化当前类型，失败时保持原连接。所有 ERP 同步服务必须从同一后端有效配置解析入口读取当前连接；缺少当前连接选择配置必须明确报错，不得各自缓存、推断、默认 TEST 或回退。
- Switch verification: 真实页面验证必须覆盖“仅选择不生效、保存后生效、刷新后保持”，并按 `docs/e2e-rules.md#全局开关类-e2e-恢复门禁` 回切和复验原始账套；数据库/API 核对只记录类型、字段存在性和脱敏结果，不记录密码、Cookie 或完整连接 JSON。
- Forbidden action: 禁止把收到 Cookie 当作登录成功；禁止用 HTML5/Silverlight 单点登录测试链接、临时时间戳签名或 `appID` 静默替代当前用户名密码认证；禁止缺少当前连接选择配置时默认测试账套、默认正式账套、读取环境变量兜底或继续执行同步；禁止为了验证登录调用 `ExecuteBillQuery`、保存目标配置、输出密码/Cookie/签名或记录完整响应。
- Evidence: `doc/tasks/20260807-kingdee-target-acct-connectivity-check/verification-report.md`、`doc/tasks/20260807-kingdee-production-order-read-check/verification-report.md`、`doc/tasks/20260813-erp-production-pick-list-sync/verification-report.md`。

### 发布后页面登录基线门禁

- Trigger: 测试服发布后需要用 Playwright 验证真实业务页面或版本浮层。
- Preflight check: 发布前只读确认测试租户账号可登录、密码未过期、租户选择接口有目标租户；凭据只能由当前进程安全注入，不写入任务文档或截图。
- Blocker: 登录返回密码过期/账号密码错误、租户选择无数据、目标前端入口不可达或页面 console errors 非零时，认证后页面门禁必须 BLOCKED；不能猜密码或改账号。
- Verification: 记录脱敏的登录页 URL、租户/账号标签、HTTP 状态、页面 title、console errors 和失败提示；公共登录页/静态 release-info 与认证后业务页结论分开。
- Forbidden action: 禁止冒用本机 admin 密码登录远端测试租户、切换正式/备份环境、API-only 冒充页面、为验收修改租户/用户密码或把过期凭据写进脚本。
- Evidence: IntRuoyiMaintance task 20260823-fix-training-rules-sfc-test-release execution-log.md ISSUE-016。

### 后台账号密码过期数据核验

- 触发场景：用户要求修复测试环境单个后台账号的密码过期，或登录返回密码过期且需要核对运行库账号状态。
- 经验规则：先按真实运行库核对 system_users 的正式字段和认证策略；不要假设存在独立的 password_expire_time。若正式策略使用 password_update_time + MAX_AGE_DAYS，有效期必须按该公式计算，不能用前端提示或猜测字段替代。
- 排查顺序：确认目标环境和租户授权 -> 核对有效账号唯一性 -> 读取正式认证策略与真实 schema -> 在单一事务内精确更新目标账号 -> 回读影响行数、密码哈希格式和更新时间 -> 通过真实登录页验证未再出现过期提示。
- 账号级延长仅在用户明确授权的测试数据操作范围内执行；按公式反推更新时间时应记录计算口径，不得修改全局认证策略、增加账号级绕过或把临时数据修复写成产品逻辑。
- 禁止做法：禁止把不存在的过期列当作事实、把 NOW()+窗口的移动值与延迟回读结果直接作严格相等比较、API-only 冒充真实登录，或在日志中记录密码、哈希和连接凭据。

### 离职转岗账号联动停用门禁

- Trigger: 用户管理、离职账号、转岗账号、账号生命周期、HR/BPM 单据要求账号停用时间与单据生效时间一致。
- Preflight check: 先确认 `system_users` 是否有 `lifecycle_document_type`、`lifecycle_document_no`、`lifecycle_document_time`、`lifecycle_effective_time`、`lifecycle_deactivated_time`；再确认登记入口、到期任务和导出/详情字段都走这些正式字段。定时任务迁移必须按 `handler_name=userLifecycleDeactivateJob` 注册，并显式配置 `{"limit":正整数}`。
- Blocker: 只有手工启停用、缺少单据编号/单据时间/生效时间、联动停用时间取扫描时间而非单据生效时间、已联动停用账号仍可手工启用、或任务参数缺失仍默认继续时必须停止。
- Verification: 后端单测覆盖立即离职停用、未来转岗到期停用、令牌移除、禁止手工启用和 job 参数 fail-fast；静态迁移合同覆盖字段、索引、`infra_job.handler_name` 业务键与无固定任务 ID；release migration policy gate 必须通过。
- Forbidden action: 禁止用备注字段、状态原因文本、管理员手工操作日志或默认任务参数代替正式单据字段；禁止迁移时自动禁用存量账号；禁止没有 HR/BPM 来源表时模拟单据来源。
- Evidence: `doc/tasks/20260830-system-user-lifecycle-deactivation/verification-report.md`。

### 账号锁定解锁与空闲退出门禁

- Trigger: 连续错误登录锁定、账号解锁、失败计数清零、后台登录后空闲自动退出或锁屏。
- Preflight check: 先确认 `system_users` 已有失败计数、锁定标记和锁定时间正式字段；重置锁定必须显式清零失败计数、锁定标记和锁定时间，不能依赖 ORM 默认忽略 null；前端空闲退出必须挂在认证布局的统一 hook，监听鼠标、键盘、触摸、滚动和可见性变化。
- Blocker: 失败次数达到阈值但未锁定、解锁后锁定时间仍残留、成功登录未清零、前端空闲超时只弹窗不退出/锁屏、或超时后未清空标签/锁状态时必须停止。
- Verification: 后端单测必须覆盖第 5 次失败锁定、锁定后拒绝登录、成功登录清零、解锁后恢复；真实 E2E 中第 5 次错误登录可直接返回“账号已锁定”，断言应以“最终进入锁定状态且随后正确密码仍被拒绝”为准；前端静态合同或类型检查必须覆盖 15 分钟定时器、活动事件监听、登录页重定向和注销时清空锁状态。
- Forbidden action: 禁止用 fallback 解锁、默认成功清零、吞掉登录失败、或只改 UI 文案不改正式会话状态。
- Evidence: `doc/tasks/20260827-login-security-controls/verification-report.md`；`doc/tasks/20260827-login-security-int-main-e2e/verification-report.md`

### ERP 表格全量同步写入门禁

- Trigger: ERP 自动同步页面新增“全量同步”、需要补齐某张 ERP 表，或发现全量任务把已有本地数据更新/重复计数。
- Timeout/page-window check: 对可能返回大量数据的 ERP 全量查询，必须把业务时间窗口和单页规模作为任务上下文显式传入查询客户端，并验证分页会继续读取；不能依赖客户端内部隐式日期、单次大响应或把读取超时当成成功。
- Preflight check: 全量入口必须传递显式同步类型并从统一当前连接解析入口取得账套；逐表冻结正式业务编号字段和本地唯一键；执行前确认任务记录能分别表达新增、更新、跳过和失败。
- Production order selectOne check: 金蝶生产订单同步涉及同步记录、生产工单编码、物料编码、默认物料分类、计量单位编码/名称、排产有效工单等同租户唯一查询；任一查询出现 `TooManyResultsException` 时必须用生产订单同步错误码返回包含冲突键的业务 blocker。不得复用采购订单错误码、不得 `selectFirstOne`、不得默认取第一条继续同步。
- Blocker: 当前连接未明确选择、同步类型不在正式白名单、业务编号字段缺失/来自页面文案猜测、全量路径复用增量水位、或已有相同业务编号仍会更新本地记录时必须停止。
- Verification: 以一个本地已存在编号和一个本地不存在编号做服务级回归；前者必须只计入 `skipped` 且无 insert/update，后者才计入 `created`；全量任务记录的 `updated` 在跳过合同下必须为 0。生产订单同步重复唯一键修复后，必须至少覆盖一组 Java 回归和静态契约，并用当前模块 `clean test` 证明编译产物不是旧缓存。
- Forbidden action: 禁止用增量时间窗口伪装全量、按单据分批冒充全量、默认切换测试/正式账套、覆盖已有本地记录、把跳过计入新增或更新，或吞异常返回成功。
- Evidence: `doc/tasks/20260821-erp-table-incremental-full-sync-actions/verification-report.md`；`doc/tasks/20260830-test-server-production-order-sync-selectone/verification-report.md`。
