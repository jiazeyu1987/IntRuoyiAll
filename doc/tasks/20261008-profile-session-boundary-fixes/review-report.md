# 个人中心会话边界独立静态复审

## 范围与结论

独立reviewer：profile_static_sources_recheck。实现worker：profile_session_fix_worker。主Agent核验日志及归档证据。基线5b77574d94096efda06160a88320ee4139c86b5a，分支codex/profile-session-boundaries。

前端全链、后端正式合同与定向证据PASS，未发现阻止本次修复集成的新增缺陷。全量类型检查仍BLOCKED（3项原有错误），真实E2E与数据落库未验。结论仅适用于授权的两项会话缺陷，不将原有其他会话/聚合问题扩展为本次范围。

## 前端业务节点

| 节点 | 代码依据（前端根IntRuoyiFronted/src） | 判断 |
| --- | --- | --- |
| 资料保存入口及正式回读 | views/Profile/components/BasicInfo.vue:136更新、137回读、139昵称同步；api/system/user/profile.ts:39/44正式GET/PUT | 保存顺序与请求合同保留 |
| 续期缓存及租户上下文 | utils/auth.ts:34新登录清旧身份；40续期仅写access/refresh；config/axios/service.ts:203-218先校验业务码及完整令牌再持久化 | PUT/GET续期不删USER/ROLE_ROUTERS/VisitTenantId；store同步不会因续期丢USER |
| 改密成功退出 | ResetPwd.vue:108调用后端、117清会话、130导航；store/modules/user.ts:84-87先移除token再清缓存/重置 | 未改变后端成功才退出的时序 |
| clearSession同步监听 | ProfileWorkbench.vue:342认证守卫；373-379所有load入口先核对token、正用户ID及disposed；565-574同步scope watch | permissions/roles/user重置中间状态均不发匿名page/count |
| 页面和角标迟到响应 | ProfileWorkbench.vue:344-349保留代际/scope/query/disposed，增加认证；402成功角标依赖页面提交；411-421错误/finally角标同样通过页面守卫 | clearSession时匿名load使旧page及badge epoch失效；退出、换权限、卸载后旧结果不覆盖当前上下文 |
| 无refresh的401及登录页 | service.ts:196-201令牌前置检查在isRefreshToken前；handleAuthorized登录页显式Promise.reject | 匿名请求失败明确，后续新登录续期不会被空刷新队列锁死 |
| 并发刷新 | service.ts原requestList回放、finally清队列与锁保持；续期入口保留访问租户 | 并发成功共享单次刷新并全部结束；业务失败均拒绝，后续可恢复。未修改既有失败回放策略 |
| 新登录身份重建 | LoginForm、MobileForm、RegisterForm、SocialLogin各handler仍使用setToken；permission.ts:77正式加载用户信息 | 只有Axios续期使用setRefreshedToken，身份切换仍清旧缓存，不混用入口 |

## 证据独立核对

- reviewer读取新增8项测试的实际代码：执行真实Vue/Pinia/Axios和生产模块/组件脚本，外部传输/缓存/UI边界受控；并非只验证字符串。读取session-green.log确认8/8。
- 读取session-regression.log：原资料/改密/会话55/55；静态组4/4。
- 读取session-eslint.exit.txt=0、空诊断日志；lint规则未放宽。
- baseline/fixed类型日志完整SHA256相同，均为E44F7F985C0E9CB069CA212FAA57573BAC5FEFAF73B5702AE8266F948443FDF5，均exit2；三项原诊断，无新增。此证据只能证明基线无增量，不能记为全量类型通过。
- reviewer只读，不执行测试、构建、E2E、API/DB或Git写操作；主Agent记录已执行验证的实际结果。具体命令与初次失败/修正见verification-report.md及execution-log.md。

## 后端正式节点

以下路径在IntRuoyiBackend内，行号对应本次基线；本次未改后端。

| 节点 | 正式代码依据 | 判断 |
| --- | --- | --- |
| 登录身份与访问前置 | yudao-framework/yudao-spring-boot-starter-security/src/main/java/cn/iocoder/yudao/framework/security/core/filter/TokenAuthenticationFilter.java:74-90校验access及ADMIN；同层core/util/SecurityFrameworkUtils.java:89/122提供登录ID；config/YudaoWebSecurityConfigurerAdapter.java:148默认authenticated | 资料与改密使用安全上下文用户，不能由前端任意指定人员 |
| 资料PUT与GET | yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/user/UserProfileController.java:51-69正式GET/PUT、@Valid及getLoginUserId；service/user/AdminUserServiceImpl.java:304-316唯一性校验、按正式ID更新非null字段；545-546 selectById回读；dal/mysql/user/AdminUserMapper.java继承BaseMapperX | 地址、载荷、VO格式与租户拦截未变，前端仍正式回读后同步昵称 |
| 本人改密校验与落库 | 同Controller:73-78 oldPassword/newPassword非空、登录ID、日志脱敏；AdminUserServiceImpl.java:319-335事务内正式账号锁、旧密码、强度及历史重复校验，历史825-835，新密码编码、更新时间、ACTIVE、updateById | 前端成功才退出；未绕过正式密码策略或历史 |
| 锁及全部会话撤销 | AdminUserServiceImpl.java:439-455可写事务/tenant/正ID与主体锁；AdminUserMapper.java:25 selectPermissionSubjectForUpdate；AdminUserPasswordHistoryMapper.java:15-22按tenant/user/deleted当前读取最新历史；service/oauth2/OAuth2TokenServiceImpl.java:439-479同tenant锁、当前读并逐项删除access/refresh、核对行数、删除Redis、异常传播 | 未把后端会话撤销替换为前端假注销；正式事务及租户合同保留 |
| 续期入口及正式身份 | controller/admin/auth/AuthController.java:134-139 PermitAll仅允许持required refreshToken续期；service/auth/AdminAuthServiceImpl.java:247-249固定客户端；OAuth2TokenServiceImpl.java:211-217正式refresh/ADMIN/正ID，221-285凭证tenant事务与账号/access/refresh锁内校验，313-320禁用/锁定；501-520继承正式身份/scopes/tenant生成access并写DB/Redis；AdminAuthServiceImpl.java:240-243正式返回token | 前端继续核对业务码及完整两项令牌；仅改变同会话本地缓存策略，未弱化后台刷新条件 |

后端补充结论PASS仅为代码调用、身份、落库、撤销及刷新合同吻合；未执行API、DB或真实账号验证，不能据此声称真实资料更新、改密或撤销已运行通过。

## 收尾限制

2026-10-08用户已授权提交本次修复、推送修复分支并合入本地int_main。主工作区并行资产保持原状；通过的静态与受控测试不得冒充真实E2E、数据源快照或容量验证。
