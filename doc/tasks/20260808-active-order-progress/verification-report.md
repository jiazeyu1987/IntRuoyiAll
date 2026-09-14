# Verification Report

## Result

blocked

## Passed

- `node tests\e2e\production-leader-active-order-route-labels-static.spec.js` -> PASS，确认前端 API 类型、后端响应字段、控制器投影、活跃订单表格列和列配置均包含生产进度与检验进度。
- `git diff --check -- <active-order-progress task paths>` -> PASS，仅有 Git CRLF 工作区提示，无空白错误。

## Blocked

- 后端 Maven 编译与定向单测未完成。
- 阻塞原因：共享工作区中出现其他默认 `target` 的 MES Maven 测试/编译进程；继续运行本任务 Maven 会并发写同一 `IntRuoyiBackend\yudao-module-mes\target`，存在破坏编译产物的风险。
- 已执行动作：停止本任务启动的 Maven 进程，未终止其他任务或用户进程。

## Pending Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldListActiveOrdersWithProductionAndInspectionProgressByFormalProcessCount,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
