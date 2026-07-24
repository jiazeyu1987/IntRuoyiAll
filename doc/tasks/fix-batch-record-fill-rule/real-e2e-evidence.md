# eDHR 批次执行真实路径 E2E Evidence

- Task ID: `fix-batch-record-fill-rule`
- 状态：FAIL
- 前端入口：`http://localhost:8081`
- 测试租户：`测试租户`；账号名默认 `aoteman`，密码由环境变量注入。

## BDD

- BDD: 创建/打开批次执行 -> Given 测试租户存在真实工单、产品、路线和默认批记录绑定 When 用户从工作台创建或打开批次 Then 页面进入批次详情并展示按路线排序的任务。
- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。
- BDD: 关闭和归档入口 -> Given 批次任务完成且后端返回 canClose=true When 用户关闭批次并生成归档 Then 前端调用真实关闭、生成、下载接口并暴露打印入口。

## Result

- RED: 真实前端路径失败，打开工序任务业务响应必须成功：eDHR 批次缺少唯一批记录路线

1040750412 !== 0

