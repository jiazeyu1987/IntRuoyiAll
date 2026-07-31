# 执行日志

## 2026-07-30

- User intent: 在 worktree `20260730-banzuzhang` 里完成文档里的开发工作；按当前文档理解为 F9/F10 班组长工作台与班组维护。
- Preflight: 已读取 `docs/worktree-restrictions.md`、`docs/task-closeout-rules.md`、`docs/branch-runtime-ports.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`。
- Skill gates: 已读取 `backend-api-delivery`、`frontend-feature-delivery`、`database-schema-delivery` 及其 evidence contract。
- Worktree: `git worktree add -b codex/20260730-banzuzhang D:\IntRuoyiWorktree\20260730-banzuzhang int_main` -> PASS。
- Slot reservation first attempt: `reserve-worktree-slot.ps1` without required parameters -> FAIL，原因是脚本要求 `Name/Path/Branch/Profile`。
- Slot reservation GREEN: `reserve-worktree-slot.ps1 -Name 20260730-banzuzhang -Path D:\IntRuoyiWorktree\20260730-banzuzhang -Branch codex/20260730-banzuzhang -Profile int_main -AsJson` -> PASS，slot `17`，frontend `8098`，backend `48098`。
- Experience index: 已读取 `docs/experience-index.md`；适用门禁已摘入 `task.md`。
- Documentation: 根据当前系统业务新增 `docs/acceptance/production-line-process-pool/open-questions-blockers.md`，定义 20 个 Open Questions 与 12 个 Blockers，并在 `docs/inception/project-brief.md`、`docs/inception/evidence-inventory.md`、`docs/acceptance/production-line-process-pool/{bdd-scenarios.md,tdd-plan.md,e2e-plan.md,test-data.md}` 挂接引用。
- Verification: `Get-Content -Encoding utf8 docs\acceptance\production-line-process-pool\open-questions-blockers.md` + OQ/BLK 计数 -> PASS，`open_questions=20`，`blockers=12`。
- Documentation refinement: 按当前系统业务补充 `Current System Business Baseline`，明确生产报工、eDHR 记录本、正式批记录、生产工单、报工余量池、工艺路线/工序/设备/用户、电子签名/审计、班组长权限在 OQ/BLK 初始定义中的定位、可复用部分和不可替代缺口。
- Verification: `Select-String` 结构校验 -> PASS，`open_questions=20`，`blockers=12`，`baseline_sections=1`。

## BDD Scenarios

- BDD: 生产班组长按负责范围查看员工提交 -> Given 班组长负责员工 E1001/E1002 且其他员工也有工序池提交 / When 打开提交看板 / Then 只展示负责员工提交并显示复核、PQC、异常和追溯入口。
- BDD: PQC 班组长按负责范围查看 PQC 提交 -> Given PQC 班组长负责 PQC 员工 Q1001 / When 打开 PQC 提交看板 / Then 展示对应 PQC 过程检验事件且不可改写原始结果。
- BDD: 班组长复核员工提交 -> Given 负责员工存在待复核工序池提交事件 / When 班组长提交复核说明 / Then 只写复核状态、复核人、服务端时间和说明，不改写原始 payload、报工、记录本、签名或提交时间。
- BDD: 班组长查看所有生产工单并异常上报 -> Given 班组长具备异常处理权限 / When 对生产工单标记异常并上报 / Then 保存异常和上报记录，且不得泄露非负责员工提交明细。
- BDD: 班组长添加员工后进入后续候选 -> Given 班组长负责工序 P10 / When 添加员工 E3001 到 P10 / Then 后续一线切换员工可选择 E3001，历史提交不变。
- BDD: 班组长禁用员工不影响历史追溯 -> Given 员工 E1001 已有提交 / When 班组长禁用 E1001 / Then 后续候选不再出现 E1001，历史提交、签名和时间轴仍可查询。
- BDD: 班组长维护不良原因 -> Given 班组长具备维护权限 / When 新增不良原因 / Then 后续损耗、不合格或 PQC 失败模板可选，历史原因快照不被改写。
- BDD: 班组长维护工序设备和参数上下限 -> Given 设备来自正式设备台账 / When 绑定到负责工序并设置参数上下限 / Then 一线模板可选择设备，审核副本按上下限修正，原始提交不被硬拦截。
- BDD: 越权访问和维护失败 -> Given 班组长不负责目标员工或工序 / When 查看、复核或维护 / Then 系统拒绝并不返回原始详情或写配置。

## RED / GREEN Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderScopeServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesWorkOrderAbnormalReportServiceTest,MesTeamEmployeeBindingServiceTest,MesDefectReasonCatalogServiceTest,MesProcessDeviceParameterRuleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: F9/F10 schema、Controller、Service、Mapper、DO 和权限契约尚未实现。
- GREEN: 新增 F9/F10 SQL 迁移、Controller、VO、Service、Mapper、DO、错误码和时间轴员工范围过滤；实现班组长负责范围、提交看板、详情范围校验、复核、异常上报、员工绑定/禁用、不良原因和设备参数上下限维护。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderScopeServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesWorkOrderAbnormalReportServiceTest,MesTeamEmployeeBindingServiceTest,MesDefectReasonCatalogServiceTest,MesProcessDeviceParameterRuleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`BUILD SUCCESS`，16 tests，0 failures，0 errors，0 skipped。
- RED: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> FAIL，expected reason: F9/F10 API wrapper、页面组件和路由入口缺失。
- GREEN: 新增 `src/api/mes/pro/processpool/teamLeader.ts`、`src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue` 和 `remaining.ts` 路由，覆盖提交看板、详情复核、异常上报和班组维护。
- GREEN: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS，输出 `mes-process-pool-team-leader-static PASS`。
- GREEN: `pnpm ts:check` -> PASS。
- RED: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260730_mes_process_pool_team_leader.sql --output ..\doc\tasks\20260730-banzuzhang\migration-policy-gate.json` -> FAIL，expected reason: 单文件 targeted gate 缺少依赖迁移 `20260730_mes_process_pool_review_copy`。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260730-banzuzhang\migration-policy-gate.json` -> PASS，`status=passed`，`migrationCount=397`。
- RUNTIME GREEN: `Invoke-RestMethod http://127.0.0.1:48098/actuator/health` -> PASS，`status=UP`；`Invoke-WebRequest http://127.0.0.1:8098/` -> PASS，HTTP 200；端口归属 PID `19088/36928`，命令行归属本 worktree。
- E2E RED: `node scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8098 --target-path /mes/pro/process-pool/team-leader --target-text 工序池班组长工作台` -> FAIL，expected reason: 默认 Playwright Chromium 缓存缺失；未把缺浏览器写成真实 E2E 通过。
- E2E GREEN: 同一登录前置使用本机 Chrome 显式路径复跑 -> PASS，登录 `芋道源码/admin`，目标 `/mes/pro/process-pool/team-leader` 展示 `工序池班组长工作台`。
- E2E RED: 真实页面三标签冒烟 -> FAIL，expected reason: 运行库缺少 `mes_pro_process_pool_team_leader_scope`，目标提交看板接口返回业务 `code=500`；日志证据为 `Table 'ruoyi-vue-pro.mes_pro_process_pool_team_leader_scope' doesn't exist`。
- DB GREEN: 本地 Docker MySQL `int-ruoyi-mysql/ruoyi-vue-pro` 应用 `sql\mysql\20260730_mes_process_pool_team_leader.sql`；应用前探针 `team_leader_tables_before=0`，应用后 `team_leader_tables_after=7`、`team_leader_menus_after=5`。
- E2E GREEN: 真实页面三标签冒烟复跑 -> PASS，登录 `芋道源码/admin`，访问 `/mes/pro/process-pool/team-leader`，依次切换 `提交看板/异常上报/班组维护/PQC 班组长`，目标提交看板 API 两次 HTTP 200 且业务 `code=0`，无控制台 error；截图 `output\playwright\20260730-banzuzhang\team-leader-workbench-smoke.png`。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260730-banzuzhang\backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260730-banzuzhang\database-schema-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260730-banzuzhang\frontend-feature-evidence.md` -> PASS。
- Experience consolidation: 已更新 `docs\e2e-rules.md#官方登录前置与-admin-only-全量验证门禁` 和 `docs\experience-index.md`，沉淀真实 E2E 浏览器缓存缺失时使用稳定 Chrome/Edge 可执行路径、运行库迁移缺失先 RED 后应用正式迁移再 GREEN 的门禁。
- Task status: `task.md` 已更新为 `ready_for_closeout`，并在 `Cleanup Keep` 保留 backend/database/frontend evidence 与 migration gate JSON。
- Commit: `git commit -m "feat: add process pool team leader workbench"` -> PASS，最终 rebase 后实现提交为 `368ef63c`，已位于最新 `int_main` `9f84a797` 之后，`git merge-base --is-ancestor int_main HEAD` -> PASS。
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260730-banzuzhang --mode preview` -> BLOCKED，keep 列表保留 `task.md`、`execution-log.md`、`verification-report.md`、backend/database/frontend evidence 和 migration gate JSON，delete 为 `<none>`；阻塞原因是主工作区 `E:\IntRuoyi` 脏状态 `?? doc/tasks/20260730-edhr-page-graph-tab/`，不能接收 ff-only merge。
- Rebase: 主工作区 `int_main` 更新到 `9f84a797` 后，当前分支再次 `git rebase int_main` -> PASS，当前实现提交重放为 `368ef63c`。
- Closeout record commit: `git commit -m "docs: record team leader closeout blocker"` -> PASS，提交 `1f4f43f6`。
- Push: `git push origin codex/20260730-banzuzhang` -> PASS；branch runtime port guard 通过，远端分支已创建并跟踪 `origin/codex/20260730-banzuzhang`，`HEAD=origin/codex/20260730-banzuzhang=1f4f43f6`。
- Latest cleanup preview after push: `task_closeout.py --mode preview` -> BLOCKED，当前主工作区 `E:\IntRuoyi` 已被并行任务更新为 `int_main...origin/int_main [ahead 1]`，且存在 `M IntRuoyiFronted/src/router/modules/remaining.ts`、`M doc/tasks/20260730-route-admin-list-layout-unification/{task.md,execution-log.md,verification-report.md}`、`?? IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchPageGraphPage.vue`；因此当前分支不能安全 ff-only 合并进主工作区，也不能删除 worktree。
- Closeout blocker refresh: 收尾文档改为稳定 blocker 口径，不再绑定瞬时并行文件清单；当前阻塞条件为主工作区 `E:\IntRuoyi` 的 `int_main` 存在本地 ahead 提交和未清理工作区改动，不满足 task-closeout-cleanup 的 ff-only merge / worktree removal 前置条件。
- Closeout sync: `git merge int_main` -> PASS，生成 `3225bc70 Merge branch 'int_main' into codex/20260730-banzuzhang`，无冲突；合入后 `git merge-base --is-ancestor int_main HEAD` -> PASS。
- Push gate: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`frontend 8098`、`backend 48098`。
- Push blocker: `git push origin codex/20260730-banzuzhang` -> FAIL，原因 `fatal: unable to access 'https://github.com/jiazeyu1987/IntRuoyiAll.git/': Recv failure: Connection was reset`。
- Push retry blocker: 第二次 `git push origin codex/20260730-banzuzhang` -> FAIL，同样为 `Recv failure: Connection was reset`；当前 `git status --short --branch` 为 `codex/20260730-banzuzhang...origin/codex/20260730-banzuzhang [ahead 11]`，按完成门禁不能继续 cleanup apply / ff-only merge / worktree removal。
- Closeout record commit: `git commit -m "docs: record team leader push blocker"` -> PASS，提交 `345f32f2`，仅包含 `doc/tasks/20260730-banzuzhang/{task.md,execution-log.md,verification-report.md}`。
- Push retry blocker: 第三次 `git push origin codex/20260730-banzuzhang` -> FAIL，同样为 `Recv failure: Connection was reset`；当前分支为 `ahead 12`。
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260730-banzuzhang --mode preview` -> BLOCKED，keep 列表保留核心任务文档和 evidence，delete 为 `<none>`；阻塞原因为 `Main worktree is dirty and cannot receive ff-only merge: E:\IntRuoyi`。
- User closeout authorization: 用户要求“先提交代码，然后融合”；按规则将主工作区连续出现的并行脏改动分别作为独立基线提交，没有混入班组长实现提交。
- Dirty baseline: `ca088c6d 基线: 保存并行 MES 工序菜单标题任务记录`，文件为 `doc/tasks/20260730-mes-process-menu-title/{task.md,execution-log.md}`。
- Dirty baseline: `51abcc06 基线: 保存并行 MES 工序菜单标题静态契约`，文件为 `IntRuoyiFronted/tests/e2e/mes-pro-mes-process-readonly-static.spec.js`。
- Dirty baseline: `0809cd85 基线: 保存并行 MES 菜单与 eDHR 页签改动`，包含 MES 菜单 SQL/页面/静态合同及 eDHR 页签组件/静态合同共 5 个文件。
- Dirty baseline: `ec99a8c5 基线: 保存并行测试管理串行路线验证记录`，文件为 `doc/tasks/20260730-test-management-serial-routes-verification/{task.md,execution-log.md}`。
- Dirty baseline: `19f9b782 基线: 保存并行 eDHR 图谱验证记录`，包含 `doc/tasks/20260730-edhr-page-graph-tab/` 下 5 个证据和任务记录文件。
- Main branch baseline pushes: `ca088c6d`、`51abcc06`、`0809cd85` 均已成功推送到 `origin/int_main`；后续基线和本任务最终收尾记录随最终主分支 push 一并推送。
- Closeout sync: 持续把最新 `int_main` 合入 `codex/20260730-banzuzhang`，最终任务分支 HEAD 为 `4d6acc51`，`git merge-base --is-ancestor int_main HEAD` -> PASS。
- Task branch push: `git push origin codex/20260730-banzuzhang` -> PASS，远端分支更新到 `4d6acc51`。
- Cleanup preview GREEN: `task_closeout.py --task-id 20260730-banzuzhang --mode preview` -> `status: ready`，keep 为核心任务文档和 4 个 evidence，delete 为 `<none>`，blocked/warnings 均为 `<none>`。
- Cleanup apply: ff-only merge 到 `E:\IntRuoyi` 的 `int_main` -> PASS；Git worktree 登记已移除。物理目录删除首次失败，错误为 `Invalid argument`，原因是 `yudao-server-exec.jar` 仍被本任务后端进程占用。
- Runtime cleanup: `netstat -ano` 确认 `48098 -> PID 19088`、`8098 -> PID 36928`，均为本任务登记端口；停止两个任务专属进程后，两个端口均不再监听。
- Worktree cleanup: 校验目标绝对路径位于 `D:\IntRuoyiWorktree\` 下且已无 `.git` 后，受控删除 `D:\IntRuoyiWorktree\20260730-banzuzhang` -> PASS；`Test-Path` 为 `False`，`git worktree list --porcelain` 不再包含本任务。
- Slot cleanup: 使用与 `reserve-worktree-slot.ps1` 相同的注册表互斥锁和原子文件替换流程更新 `D:\IntRuoyiWorktree\.ports\worktree-ports.json`；`20260730-banzuzhang` 条目校验为 slot `17`、frontend `8098`、backend `48098` 后设置 `active=false`，`releasedAt/deletedAt=2026-07-30T23:35:46.1992651+08:00`。
- Experience consolidation: 已按 `project-experience-consolidation` 检查长期经验归宿；`docs/worktree-memory.md#Git-注册已移除但物理目录被运行态锁住` 已完整覆盖本次 `Invalid argument`、任务 PID/端口归属核验、停止运行态、删除残留目录和释放登记槽位的门禁，因此无需重复新增长期经验条目。
- Post-merge regression: 在 `int_main` 已包含任务分支并推送 `f59da91d` 后，`node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS。
- Post-merge regression: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderScopeServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesWorkOrderAbnormalReportServiceTest,MesTeamEmployeeBindingServiceTest,MesDefectReasonCatalogServiceTest,MesProcessDeviceParameterRuleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，16 tests，0 failures，0 errors，`BUILD SUCCESS`。
- Post-merge regression: `pnpm ts:check` -> PASS。
- Post-merge int_main E2E preflight: `node scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8081 --target-path /mes/pro/process-pool/team-leader --target-text 工序池班组长工作台` with password supplied from local env and redacted from logs -> PASS，登录 `芋道源码/admin`。
- Post-merge int_main E2E GREEN: 真实 Playwright 页面冒烟 -> PASS；入口 `http://127.0.0.1:8081/mes/pro/process-pool/team-leader`，后端 `http://127.0.0.1:48081/actuator/health` 为 `UP`，切换 `提交看板/异常上报/班组维护/PQC 班组长`，目标提交看板 API 初始与 PQC 切换均 HTTP 200 且业务 `code=0`，无 console error、无 pageerror、无 MES 写请求；截图 `output\playwright\20260730-banzuzhang\team-leader-workbench-int-main-smoke.png`。
- Experience consolidation check: `docs\e2e-rules.md#官方登录前置与-admin-only-全量验证门禁` 与 `docs\experience-index.md` 已覆盖本次融合后 admin-only 真实 E2E、Chrome 显式路径和无 MES 写请求门禁；无需新增长期经验文档。
- Dirty-worktree baselines: 主工作区存在并行任务持续写入；已按规则独立保存非班组长改动为 `57453152`、`1a429537`、`5a22db16`、`ce6854c6`、`91f832da`、`4764cc5b`，当前班组长提交仅选择性暂存 `doc/tasks/20260730-banzuzhang/{task.md,execution-log.md,verification-report.md}`。
- Task status: `completed`；实现、验证、任务分支推送、ff-only 合并、运行进程停止和 worktree 删除均已完成。

## Evidence Files

- Backend API: `doc/tasks/20260730-banzuzhang/backend-api-evidence.md`
- Database schema: `doc/tasks/20260730-banzuzhang/database-schema-evidence.md`
- Frontend feature: `doc/tasks/20260730-banzuzhang/frontend-feature-evidence.md`
- Migration gate output: `doc/tasks/20260730-banzuzhang/migration-policy-gate.json`
