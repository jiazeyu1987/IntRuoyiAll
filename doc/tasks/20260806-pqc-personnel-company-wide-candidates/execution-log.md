# Execution Log

## Intent

用户反馈 `新增 PQC 检验员` 弹窗下拉选择范围应为全公司范围，不应限制为当前组长下属。

## BDD

- BDD: PQC新增候选来自全公司 -> Given 当前 PQC 组长打开新增 PQC 检验员弹窗 / When 输入姓名或账号搜索 / Then 后端使用全公司正式系统用户搜索，不按当前组长下属过滤。
- BDD: PQC提交校验与候选同范围 -> Given 选择的正式用户不是当前组长下属但属于全公司系统用户 / When 提交关联 / Then 后端允许创建 `leader_type=PQC`、`scope_type=EMPLOYEE` 的 scope，重复关联仍在写库前业务拒绝。

## Command Log

- BDD: PQC新增候选来自全公司 -> Given 当前 PQC 组长打开新增 PQC 检验员弹窗 / When 输入姓名或账号搜索 / Then 后端使用全公司正式系统用户搜索，不按当前组长下属过滤。
- BDD: PQC提交校验与候选同范围 -> Given 选择的正式用户不是当前组长下属但属于全公司系统用户 / When 提交关联 / Then 后端允许创建 `leader_type=PQC`、`scope_type=EMPLOYEE` 的 scope，重复关联仍在写库前业务拒绝。
- RED: `node tests/e2e/pqc-leader-personnel-company-wide-candidates-static.spec.js` -> FAIL, old implementation still referenced `getUserListBySubordinate` inside `linkFormalInspector`.
- GREEN: `node tests/e2e/pqc-leader-personnel-company-wide-candidates-static.spec.js` -> PASS, `PASS: PQC personnel company-wide candidate contract`.
- GREEN: `node tests/e2e/pqc-leader-personnel-tab-static.spec.js` -> PASS, `PASS: PQC leader personnel tab static contract`.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` in detached verification worktree -> PASS, `BUILD SUCCESS`, `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`.
- GREEN: `mvn -pl yudao-server -am "-DskipTests" package` in detached verification worktree -> PASS, `BUILD SUCCESS`, produced `yudao-server-exec.jar` and included `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar`.
- GREEN: `jar tf yudao-module-mes-2026.04-SNAPSHOT.jar` -> PASS, found `cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcLeaderPersonnelServiceImpl.class`.
- GREEN: `git diff --check -- <task paths>` -> PASS, no whitespace errors; Git only warned that one Java file will be normalized LF to CRLF when touched.
- CLEANUP: detached verification worktree `D:\IntRuoyiWorktree\pqc-personnel-company-wide-candidates-20260806` removed with `git worktree remove --force`; `Test-Path=False` and no Git worktree registration remains.
- EXPERIENCE: project experience consolidation checked existing worktree/Maven/local-runtime gates; no new long-term experience document was needed because the applicable detached verification worktree and nested jar verification rules already exist.
- BLOCKED: `pnpm ts:check` -> FAIL, unrelated current workspace active-order type mismatch: `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue(3760,7): error TS2353: Object literal may only specify known properties, and 'routeId' does not exist in type 'TeamLeaderActiveOrderAddReqVO'`.
- BLOCKED: current local runtime refresh -> current `48081` PID `17936` runs `E:\IntRuoyi\output\runtime\int_main\backend-runtime-production-formal-users-20260806.jar`; read-only nested MES class inspection found old `getUserListBySubordinate` and `PRO_PROCESS_POOL_TEAM_SCOPE_DENIED` signals. Source and isolated build are fixed, but this running jar has not been replaced or restarted.

## Completed Work

- PQC 新增检验员候选搜索使用全公司系统用户昵称搜索，不再限制当前组长下属。
- PQC 正式检验员关联提交改为校验用户存在和重复 PQC scope，不再调用 `getUserListBySubordinate`。
- 后端接口文案更新为全量系统候选和全公司正式用户关联。
- 后端单测断言关联跨下属用户时调用 `validateUser` 且不调用 `getUserListBySubordinate`。
- 前端静态契约覆盖候选接口、远程搜索弹窗、全公司候选范围和既有 PQC 人员管理 tab 不回归。
- 已删除本任务独立验证 worktree，未启动服务、未登记端口、未影响当前 48081 运行态。

## Remaining Blocker

- `pnpm ts:check` 被当前工作区已有活跃订单相关类型不一致阻塞；该阻塞不在 PQC 候选范围改动内，未修改相关并发任务文件。
- 当前 `48081` 运行态仍是旧 jar。要让页面立即使用全公司 PQC 候选逻辑，需要后续授权替换并重启该本地后端。
