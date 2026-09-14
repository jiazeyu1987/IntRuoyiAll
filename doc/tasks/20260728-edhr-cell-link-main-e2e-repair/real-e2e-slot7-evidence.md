# eDHR 批次执行真实路径 E2E Evidence
- Task ID: `fix-batch-record-fill-rule`
- 状态：PASS
- 前端入口：`http://127.0.0.1:8088`
- 后端入口：`http://127.0.0.1:48088`
- 授权租户/账号：`测试租户/codexedhrcell01`；密码由本机安全运行参数或登录页默认值提供，脚本和证据不记录明文密码。
- 数据来源：`int-ruoyi-mysql/ruoyi-vue-pro`
- 批次执行：`BE-EDHR-CELL-20260728-104808`，任务 ID `6955`，初始执行 ID `1579`，打开后执行 ID `1579`
- 单元格链接规则：ruleId `13`，source `PRODUCTION_WORK_ORDER.batchCode`，target `1:5`
- 临时责任人切换：workTaskId `2243`，原责任人 `admin` -> `codexedhrcell01`；回滚影响行数 `1`。
## BDD
- BDD: 数据库夹具发现 -> Given 本机数据库存在授权租户账号与非作废 eDHR 批次任务 When 执行真实 E2E Then 脚本从数据库读取批次和任务；若初始执行 ID 为空，则由真实打开动作创建。
- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。
- BDD: 单元格链接自动落库 -> Given 批记录存在生产工单 batchCode 链接规则 When 用户打开执行记录 Then `task/open` 返回 `cellLinkAutoPersist`，详情接口包含已保存单元格值，页面输入框显示相同值。
## Result
- GREEN: 真实前端详情页打开填写路径已完成。
- GREEN: task/open 返回 cellLinkAutoPersist，状态 `NO_CHANGE_ALREADY_APPLIED`，目标单元格 `1:5`，值 `EDHR-CELL-20260728-104808`。
- GREEN: 执行详情 cellValues 包含目标单元格保存值；页面输入控件显示值 `EDHR-CELL-20260728-104808`。
