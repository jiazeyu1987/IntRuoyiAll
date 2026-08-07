# 验证报告

## 结论

无排产工单的已确认生产工单已可通过产品唯一正式路线绑定和唯一 ACTIVE 发布快照进入生产组长活跃订单；单排产兼容、多排产阻塞和正式来源缺失 fail-fast 契约保持有效。目标范围验证通过。

## 自动化验证

- 服务/控制器聚焦回归：37 项通过，Failures/Errors/Skipped 均为 0。
- 前端静态合同：活跃订单池、生产组长工作台和班组长工序池 3 个脚本均通过。
- Backend API evidence validator：通过。
- Database schema evidence validator：通过。
- 完整 MES `testCompile`：被无关基线 `MesTeamEmployeeBindingServiceTest` 引用不存在的 `MesTeamEmployeeBindingService` 阻塞；目标测试已在 exact-HEAD worktree 单独编译执行。

## 真实路径

- 隔离运行槽：`int_main slot=8`，前端 `http://127.0.0.1:8089`，后端 `http://127.0.0.1:48089`；两端启动检查通过。
- 身份与数据：测试租户 `测试租户`、生产组长 `acd04lead1`、任务工单 `AONS-20260807-WO/980027`。
- Playwright：候选返回 `eligible=true`；页面发送仅含 `workOrderId=980027` 的新增请求，业务码为 `0`；页面随后成功移出订单。
- 数据库只读核验：活跃订单 `40`，工序快照 1 条，数量系数 1，计划数量 100，PQC 任务 3 条，业务日期均为 ERP 计划开工日期 `2026-08-07`。

## 清理

- 精确清理任务前缀审计 2、PQC 任务 3、工序快照 1、活跃订单 1 和全部 fixture 主数据。
- `verify-clean` 证明产品、工序、路线、工单和 QA 规程任务前缀计数全部为 0。
- 任务 Java/Node/PowerShell 进程已停止，`8089/48089` 均无监听；`8081/48081` 并发运行态未被修改。
- `task-closeout-cleanup` preview/apply 均通过，三份核心任务记录保留，其余本任务 evidence、fixture、诊断、测试结果和临时运行日志已删除。
- `slot=8` 已在端口登记表标记为非活动；验证 worktree、临时分支和物理目录均已删除。

## 剩余风险

- 完整 MES 测试源码仍有上述无关基线编译阻塞；本任务不修改该并发范围。
- 无本任务剩余实现或环境清理事项。完整 MES 测试源码仍有上述无关基线编译阻塞，已由目标范围聚焦测试和真实路径验证覆盖本次变更。
- Git push blocker：`git push origin int_main` 无法通过本机代理 `127.0.0.1` 连接 `github.com:443`；本地提交尚未进入 `origin/int_main`，因此任务保持 `ready_for_closeout`。
