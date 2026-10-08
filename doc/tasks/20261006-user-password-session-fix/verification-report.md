# UM-05 Verification Report

## Decision

PASS — 主Agent已放行第一项UM-05/S-01代码，M2所需静态、定向回归、真实页面E2E和MySQL并发核验均实际通过。当前ready_for_closeout，临时产物和任务运行实例已实际清理；本地任务提交、主干融合及worktree归档被Git锁阻塞，尚未完成。

## Requirement To Evidence

| 验收 | 结果与证据 |
| --- | --- |
| AC-05-01 密码遮蔽、强度及秘密日志 | PASS。真实prompt/三输入password，取消及缺特殊字符零PUT；秘密静态契约2项和日志37项实际通过，LogRecord仅安全目标字段。 |
| AC-05-02 重置与全部旧会话撤销 | PASS。真实UI重置一次成功、操作者保留；两独立旧浏览器access/refresh401并实际自动回登录。正式access和孤立refresh同事务全量撤销，缓存不能独立授权。 |
| AC-05-03 本人改密及重新登录 | PASS。首次/重置凭据登录实际选中密码设置；局部修复后一次保存唯一RequestID2 PUT HTTP200业务0，成功提示后清会话回登录，最终新密码普通登录身份可见。 |
| AC-05-04 失败/取消/并发与原策略 | PASS。生产handler50项覆盖失败保留会话、导航失败、同步双触发、校验/请求pending、重试；后端历史复用与管理员允许复用策略分开。MySQL真实旧RR生产service拒绝4access/4refresh，DML0。 |
| S-01 正式校验/租户/机器/MEMBER边界 | PASS。共同get/check NOT_SUPPORTED暂停旧事务、明确正式tenant/user/type，最多三查；真实Spring/Mapper56项及入口/联合事务回归，ADMIN snapshot锁内签发与刷新当前读。 |

## Business Chain Static Review

主Agent按实际入口逐节点复核，以下不是关键词命中结论。代码依据中的B表示`IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/`，F表示`IntRuoyiFronted/src/`，W表示`IntRuoyiBackend/yudao-framework/`。方法锚点与行号来自当前worktree。正式测试结果沿用此前实际运行证据；本轮57项实现/测试/经验文件的UTF-8/LF SHA全部与放行清单一致，没有重跑或扩大测试范围。

| 业务节点 | 前置、输入、处理和下游结果 | 当前代码依据与审查结论 |
| --- | --- | --- |
| 管理员打开重置 | 用户列表按目标行选择；按钮要求system:user:update-password；输入遮蔽，取消/关闭不请求，强度失败明确提示且不请求。 | F/views/system/user/index.vue:295、handleResetPwd:702；F/api/system/user/index.ts/resetUserPassword:103。PASS，真实E2E另证取消/弱密码零PUT。 |
| 管理重置接口授权 | PUT /system/user/update-password；后端重新检查管理权限，@Valid验证目标与密码，成功只返回Boolean，无密码回显。 | B/controller/admin/user/UserController.java/updateUserPassword:89。PASS，不能以按钮隐藏代替接口权限。 |
| 本人改密入口与身份 | 三输入遮蔽，旧密码必填、强度/确认一致；同步锁在异步校验前获取，保存/重置和输入在处理中禁用；接口目标来自getLoginUserId，不接收任意目标ID。 | F/views/Profile/components/ResetPwd.vue/submit:73、reset:102；F/api/system/user/profile.ts/updateUserPassword:49；B/controller/admin/user/UserProfileController.java/updateUserProfilePassword:73。PASS，失败保留会话并释放锁，可重试；真实唯一PUT和50项handler回归另证。 |
| 正式账号与并发前置 | 改密必须已有可写Spring事务、明确且匹配的租户、正用户ID；以tenant/id/deleted=false FOR UPDATE读取正式账号，缺失明确拒绝。 | B/service/user/AdminUserServiceImpl.java/lockUserForSessionMutation:433；B/dal/mysql/user/AdminUserMapper.java/selectPermissionSubjectForUpdate:25。PASS，不能从UI对象、缓存或旧普通读快照获得密码事实。 |
| 本人密码校验与落库 | 锁内验证旧哈希、正式强度、当前密码及最近历史复用；保存原哈希历史，再更新新哈希/时间/ACTIVE，并调用同事务撤销。管理员重置使用独立策略、ADMIN_RESET历史及RESET_REQUIRED。 | B/service/user/AdminUserServiceImpl.java/updateUserPassword:315、336、validatePasswordNotReused:792、savePasswordHistory:819；B/dal/mysql/user/AdminUserPasswordHistoryMapper.java/selectLatestListForUpdate:14。PASS，历史tenant/user/deleted当前读；SELF_CHANGE与ADMIN_RESET策略不混用。 |
| 全部旧会话撤销 | ADMIN按正式tenant/user/type分别当前读全部access与refresh，含跨client及孤立refresh；按ID有序锁定，逐行删除检查影响行数，缓存删除错误传播使DB事务回滚。 | B/service/oauth2/OAuth2TokenServiceImpl.java/removeAccessToken(Long,Integer):439；B/dal/mysql/oauth2/OAuth2AccessTokenMapper.java/selectListForUserRevocation:42；B/dal/mysql/oauth2/OAuth2RefreshTokenMapper.java/selectListForUserRevocation:28。PASS，不能仅沿access找到refresh。 |
| 并发旧登录签发 | 认证结果形成仅调用栈快照；签发在正式用户锁内比较tenant/id/hash/updateTime/credentialStatus，变化或过期拒绝，之后才清失败计数并创建令牌。 | B/service/auth/AdminAuthServiceImpl.java/login:117；B/service/oauth2/AdminPasswordAuthenticationSnapshot.java/from:28；B/service/oauth2/OAuth2TokenServiceImpl.java/createPasswordAccessToken:167、issueAdminToken:286。PASS，重置前已认证的旧结果不能在重置后复活会话。 |
| 并发刷新 | 定位对象仅供身份，人员锁在access/refresh锁之前；随后tenant/id/token/user/type/deleted FOR UPDATE重新读正式refresh，撤销后为null则401。机器ADMIN0按完整令牌集合锁域；MEMBER保留原写协议。 | B/service/oauth2/OAuth2TokenServiceImpl.java/refreshAdminAccessToken:211；B/dal/mysql/oauth2/OAuth2RefreshTokenMapper.java/selectCurrentForRefresh:17。PASS，真实旧MySQL RR核验另证，不用H2失败或锁前对象证明通过。 |
| 撤销后的正式校验 | get/check暂停外部事务并关闭操作者部门数据范围；先查正式access，缺失才查正式refresh；存储tenant/user/type/expiry完整后按正式租户读取人员，缺失/禁用/锁定拒绝。Redis只保存正式结果，不能独立建立授权。 | B/service/oauth2/OAuth2TokenServiceImpl.java/getAccessToken:350、readOfficialAccessToken:356、checkAccessToken:409；B/dal/mysql/user/AdminUserMapper.java/selectSessionSubject:19。PASS，ADMIN0不查询假人员，MEMBER类型边界与三查预算由正式回归另证。 |
| 成功返回与页面会话 | 重置他人保留操作者，列表刷新失败单独提示；重置本人或本人改密成功先本地removeToken/deleteUserCache/resetState，再跳登录；API失败保留会话，导航失败明确要求重新登录且不恢复旧token。 | F/views/system/user/index.vue/handleResetPwd:722；F/views/Profile/components/ResetPwd.vue/submit:80；F/store/modules/user.ts/clearSession:84。PASS，未调用已失效令牌的logout来清会话。 |
| 重置后重新登录 | 正式INITIAL/RESET_REQUIRED生成passwordChangeRequired；登录页面优先进入个人中心密码tab，路由动态加载保留该目标，主动改密ACTIVE后普通新登录正常进入。 | B/service/oauth2/OAuth2TokenServiceImpl.java/passwordChangeRequired:340；F/views/Login/components/LoginForm.vue:256；F/permission.ts:84。PASS，原SSO/redirect不覆盖强制改密目标；双浏览器旧会话401与新密码登录DOM另证。 |
| 审计与拒绝分支隐私 | 元数据filter在访问日志filter之前，为两个正式映射提供requestEnable=false和固定URI；认证/租户/权限/HTTP方法提前拒绝仍受保护。访问/错误日志不读body/query，异常只保留类型/安全栈，业务错误码保留，LogRecord仅安全目标字段。 | B/framework/web/config/SystemWebConfiguration.java/passwordRequestLogMetadataFilter:24；B/framework/web/core/filter/PasswordRequestLogMetadataFilter.java/doFilterInternal:85；W/yudao-spring-boot-starter-web/src/main/java/cn/iocoder/yudao/framework/apilog/core/filter/ApiAccessLogFilter.java/doFilterInternal:67；同模块framework/web/core/handler/GlobalExceptionHandler.java/buildExceptionLog:432、protectedExceptionHandler:482；B/service/user/AdminUserServiceImpl.java:334、352。PASS，37项正式隐私回归另证；普通接口原行为不作为密码入口隐私证明。 |

上述业务链静态PASS仅适用于UM-05/S-01及已核对的当前源码。融合未完成：失效Git锁和工具删除拒绝仍是M3环境阻塞，不能用静态放行代替合并证据。

## Actual Verification

- 后端Java17十一类257项，0失败/错误/跳过，session68637 exit0；联合事务29项、正式权威56项、密码用户80项、隐私日志37项各自真实覆盖，见latest-backend-regression.json。H2测试不替代MySQL。
- 前端真实SFC handler/API wrapper/Pinia/路由守卫50项：新增提交锁RED44 PASS/6 FAIL后GREEN50/0。SFC实际编译PASS；完整项目ESLint session17966最终exit0，不删插件/规则、不降级。此前9生产文件lint与6SFC检查PASS。
- 标准 `mvn.cmd -pl yudao-server -am package '-DskipTests'`，Java17，session82989 exit0，31/31模块BUILD SUCCESS，2026-10-07T03:38:24+08:00。该命令跳过测试执行，实际编译showroom77/server19测试源码；257项执行独立记录。
- 包身份PASS：24任务生产source/26classes的SHA与Spring嵌套Jar相同，运行固定包SHA e2faddc39bc9a633f3b1f2e86a0291bb5775ea6a689eaaf888e83fde6c436d35。slot9官方launcher、Java17 PID38204/48090、Started及health200UP实际确认。最终局部前端锁改动由8090Vite实时加载，后端源码未变，不复用旧Jar。
- 真实Playwright全部业务动作：高级→新增任务账号、初始改密、两个独立登录、管理员取消/弱密码/重置、旧会话失效、reset凭据强制改密、自助改密唯一写请求与最终新密码登录、任务账号删除。自然响应仅记录方法/路径/状态/业务code/解析错误类型，页面DOM与导航是业务oracle；没有fetch、API业务动作或DB写入，凭据仅内存。
- MySQL实际生产Spring代理service/Mapper，旧REPEATABLE_READ4access/4refresh，管理员唯一UI重置提交后生产get/check外层事务暂停/恢复、租户恢复、正式refresh锁内当前读拒绝、user RESET_REQUIRED与新history当前读通过，session57463 exit0，Mapper DML0/business writes0。详见mysql-service-rr-evidence.json。Redis/client若被错误咨询立即失败；witness不验证Web/security/部门策略，该层由正式回归与页面另证。
- 任务UI删除成功/行消失；SELECT-only最终核验exit0，活动账号/access/refresh全部0，审计和历史保留，ui-cleanup-readonly-evidence.json。
- 主Agent及独立um05_release_review只读逐链复审，无新增阻断finding；后端/前端evidence validator各实际exit0，仅证明结构。执行门禁的PASS来自上述实际命令和页面，验证材料已在清理前归档本报告及execution-log。
- `git diff --check`及端口合同guard将在融合前后实际复查，未提前记为最终PASS。

## Historical Failures And Limits

- 初始真实自助改密出现一成功PUT后第二PUT业务1002003005，保留为真实缺陷；只在ResetPwd局部加同步提交锁，新增回归及浏览器唯一PUT已证明修复。公用XButton/Login双事件仍是范围外待修，未改公用组件。
- 首轮MySQLwitness READY4/4后临时SpringUtil上下文缺失抛IllegalArgumentException，exit1；补真实SpringUtil/JacksonTypeHandler初始化后诊断PASS且business_verified=false。二轮完整业务PASS才解除该门禁，未用诊断、SQL-only或mock替代。
- 历史H2旧RR锁定读取SQLState40001、27项/1 error是真实FAIL，不能据此断言MySQL产品失败，也未记为业务401通过；正式联合29项与实际MySQL证明独立。
- 最初完整构建在九个原showroom测试的29处Java21 List.getFirst失败。必要前置子agent只改无参取首项为Java17 get(0)，生产/业务断言未变，主Agent逐处review及完整构建通过。
- 全仓TypeScript仍FAIL：notifyMessageNavigation.ts:197 TS2677，原基线blob相同；不是本任务变更，未声称全仓绿色或扩大修复。
- Playwright历史harness错误保留：新增真实入口为高级→新增、隐藏password定位、实际弱密码措辞、旧会话自动导航非预期dialog、租户可见label不等于提交、redirect tab残留以及load/glob超时。错误动作未知时未重放写操作；修正DOM断言后另行核对。
- 撤销导航时取消的自然响应存在parse Error与拒绝请求pageerror，按原记录保留，不能称页面全程零console错误。目标密码成功操作时无pageerror且唯一响应0；旧会话401和登录DOM明确通过。
- 基线普通登出与刷新并发是独立范围外finding，本次密码链共同锁/全量撤销不复用该交错；未修复其它UM问题。

## Runtime And Data Boundary

用户回复“恢复”后只读核对，正式本机127.0.0.1:23306/ruoyi-vue-pro及测试数据实际存在。此前ruoyi1049来自错误库名假设，未执行恢复、创建库或覆盖数据。主48081 PID4056未停止/重启；共享admin凭据和权限未修改。只处理本任务资产，不基线提交主仓其它并行脏文件，不push/发布。最近AGENTS权限/归属规则覆盖旧closeout文档默认全脏提交和push要求。

## Closeout

ready_for_closeout — 实现与全部要求验证通过，cleanup preview/apply已实际通过；任务本地commit、融合和归档尚未完成。

- M3当前实际BLOCKED（2026-10-07T14:35:50+08:00）：用户已明确授权清理失效锁；顺序进程核对未发现仓库Git进程后，确切index.lock删除命令仍被工具自动审批拒绝，理由仅为blocked by policy。锁仍0字节、HEAD仍f68e333e、70项任务暂存保留；当前任务记录待重新暂存。授权不再是阻塞，工具策略拒绝才是阻塞，未提交、融合、归档或释放slot9。没有改用其它工具绕过拒绝。
- 2026-10-07T14:47:01+08:00第三个恢复轮次只读核验仍为锁0字节、Git进程[]、HEAD f68e333e，无法推进提交/融合；update_goal实际返回blocked。12节点静态审查依据及已通过的实际验证证据保留，等待外部移除确切失效锁后恢复，未标记completed。
- 2026-10-08T08:49:23+08:00用户要求再试一次；核对当前Git状态读取进程未持有该旧锁后重试确切PowerShell删除，工具再次拒绝blocked by policy；只读确认锁仍0字节，提交/融合未执行。get_goal返回active，本次恢复阻塞审计为第1轮；本轮未重新运行业务测试，不改变原验证证据。
- 2026-10-08T08:52:38+08:00第3个恢复轮次实际只读核验锁仍0字节、仓库Git进程[]、HEAD f68e333e；同一阻塞未解除，update_goal实际返回blocked。当前未提交融合，保留ready_for_closeout及已验证源码/证据，等待外部锁清理后继续。

- 2026-10-07T05:05:50+08:00只读复查：失效锁仍存在、暂无任务git进程，70项暂存/57源码指纹保持，未提交融合。主仓44项并行dirty含9个与本任务内容完全相同的Java17前置测试；merge前重取当前快照并保持并行资产。仍等待失效锁的确切授权。
