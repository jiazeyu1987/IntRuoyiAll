# G44 正式页面审计人员身份

Status: ready_for_closeout — source frozen for Root review/package/runtime. System GxpAuditServiceImpl / existing Test 初始唯一生产/测试归属；legacy_occupancy_core 独占 Maven。无实际库/Redis/Token/服务/打包/Git动作，Root管理真实运行。

真实页面六个项目/产品事件actor_id=1正确、display匹配，但actor_username错误为SYSTEM_ACTOR。正式OAuth管理员userinfo仅nickname/deptId，普通GxpAuditService缺username时默认SYSTEM_ACTOR，维护入口专属修复不能覆盖页面。

- Given 正式已认证ADMIN LoginUser信息仅nickname/deptId，When 追加真实人工作审计，Then 当前同id/tenant启用服务器账号目录提供真实username/nickname，不从缓存猜用户名、不修改LoginUser/cache；事件hash包含实际identity。
- Given 人工作者目录缺失/停用/id或tenant不符、上下文tenant不符或ignore，When append（含历史重放），Then ACTOR_MISMATCH且零新事件/sequence；历史事件不修改。
- Given DccWorkflowFileStateAudit明确systemAction临时清空security并保留tenant，或精确声明SYSTEM_ACTOR(-1)，When append，Then 保留明确系统事件身份，不查询或冒充人账户。

最小实现：复用正式AdminUserApi当前用户读取端口，仅审计边界补当前身份，不改变全局OAuth、角色、策略或历史。系统case不将带残缺LoginUser伪装系统。BDD有效RED后System kernel GREEN，再必要DCC真实Gxp同事务组合；不扩大平台/审批功能。

## 结果

有效RED为真实H2事件username预期qa.admin却SYSTEM_ACTOR（1assertionfailure/0error）。System GREEN15/2全通过。初次组合System15/DCC169中仅旧Ledger fixture当前actor9无目录返回造成22errors，日志保留；只该测试显式启用tenant1 applicant9、改正式nickname/deptId LoginInfo并断言真实event.actorUsername，未绕Guard。最终184/11（System15+DCC169）全通过，零failure/error/skipped，18:50:33；Kernel继承22登记场景，重复执行不当独立场景累加。

仅生产System GxpAuditServiceImpl一文件，测试System kernel和DCC Ledger两个文件。AdminUserApiImpl正式当前读取不改，普通OAuth不改，DCC Auth/G43六源码、原41、OWNER全部资产和策略原文零漂移。

人工作者使用正式AdminUserApi当前目录的username/nickname，其id/tenant/启用状态及当前安全context验证在历史幂等回读前执行，且在任何事件/sequence写前。缓存username/nickname不是账户身份权威，函数只返回独立actor投影、不修改原LoginUserInfo/cache，也不设置角色。缺失/停用/目录id或tenant不符/contexttenant或ignore/错type失败零写入；历史重放只读原event，不用当前昵称覆盖旧事件。

明确系统动作仍支持DccWorkflowFileStateAudit临时清空security保留tenant的既有链；精确声明-1/ADMIN/SYSTEM_ACTOR双名字也保留。残缺LoginUser不会因id缺失降为系统。所有这类事件源仍相同事务Gxp账本，不制造电子签名。

既有Root实际六条旧actorusername SYSTEM_ACTOR事件不可被本修复改写。本Agent没有读取/写实际库、Redis、Token、启停服务、打包、Git或浏览器；后续Root实际部署/真实新动作审计核验另行管理。
