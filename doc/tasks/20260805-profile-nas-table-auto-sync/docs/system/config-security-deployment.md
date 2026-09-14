# NAS 表格自动同步 Config Security Deployment

## Purpose and Scope

本设计定义 NAS 表格自动同步的配置来源、权限、安全控制、部署和可观测性。目标是让业务用户在个人工作台配置页维护同步计划，同时复用已有 NAS 与 Quartz 基础设施。

## Evidence Reviewed

- `Profile/Index.vue` 使用 `mes:pro-batch-record-execution:golden-finger` 判断配置页签可见性。
- infra NAS 现有配置接口保存服务器、共享、账号和密码，本功能不复制这些密钥。
- `application-local.yaml` 存在本地 Quartz 自动运行白名单，新增 handler 不应默认加入本地自动运行白名单。

## Configuration

- 业务配置在 `erp_nas_table_sync_plan` 与 item 表保存。
- NAS 连接配置只从 `NasSettingsService.getRequiredNasConfig()` 获取。
- 每日开始时间由后端转换成 cron，例如 `HH:mm:ss -> second minute hour * * ?`。

## Secrets

- 不新增 NAS 密码字段，不在 run log、Job 参数、前端响应或任务日志中输出 NAS 密码。
- 测试写入返回相对路径、文件名和状态，不返回连接串或凭据。

## Permissions

- 前端入口沿用 `mes:pro-batch-record-execution:golden-finger`。
- 后端全部配置、执行和日志接口也使用相同 permission，保证“能看配置页签的人才能看”。
- 不新增菜单入口；若未来改成独立菜单，需另做菜单和角色授权迁移。

## Security Controls

- NAS 相对目录必须规范化，禁止空白、`..`、绝对路径和 Windows 盘符穿越共享根。
- 文件名模式只允许业务变量和安全字符，最终文件扩展名固定 `.xlsx`。
- NAS 写入失败必须抛出明确业务错误并记录 run 失败。

## Deployment

- 数据库迁移新增业务表和默认停用 Job。
- 新 JobHandler Bean 名为 `erpNasTableAutoSyncJob`。
- 本地 worktree 使用 slot 7：frontend `8088`、backend `48088`；启动前需确认端口未被其他进程占用。

## Observability

- 每次执行写入 run 与 run item，记录开始、结束、输出路径、成功/失败表数和失败原因。
- JobHandler 返回摘要字符串用于 infra job log。
- 前端展示最近运行结果，便于用户判断自动同步是否健康。

## Open Questions

- 无开放问题；若后续需要通知或告警，应作为独立迭代接入现有通知体系。

## Design Blockers

- 未完成真实 NAS 写入测试前，不得把静态合同或 API-only 响应视为 E2E 通过。
