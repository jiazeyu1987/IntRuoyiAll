# E2E前置期间原生BPM待办聚合异常

## Current Status
blocked — 业务修复及真实UI验证通过；工程Git/清理随主协调任务保留。

## 目标与设计约束
R1115-11：真实前端多次触发APPROVAL_ADAPTER_PAGE_INCONSISTENT，总数23/25但首屏列表为空。BpmNativeApprovalTaskProvider.pageTodo在分页后过滤DCC三条正式流程，仅减去当前页过滤数，无法计算后续页总数。Root唯一负责此直接前置问题，保留聚合一致性校验、DCC正式任务入口和权限，不改数据库或已有任务。不扩到已办/抄送/其它业务流程。

## BDD与验证
Given 同租户有多于一页的DCC待办及两条原生BPM待办；When 文控已由专用provider接管，原生provider查其分页；Then count与listPage在同一查询排除三个DCC流程键，两条原生待办首屏可见且总数2。默认普通BPM查询不携带排除条件时仍保留原行为。先真实私有Flowable/H2查询RED，再最小VO/查询/provider改动GREEN及既有provider/聚合回归，Root统一运行。无目标空页兜底或吞异常。

## 里程碑
1. 实际日志和直接分页/过滤源码归因已完成。
2. 真实引擎测试RED已复现总数7而预期2；GREEN及provider/聚合回归63项PASS。
3. 统一包部署PID2812，health UP；fresh管理员通过正式待办菜单看到首屏20行、总数68且无错误toast，0业务写入。证据bpm-todo-ui-after-fix.json/png。业务修复验证完成，Git/任务清理收口随总协调任务。
