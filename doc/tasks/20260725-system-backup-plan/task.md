# 系统管理备份计划入口

## Task Goal

在系统管理中新增“备份计划”入口，让管理员可以用简单表单开启/关闭定时备份、设置每天/每周备份时间、查看当前计划状态、手动触发一次备份，并用标准列表模板查看历史备份包。

## Milestones

1. 建立后端备份计划 API、调度器抽象和配置读写能力。
2. 补齐菜单权限 SQL 与前端 API 包装。
3. 新增系统管理备份计划页面，使用标准列表模板展示历史备份包。
4. 增加后端、前端静态合同与验证记录。
5. 完成回归验证并记录发布/正式服恢复任务的剩余门禁。

## Expected Verification

- 后端：备份计划服务测试覆盖状态读取、每天/每周保存、启用/停用、脚本缺失 fail-fast、立即备份确认链路。
- 前端：静态合同确认页面使用 `UnifiedListTemplate`、不暴露 Cron、包含简单频率/时间控件、失败状态可见。
- 菜单：SQL 包含 `system:backup-plan:query`、`system:backup-plan:update`、`system:backup-plan:execute` 与组件 `system/backup-plan/index`。
- 回归：运行受影响后端测试、前端静态合同；真实 E2E 和正式服发布需要在授权运行态下执行。

## Current Status

implementation_done_readonly_e2e_passed_write_e2e_requires_authorized_scheduler

## Completed Work

- 后端新增 `/infra/backup-plan` 独立接口、备份计划服务、Windows 计划任务网关、运行控制台立即备份网关。
- 备份脚本注册流程支持 `DAILY/WEEKLY`、星期配置和正式服备份确认参数。
- 前端新增 `系统管理 > 备份计划` 页面与 API 包装，页面使用 `UnifiedListTemplate` 展示备份包历史，不暴露技术配置项。
- 新增菜单迁移 `20260725_system_backup_plan_menu.sql` 和静态合同测试，包含查询、更新、执行三项细粒度权限。

## Remaining Blockers

- 写入型真实页面 E2E 未执行：保存计划、开启/关闭自动备份和“现在备份一次”会修改真实 Windows 计划任务或触发备份动作，需要明确授权的测试调度环境；当前只完成只读真实页面 E2E。
- 正式服发布未执行：需要用户再次明确授权正式服发布、任务重注册和发布后验证。

## Resolved Blockers

- 已修复既有迁移元数据格式：`20260723_mes_edhr_assignment_rule_candidate_nullable.sql` 的 `dependsOn` 移除 `.sql` 后缀。
- 全量迁移策略门禁已通过：`python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，372 个迁移。
- 已修复 branch runtime 识别：`scripts/runtime/branch-runtime-profile.ps1` 现在读取 `D:\IntRuoyiWorktree\.ports\worktree-ports.json`，当前 worktree 默认解析为 `profile=int_main`、`slot=2`、前端 `8083`、后端 `48083`，并对重复登记、缺字段、分支不匹配、端口不匹配 fail-fast。
- 已完成只读真实页面 E2E：本地后端 `48083` health `UP`，本地前端 `8083` HTTP 200，应用本地菜单 SQL 后通过真实登录打开 `/system/backup-plan`，确认状态卡、每天/每周控件、历史列表、状态/历史 API 和不暴露 `manifest` 技术词。
- 2026-07-25 已复跑本地只读 E2E：`node IntRuoyiFronted\tests\e2e\system-backup-plan-real-readonly.e2e.js` -> PASS，截图 `output\playwright\system-backup-plan-readonly.png` 已生成；任务自有本地运行进程已停止，`8083/48083` 无残留监听。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。Linux/未知调度器不支持时必须明确 fail-fast，不静默降级。
- `是否从根因和长期维护角度解决`：是。通过独立 API、调度器抽象、配置读写和菜单权限交付正式功能，而不是只修当前计划任务。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Trigger: 备份、恢复、发布、正式服定时任务、备份包历史或生产调度变更。
- Preflight check: 必须读取 `docs/release-backup-restore.md`、`docs/worktree-restrictions.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/database-rules.md`、`docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/powershell-encoding.md`。
- Blocker: 缺少备份目标、恢复脚本、数据盘、MinIO、数据库连接、菜单权限证据、真实页面入口、登录权限或正式服授权时，停止对应发布/恢复/真实 E2E。
- Verification: 记录 RED/GREEN、菜单 SQL 核对、前端标准列表合同、后端服务测试、计划任务状态字段与发布后正式服验证项。
- Forbidden action: 禁止未授权操作正式服、静默切换环境/脚本/数据源、用 API-only 代替页面 E2E、把禁用或脚本路径错误的计划任务声明为正常。
- Evidence: 当前任务 `doc/tasks/20260725-system-backup-plan/`。
