# Execution Log

## User Intent

- 用户确认：产品“球囊扩张压力泵”的默认填写人要按批记录表单逐表单配置为角色。
- 用户确认：在“芋道源码”租户内执行。
- 角色命名口径：例如“粗洗工序生产记录”对应“粗洗工序填写者角色”，“精洗工序生产记录”对应“精洗工序填写者角色”；以此类推，有几个表单创建几个填写者角色。
- 每个填写者角色随机赋予 3 个当前租户正常启用普通账号。

## BDD Scenarios

- `BDD: 每个批记录表单都有对应填写者角色 -> Given “芋道源码”租户中存在产品“球囊扩张压力泵”的批记录表单目录 When 执行默认填写人角色配置 Then 目录内每个非空表单名称都有一个对应的“填写者角色”`
- `BDD: 每个填写者角色绑定三个账号 -> Given 当前租户存在至少 3 个正常启用普通账号 When 为每个表单创建或复用填写者角色 Then 每个角色拥有随机选择的 3 个账号`
- `BDD: 表单默认填写人显示角色 -> Given 目标表单已绑定对应填写者角色 When 打开批记录表单列表或填写人设置小弹窗 Then 默认填写人来源为角色且可继续更换填写人`

## Milestone Notes

- 2026-07-28：创建任务目录，准备核对适用经验门禁、本机运行态、目标租户、产品表单、系统角色/用户与批记录表单填写规则链路。
- PRECHECK: 已读取 `docs/database-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`。
- PRECHECK: 已读取 `docs/experience-index.md`，命中 eDHR 填写人、批记录表单/表单槽位边界、数据库、登录、本机运行态、PowerShell UTF-8 相关门禁，并补入 `task.md`。
- PRECHECK: 已读取历史任务 `doc/tasks/20260725-edhr-pressure-pump-v13-filler-role/` 与 `doc/tasks/20260728-pressure-pump-initial-assist-mapping/`，确认当前本机数据历史上“球囊扩张压力泵”最新 V14.0 为 `batchRecordVersionId=130`、15 张 `MAIN` 批记录表单。

## 2026-07-29 Continuation Evidence

- READONLY: `http://127.0.0.1:48081/actuator/health` -> `UP`；`8081` 前端存在监听；Docker `int-ruoyi-mysql` 与 `int-ruoyi-redis` 运行中。
- READONLY: schema 核对确认 `system_tenant`、`system_users`、`system_role`、`system_user_role`、`system_role_category`、`mes_pro_batch_record_version`、`mes_pro_batch_record_report`、`mes_pro_edhr_process_form_permission_rule` 存在，目标字符列为 `utf8mb4_unicode_ci`。
- READONLY: 租户 `芋道源码` 为 `tenant_id=1`；目标“球囊扩张压力泵”当前 V14.0 为 `batchRecordVersionId=130`，存在 15 张 `MAIN` 批记录表单。
- READONLY: 续跑时目标配置已在本机库中达到业务目标：15 个填写者角色 `910415..910429` 均存在，15 条 form-level `FILL/ALL` 规则均为 `candidateSourceType=ROLE`，每个角色绑定 3 个启用普通账号且无 admin。
- RED: `python -X utf8 doc\tasks\20260728-pressure-pump-batch-record-role-fillers\verify_pressure_pump_role_fillers.py --verify` -> FAIL, expected reason: DB 角色和成员已正确，但旧运行态 `get-by-report` 的 `fillRule.candidateSourceNames` 为空。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest#getRuleByReport_returnsRoleSourceNamesForFormLevelFillRule" test` -> FAIL, expected reason: `candidateSourceNames` expected `[粗洗工序填写者角色]` but was `null`。
- IMPLEMENTATION: `MesProEdhrProcessFormPermissionRuleServiceImpl#toCandidateResp` 复用 `candidateUsers` 并补齐 `resolveCandidateSourceNames(rule.getCandidateSourceType(), sourceIds, candidateUsers)`，使 form-level 默认填写人和辅助填写分配的角色名称回显一致。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest#getRuleByReport_returnsRoleSourceNamesForFormLevelFillRule" test` -> PASS。
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest" test` -> PASS, `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`。
- BUILD: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS。
- RUNTIME: `restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS；新 PID `52824`，运行 jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260729-001727.jar`，SHA256 `91583596BAFA1979F385279430DE448D6137A38D00C238B86354D49B454D00AB`，48081 health `UP`。
- GREEN: `python -X utf8 doc\tasks\20260728-pressure-pump-batch-record-role-fillers\verify_pressure_pump_role_fillers.py --verify` -> PASS, `reports=15 roles=15 usersPerRole=3 apiVerified=15`。
- E2E: `node doc\tasks\20260728-pressure-pump-batch-record-role-fillers\pressure_pump_role_filler_ui_readonly.e2e.js` -> PASS, 真实前端列表行和“批记录表单填写人设置”弹窗均显示 `粗洗工序填写者角色`。
- EXPERIENCE: 已将“form-level ROLE 必须返回 candidateSourceNames”合并到 `docs/backend-development.md#批记录表单角色填写人名称回显边界`，并在 `docs/experience-index.md` 增加关键词路由。
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260728-pressure-pump-batch-record-role-fillers\bug-regression-evidence.md` -> PASS。
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-pressure-pump-batch-record-role-fillers --mode preview` -> PASS, keep 正式记录、验证脚本与验证 JSON；delete 仅本任务 `__pycache__`、旧 `backend-api-evidence.md`、旧 `verify_pressure_pump_role_fillers_real.e2e.js`、旧 `pressure-pump-role-fillers-real-e2e.png`。
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-pressure-pump-batch-record-role-fillers --mode apply` -> PASS。
- CLOSEOUT: 保留脚本检查显示 `configure_pressure_pump_role_fillers.mjs` 被 `.gitignore:98:doc/tasks/**/*.mjs` 忽略；若后续提交该脚本，需要使用 `git add -f` 明确暂存。
- CLOSEOUT: `task.md` 当前状态保持 `ready_for_closeout`；最终 `git status --short --branch --untracked-files=all` 显示仓库当前 `int_main` 落后 `origin/int_main` 22 个提交且存在多个无关脏改动，本任务暂不提交和推送，避免混入并行改动。
- BASELINE: 实施前工作区已有大量无关脏改动与并行任务进程；本任务只触碰 `MesProEdhrProcessFormPermissionRuleServiceImpl`、对应单测和 `doc/tasks/20260728-pressure-pump-batch-record-role-fillers/`，不回退、不提交无关文件。
- PRECHECK: `http://127.0.0.1:48081/actuator/health` 与 `http://127.0.0.1:8081/` 可用；目标租户为“芋道源码” tenantId=1。
- PRECHECK: 只读核对“球囊扩张压力泵”最新版本 V14.0，批记录表单目录共 15 张表单；角色分类“批记录/batch-record”存在且启用；当前租户可选启用普通账号数大于 3。

## RED / APPLY / GREEN

- `RED: node doc\tasks\20260728-pressure-pump-batch-record-role-fillers\configure_pressure_pump_role_fillers.mjs --verify -> FAIL, expected reason: 15 个对应填写者角色缺失，目标表单仍存在历史个人填写人配置或未返回角色填写规则。`
- `APPLY: node doc\tasks\20260728-pressure-pump-batch-record-role-fillers\configure_pressure_pump_role_fillers.mjs --apply -> PARTIAL DATA APPLIED, created/reused 15 roles and saved role fill rules; post-verify failed because running backend jar did not return fillRule.candidateSourceNames for ROLE source.`
- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest#getRuleByReport_returnsRoleSourceNamesForFormLevelFillRule" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason: fillRule.candidateSourceNames was null for form-level ROLE fill rule.`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest#getRuleByReport_returnsRoleSourceNamesForFormLevelFillRule" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS.`
- `REGRESSION: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> first run FAIL because 3 existing ROLE-source tests missed roleApi.getRoleList stubs; fixed test fixtures, second run PASS, 33 tests.`
- `GREEN: node doc\tasks\20260728-pressure-pump-batch-record-role-fillers\configure_pressure_pump_role_fillers.mjs --verify -> PASS, verified tenantId=1, product=球囊扩张压力泵, version=V14.0, reportCount=15, each role has assignedUserCount=3.`
- `GREEN: node --check doc\tasks\20260728-pressure-pump-batch-record-role-fillers\verify_pressure_pump_role_fillers_real.e2e.js -> PASS.`
- `GREEN: node doc\tasks\20260728-pressure-pump-batch-record-role-fillers\verify_pressure_pump_role_fillers_real.e2e.js -> PASS, verified real page row “粗洗工序生产记录” displays “角色：粗洗工序填写者角色” and opens “批记录表单填写人设置” dialog.`
- `GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260728-pressure-pump-batch-record-role-fillers/backend-api-evidence.md -> PASS, Backend API evidence is valid.`
- `CLEANUP: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-pressure-pump-batch-record-role-fillers --mode preview -> PASS, delete=<none>, blocked=<none>.`
- `CLEANUP: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-pressure-pump-batch-record-role-fillers --mode apply -> PASS, deleted_paths=<none>.`

## Runtime Evidence

- Standard local backend rebuild/restart produced `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260729-001727.jar`, SHA256 `91583596BAFA1979F385279430DE448D6137A38D00C238B86354D49B454D00AB`.
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health -> {"status":"UP"}` after reload.
- Frontend `http://127.0.0.1:8081/` returned HTTP 200 before real E2E.
- Real E2E screenshot: `doc/tasks/20260728-pressure-pump-batch-record-role-fillers/pressure-pump-role-fillers-real-e2e.png`.
- Backend API evidence validator: PASS.
- Task closeout cleanup preview/apply: PASS, no task-owned files deleted.
- `rg -n "eDHR 批记录表单角色填写人|candidateSourceNames 空|批记录表单角色填写人名称回显边界" docs\experience-index.md docs\backend-development.md` -> PASS, experience route locates `docs/backend-development.md#批记录表单角色填写人名称回显边界`.
- `git diff --check` -> PASS with line-ending normalization warnings only; no whitespace errors.
- `git status --short --branch` -> `int_main...origin/int_main [behind 22]` with multiple unrelated dirty files and untracked parallel task directories; commit/push blocked for this task.

## Configured Roles

- 产品信息 -> 产品信息填写者角色, roleId=910415, assignedUserCount=3。
- 粗洗工序生产记录 -> 粗洗工序填写者角色, roleId=910416, assignedUserCount=3。
- 精洗工序生产记录 -> 精洗工序填写者角色, roleId=910417, assignedUserCount=3。
- 清洗工序生产记录 -> 清洗工序填写者角色, roleId=910418, assignedUserCount=3。
- 清洁工序生产记录 -> 清洁工序填写者角色, roleId=910419, assignedUserCount=3。
- 组装Ⅰ工序生产记录 -> 组装Ⅰ工序填写者角色, roleId=910420, assignedUserCount=3。
- 光固Ⅰ工序生产记录 -> 光固Ⅰ工序填写者角色, roleId=910421, assignedUserCount=3。
- 硅化Ⅰ工序生产记录 -> 硅化Ⅰ工序填写者角色, roleId=910422, assignedUserCount=3。
- 硅化Ⅱ工序生产记录 -> 硅化Ⅱ工序填写者角色, roleId=910423, assignedUserCount=3。
- 组装Ⅱ工序生产记录 -> 组装Ⅱ工序填写者角色, roleId=910424, assignedUserCount=3。
- 检测工序生产记录 -> 检测工序填写者角色, roleId=910425, assignedUserCount=3。
- 光固Ⅱ工序生产记录 -> 光固Ⅱ工序填写者角色, roleId=910426, assignedUserCount=3。
- 单包装工序生产记录 -> 单包装工序填写者角色, roleId=910427, assignedUserCount=3。
- 中包装工序生产记录 -> 中包装工序填写者角色, roleId=910428, assignedUserCount=3。
- 大包装工序生产记录 -> 大包装工序填写者角色, roleId=910429, assignedUserCount=3。

## Remaining Closeout Note

- 代码、数据和真实页面验证已通过；因主工作区存在大量非本任务脏改动、并行任务进程且当前分支落后 `origin/int_main`，未执行任务提交/推送，避免把无关改动混入本任务提交。
