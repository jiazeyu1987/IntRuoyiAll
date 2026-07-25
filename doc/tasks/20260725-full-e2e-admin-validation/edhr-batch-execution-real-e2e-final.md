# eDHR 批次执行真实路径 E2E Evidence
- Task ID: `20260725-full-e2e-admin-validation`
- 状态：PASS
- 前端入口：`http://localhost:8081`
- 授权租户/账号：`芋道源码/admin`；密码由登录页本机默认值提供，脚本和证据不记录明文密码。
- 数据来源：`int-ruoyi-mysql/ruoyi-vue-pro`
- 批次执行：`EDHRB-1784485509402`，任务 ID `3394`，执行 ID `1092`
## BDD
- BDD: 数据库夹具发现 -> Given 本机数据库存在授权租户 admin 与非作废 eDHR 批次任务 When 执行真实 E2E Then 脚本从数据库读取批次、任务和执行 ID，不要求人工注入工单或批次环境变量。
- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。
## Result
- GREEN: 真实前端详情页打开填写路径已完成。
