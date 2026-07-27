# Config, Security, and Deployment Design

## Purpose and Scope

本设计定义表单模板三按钮对齐批记录表单行为时的配置、安全、权限、部署和可观测性要求。目标是在不引入新外部服务、不降级错误、不泄露跨租户报表的前提下完成对齐。

## Evidence Reviewed

- 前端现有按钮使用 `v-hasPermi` 控制部分表单模板操作权限。
- 批记录设计器和编辑器路径由后端接口生成，前端 wrapper 追加 refresh token。
- 表单中心模板池接口由 `form:template:query` 权限保护。
- 批记录报表设计器、编辑路径和规则接口在 MES 模块按 `reportId` 校验元数据。

## Configuration

- 不新增环境变量。
- 不新增前端路由常量时，应复用已有批记录路径，避免重复定义魔法字符串。
- 如新增共享 helper，必须放在前端已有模块边界内，不引入新状态库。
- 当前实现不新增外部服务、后台任务或运行端口。

## Secrets

- 不记录 token、cookie、refresh token 或设计器 URL 中的认证参数到任务日志。
- 验证日志只记录接口路径、状态和脱敏后的 `reportId` 示例。

## Permissions

- 模板列表读取仍由 `form:template:query` 控制。
- 红框三按钮在表单模板页只负责基于已绑定 `reportId` 跳转；批记录设计器预览、编辑和模板模拟填写页继续由下游路由、菜单和接口权限控制。
- `编辑` 不得仅凭 `form:template:create` 放行下游编辑能力；若下游批记录设计器缺少独立权限，后续权限治理任务必须补齐。
- 权限不足时显示真实 403 或统一错误，不做前端隐藏成功。

## Security Controls

- 绑定写入链路必须校验租户一致性；模板池查询只返回当前租户模板版本行上的绑定摘要。
- 前端不得允许用户手写 `reportId` 绕过绑定；路由跳转来自后端响应字段。
- 绑定 `BROKEN` 时不得暴露其他租户或已删除报表细节，只显示可操作阻塞原因。

## Deployment

- 已提供 additive 迁移脚本和 SQL 迁移契约测试：`20260727_bpm_form_template_batch_record_binding.sql`。
- 本地 Docker MySQL 已应用迁移并用 `information_schema` 核对字段和索引；目标环境发布前仍必须按 release migration 流程重复该核对。
- 本地运行态已使用包含 `FormCenterTemplateRespVO`、`FormTemplateVersionDO` 和 `toTemplateResp` 新字段映射的完整构建 jar 完成真实 E2E；只看 health UP 不足以证明目标 Controller 已加载新字段，仍需模板池响应字段核对。
- 前端部署不需要新增运行端口或服务；验证复用现有 int_main 运行态。

## Observability

- 查询接口可在 debug 级别记录绑定状态计数，但不得记录敏感认证信息。
- 三按钮点击失败需在 UI 显示阻塞原因，并在浏览器控制台保留真实接口错误。
- 后端 `BROKEN` 绑定应有明确错误码或响应状态，便于后续巡检。

## Decisions And Remaining Scope

- 本次不新增环境变量、服务、端口、定时任务或跨模块运行时依赖。
- `BROKEN` 绑定巡检、批量绑定状态校验和独立批记录设计器编辑权限治理均为后续范围。
- 运行态验收已使用可恢复的本地临时绑定夹具完成；目标环境仍需使用正式绑定数据或授权测试数据复验。

## Verification Gates

- 发布或本地运行态验收前必须确认迁移已应用到当前后端连接库。
- 真实页面 E2E 必须证明 `打开 / 编辑 / 填写` 从表单模板页进入批记录同源路径，且无旧弹窗调用。
- 缺少权限定义时，不得把 `编辑` 暂时放给所有可查询模板的用户。
