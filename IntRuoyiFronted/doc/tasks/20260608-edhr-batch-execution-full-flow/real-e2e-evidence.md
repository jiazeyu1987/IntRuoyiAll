# eDHR 批次执行真实路径 E2E Evidence

- Task ID: `20260608-edhr-batch-execution-full-flow`
- 状态：PASS
- 前端入口：`http://localhost:8081`
- 测试租户：`测试租户`；账号名默认 `aoteman`，密码由环境变量注入。

## BDD

- BDD: 创建/打开批次执行 -> Given 测试租户存在真实工单、产品、路线和默认批记录绑定 When 用户从工作台创建或打开批次 Then 页面进入批次详情并展示按路线排序的任务。
- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。
- BDD: 多人电子签名 -> Given 单张 eDHR 执行存在 FIELD_CHANGE/SUBMIT/FORM_REVIEW/APPROVE 签名要求 When 操作员填写、复核人复核、审批人审批 Then 签名记录可追溯且批次关闭校验通过。
- BDD: 关闭和归档入口 -> Given 批次任务完成且后端返回 canClose=true When 用户关闭批次并生成归档 Then 前端调用真实关闭、生成、下载接口并暴露打印入口。
- BDD: 复盘查看 -> Given 批次已归档 When 用户打开复盘页 Then 能看到批次时间线、工序任务、签名记录和归档版本。

## Result

- GREEN: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS，真实测试租户批次打开和工序打开路径通过。
- GREEN: Playwright 真实全流程 -> PASS，批次 `EDHR-BATCH-122-FULL-0609020810`，工单 `922139`，路线 `922045`。
- GREEN: 在线填写 -> PASS，首道单表 execution `75` 保存 FIELD_CHANGE，字段审计链 `VALID`。
- GREEN: 多人电子签名 -> PASS，15 张必填单表均存在 `FORM_REVIEW`、`SUBMIT`、`APPROVE`；首张还存在 `FIELD_CHANGE`。
- GREEN: 流转/审批 -> PASS，15 张必填单表 execution 均为 `status=3`，`domain_trace_status=VERIFIED`。
- GREEN: 批次结束 -> PASS，批次 `id=3` 为 `status=40`，`task_total=21`，`task_approved_count=15`，`blocked_count=0`，6 道无默认批记录工序按“无需填写”处理。
- GREEN: 最终表单打印/导出 -> PASS，生成 `SEALED` 归档 `EDHR-BATCH-122-FULL-0609020810-edhr-final.pdf`，下载成功，打印窗口打开成功。
- GREEN: 复盘查看 -> PASS，复盘页展示 `BATCH_CLOSE` 和最终归档版本。
- GREEN: 芋道源码/admin 只读验证 -> PASS，批次执行页可打开；未对芋道源码租户执行写操作。
