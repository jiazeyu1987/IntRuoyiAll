# 验证报告

## 结论

PASS：一线 PQC 的工序列表现已展示活跃订单正式发布路线快照中的全部工序，不再按 QA 规程、待执行任务或当前进行状态缩减。

## 行为证据

- 订单：`881MO090889`。
- 产品：球囊扩张压力泵。
- 页面：`http://127.0.0.1:8081/mes/pro/feedback/edhr-batch-pqc-fill`。
- 工序数量：14。
- 工序：粗洗、精洗、清洗、清洁、组装Ⅰ、光固Ⅰ、硅化Ⅰ、硅化Ⅱ、组装Ⅱ、检测、光固Ⅱ、单包装、中包装、大包装。
- 无待检任务的工序仍可见，但不返回伪造任务、检验项或任务选项；提交仍要求正式待检任务。

## 自动化验证

- RED：目标测试在旧实现下 3/3 失败，路线 `[4001,4002]` 被缩减为单一 QA 工序。
- GREEN：隔离编译后运行 `MesFrontlinePqcContextServiceTest` 与 `MesProcessPoolActiveOrderMapperTest`，38/38 PASS。
- 真实 E2E：`frontline-pqc-all-published-processes.e2e.mjs` PASS；UI 与接口 14 项逐项一致，接口业务码 0。
- 浏览器健康：`pqcSubmitRequests=[]`、`targetNetworkFailures=[]`、`pageErrors=[]`、`consoleErrors=[]`。

## 运行态

- 前端：`8081` HTTP 200。
- 本任务运行验证时：后端 `48081` health UP，PID `46904`，运行 Jar 为 `backend-runtime-control-20260809-pqc-all-published-processes.jar`，SHA256=`11C1C03339D853C31C76DE7E881B9DA1B876C535F0AE385BAE2BD2B1FF59C27E`；内嵌 `yudao-module-mes` 使用 stored nested entry，补丁前后模块哈希已核对。
- 最终收尾复核时：并发任务已将后端重启为 `backend-runtime-control-20260809-123324-batch-record-codex-response.jar`（PID `37220`）；health 仍为 UP，且其中 `MesFrontlinePqcContextServiceImpl.class` SHA256=`AC613457A913071217AF6BBBB7DDE2960AAC3E4FCA8F4357B6323DE43B28DB92`，与本任务已验证编译产物完全一致。

## 已知环境阻塞

标准 Maven `testCompile` 被当前工作区其它任务缺失的 19 个 `MesTeamLeaderActiveOrderRelease*` 类型阻塞。本任务源码主编译成功，并通过仓库既有隔离参数完成 38 项相关回归；未修改或绕过这些无关任务文件。

## 非目标信号

页面导航期间 `/mes/pro/work-order/page` 的一个只读请求被浏览器取消；该请求不属于当前一线 PQC 目标链路，目标接口和 UI 均成功。页面自动执行一次非事务性的 `pqc/switch-employee` 上下文解析 POST，未触发 `pqc/submit`。
