# Execution Log

## User Intent

- 用户要求继续实现：将“PQC填写”从批次执行页面内部 tab 提取为独立页签，页签名为“一线PQC”，且验证时 admin 账号能够看到该页签。

## Applied Rules And Skills

- 使用技能：`frontend-feature-delivery`。
- 已读取触发规则：`docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/database-rules.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。

## BDD Scenarios

- BDD: PQC独立页签 -> Given admin 登录系统并拥有 MES 批次执行相关权限 / When 打开菜单或动态页签入口 / Then 能看到独立入口“一线PQC”，并进入正式 PQC 填写页面。
- BDD: 批次执行内部移除PQC -> Given 用户进入批次执行页面 / When 查看页面内部 tab / Then 不再出现“PQC填写”内部 tab。
- BDD: 正式入口不降级 -> Given PQC 填写依赖正式路由、组件和权限配置 / When 独立入口加载 / Then 不使用 mock、默认成功或吞异常绕过缺失配置。

## TDD Evidence

- RED: pending
- GREEN: pending

## Milestone Updates

- M0: 发现开始前工作区已有非本任务脏改动；按规则独立提交基线 `4cd8ec941`，未混入本任务文件。
- M1: 读取 `docs/experience-index.md` 后命中动态菜单页签重命名门禁和 MES PQC 项目级检验快照门禁；已写入 `task.md`，本任务限定为入口/页签拆分，不改 PQC 业务事实链路。
