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

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_frontline_pqc_menu_sql.py -q` -> FAIL，新增断言捕获 PQC 菜单迁移缺少 `2 AS type`，会导致 `INSERT` 列和值数量错位。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_frontline_pqc_menu_sql.py -q` -> PASS，3 passed。
- GREEN: `node tests\e2e\edhr-frontline-pqc-tab-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> PASS。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_frontline_pqc_menu_sql.py IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py -q` -> PASS，6 passed。
- GREEN: `node tests\e2e\edhr-frontline-pqc-menu-real.e2e.js` -> PASS，`芋道源码/admin` 可见并打开“一线PQC”。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `run-release-migration-policy-gate.py` with explicit dependency closure -> PASS，`migrationCount=19`。
- GREEN: `git diff --check -- <task-owned files>` -> PASS。

## Milestone Updates

- M0: 发现开始前工作区已有非本任务脏改动；按规则独立提交基线 `4cd8ec941`，未混入本任务文件。
- M1: 读取 `docs/experience-index.md` 后命中动态菜单页签重命名门禁和 MES PQC 项目级检验快照门禁；已写入 `task.md`，本任务限定为入口/页签拆分，不改 PQC 业务事实链路。
- M2: 新增/更新静态合同与 SQL 合同，覆盖内部 tab 移除、独立路由、菜单迁移、租户套餐和 admin 角色绑定。
- M3: 前端移除 `EdhrBatchRecordTabs.vue` 内部 `PQC填写` tab；`BatchPqcFillPage.vue` 改为独立标题 `一线PQC` 并继续复用正式 PQC 面板；路由标题更新为 `一线PQC`；新增 `20260805_mes_edhr_frontline_pqc_menu.sql`。
- M4: 本机执行 `20260804_mes_edhr_qa_menu.sql` 与 `20260805_mes_edhr_frontline_pqc_menu.sql` 后，DB 核对 `system_menu.id=900438` 的 `HEX(name)=E4B880E7BABF505143`，并确认默认 admin 可见范围角色已绑定 `menu_id=900438`。
- M4: Playwright 真实路径使用本机 `芋道源码/admin` 登录，权限响应包含 `一线PQC`，左侧菜单可见并点击进入 `/mes/pro/feedback/edhr-batch-pqc-fill`，页面标题可见，内部批次执行 tabs 数量为 0。
- M4: 本轮发现并修复目标迁移 `INSERT` 缺少 `type` 值的问题；新增 SQL 合同防止复发。
- M5: 已进入 `ready_for_closeout`，但全量 migration gate 和全量 `git diff --check` 被无关文件阻塞，暂未做最终提交/推送。

## Blockers

- Full `run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql` blocked by unrelated `20260805_erp_nas_table_auto_sync.sql` metadata `type=schema,job`。
- Full `git diff --check` blocked by unrelated conflict markers in backend Java tests and `docs/powershell-memory.md`。
- 本任务拥有文件的定向检查、类型检查、迁移依赖闭包门禁和 admin 真实可见性验证均已通过。
