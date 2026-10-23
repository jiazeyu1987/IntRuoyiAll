# G43 Auth 到期时间精度

Status: ready_for_closeout — source frozen, Root review/package/runtime remain. 本批由 legacy_occupancy_core 独占 AuthAdapter/EntryTest 与 Maven；无实际数据库、Redis、令牌、服务、打包、Git 动作。此前 G43 封存材料保持原字节。

Root 真实只读诊断：库 expires_time 为 DATETIME(0)，缓存毫秒部分 796，库秒级结果比缓存晚 204ms；当前 SQL mode 无 TIME_TRUNCATE_FRACTIONAL。正式 MySQL schema 为 datetime 无显式 precision（默认 0）；OAuth 创建 LocalDateTime.now()+validity 后原对象写缓存，正式序列化以系统时区 epoch milliseconds 保存，缓存与库表示相同到期时刻的不同精度。

- Given 同一个真实存储 access token，所有身份/租户/类型/scopes 完全一致且库和缓存都未到期，When 缓存为毫秒而库为正常 DATETIME(0) 四舍五入秒，Then 允许该精确量化结果，既有纳秒完全相等也继续允许。
- Given 库到期秒不等于缓存应有舍入秒，或库有非零分数且与缓存不完全相等，When 验证，Then 拒绝，不使用秒差容忍、截断或 refresh fallback。
- Given 真实存储 token 已撤销、任一到期、跨租户/类型/scopes/账号不符，When 验证，Then 拒绝且零 action，原安全上下文恢复。

验证：新增正常舍入正向行为先有效 RED，再精确秒量化 GREEN；覆盖 .499 下舍、.500/.796 上舍及跨日、错误秒和非 whole precision 拒绝；保持既有撤销/跨租户/到期测试并扩双侧未到期，最终运行原 G43 7 类相关组。只修改专属 AuthAdapter 与 EntryTest，不改全局 OAuth/Jackson/SQL/schema。

官方依据：[MySQL 8.0 Fractional Seconds in Time Values](https://dev.mysql.com/doc/refman/8.0/en/fractional-seconds.html)：省略 precision 默认 0；较低精度正常舍入，TIME_TRUNCATE_FRACTIONAL 才会截断。本批不使用截断逻辑。

## 结果与边界

有效 RED：1 个真实 Adapter 正向 case 被旧纳秒 equality 拒绝，错误 VERIFIED_TOKEN_IDENTITY_INVALID。修复后原7类相关回归134次执行全部通过（包括继承重复22场景）；最后新增实际 JsonUtils DO serialization/deserialization roundtrip 后 Entry12 次全部通过，生产未再修改。两批均零失败/错误/跳过；不将134+12相加冒充独立场景。

只改 AuthAdapter/EntryTest 两文件。先前 G43 六源码、原41、OWNER四源码、Policy/Runner字节不变；前封存材料均未覆写。

实际本地 Connector/J9.7.0 TimeUtil 离线 probe：499000000ns→micro499000000→DB原秒；499999499ns→micro499999000→原秒；499999500ns→micro500000000→下一秒；500000000ns 和796123456ns→下一秒。cached epochMillis 丢失 submillis，因此极窄499999500..499999999ns 原始时刻可能变成499ms缓存+下一秒DB，无法从缓存单独唯一反推。本实现明确拒绝这个 alternate nextsecond，不增加±1秒/双候选/截断容差。正常Root已证实796ms+DB晚204ms可以精确匹配。

cache miss已whole秒保留exact equality；非zero库分数只有全值相等能通过。两侧到期测试使用确定性 now 隔离，明确拒绝“缓存已到期但舍入库时间未到”及反向场景；撤销/跨tenant/类型/scopes/current账号/context恢复仍有既有行为回归。

无实际数据库、Redis、Token、服务、打包或Git操作。本批只凭Root真实只读诊断+正式schema/源码+H2和隔离OAuth读取端口验证；不声明真实登录/维护激活或E2E PASS。
