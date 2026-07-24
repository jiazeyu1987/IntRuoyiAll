# 执行日志：审批中心 provider 异常清理

BDD: provider 空任务页不应触发 BPM 空集合异常 -> Given 审批中心聚合查询某 provider 返回空任务页 / When provider 继续构建流程实例映射 / Then 返回空分页结果而不是抛出 Set of process instance ids is empty。
BDD: DCC 已办历史记录在源文件已删除时仍可显示摘要 -> Given DCC 历史审批任务仍存在但对应 controlled file 行已被物理删除 / When 审批中心查询已办列表 / Then 返回可展示的历史审批摘要并明确标记业务已删除，而不是整页报错。
BDD: 审批中心真实页面不再出现 DCC provider 红字异常 -> Given 本机前后端运行最新审批中心修复代码 / When 测试租户用户进入 /approval-center/todo / Then 页面不显示 Set of process instance ids is empty 或 APPROVAL_BUSINESS_OBJECT_REQUIRED 红字，并可继续完成审批中心真实链路验证。
GREEN: previous-task-check -> PASS, 最近同仓后端任务 task.md 为 COMPLETED。
GREEN: experience-preflight-not-required -> PASS, 本轮仅做本地后端代码与定向测试，不涉及真实 E2E、服务器写入、发布、恢复或其他高风险动作。
GREEN: experience-preflight -> PASS, 已补齐 login-access 与 PowerShell 门禁，本轮真实验证仅针对本机 localhost:8081 / localhost:48081。
GREEN: node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /approval-center/todo --target-text 审批中心 -> PASS
RED: $env:APPROVAL_CENTER_E2E_BASE_URL='http://localhost:8081'; $env:APPROVAL_CENTER_E2E_BACKEND_URL='http://localhost:48081'; node yudao-ui-admin-vue3/tests/e2e/approval-center-phase2-real.e2e.mjs -> FAIL, 脚本仍等待旧标题“统一审批中心”，未对齐审批中心新容器文案与子路由入口。
GREEN: apply_patch -> PASS, 对齐真实 E2E 到新契约：标题改为“审批中心”、入口改为 /approval-center/todo，并新增两条红字不存在断言。
RED: $env:APPROVAL_CENTER_E2E_BASE_URL='http://localhost:8081'; $env:APPROVAL_CENTER_E2E_BACKEND_URL='http://localhost:48081'; node yudao-ui-admin-vue3/tests/e2e/approval-center-phase2-real.e2e.mjs -> FAIL, 前端真实请求命中 http://127.0.0.1:48081，脚本后端基址断言仍写 localhost。
GREEN: $env:APPROVAL_CENTER_E2E_BASE_URL='http://localhost:8081'; $env:APPROVAL_CENTER_E2E_BACKEND_URL='http://127.0.0.1:48081'; node yudao-ui-admin-vue3/tests/e2e/approval-center-phase2-real.e2e.mjs -> PASS
GREEN: real-e2e-result -> PASS, 审批中心页面无 Set of process instance ids is empty 与 APPROVAL_BUSINESS_OBJECT_REQUIRED 红字，且真实完成列表、轨迹、详情跳转链路。
