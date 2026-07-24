# 任务：NAS 转移大任务状态性能与会话恢复

## 任务目标

- 修复 NAS 转移任务越到后期状态查询越慢的问题。
- 确保长时间转移期间前端登录过期或页面刷新后，用户重新进入 NAS 管理仍可恢复查看当前任务。
- 保持后台任务与前端会话解耦，不因前端超时导致已分析、已入队或已导入的数据丢失。

## BDD 场景

- BDD: 大任务状态查询不扫全量明细 -> Given NAS 转移任务已有大量任务项 / When 前端轮询任务状态 / Then 后端用聚合统计和失败明细查询返回状态，不加载全部任务项到内存。
- BDD: 前端会话恢复任务上下文 -> Given 用户已创建 NAS 转移任务且登录会话过期 / When 用户重新登录并打开 NAS 管理 / Then 页面恢复最近任务编号并继续轮询最新状态。

## 里程碑

- [x] M1：记录任务文档和失败复现证据。
- [x] M2：后端状态统计改为聚合查询，保留失败明细。
- [x] M3：前端保存并恢复最近 NAS 转移任务。
- [x] M4：完成目标测试和静态验证。
- [x] M5：收尾记录和提交。

## 预期验证

- RED/GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#nasTransferTaskStateShouldUseAggregatedSummaryWithoutLoadingAllItems" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- GREEN: 前端静态检查验证 NAS 管理页持久化任务 ID 并在加载时恢复轮询。

## 当前状态

completed
