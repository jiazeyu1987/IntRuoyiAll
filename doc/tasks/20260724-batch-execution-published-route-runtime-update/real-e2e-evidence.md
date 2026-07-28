# eDHR 批次执行真实路径 E2E Evidence

- Task ID: `20260724-batch-execution-published-route-runtime-update`
- 状态：PASS
- 前端入口：`http://localhost:8081`
- 登录身份：租户 `芋道源码`；账号 `admin`，密码由环境变量注入。
- 批次执行：`900000000805 / EDHRB-1784966806564`；批次号 `BRS20260725160633`；路线 `922119 / RT000028`

## BDD

- BDD: 创建/打开批次执行 -> Given 测试租户存在真实工单、产品、路线和默认批记录绑定 When 用户从工作台创建或打开批次 Then 页面进入批次详情并展示按路线排序的任务。
- BDD: 冻结发布路线 -> Given 授权租户存在工单和 ACTIVE 路线且路线仍有草稿 When 用户从页面创建批次 Then 批次进入详情页并在数据库中冻结 ACTIVE 版本而非草稿。
- BDD: 打开当前填写任务 -> Given 新建批次存在当前活动填写任务 When admin 在批次详情按后端 allowedActions 走“打开填写”或“管理员接管并填写”正式入口 Then 系统打开真实填写表单。


## Result

- GREEN: 真实前端创建批次路径已完成。
- GREEN: 直接打开填写路径已完成，workTask `1779`，openTask 返回 execution `1280`、formCenterInstance ``。
