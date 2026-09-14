# E2E Plan

## Purpose and Scope

通过 Playwright 真实页面路径验证个人工作台配置页签可为 ERP 表格自动同步选择时间和同步类型，并保存后回显。API 只用于最终读回核对。

## User Paths

1. 打开 worktree 前端端口 `8083`。
2. 使用确认的测试账号登录。
3. 进入个人工作台。
4. 切换到“配置”页签。
5. 打开“ERP表格自动同步”。
6. 启用自动同步，选择每日开始时间。
7. 勾选至少两个 ERP 表格，例如 ERP 商品和库存。
8. 保存配置。
9. 刷新或重新进入页签，确认配置回显。

## API Verification

- 读回 `/erp/kingdee-table-auto-sync/plan/get`。
- 必要时读回 `/erp/kingdee-table-auto-sync/run/page` 和 `/watermark/list`，验证运行状态来源为正式 Kingdee 同步记录。

## Test Blockers

- 缺登录账号、验证码未关闭、数据库未应用迁移、前端未启动、后端 health 未 UP、或外部 ERP 配置缺失时，真实 E2E 必须 fail fast。
