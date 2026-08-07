# Execution Log

## User Intent

- 一线 PQC 可选择的订单应为所有生产组长 ACTIVE 订单的集合。
- 订单选择弹框应支持手动输入订单号来快速选择。

## BDD

- BDD: 展示所有生产组长活跃订单 -> Given 多个生产组长各自维护 ACTIVE 订单；When 一线 PQC 打开订单选择弹框；Then 页面从无当前组长过滤的 PQC 活跃订单接口加载并展示去重后的全部候选。
- BDD: 输入订单号过滤候选 -> Given 订单弹框已加载全部 ACTIVE 订单；When PQC 输入订单号的全部或部分字符；Then 页面仅显示订单号包含该输入的正式候选，清空输入后恢复全部候选。
- BDD: 回车快速选择 -> Given 输入订单号后存在订单号完全匹配或唯一过滤结果；When PQC 按回车；Then 页面选择该订单并继续既有工序加载链路；多条模糊结果或零结果不得猜测选择。
- BDD: 接口失败不得降级 -> Given 活跃订单接口失败或正式候选为空；When 页面初始化；Then 保留既有明确错误，不使用 mock、当前组长局部数据或默认订单冒充成功。

## Command Intent And Evidence

- 2026-08-07：读取项目触发规则、技能契约、经验索引、现有一线 PQC 组件、API 和后端服务。
- 2026-08-07：确认 `MesFrontlinePqcContextServiceImpl.listActiveOrders()` 调用 `MesProcessPoolActiveOrderMapper.selectActiveList()`；该 mapper 仅按 `activeStatus=ACTIVE` 查询，没有 `leaderUserId` 或登录人过滤，并在服务层按 `workOrderId + routeId` 去重，已是所有生产组长 ACTIVE 订单的统一集合。
- 2026-08-07：确认现有 `MesFrontlinePqcContextServiceTest.shouldListActiveOrdersFromUnifiedActiveOrderAuthority` 锁定使用全局 active-order mapper，且禁止回退工序池活跃列表。
- 2026-08-07：确认相邻在途任务 `20260807-frontline-pqc-latest-active-version` 会修改 PQC 路线版本链路；本任务不修改其后端服务或测试。
- RED: `node tests\\e2e\\mes-frontline-pqc-all-active-orders-search-static.spec.cjs` -> FAIL, 一线 PQC 订单弹框缺少订单号搜索输入、正式候选过滤和确定性回车选择逻辑。
- 2026-08-07：Playwright CLI 已按技能要求完成 npx 前置和真实登录页快照，但 Windows 会话在登录后未保持并出现 CLI 运行时断言；未将该结果记为 E2E 通过，改用项目既有 Playwright 库运行任务自有只读脚本，且脚本只从本机环境读取凭据、不输出或保存凭据。

## Milestone Status

- M1：completed。
- M2：in_progress。
- M3：pending。
- M4：pending。
- M5：pending。

## Blockers

- 当前无实现 blocker。
