# G43 正式用户目录审计身份

Status: ready_for_closeout — software frozen for Root review/package/runtime. 唯一 AuthAdapter/EntryTest/Maven owner，实际库/Redis/令牌/服务/打包/Git 由 Root 独占，本批不执行。

Root 真 UI 登录的真实只读诊断证明：缓存 checkedInfo 只有 deptId/nickname，当前账号启用、tenant 一致、username/nickname 非空，nickname 匹配。正式 OAuth2TokenServiceImpl.buildUserInfo 创建 ADMIN map 仅 nickname/deptId，不含 username。旧专属维护认证强制缓存 username 导致 CURRENT_ACCOUNT_INVALID。源约束应与正式创建合同相符，不能全局改 OAuth。

- Given 正式 token map 只有 nickname/deptId 且昵称匹配当前同租户启用账号，When 验证，Then 用当前服务器账号目录的实际 username/nickname 构建新的 principal info，保留其余 checked info，不修改原 map。
- Given 缓存有显式 username 但与目录不同、缺/错 nickname、账号停用/ID/tenant 不符，When 验证，Then 拒绝并恢复原上下文，零 action。
- Given token 撤销或时间/identity/type/tenant/scopes 不符，When 验证，Then 原有拒绝不变；前精确 DATETIME0 舍入规则不改变。

先记录有效 RED（正式 map 缺 username 被误拒），再有限 GREEN；最终原7类相关组回归。保持质量开发入口、expiry 两侧future、真实 Gxp事务、普通OAuth/Jackson/schema 不变。旧 G43/expiry 封存材料保持字节。

## 最终验证

RED实际正式map无username被旧认证CURRENT_ACCOUNT_INVALID拒绝，1case/expected1error。GREEN原7类组合137次执行全部通过，无失败/错误/跳过，16:08:19完成；Kernel继承22登记场景，执行总数含重复。

仅AuthAdapter/EntryTest两源码变化；前G43六源码、原41、OWNER4、策略、Runner无漂移，旧expiry和封存材料未覆盖。当前目录username/nickname组成新HashMap（允许正式deptId为null），来源map可为immutableMap.of且未改动。显式username键null/blank/wrong拒绝；missing/wrongnickname、disabled、目录id/tenant不符均拒绝/恢复上下文。精确expiry函数和双future检查不变。

正式OAuth constructor nickname/deptId及Gxp consumer principal.info username有源级证据；Entry断言目录真实username，既有Kernel真实Gxp事件测试保留通过。没有伪造字段或默认SYSTEM_ACTOR；也未新增角色授权。OAuth/用户目录读取是测试隔离端口，未宣称实际登录/维护激活或E2E通过。

Maven已停，Root独占后续Review/打包/真实运行；本Agent无实际DB/Redis/Token/service/package/Git操作。
