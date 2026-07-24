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
- GREEN: experience-preflight -> PASS，`docs/powershell-memory.md` 已恢复并完成读取；同时复核 `docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/e2e-rules.md`、`docs/login-access.md` 和 `docs/task-closeout-rules.md`。
- 运行态归属：`48081` 当前监听 PID `39264`，命令行为 `java -jar E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --server.port=48081 ... --yudao.runtime-control.repo-root=E:\IntRuoyi\IntRuoyiBackend`，可确认为当前 `int_main` 旧后端；`8081` 为 `E:\IntRuoyi\IntRuoyiFronted` Vite 前端。
- 构建策略：主工作区仍有大量并行脏改动，不直接从主工作区打包；创建 `D:\IntRuoyiWorktree\batch-route-snapshot-e2e-20260724` 干净 worktree，从当前 HEAD 构建新 Jar，再加载到 `int_main` 后端目标路径。

## Runtime Reload And E2E Verification

- GREEN: isolated backend jar -> PASS，`D:\IntRuoyiWorktree\batch-route-snapshot-e2e-20260724\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` 构建产物 SHA256 为 `10C7B39A5B3920FEB3E8C71C3719AAC06840C808E729A1E19867A26F9B725C44`。
- GREEN: backend reload -> PASS，确认旧 `int_main` 后端 PID `57944` 运行 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --server.port=48081 --yudao.runtime-control.repo-root=E:\IntRuoyi\IntRuoyiBackend` 后停止；用隔离构建 Jar 覆盖目标 Jar 并启动新 PID `47120`。
- GREEN: health check -> PASS，`Invoke-WebRequest http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`；目标 Jar SHA256 与隔离构建 Jar 一致。
- RED: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> FAIL，测试脚本登录时只填租户未点击可见下拉项，导致真实前端未发出 `/system/auth/login`；已修正为点击真实租户选项。
- RED: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> FAIL，路线下拉页面显示编码/名称/ID 文本但脚本只按 ID 即时查找；已修正为按可见路线编码/名称/ID 显式等待。
- RED: route `922185` create attempt -> FAIL，业务响应 `1040750243`，原因是该测试路线对应批记录模板存在未确认填写规则；这不是原始缺少发布批记录配置错误，说明后端已进入正式批记录校验链路。
- GREEN: `node --check tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS，真实前端 `http://localhost:8081` 使用测试租户 `测试租户/aoteman`，工单 `925555 / TESTERPA9ED2D417434`，路线 `922186 / E2E-OSF-20260721042549`，创建批次 `BRS20260724195134` 并打开 eDHR 执行页。
- GREEN: final DB verification -> PASS，批次 `900000000787` 持久化 `route_id=922186`、`route_version_id=239`、`route_version_no=V2`、`route_snapshot_json` 长度 `40670`，`configSnapshots.batchUseConfigs` 数量 `2`，`task_total=8`，`blocked_count=0`。
- GREEN: draft independence evidence -> PASS，路线 `922186` 当前 ACTIVE 版本仍为 `239 / V2`，同时存在 `open_draft_count=1`；本次批次冻结的是 ACTIVE `V2` 而非草稿。
- GREEN: bug evidence validation -> PASS，`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ...\bug-regression-evidence.md` 返回 `Bug regression evidence is valid.`
- 当前状态：实现和 required verification 已完成；任务进入 `ready_for_closeout`，仍待 cleanup preview/apply、提交与推送门禁。
- GREEN: project experience consolidation -> PASS，已更新 `docs/local-runtime.md#2026-07-24-隔离构建-jar-加载门禁`、`docs/e2e-rules.md#element-plus-下拉选择门禁`，并在 `docs/experience-index.md` 登记关键词。
- BLOCKER: cleanup closeout -> `task_closeout.py --mode preview` 在隔离 worktree 返回 blocked：当前分支 `e2e/batch-route-snapshot-20260724` 不能 fast-forward 合并到 `int_main`，主工作区 `E:\IntRuoyi` 仍有其他任务脏改动，且 worktree 存在非本任务改动 `MesProRouteFlowConfigServiceImpl.java`。按任务归属和 no-fallback 规则，未执行自动合并、删除 worktree 或提交无关文件。
- GREEN: cleanup keep scope -> PASS，已在 task.md 增加 `Cleanup Keep`，保留 `bug-regression-evidence.md` 和 `real-e2e-evidence.md`，避免后续清理误删关键验证证据。
- GREEN: main workspace cleanup apply -> PASS，`task_closeout.py --mode apply` 仅删除本任务 `artifacts/login-debug-111111.png` 与 `artifacts/login-debug.png`，保留 task.md、execution-log.md、verification-report.md 及两份 evidence 文档。
