# eDHR 批次执行真实路径 E2E Evidence
- Task ID: `20260727-edhr-cell-link-auto-persist-implementation`
- 状态：FAIL
- 前端入口：`http://localhost:8081`
- 授权租户/账号：`芋道源码/admin`；密码由登录页本机默认值提供，脚本和证据不记录明文密码。
- 数据来源：`int-ruoyi-mysql/ruoyi-vue-pro`
- 批次执行：`EDHRB-1785116357526`，任务 ID `6666`，执行 ID `1571`
- 单元格链接规则：ruleId `12`，source `PRODUCTION_WORK_ORDER.batchCode`，target `3:3`
- 临时责任人切换：workTaskId `2227`，原责任人 `jiazeyu` -> `admin`；回滚影响行数 `1`。
## BDD
- BDD: 数据库夹具发现 -> Given 本机数据库存在授权租户 admin 与非作废 eDHR 批次任务 When 执行真实 E2E Then 脚本从数据库读取批次、任务和执行 ID，不要求人工注入工单或批次环境变量。
- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。
- BDD: 单元格链接自动落库 -> Given 批记录存在生产工单 batchCode 链接规则 When 用户打开执行记录 Then `task/open` 返回 `cellLinkAutoPersist`，详情接口包含已保存单元格值，页面输入框显示相同值。
## Result
- RED: 真实前端路径失败，打开工序任务业务响应必须成功：系统异常

500 !== 0

