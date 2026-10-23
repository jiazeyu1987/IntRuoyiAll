# G43 维护令牌到期精度独立审查

状态：NO_OPEN_P1_P2_IN_REVIEWED_AUTH_PRECISION_SCOPE。2026-10-04。最终封存源码、保存日志及当前XML已核对，当前.796缓存/正常DATETIME(0)舍入误拒的修复正确；下文明确保留submillis信息不足的拒绝边界。本次只读正式OAuth写入/缓存/验证链与维护adapter，向实现Owner提供精确风险建议；不改生产/测试，不运行构建/数据库/Redis/服务，不读取或获取实际令牌。不宣称实际维护登记或重新登录已通过。

## 已确认的真实触发与源码原因

Root安全诊断 `g43-auth-cache-time-diagnostic.json` 记录：缓存fractionMillis796，库期限相对该缓存的Asia/Shanghai表示晚204ms；无数据库或缓存写、无token输出。Root已核实际列DATETIME(0)和无TIME_TRUNCATE_FRACTIONAL；本Agent不重复实际查询。

正式 `OAuth2TokenServiceImpl#createOAuth2AccessToken` 使用 `LocalDateTime.now()+validity` 创建到期时间，先insert数据库，再把同一个原对象交 `OAuth2AccessTokenRedisDAO#set`。缓存 `JsonUtils` 的TimestampLocalDateTimeSerializer以systemDefaultZone转epochMillis，Deserializer按相同系统时区还原毫秒LocalDateTime；并不从已保存数据库行回读期限覆盖该原对象。因此，原adapter要求stored.equals(checked)会误拒DB正常舍入后的合法同token。

只读完整链确认：正式checkAccessToken优先缓存、缺缓存再读库，另有refresh兼容路径；维护adapter在调用其前先查精确accessToken真实数据库行，保持撤销后或refresh-only值拒绝。修复无需新增refresh、改全局OAuth行为、修改到期行、删缓存或重建token。

## 舍入策略与授权边界

实现Owner当前策略是保持严格完全相等；不相等时只允许stored为whole second且等于 `checked.plusNanos(500_000_000).withNano(0)`。它表达DATETIME(0)的明确半秒进位：.499下舍、.500/.796上舍，跨秒/跨日自然进位；不是“差一秒以内均可”。缓存miss时checked已经是数据库whole second，原精确相等继续通过。

数据库和缓存的**原始**expiresTime仍分别与同一次 `now` 比较，二者都严格future。舍入相等不能延长已过期缓存或已过期库行；不是先normalize后统一判过期。真正stored秒与应有舍入秒不同、非whole stored且不完全相等时拒绝。

userId/userType/tenant/scopes/currentenabled用户及用户名/昵称精确比对、原上下文恢复、tenant-ignore拒绝、先查access数据库行、禁止create/refresh等边界必须保留。仅比较expires的一处明确定义精度，不改变业务办理身份或给admin新的权限。

## 已给Owner的建议

- 用真实JsonUtils序列化/还原一次原始纳秒期限，证明缓存毫秒与数据库舍入秒匹配，而非只手工设两个相近时间。
- 覆盖向下/向上舍入及跨日、whole秒缓存miss、原始时间完全相等、wrong秒/不同fraction、数据库已过期和缓存已过期两个方向；拒绝时零action。
- 保持token数据库不存在时在正式check/refresh兼容前拒绝，以及全部真实身份守卫。
- 本地ConnectorJ9.7.0编码链包含adjustNanosPrecision；缓存丢失submillis后，在原始微秒舍入的极窄半秒边界可能无法唯一重建库结果。该信息不足时保持精确不匹配拒绝，不增加±1秒、±1毫秒、截断或另一来源兜底。此提醒不是当前.796真实故障的第二个已复现问题，也不扩大本轮全局OAuth/schema修复。

正常开发不要求质量批准；本精度修复不恢复质量前置，不取消真实accessToken/doc_control/update/原件/schema/Gxp事务等门禁。接下来的真实维护运行由Root完成，软件测试不替代实际登记收据。

## 最终独立核对

最终AuthAdapter仅将期限精确比对委托 `sameStoredExpiry`，其它token/identity/scopes/user授权及context finally保持；helper先拒null，再保原精确相等，否则只允许stored nano0与明确half-up秒一致。两个原expiry仍同now各自strict future。没有新增token create、refresh或缓存/数据库写调用。

最终测试已加入实际JsonUtils roundtrip：原nanos796123456序列化/还原为796000000，库结果为下一整秒；真正AuthAdapter action被调用并恢复context。其它测试覆盖.001/.499/.500/.796/.999、跨日、缓存misswhole秒、原fraction完全相等、错误相邻秒、不同存储fraction、双向原期限过期、撤销/refresh-only、tenant/type/scopes/currentuser及tenant-ignore。外部OAuth/User/Mapper在此entry测试隔离，真实页面登录/令牌仍由Root执行。

Driver probe已用本地官方ConnectorJ9.7.0 `TimeUtil.adjustNanosPrecision` 验明：原nanos499999500先micro进位500000000，而cacheMillis仍499，所以此窄边界确实信息不足，当前策略拒绝该alternate nextsecond。这是有意fail-closed的明确限制，不能写成所有合法token精度差均已处理；本轮不扩大容忍值或修改平台OAuth保存。它不影响已诊断.796→nextsecond正向。

保存的7类134次回归全部0fail/error/skip，随后最终Entry12次全部通过并含新增roundtrip；12次与原Entry11重叠，**不累计成146个独立测试**。Kernel还继承22个registration场景，134也不是134个互不重复场景。最终2源资产bytes/SHA、RED/regression/finalEntry/driver4日志与Owner收据一致，当前7个Surefire XML均无失败；Entry XML为12而不是旧11，反映最后一次运行。本Agent未重跑。

| 最终资产 | SHA-256 |
|---|---|
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceAuthAdapter.java | dfe025f80d2b7bd97acf1a3021a306d2c9771d4e1b9b7444c2db5a1f230ba130 |
| IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceEntryTest.java | 014ef61ccd88c51b4b3f366870a74cf2107513caab3a98a8e55298c571e0d0e6 |
| g43-auth-expiry-fingerprints.json | 6357fa2f2c0da4f2530dff94a36f81dba13c334392a42dd52c4b2d286be9ab28 |
| g43-auth-expiry-verification-receipt.json | 8b431d56e7bf3a39978612004607482cf3cd7492abf599e08e59c2499bd5880c |
| 最终Entry XML | f57507c86a26cb523ae44ad60af18162820913fc932bf45090071b68d518b736 |

未发现本轮新增确定P1/P2。Root应打包上述最终Auth源码后进行实际维护，保留真实结果；源码/测试PASS不证明运行中已经加载该修复或登记已成功。报告已按Root指令列入既有任务Cleanup Keep，不删除此前失败证据。
