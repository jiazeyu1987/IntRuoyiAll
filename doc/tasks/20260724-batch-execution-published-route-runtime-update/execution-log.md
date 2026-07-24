# 执行日志：批次执行已发布工艺路线快照运行态更新

## User Intent

创建批次执行必须读取最新已发布工艺路线，创建后冻结；不得与草稿配置产生依赖。

2026-07-24：用户要求进行 E2E 验证。

## BDD Scenarios

- BDD: 创建批次使用已发布路线快照 -> Given 工艺路线存在 ACTIVE 版本且草稿配置已发生变化 / When 创建 eDHR 批次执行 / Then 批次持久化 ACTIVE 版本和路线快照，并仅从该快照生成任务
- BDD: 已创建批次不受草稿影响 -> Given 批次已经按 ACTIVE 路线快照创建 / When 修改当前草稿配置 / Then 批次任务和其表单绑定保持创建时冻结内容

## Initial Evidence

- 已发现源码 `openOrCreate` 在生成批次任务前写入 `routeVersionId`、`routeVersionNo` 和 `routeSnapshotJson`，并调用冻结快照感知的任务构建方法。
- `GREEN: node src\test\js\edhr-route-form-slot-frozen-runtime-static.spec.cjs` -> PASS，确认新建和质量拒收重执行均先写入 ACTIVE 路线快照，再从冻结快照生成任务。
- `GREEN: mvn.cmd '-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft,MesProEdhrBatchExecutionRouteVersionFreezeTest' surefire:test` -> PASS，2 项测试通过；其中服务级回归证明发布后修改当前草稿不会影响新批次的表单绑定。
- 当前 `48081` 被 Java PID `39264` 占用，启动时间为 `2026-07-24 14:28:55`；进程命令行未能读取，不能确认其为当前 `int_main` 后端。
- 后端工作区包含大量其他并行 eDHR 未提交改动；从该工作区重新打包并重启会部署非本任务变更。

## Blockers

- BLOCKER: local-runtime update -> PID `39264` 的运行命令和归属无法确认，且 `IntRuoyiBackend` 脏工作区包含其他任务改动。根据本地运行态与任务归属规则，禁止停止该进程或从该输入直接构建部署。
- E2E 前置：创建批次属于写入型真实路径，必须先确认测试租户、测试账号、任务自有工单和清理方案；禁止使用工单 `881MO090935`。
- GREEN: Playwright `batch-route-snapshot-e2e` -> PASS（只读路径），使用本机 `http://127.0.0.1:8081` 登录后依次进入 `MES 系统 -> eDHR批记录 -> 批次执行 -> 打开/创建`，成功显示工单、路线、批次号和备注字段；已点击取消并关闭浏览器。
- BLOCKER: write E2E -> 默认身份页面显示大量现有待办和业务批次，不能确认其为专用测试租户；当前后端仍是未包含本修复的旧 Jar。提交创建请求将既污染未知数据，又不能验证新逻辑，已停止在提交前。
- BLOCKER: experience-preflight -> `docs/experience-index.md` 将 PowerShell 命令编排与本地重启路由至 `E:\IntRuoyi\docs\powershell-memory.md`，该权威门禁文件不存在。按 no-fallback 和高风险任务门禁，未读取该文件不得继续构建、停止 PID、重启后端或提交写入型 E2E。
