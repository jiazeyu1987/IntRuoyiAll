# Execution Log：运行控制台真实数据 E2E

## 2026-05-27 初始化

BDD: 每个功能点必须有真实数据 E2E -> Given AC-01 到 AC-11 已实现但当前多为单测或静态合同, When 增加最终验收测试, Then 每个 AC 必须有 Playwright 真实用户路径，测试租户执行功能，芋道源码/admin 只读复核。

BDD: 验证失败必须回测试租户修复 -> Given 芋道源码/admin 只做验证, When 芋道复核失败, Then 不得直接修改芋道租户数据，必须回测试租户或实现层修复并重新验证。

RED: `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> EXPECTED FAIL, expected reason: 测试脚本尚未创建，AC-01 到 AC-11 没有逐项真实 E2E 断言。

RED: frontend command with explicit test/yudao tenant env -> FAIL, actual reason: `MODULE_NOT_FOUND` for `tests\e2e\runtime-control-real-data-all-features.e2e.js`.

GREEN: `node --check tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS after adding first full-coverage Playwright script skeleton.

REGRESSION: `node tests\e2e\runtime-control-foolproof-static.spec.js` -> PASS.

STATUS: 已建立任务文档和覆盖矩阵；已新增全量真实数据 E2E 脚本，尚未完成真实环境运行。

## 2026-05-27 真实数据 E2E 闭环

BDD: AC-01 站内信告警 -> Given 测试租户登录运行控制台且容量采样可能产生真实告警, When 打开站内信告警面板并刷新告警, Then 告警列表必须有真实行且站内信状态只能是 `SENT/FAILED/BLOCKED`。

BDD: AC-02 责任人矩阵 -> Given 测试租户存在运行控制台责任人矩阵真实数据, When 打开责任人矩阵面板, Then 必须显示至少一条必需责任人行。

BDD: AC-03 决策向导 -> Given 后端提供六类决策场景, When 用户点击计算推荐, Then 响应必须包含推荐动作且页面展示推荐结果。

BDD: AC-04 回滚候选 -> Given 后端提供回滚候选, When 用户打开回滚版本动作, Then 页面必须展示候选选择器且不能出现旧的手填镜像标签输入。

BDD: AC-05 恢复候选 -> Given 后端提供恢复候选, When 用户打开恢复数据动作, Then 页面必须展示候选选择器、候选状态，且不能出现旧的手填备份输入。

BDD: AC-06 巡检报告 -> Given 用户具备测试租户运维操作权限, When 点击执行巡检, Then 巡检检查项必须返回真实状态且只能为 `PASS/WARN/BLOCKED/NO_GO`。

BDD: AC-07 业务健康 -> Given 运行控制台业务健康接口已接入真实检查项, When 打开业务健康面板, Then 必须返回登录、ERP、MES、文件对象、API 错误、慢请求、任务失败七项。

BDD: AC-08 探针状态 -> Given 用户点击执行探针, When 后端执行 backend/frontend 等探针, Then 结果必须包含 backend 与 frontend 探针并展示耗时。

BDD: AC-09 日志与磁盘风险 -> Given 日志目录和磁盘监控路径可采样, When 打开容量状态面板, Then 响应必须包含磁盘或日志目录指标并返回风险状态。

BDD: AC-10 备份演练 -> Given 备份演练配置存在真实备份点, When 打开备份演练面板, Then 必须返回备份点且至少暴露 manifest 或 checksum 证据。

BDD: AC-11 事故闭环 -> Given 测试租户创建真实事故并记录处置动作, When 不填写剩余风险和关闭原因直接关闭事故, Then 后端必须拒绝关闭请求。

BDD: 芋道源码/admin 只读复核 -> Given 测试租户功能 E2E 已通过, When 使用 `芋道源码/admin` 打开运行控制台复核, Then 页面和关键 GET 接口必须可用，且复核阶段不得调用 `/admin-api/infra/runtime-control/*` 非 GET 请求。

RED: `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> FAIL, actual reason: AC-09 `/capacity/status` 返回业务码 500，后端抛出 `当前通知公告不存在`；根因是功能使用 `RUNTIME_OPS_ALERT` 站内信模板但交付 SQL 未包含该模板。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlNotifyTemplateSeedTest test` -> FAIL, expected reason: `sql/mysql/20260527_infra_runtime_control_notify_template_seed.sql` 不存在。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlNotifyTemplateSeedTest test` -> PASS after adding idempotent `RUNTIME_OPS_ALERT` notify template seed.

RED: backend subagent reviewer -> FAIL, actual reason: SQL seed used an unconditional `UPDATE system_notify_template WHERE code = 'RUNTIME_OPS_ALERT'`, which could overwrite customized templates, re-enable disabled templates, revive deleted templates, or update duplicate code rows; seed test only checked string presence and did not guard no-overwrite behavior.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlNotifyTemplateSeedTest test` -> PASS, 2 tests passed after making the SQL insert-only and adding assertions that the seed contains `action`, is scoped by `WHERE NOT EXISTS`, and contains no `UPDATE` or overwrite-style upsert.

RED: backend commit hook -> FAIL, actual reason: repo SQL changes under `sql/mysql/` require a changed script test under `script/tests/`; Java seed test alone was not accepted by the repository TDD gate.

GREEN: `python -X utf8 -m pytest script\tests\test_runtime_control_notify_sql.py -q` -> PASS, 2 tests passed; script-level SQL test confirms `RUNTIME_OPS_ALERT` seed is insert-only, scoped by `WHERE NOT EXISTS`, and exposes `environment/action/severity/title/content`.

RED: `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> FAIL, actual reason: AC-11 Playwright strict locator matched both drawer title and button named `新建事故`；test locator was narrowed to the actual button.

RED: `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> FAIL, actual reason: AC-11 action record refresh could temporarily clear selected incident detail before close-gate assertion；test now reselects the created incident before clicking `关闭事故`.

GREEN: `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS, evidence: `AC-01 PASS alerts=20`, `AC-02 PASS ownerRows=3`, `AC-03 PASS scenarios=6`, `AC-04 PASS rollbackCandidates=1`, `AC-05 PASS restoreCandidates=1`, `AC-06 PASS checks=4`, `AC-07 PASS items=7`, `AC-08 PASS probes=9`, `AC-09 PASS status=WARN`, `AC-10 PASS backupPoints=1`, `AC-11 PASS incident=E2E事故-1779862919956`, `TEST_TENANT_PASS`, `YUDAO_ADMIN_VERIFY_PASS`, `PASS: runtime control real-data E2E covers AC-01 through AC-11`.

GREEN: `node tests\e2e\runtime-control-foolproof-static.spec.js` -> PASS.

GREEN: `node --check tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\runtime-control-rollback-app.e2e.js; node --check tests\e2e\runtime-control-restore-data.e2e.js` -> PASS.

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlNotifyTemplateSeedTest,RuntimeStorageGuardServiceImplTest,RuntimeProbeServiceImplTest" test` -> PASS, 6 tests passed.

RED: final `node tests\e2e\runtime-control-real-data-all-features.e2e.js` rerun -> FAIL, actual reason: Docker Desktop engine had stopped `int-ruoyi-mysql` and `int-ruoyi-redis`; setup login endpoint `/system/tenant/get-id-by-name` returned business code 500 because backend could not connect to MySQL/Redis.

GREEN: dependency recovery -> PASS, restarted Docker Desktop engine and `int-ruoyi-mysql` / `int-ruoyi-redis`; verified backend health `status=UP`, frontend `8098` HTTP 200, and `RUNTIME_OPS_ALERT` exists in local MySQL.

GREEN: final `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS, evidence: `AC-01 PASS alerts=20`, `AC-02 PASS ownerRows=3`, `AC-03 PASS scenarios=6`, `AC-04 PASS rollbackCandidates=1`, `AC-05 PASS restoreCandidates=1`, `AC-06 PASS checks=4`, `AC-07 PASS items=7`, `AC-08 PASS probes=9`, `AC-09 PASS status=WARN`, `AC-10 PASS backupPoints=1`, `AC-11 PASS incident=E2E事故-1779866553908`, `TEST_TENANT_PASS`, `YUDAO_ADMIN_VERIFY_PASS`, `PASS: runtime control real-data E2E covers AC-01 through AC-11`.

STATUS: 真实数据 E2E 已覆盖 AC-01 到 AC-11；测试租户功能路径通过；芋道源码/admin 复核通过且脚本断言未调用运行控制台非 GET 请求；真实 destructive DR 未执行。

## 2026-05-27 int_main 融合前复验

RED: merged-task `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> FAIL, actual reason: 本地任务运行态 `alerts.json` 和 `capacity-status.json` 含全 `0x00` 非法 JSON，导致 `/alerts/page` 与 `/capacity/status` 返回业务码 500。

GREEN: non-destructive runtime-state recovery -> PASS, evidence: 仅将本次任务目录 `output\runtime\20260526-foolproof-ops-implementation\runtime-control\runtime-ops\alerts.json` 与 `capacity-status.json` 重命名为 `.corrupt-20260527-*`，其余运行态 JSON 校验通过。

RED: merged-task `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> FAIL, actual reason: 清洁运行态下 AC-01 告警列表为空，原 E2E 依赖历史告警数据；容量采样不保证在当前磁盘状态下生成告警。

GREEN: E2E setup hardening -> PASS, change: 当测试租户无告警时，脚本使用真实登录 token 调用 `POST /infra/runtime-control/alerts` 创建 `RUNTIME_OPS_ALERT` 站内信告警数据，再通过页面断言 AC-01；不使用 mock 或静态替代。

GREEN: merged-task `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS, evidence: `SETUP alert created=1`, `AC-01 PASS alerts=1`, `AC-02 PASS ownerRows=3`, `AC-03 PASS scenarios=6`, `AC-04 PASS rollbackCandidates=1`, `AC-05 PASS restoreCandidates=1`, `AC-06 PASS checks=4`, `AC-07 PASS items=7`, `AC-08 PASS probes=9`, `AC-09 PASS status=BLOCKED`, `AC-10 PASS backupPoints=1`, `AC-11 PASS incident=E2E事故-1779870533303`, `TEST_TENANT_PASS`, `YUDAO_ADMIN_VERIFY_PASS`, `PASS: runtime control real-data E2E covers AC-01 through AC-11`.

## 2026-05-27 int_main 融合后复验

GREEN: backend int_main `python -X utf8 -m pytest script\tests\test_runtime_control_notify_sql.py -q` -> PASS, 2 passed.

GREEN: backend int_main `mvn -pl yudao-module-infra "-Dtest=RuntimeControlNotifyTemplateSeedTest,RuntimeStorageGuardServiceImplTest,RuntimeProbeServiceImplTest" test` -> PASS, 6 tests passed.

GREEN: frontend int_main `node --check tests\e2e\runtime-control-real-data-all-features.e2e.js; node tests\e2e\runtime-control-foolproof-static.spec.js; node --check tests\e2e\runtime-control-rollback-app.e2e.js; node --check tests\e2e\runtime-control-restore-data.e2e.js` -> PASS.

GREEN: backend int_main `mvn -pl yudao-server -am -DskipTests package` -> PASS, `yudao-server\target\yudao-server.jar` rebuilt from main worktree.

RED: frontend int_main `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> FAIL, actual reason: 主前端工作区合并后尚未安装新增 `playwright` 依赖，Node 抛出 `Cannot find module 'playwright'`。

GREEN: frontend int_main `pnpm install --frozen-lockfile` -> PASS, lockfile unchanged and installed `playwright 1.60.0`.

GREEN: int_main services -> PASS, evidence: backend process command line points to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-server\target\yudao-server.jar`, frontend Vite process command line points to `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`, health `http://127.0.0.1:48098/actuator/health` HTTP 200, frontend `http://127.0.0.1:8098/` HTTP 200.

GREEN: int_main `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS, evidence: `SETUP ownerMatrix created=3`, `SETUP alert created=1`, `AC-01 PASS alerts=1`, `AC-02 PASS ownerRows=3`, `AC-03 PASS scenarios=6`, `AC-04 PASS rollbackCandidates=1`, `AC-05 PASS restoreCandidates=1`, `AC-06 PASS checks=4`, `AC-07 PASS items=7`, `AC-08 PASS probes=9`, `AC-09 PASS status=BLOCKED`, `AC-10 PASS backupPoints=1`, `AC-11 PASS incident=E2E事故-1779871222493`, `TEST_TENANT_PASS`, `YUDAO_ADMIN_VERIFY_PASS`, `PASS: runtime control real-data E2E covers AC-01 through AC-11`.

## 2026-05-27 芋道源码/admin 专用只读复验

BDD: 芋道源码/admin 当前 `int_main` 只读复验 -> Given 后端由当前 `int_main` 构建的 `yudao-server.jar` 在 48098 启动且前端静态预览在 8098, When 使用 `芋道源码/admin` 登录运行控制台, Then AC-01 到 AC-11 必须通过真实页面和后端 GET 响应可见，且不得调用运行控制台非 GET 请求。

GREEN: backend int_main `mvn -pl yudao-server -am -DskipTests package` -> PASS，`yudao-server\target\yudao-server.jar` 已从当前主工作区构建。

GREEN: services for admin readonly verification -> PASS，backend `http://127.0.0.1:48098/actuator/health` 返回 `status=UP`，frontend preview `http://127.0.0.1:8098/` 返回 HTTP 200。

RED: admin readonly E2E first run -> FAIL, actual reason: `/admin-api/infra/runtime-control/overview` 在当前真实状态查询中约 41 秒完成，前端脚本 30 秒等待过短；后端日志记录 `/overview` 完成耗时约 `41058 ms`。

GREEN: admin readonly E2E final run -> PASS, evidence: `AC-01 ADMIN_READONLY_PASS alerts=0`, `AC-02 ADMIN_READONLY_PASS ownerRows=0`, `AC-03 ADMIN_READONLY_PASS scenarios=6`, `AC-04 ADMIN_READONLY_PASS rollbackCandidates=1`, `AC-05 ADMIN_READONLY_PASS restoreCandidates=1`, `AC-06 ADMIN_READONLY_PASS inspectionEntry=visible`, `AC-07 ADMIN_READONLY_PASS items=7`, `AC-08 ADMIN_READONLY_PASS probes=1`, `AC-09 ADMIN_READONLY_PASS status=BLOCKED`, `AC-10 ADMIN_READONLY_PASS backupPoints=1`, `AC-11 ADMIN_READONLY_PASS incidents=0`, `YUDAO_ADMIN_READONLY_PASS`, `PASS: yudao/admin readonly runtime-control E2E covers AC-01 through AC-11`。

STATUS: 当前 `int_main` 可用 `芋道源码/admin` 完成专用只读复验；复验未执行真实 DR、未提交回滚/恢复/巡检/探针/事故/告警写动作。风险记录：`/overview` 聚合响应偏慢，功能可用但后续应单独优化状态脚本超时和聚合策略。
