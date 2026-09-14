# 20260812 Frontline PQC DCC QA DF02

## Task Goal

实现 DF02 - Active-order route snapshot resolver：只根据用户选择的 activeOrderId 读取当前租户内有效活跃订单的生产路线快照与 QA 锁定快照，拒绝不存在、已移除、跨租户非法引用和快照缺失，不接受客户端路线覆盖，不写数据库。

## Milestones

- M1 规则与合同确认：已读取后端、数据库、PowerShell 编码、任务收尾、监督计划、DF02 设计和共享接口/数据合同。
- M2 BDD/RED：先记录 Given/When/Then，再新增专属 resolver 测试并运行目标 Maven 得到行为 RED。
- M3 GREEN：新增最小 ActiveOrderSnapshotResolver 正式实现，只读解析有效 active order 快照。
- M4 Regression：复跑 DF02 指定 Maven 命令，检查无 fallback、无 route override、无写库路径、无越权文件。
- M5 Closeout：记录验证报告和最终状态；不提交、不合并、不删除 worktree、不启动服务。

## Expected Verification

- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderSnapshotResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- 源码检查：resolver 只读取 active-order 快照字段，不写数据库，不重新选择路线，不按 product 推算，不区分跨租户记录是否存在。

## Applicable Gates

- PQC 待检准入和工序选择分离：active order 是唯一选择身份，PENDING task 不参与本 resolver 准入。
- MES PQC 项目级检验快照：读取 activeOrderId 上的 route/QA 快照，不用当前产品路线、formBindings 或客户端 route 参数补齐。
- PowerShell Maven -D 参数引号门禁：所有 -D... 参数整体加双引号，并保留 -pl yudao-module-mes -am。
- Maven 目标目录异常门禁：若编译/testCompile 未到达 Surefire 且指向 target 损坏或并发 Maven，记录环境 blocker，不冒充业务 RED/GREEN。

## Current Status

ready_for_closeout

- Implementation and required DF02 verification passed.
- Cleanup apply, commit, merge, push, worktree deletion, deployment, service start, and shared-data changes are intentionally not performed in this worker scope.

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；通过独立 resolver 固化 activeOrderId -> 订单快照的服务端只读合同。
- 是否存在临时补丁或绕过：否。
