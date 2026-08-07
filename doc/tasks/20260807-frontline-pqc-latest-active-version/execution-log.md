# Execution Log

## User Intent

- 用户复现点击“一线 PQC”时报错：`当前工序缺少已发布 QA 检验规程，activeOrderId=30，routeProcessId=980645，processId=922985`。
- 用户确认业务规则：待执行 PQC 每次定位最新 ACTIVE 路线版本和最新已发布规程；旧 PENDING 任务需要同步刷新；已提交任务保留历史冻结版本。

## BDD

- BDD: 待执行 PQC 使用最新 ACTIVE 路线 -> Given 活跃订单仍记录旧路线版本且旧 PENDING 任务引用旧 routeProcess，当前路线已有更新的唯一 ACTIVE 版本及匹配已发布 QA 规程；When 一线 PQC 加载该活跃订单工序；Then 系统按最新 ACTIVE 路线和当前 routeProcess 生成或复用同身份 PENDING 任务并返回，不得组合旧 routeVersion 与新 routeProcess。
- BDD: 旧 PENDING 任务不得跨版本复用 -> Given 活跃订单存在旧路线版本的 PENDING PQC 任务；When 最新 ACTIVE 路线版本发生变化；Then 旧 PENDING 任务被明确失效并按最新路线与规程重建，不能作为当前待执行任务返回。
- BDD: 已提交 PQC 保留冻结版本 -> Given 旧路线版本的 PQC 任务已经 SUBMITTED；When 路线发布新 ACTIVE 版本并加载一线 PQC；Then 已提交任务的 routeVersionId、routeProcessId、regulationVersionId 和历史明细保持不变。
- BDD: 最新版本缺正式规程时失败 -> Given 当前唯一 ACTIVE 路线版本的工序缺少匹配 PUBLISHED QA 规程或正式检验项目；When 一线 PQC 加载工序；Then 后端明确失败，不得回退旧规程、旧任务或空成功。

## Command Intent And Evidence

- 2026-08-07：只读数据库核对确认 `activeOrderId=30` 绑定旧 `routeVersionId=448`，当前 ACTIVE 路线版本为 `490`；当前工序 `980645`，旧任务和唯一已发布规程仍引用旧工序 `928609`；活跃订单工序快照数量为 0。
- 2026-08-07：代码核对确认 `MesFrontlinePqcContextServiceImpl.listProcessesByActiveOrder` 从当前 `mes_pro_route_process` 取工序，却用 `activeOrder.routeVersionId` 查询规程，形成混合身份。
- 2026-08-07：任务开始时 Git 为 `int_main...origin/int_main [ahead 2]`；存在进入本任务前的一个测试文件改动和四个无关任务目录。按项目规则先建立独立脏工作区基线提交，本任务文件不混入该基线。

## Milestone Status

- M1：in_progress。
- M2：pending。
- M3：pending。
- M4：pending。
- M5：pending。

## Blockers

- 当前无业务设计 blocker。

