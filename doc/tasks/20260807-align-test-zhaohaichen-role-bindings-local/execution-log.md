# 执行日志

## User Intent

- 用户反馈本机 `zhaohaichen` 可见 `电子签名`、`文控中心`、`基础数据`，测试服同账号不可见。
- 只读诊断确认环境角色与菜单授权不一致；用户批准同步测试服角色及相关菜单授权，并明确继续禁止 DCC 文件下载。
- 现有 `wenkong` 被证明会恢复下载能力后，用户回复“继续”；正式方案收敛为创建并分配独立 `wenkong_no_download`，不绑定高权限 `wenkong`。

## Rule And Skill Reads

- 已读取项目 `AGENTS.md` 及 `docs/server-access.md`、`docs/login-access.md`、`docs/database-rules.md`、`docs/local-runtime.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/task-closeout-rules.md`、`docs/release-backup-restore.md`、`docs/powershell-memory.md`。
- 已使用 `database-schema-delivery` 技能并读取 `references/database-contract.md`。

## BDD And TDD Evidence

- BDD: 测试服账号业务入口与本机对齐 -> Given 本机 `tenant_id=1/zhaohaichen` 通过 `wenkong` 获得三个业务入口，测试服仅有 `approval_center_entry` / When 测试服创建并分配正式 `wenkong_no_download` 角色且不恢复 `doc_control` / Then 测试服保留审批中心角色并获得与本机一致的三个业务入口。
- BDD: 三个根菜单可见 -> Given 测试服三个菜单定义均启用 / When `zhaohaichen` 通过 `wenkong_no_download` 解析菜单 / Then 权限链包含 `6800 文控中心`、`900218 电子签名`、`990200 基础数据`。
- BDD: DCC 下载能力继续禁用 -> Given 旧任务通过删除 `zhaohaichen/doc_control` 移除了下载能力 / When 本次仅启用无下载规则的 `wenkong_no_download` 并补齐根菜单 / Then `doc_control` 仍为删除状态且用户、角色、岗位、部门四类下载放行来源均为 0。

## Current Evidence

- 本机只读 SQL：`zhaohaichen(id=376, tenant_id=1)` 有效角色为 `wenkong(910231)`、`approval_center_entry(910295)`；`wenkong` 有效根菜单为 `6800/900218/990200`。
- 测试服只读 SQL：`zhaohaichen(id=376, tenant_id=1)` 当前仅有 `approval_center_entry(910295)`；`doc_control(910233)` 用户角色关系于 `2026-08-02 23:54:20` 被 `codex-20260802-dcc-download` 软删除。
- 测试服菜单定义 `6800/900218/990200` 均存在、启用且未删除；目标用户无活动动态授权账本记录。
- 测试服 `wenkong(910305)` 当前有 `6800/990200` 根菜单，缺 `900218`。
- Git preflight：当前分支 `int_main`，`origin` 存在；按项目门禁创建共享脏工作区基线提交 `fca53dda5 chore: baseline concurrent changes before zhaohaichen role alignment`，本任务目录未混入该提交。精确文件列表由不可变 Git 对象 `git show --format= --name-status fca53dda5` 保存，共 71 项。
- RED: 测试服 schema 与现有角色权限只读 SQL -> FAIL, expected reason：`wenkong(910305)` 同时有效绑定 `6801/dcc:controlled-file:directory:manage` 与 `6811/dcc:controlled-file:download`；不能直接分配给要求继续禁用下载的 `zhaohaichen`。
- RED: 后端正式下载判定代码核对 -> FAIL, expected reason：`DccDirectoryAccessPermissionServiceImpl.hasDirectoryManagementPermission` 对 `directory:manage` 或 `access-rule:manage` 任一权限返回 true；`DccControlledFileQueryServiceImpl.decideDownloadBinary` 将该结果直接用于类别与目录下载放行。绑定现有 `wenkong` 会恢复有效下载能力。
- TEST SERVER HEALTH: `http://172.30.30.58:48081/actuator/health` -> `UP`。
- TEST SERVER SCHEMA: MySQL `8.0.39`；`system_user_role`、`system_role_menu` 均为 InnoDB，已核对主键、审计字段、删除标记与租户字段。
- EXPERIENCE GATE: 命中 `docs/database-rules.md#系统角色菜单授权-tenant-1-admin-门禁`、`docs/frontend-development.md#前端权限页签正向授权门禁`、`docs/frontend-development.md#dcc-基础条目关联文档分类树门禁`、远端 MySQL/SSH UTF-8 门禁；摘要已写入 `task.md`。
- GREEN: `change.sql` 通过 SSH stdin 在测试服 MySQL 单事务执行 -> PASS，返回 `COMMITTED user_id=376 role_id=910417 role_menu_count=10`。
- GREEN: 精确删除 Redis 角色缓存键 `user_role_ids::376`、`user_role_ids:376` -> PASS，返回 `0`，表示执行时不存在残留键；未使用全库清理。
- GREEN: `verify.sql` 只读复验 -> PASS，有效角色为 `approval_center_entry(910295)`、`wenkong_no_download(910417)`，`doc_control(910233)` 仍为删除状态；三个根菜单均由 `wenkong_no_download` 解析。
- GREEN: 下载权限链复验 -> PASS，角色危险权限、角色类别下载规则、角色目录下载规则、用户直接下载规则、岗位下载规则和部门链下载规则计数均为 `0`；目标用户岗位集合为空，部门链为 `124,143,226`。
- GREEN: 动态授权复验 -> PASS，目标用户活动动态授权账本计数为 `0`。
- GREEN: 回滚脚本语法验证 -> PASS；仅创建后立即删除存储过程，未执行业务回滚；`information_schema.routines` 复验残留过程数为 `0`。
- AUTHENTICATED UI/API: 测试服目标用户活动 OAuth token 计数为 `0`，本任务未持有账号密码，因此未伪造已登录页面或 API 结果；用户重新登录后会重新拉取角色菜单。
- GREEN: Database schema validator self-test -> PASS：`Database schema validator self-test passed.`。
- GREEN: Database schema evidence validator -> PASS：`Database schema evidence is valid.`。
- GREEN: `git diff --check -- doc/tasks/20260807-align-test-zhaohaichen-role-bindings-local` -> PASS。
- EXPERIENCE CONSOLIDATION: 使用 `project-experience-consolidation` 将“DCC 菜单恢复必须同时排除目录管理下载旁路并核对用户/角色/岗位/部门下载规则”的通用门禁合并到现有 `docs/database-rules.md`，并补充 `docs/experience-index.md` 路由；未新建长期经验文档。并行任务在提交同一索引文件时将本任务新增索引行一并纳入 `fe2d19dac`，已用 `git show fe2d19dac -- docs/experience-index.md` 确认该行存在；本任务不重复修改或回退该提交。

## Dirty Worktree Baseline

- Commit: `fca53dda5 chore: baseline concurrent changes before zhaohaichen role alignment`。
- 本任务目录未包含在该提交中；精确文件清单如下：

```text
M AGENTS.md
M IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileUploadApiTest.java
M IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImplTest.java
M IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java
D IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamEmployeeBindingDisableReqVO.java
D IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamEmployeeBindingSaveReqVO.java
D IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamProcessEmployeeBindingSaveReqVO.java
D IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamEmployeeBindingDisableReqBO.java
D IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamEmployeeBindingSaveReqBO.java
D IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamEmployeeBindingService.java
D IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamEmployeeBindingServiceImpl.java
M IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderRuntimeConfigService.java
M IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderRuntimeConfigServiceImpl.java
D IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamProcessEmployeeBindingSaveReqBO.java
M IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderControllerTest.java
M IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineEmployeeSwitchServiceTest.java
M IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderRuntimeConfigServiceTest.java
M IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteProcessFlowServiceImplTest.java
M IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionPublishProjectionServiceTest.java
M IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts
M IntRuoyiFronted/src/views/dcc/controlled-file/categories/components/CategoryTrainingRulesTab.vue
M IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue
M IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue
M IntRuoyiFronted/tests/e2e/dcc-training-ux-prechecks-static.spec.cjs
M IntRuoyiFronted/tests/e2e/dcc-upload-category-leaf-real.e2e.js
M IntRuoyiFronted/tests/e2e/dcc-upload-category-permission-static.spec.js
M IntRuoyiFronted/tests/e2e/edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs
M IntRuoyiFronted/tests/e2e/mes-process-pool-team-leader-static.spec.js
M IntRuoyiFronted/tests/e2e/production-employee-inherits-leader-processes-static.spec.cjs
M IntRuoyiFronted/tests/e2e/production-personnel-management-real.e2e.js
M IntRuoyiFronted/tests/e2e/team-leader-workbench-real-flow.e2e.js
M IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs
M IntRuoyiFronted/tests/e2e/work-order-abnormal-minimal-report-static.spec.js
A doc/tasks/20260807-agents-no-required-git/execution-log.md
A doc/tasks/20260807-agents-no-required-git/task.md
A doc/tasks/20260807-agents-no-required-git/verification-report.md
A doc/tasks/20260807-category-training-remove-permission-precheck/execution-log.md
A doc/tasks/20260807-category-training-remove-permission-precheck/frontend-feature-evidence.md
A doc/tasks/20260807-category-training-remove-permission-precheck/task.md
A doc/tasks/20260807-dcc-upload-hide-category-permission-hint/execution-log.md
A doc/tasks/20260807-dcc-upload-hide-category-permission-hint/frontend-feature-evidence.md
A doc/tasks/20260807-dcc-upload-hide-category-permission-hint/task.md
A doc/tasks/20260807-dcc-upload-hide-category-permission-hint/verification-report.md
M doc/tasks/20260807-dcc-upload-permission-at-approval/execution-log.md
M doc/tasks/20260807-dcc-upload-permission-at-approval/task.md
M doc/tasks/20260807-frontline-route-process-workstation-binding-fix/execution-log.md
A doc/tasks/20260807-frontline-route-process-workstation-binding-fix/runtime-patch/publish-workstation-repair.ps1
A doc/tasks/20260807-frontline-route-process-workstation-binding-fix/runtime-patch/restart-patched-backend.ps1
M doc/tasks/20260807-pqc-leader-management-five-records/database-schema-evidence.md
M doc/tasks/20260807-pqc-leader-management-five-records/execution-log.md
A doc/tasks/20260807-pqc-leader-management-five-records/fixture.sql
A doc/tasks/20260807-pqc-leader-management-five-records/rollback.sql
A doc/tasks/20260807-pqc-leader-management-five-records/run-e2e.ps1
M doc/tasks/20260807-pqc-leader-management-five-records/task.md
A doc/tasks/20260807-pqc-leader-management-five-records/verify.sql
D doc/tasks/20260807-production-leader-active-order-five-records/database-schema-evidence.md
M doc/tasks/20260807-production-leader-active-order-five-records/execution-log.md
D doc/tasks/20260807-production-leader-active-order-five-records/fixture.sql
D doc/tasks/20260807-production-leader-active-order-five-records/playwright-add.ps1
M doc/tasks/20260807-production-leader-active-order-five-records/task.md
A doc/tasks/20260807-production-leader-active-order-five-records/verification-report.md
D doc/tasks/20260807-production-leader-active-order-five-records/verify.sql
A doc/tasks/20260807-production-leader-feedback-five-records/data-change.sql
M doc/tasks/20260807-production-leader-feedback-five-records/database-schema-evidence.md
M doc/tasks/20260807-production-leader-feedback-five-records/execution-log.md
M doc/tasks/20260807-production-leader-feedback-five-records/task.md
A doc/tasks/20260807-shared-word-parser-implementation/prd.md
A doc/tasks/20260807-shared-word-parser-implementation/request-analysis.md
M doc/tasks/20260807-shared-word-parser-implementation/task-state.json
M docs/experience-index.md
M docs/frontend-development.md
```

## Remaining Closeout

- 数据变更、核心验收、database evidence validator 和经验沉淀已完成；待 cleanup preview/apply、任务提交和推送。
- 共享工作区仍有其它任务改动；后续仅显式暂存本任务文件，不终止其它任务进程，不把并行任务文件混入本任务提交。
