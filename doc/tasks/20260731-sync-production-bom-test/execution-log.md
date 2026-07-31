# Execution Log

## Intent

- 用户请求：把本地芋道源码的生产用料清单同步到测试服务器。
- 任务解释：同步本地源码中相关功能到测试服务器 `172.30.30.58`，默认不执行业务数据同步。

## BDD

- BDD: 测试服务器源码同步 -> Given 本地源码包含生产用料清单相关实现，When 执行授权的测试服同步/发布流程，Then 测试服务器前端可访问且后端健康检查通过，并能承载当前源码版本。

## Milestone Log

- 2026-07-31：读取 `docs/server-access.md`、`docs/release-backup-restore.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md` 与 CI/CD skill 指南。
- 2026-07-31：确认当前分支 `int_main`，`origin` 为 `https://github.com/jiazeyu1987/IntRuoyiAll.git`，当前状态为 `ahead 18` 且存在既有脏改动，需记录为发布前风险边界。
- 2026-07-31：读取测试服发布门禁、发布构建经验、数据库规则、worktree 限制、分支端口矩阵、后端规则和前端规则。
- 2026-07-31：定位生产用料清单源码链路：`20260613_erp_production_material_list_menu.sql`、`20260613_mes_kingdee_production_material_list.sql`、`KingdeeProductionMaterialListSyncJob`、`MesKingdeeProductionMaterialList*`、前端 `/erp/production-material-list` API 与相关列表页。
- 2026-07-31：创建本轮干净 release worktree `D:\IntRuoyiWorktree\pml-test-r260731`，冻结提交 `363a887f03200bf58c6e8c649b8805c0fe66b06b`；`git status --porcelain --ignored=no` 无输出，说明发布输入 clean。

## Verification Evidence

- GREEN: experience-preflight -> PASS，已按测试服发布、code-only、worktree、服务器访问、发布备份恢复、PowerShell/Git、数据库、后端和前端门禁建立任务证据。

## Blockers

- 主工作区 `ahead 18` 且存在既有脏改动；本轮只发布冻结 HEAD 的已提交内容，脏工作区内容不会进入发布包。
