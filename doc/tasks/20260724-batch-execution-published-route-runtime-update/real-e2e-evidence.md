# eDHR 批次执行真实路径 E2E Evidence

- Task ID: `20260724-batch-execution-published-route-runtime-update`
- 状态：PASS
- 前端入口：`http://localhost:8081`
- 登录身份：租户 `芋道源码`；账号 `admin`，密码由环境变量注入。
- 批次执行：`900000000790 / EDHRB-1784958302427`；批次号 `BRS20260725134444`；路线 `922119 / RT000028`

## BDD

- BDD: 创建/打开批次执行 -> Given 测试租户存在真实工单、产品、路线和默认批记录绑定 When 用户从工作台创建或打开批次 Then 页面进入批次详情并展示按路线排序的任务。
- BDD: 冻结发布路线 -> Given 授权租户存在工单和 ACTIVE 路线且路线仍有草稿 When 用户从页面创建批次 Then 批次进入详情页并在数据库中冻结 ACTIVE 版本而非草稿。


## Result

- GREEN: 真实前端创建批次路径已完成。

## DB Freeze Verification

- GREEN: final DB verification -> PASS，批次 `900000000790 / BRS20260725134444` 持久化 `route_id=922119`、`route_version_id=358`、`route_version_no=V14`，`route_snapshot_json` 长度 `38089`，`configSnapshots.batchUseConfigs=14`，`task_total=21`，`blocked_count=0`。
- GREEN: draft independence -> PASS，路线 `922119 / RT000028` 当前 active 发布版本为 `358 / V14`，同时存在草稿 `361 / V15`；创建批次冻结 ACTIVE 发布版本而非草稿。
