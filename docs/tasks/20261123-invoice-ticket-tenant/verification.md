# 打印助手匿名验票租户绑定

## 故障与根因

测试发布 2026-11-23-20-22-13 / d9022d50a 实际页面进入助手失败，回调403。维护线程只读日志确认：匿名验票入口 `@TenantIgnore` 没有租户上下文，票据仅存用户、权限和过期时间，随后 `TenantConfigService.scope()` 读取ERP租户配置时抛出缺少租户编号异常。助手将后端业务异常展示为403。不是公开地址、内部健康探测地址或助手离线问题。

调用链：前端现有打印页面签发票据 → 助手 `/auth/callback` → `AuthController.validateInvoiceVoucherPrintTicket` → `ErpInvoiceVoucherPrintKingdeeConfigProvider` → `ErpKingdeeConfigServiceImpl` → `TenantConfigService`。

## 正式修复

- 登录且通过现有财务角色检查后，签发票据将当前已认证请求租户与用户、权限、过期时间一并保存在服务端 Redis。浏览器仍只取得随机票据，不取得ERP配置。
- 匿名验票从服务端记录解析可信租户；不依赖或采用匿名客户端租户头。不增加客户端租户参数，不使用默认租户或全局ERP配置兜底。
- 校验票据格式、期限和权限后，通过现有 `TenantUtils.execute` 的 Runnable 重载，在票据租户内且关闭忽略租户的上下文读取配置。finally恢复原租户及ignore标记；业务异常原样传播，不吞异常。
- Redis `getAndDelete` 原子消费，防止并发重复验票；原票据有效期120秒不变。使用部署 Redis 7 合同支持的 GETDEL。旧三段票据没有可信租户，明确拒绝并要求从页面重新进入取得新票据，不推断租户。无需数据库迁移，助手源码及前端协议不变。

## 验证范围

- AuthControllerTest、InvoiceVoucherPrintAssistantServiceImplTest、InvoiceVoucherPrintAssistantErpConfigBridgeContractTest：55项通过。
- 新增回归覆盖签发租户保存、匿名无租户验票、传入其他租户与ignore标记时仍读取票据租户且恢复上下文、配置异常清理、重复票据，以及旧格式/空租户/负租户/错权限/过期/非法时间拒绝且不读取配置。
- 前端静态检查及实际组件行为测试10项通过；它们不是在线页面验收。
- 下游 TenantConfigServiceTest 4项与 ErpInvoiceVoucherPrintKingdeeConfigProviderTest 3项通过，包括真实H2租户配置隔离与缺租户拒绝。

本轮不操作测试服务器，不重放维护证据中的票据，不读取或提交含票据的原始日志。仅使用维护提供的脱敏异常调用栈确认实际故障。独立的提醒任务 tenant169 缺业务配置问题不在本项修复范围，不修改任何租户配置。

维护须使用新提交重新构建部署，然后用Playwright从原菜单重新进入打印页面取得新票据，确认回调302、助手页面正常显示、票据重复使用被拒绝、无财务权限账号仍不能进入，并核对不同租户使用各自配置。完成前不能声明线上403已关闭。
