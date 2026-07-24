# eDHR 批次执行真实路径 E2E Evidence

- Task ID: `20260724-batch-execution-published-route-runtime-update`
- 状态：PASS
- 前端入口：`http://localhost:8081`
- 测试租户：`测试租户`；账号名默认 `aoteman`，密码由环境变量注入。

## BDD

- BDD: 创建/打开批次执行 -> Given 测试租户存在真实工单、产品、路线和默认批记录绑定 When 用户从工作台创建或打开批次 Then 页面进入批次详情并展示按路线排序的任务。
- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。
- BDD: 关闭和归档入口 -> Given 批次任务完成且后端返回 canClose=true When 用户关闭批次并生成归档 Then 前端调用真实关闭、生成、下载接口并暴露打印入口。

## Result

- GREEN: 真实前端路径已完成。
- 命令：`node tests\e2e\edhr-batch-execution-real-flow.e2e.js`
- 前端路径：登录 `http://localhost:8081`，进入 `MES 系统 -> eDHR批记录 -> 批次执行 -> 打开/创建`，选择工单 `925555 / TESTERPA9ED2D417434` 与路线 `922186 / E2E-OSF-20260721042549`。
- 创建结果：批次 `900000000787 / BRS20260724195134` 创建成功，并打开 eDHR 执行页。
- 冻结快照核验：`route_id=922186`，`route_version_id=239`，`route_version_no=V2`，`route_snapshot_json` 长度 `40670`，`configSnapshots.batchUseConfigs=2`。
- 草稿独立性核验：路线 `922186` 当前 ACTIVE 仍为 `239 / V2`，同时存在 `open_draft_count=1`；新批次冻结 ACTIVE 版本，未读取草稿。
- 任务核验：批次任务 `8` 个，传统批记录任务 `4` 个，已打开执行任务 `4` 个，`blocked_count=0`。
