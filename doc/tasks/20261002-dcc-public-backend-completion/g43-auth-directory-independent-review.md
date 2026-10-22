# G43 正式 OAuth 用户信息到审计身份的独立审查

状态：NO_OPEN_P1_P2_IN_FROZEN_DIRECTORY_MAPPING_SCOPE。2026-10-04。最终源码、测试、日志和7个XML已核对，无新确定P1/P2；源码修正了正式userinfo缺username的合同误拒，未宣称实际维护登记PASS。本次只读正式OAuth、API、LoginUser与Gxp审计身份来源，已给实现Owner具体映射建议；不改生产/测试、不运行Maven、数据库、Redis、服务，不获取或使用实际token。

## 确认的第二个真实合同问题

Root `g43-current-account-diagnostic.json` 记录真实登录后账号启用/同tenant、用户名和昵称非空、缓存字段只有deptId/nickname、昵称相等，sidecar scope/evidence/reservation仍0，诊断无写/secret输出。原专属维护adapter强制 `current.username == checked.userInfo['username']`，缺键因此拒绝。

正式 `OAuth2TokenServiceImpl#buildUserInfo` 对ADMIN用户的map只放 `LoginUser.INFO_KEY_NICKNAME` 和 `INFO_KEY_DEPT_ID`；`OAuth2TokenApiImpl#checkAccessToken` 以BeanUtils把已有DO转换DTO，不另加username。`LoginUser` 正式INFO常量也是nickname/deptId。原维护测试人为夹具带了username，没有覆盖真实全局userinfo合同。这不是正式username安全控制被删除，而是维护adapter误要求了正式token未提供的字段。

Gxp `GxpAuditServiceImpl#resolveActorUsername` 从principal.info的username读取，缺失默认SYSTEM_ACTOR；真实注册service又要求principal用户名与当前用户目录一致。因此修复应在真实同id/tenant/ADMIN/expiry/scopes已验证之后，使用当前正式启用账号目录的真实username补齐**新的principal info**，不伪造账号、不改变OAuth缓存。

## 有限正确性边界与已给Owner建议

1. 保留当前目录getUser所得ID/tenant/enabled、username/nickname非空，以及缓存nickname与目录nickname精确相等；缓存有明确username字段时仍要求等于当前目录，null/blank值不能绕过。
2. `new HashMap/LinkedHashMap(checked.info)`复制后补actualusername/currentnickname；不可修改原checked/cachedmap，不用Map.copyOf误拒合法null deptId。正式buildUserInfo会用StrUtil.toStringOrNull表示deptId，null并不授权或伪造部门。
3. principal info只是审计身份补全，role/doc_control/update仍由当前正式PermissionApi校验；不从checkedmap加权限，不改globalOAuth、Jackson、schema或账号授权。
4. 前次精确DATETIME(0)expiry匹配、两个原始期限future、撤销数据库access行优先检查、type/tenant/scopes精确匹配、上下文finally恢复及禁止create/refresh保持不动。
5. 正向用正式immutable `Map.of(nickname,deptId)`（无username）实际adapter行为证明principal username来自当前目录、原map不变；错误可选username/昵称、currentID/tenant/停用零action。已有真实Gxp kernel查actor_username链可复核，不把只构造principal当实际维护登记成功。

该修复仍限定被授权本机维护，不请求质量批准、不替代真实前端业务动作；待Root打包执行后的真实收据判断运行结果。

## 最终实现与验证核对

最终AuthAdapter保留当前ID/tenant/enabled/非空用户名昵称，缓存nickname精确匹配；缓存 `containsKey("username")` 时值必须等于当前真实username，错误/null/blank均拒绝。正常正式map无该键时允许。随后 `new HashMap<>(checked.getUserInfo())` 创建独立principalInfo，加入当前directory username/nickname，再用于真实principal；checked原map不被改变，nullable部门字段无需Map.copyOf也可保留。权限、角色、到期、scopes、撤销、context finally未放宽，原expiry helper文本不变。

新增正向实际adapter测试用不可变nickname/deptId map，断言principal.username来自当前account、nickname/deptId保留、principal map不是cached map、checked map本身与对象引用不变，结束恢复原security context。新增负向覆盖explicit wrong/null/blank username、wrong/missing nickname、当前account停用/错ID/tenant；拒绝时零action、源map不变且原context/tenant恢复。

既有真实Spring/H2/Gxp kernel继续回归并查真实actor_username。其原夹具仍带cached username，因此不能声称新增缺username结构已单独走完整Gxp kernel或实际登录；本次新的principal行为测试，加正式 `GxpAuditServiceImpl#resolveActorUsername` 源码消费链，证明需要且正确完成审计身份映射。实际登记还需Root运行。

最终7类137次执行、0fail/error/skip、BUILD SUCCESS；Entry14次，Kernel继承22个registration场景，不累计前次134或Entry12为新的独立案例。2源资产bytes/SHA、RED/GREEN日志SHA和全部7当前XML均相符；本Agent未重跑。初次只读验证脚本误使用receipt小写red键（正式为RED），KeyError无产品/运行效果，纠正字段后验证通过。

| 封存资产 | SHA-256 |
|---|---|
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceAuthAdapter.java | 84d9b8a1cea0fdf03b4a703b23affafe8b3610f58e96d9a2770f40449e365bf5 |
| IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceEntryTest.java | fd387a4c69730c58ab07747414f23ad7fb8440bdff14153b55567386aa8f511b |
| g43-auth-directory-fingerprints.json | 462cb0766e13d374e6fff1f7114df221fcf8b6994575aad1239fe03c9ef842df |
| g43-auth-directory-verification-receipt.json | fb2f81842f9563ea4e72b5a6d21462769b84e74f8a74481ef38de91972ff77e0 |
| 最终Entry14 XML | 30046abdf51d01d7ed3f7579b796fc11b22f0d92c839b2a36f195222cdc357db |

本报告覆盖新userinfo映射版Auth；前 `g43-auth-independent-review.md` 的expiry结论及窄精度限制仍保留，但其中旧Auth文件指纹已被此版本替代，不把旧文件哈希当当前包依据。未发现应阻止Root按新同源包继续真实运行的确定问题。
