# E2E14-ROUTE-01 上传审批预检执行契约阻塞（历史已解决）

## 2026-10-18 状态

已在当前新包原页面复核解决，本例已完成真实审批与生效。下文为2026-10-16失败现场历史，不能作为当前blocker；最终结论见verification-report.md。

## 2026-10-16 历史结论

账号 910170 已恢复，审计 11569；当前阻塞与账号恢复、五份 schema 迁移无关。本例 U 已通过真实页面完成源 PDF 上传并正常显示一页 PDF；审批人链路预检显示“系统异常”，创建受控文件按钮保持禁用。没有点击创建或最终确认，没有发出 submit 请求。

## Given / When / Then

- Given 本例项目 DCC241016C14P（284），D 为正式 OWNER，U 为正式 EDIT；本例类别为 APPLICATION_CUSTOM，U 拥有本例类别 UPLOAD，本例部门拥有 VIEW；U→S 直属主管和部门负责人 L 已真实回读，选择本次部门、批准 A、文控 D，以及立即生效方式。
- When U 在正式上传页面选择本例 PDF 并完成预览上传，页面自然请求 POST /admin-api/dcc/controlled-files/route-preview。
- Then 审批链应返回可供核对的正式节点与人员；实际页面为“审批人链路 / 不完整 / 系统异常”，创建按钮禁用。

## 真实证据

- 运行包：Root 提供 SHA256 caafeb2e008f0980d5c499448628bace9f87a2a0c69344000135e2ba650427ff，48061 / PID 43284，health UP。
- 命令：node doc/tasks/20261016-dcc-e2e-14/prepare-controlled-file.cjs --submit；exit2，sourceUploaded=true，submitClicks=0，confirmationClicks=0，submitRequests=0。
- 结果：upload-preparation-result.json；截图及已脱敏 trace：upload-preparation/2026-10-16T13-30-32-890Z/。
- 页面 PDF 预览显示 1/1 页、文件编号 DCC241016C14IMMEDIATE；本轮没有正式受控文件 ID 或流程实例。
- 自然请求仅记录 method/path/status，未调用 API 脚本、读取响应 JSON 或写数据库；HTTP 200 没有被当作业务成功。
- 日志摘录：route-preview-runtime-error.txt。2026-10-16 21:30:39—21:30:41（上海时间）的实际异常是 IllegalStateException: EXECUTION_CONTRACT_UNREGISTERED_DEFINITION。

## 源码定位和边界

调用链：DccControlledFileWorkflowServiceImpl.previewCustomRoute:434 → DccApplicationExecutionContractService.preparePublicPreview:303 → verify:348 → DccWorkflowDefinitionCatalog.require:23。

preparePublicPreview 对上传键 dcc-controlled-file-upload 选取活动流程定义，并要求 PUBLIC_MATRIX_NATIVE_V2；require 校验契约、流程键与模型 digest 的固定目录。错误说明三者未通过校验，但本线程没有读取活动定义 ID/digest，因此不把“具体部署了哪个旧版本”作为已证事实。

需 Root 核对活动上传流程定义、模型 digest 与契约目录。不得修改固定 digest 目录来伪造已注册，不得用 mock、接口提交、扩大 U 权限或直接写流程表绕过。本线程除已授权部门修复外不扩展生产代码、Maven、BPM 部署、迁移或服务操作。

前一条“Current user is not the DCC project owner”是缺少本例类别 UPLOAD 权限时复用的错误文案。补齐并真实回读本例类别 UPLOAD/VIEW 后，PDF 已正常上传；该前置已解决，不能继续当作当前阻塞，也不能推断 EDIT 无效。

## 继续入口

Root 处理活动流程定义后，先核对 upload-preparation-result.json 的 submitRequests。当前为 0，可以用同一任务脚本真实重试；只要已有一次 submit 请求，就停止自动重提并改为只读定位。原 14-A—14-D 尚未进入文控任务，仍为 BLOCKED。
