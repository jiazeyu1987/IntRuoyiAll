# Verification Report

## Summary

问题由两个独立约束组成，当前实现均满足：

- 个人工作台不再把关闭、归档、驳回、作废批次的残留 `TODO` 展示为可处理待办；`openTask` 继续对终态批次 fail-fast。
- 新建 eDHR 业务在创建边界解析并冻结对应批记录定义下最新 `APPROVED` 表单版本；历史业务继续使用既有冻结快照。

本任务未新增生产代码，因为正式修复和最新版本选择逻辑均已在当前 HEAD 中存在；未添加重复 fallback、兼容分支或异常吞噬。

## Backend Verification

- `MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches`
- `MesProEdhrBatchExecutionServiceTest#openOrCreate_resolvesLatestApprovedRouteBindingReportAndShowsCurrentFillersToReadonlyViewer`
- `MesProEdhrBatchExecutionServiceTest#openOrCreate_persistsBatchRecordVersionSnapshotFromRouteBindingToTask`
- `MesProEdhrBatchExecutionServiceTest#getDetail_showsLatestCurrentFillersForExistingOldVersionRouteTaskWithoutMigratingTask`
- `MesProEdhrBatchExecutionServiceTest#openTask_rejectsClosedBatch`

聚焦命令结果：`Tests run: 5, Failures: 0, Errors: 0, BUILD SUCCESS`。

制品命令：`mvn.cmd -pl yudao-server -am -DskipTests package -> BUILD SUCCESS`。

Bug regression evidence validator：`Bug regression evidence is valid`。

## Runtime Verification

- 旧 `int_main` 后端 PID：`53292`，经并发重启任务明确交接后停止。
- 新 `int_main` 后端 PID：`14740`。
- 运行 Jar：`E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- Source/target SHA256：`48324A7C340C025B84D3CD78C59D6BD10B4C6BC02F7C74EDE79A5F94161A8F85`。
- 健康检查：`http://127.0.0.1:48081/actuator/health -> {"status":"UP"}`。
- 前端入口：`http://127.0.0.1:8081/ -> HTTP 200`。

## Frontend And Data Verification

- 批记录定义 `47` 当前最新已发布版本为 `130/V14.0/APPROVED`；旧版本 `118/V13.0`、`98/V12.0`、`79/V4.0` 为 `OBSOLETE`。
- 截图中的目标任务属于状态 `60/VOIDED` 的历史批次，因此不应进入个人可处理待办，也不应放宽 `openTask`。
- 本机默认身份 `芋道源码/admin` 通过 Playwright 登录前置进入 `/user/profile`，证明真实前端与新后端联通。
- 提交 `bd08562f` 的既有真实 Playwright 证据使用 `芋道源码/zhangkeying` 验证：目标任务不在 `my-page` 响应和页面正文中，且未出现“当前 eDHR 批次状态不允许该操作”。

## Verification Boundary

本轮当前批准的本机凭据来源不包含 `zhangkeying` 密码，因此未重复登录该责任人账号；未重置密码、伪造 token 或用 API-only 冒充责任人 E2E。精确责任人真实路径由同一修复提交的既有 Playwright 证据覆盖，本轮补充了源码祖先、聚焦回归、制品哈希、运行态健康和默认账号真实前端联通验证。

## Result

PASS，进入 `ready_for_closeout`。
