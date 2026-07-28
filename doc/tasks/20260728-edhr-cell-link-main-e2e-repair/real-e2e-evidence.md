# eDHR 批次执行真实路径 E2E Evidence
- Task ID: `20260728-edhr-cell-link-main-e2e-repair`
- 状态：BLOCKED
- 前端入口：`http://127.0.0.1:8081`
- 后端入口：`http://127.0.0.1:48081`
- 授权租户/账号：`芋道源码/admin`；密码由登录页本机默认值提供，脚本和证据不记录明文密码。
- 数据来源：`int-ruoyi-mysql/ruoyi-vue-pro`
## BDD
- BDD: 数据库夹具发现 -> Given 本机数据库存在授权租户 admin 与非作废 eDHR 批次任务 When 执行真实 E2E Then 脚本从数据库读取批次、任务和执行 ID，不要求人工注入工单或批次环境变量。
- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。
- BDD: 单元格链接自动落库 -> Given 批记录存在生产工单 batchCode 链接规则 When 用户打开执行记录 Then `task/open` 返回 `cellLinkAutoPersist`，详情接口包含已保存单元格值，页面输入框显示相同值。
## Result
- BLOCKED: `node tests/e2e/edhr-batch-execution-real-flow.e2e.js` -> FAIL, 真实 E2E 本地运行态或数据库夹具前置条件缺失。
- 缺失前置：`LOCAL_DATABASE_FIXTURE`，可打开批次任务 未找到符合条件的本地数据库记录
- 影响：无法在真实前端页面完成批次详情打开和工序填写验证；未使用 mock、API-only 或测试专用控件。
