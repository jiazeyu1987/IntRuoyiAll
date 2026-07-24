# eDHR 附件上传真实前置创建 Evidence

- Task ID: `20260612-edhr-attachment-prepare-upload-api`
- 状态：PASS
- 前端入口：`http://localhost:8081`
- 创建方式：Playwright 操作测试租户真实前端页面；密码由环境变量注入，不写入仓库。

## BDD

- BDD: 先创建附件上传前置 -> Given 测试租户存在真实批记录模板和生产工单 / When 通过前端配置模板上传字段并打开批次工序 / Then 生成包含 upload-file 字段的 DRAFT execution 和 TODO/DOING 工作任务。

## Result

- GREEN: 已通过真实前端路径创建附件上传 E2E 前置。
- reportCode：`EBR_TN122_A_T01`
- workOrderCode：`CODexERP20260610E`
- routeId：`922045`
- batchCode：`E2E-122-ATTACH-20260612212514`
- executionId：`326`
- workTaskId：`103`
