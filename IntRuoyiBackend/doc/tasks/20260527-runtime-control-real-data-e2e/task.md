# 任务：运行控制台十项能力真实数据 E2E 覆盖

## 任务目标

- 为 AC-01 到 AC-11 每个功能点增加真实数据 Playwright E2E 验证。
- 默认在测试租户 `测试租户/aoteman` 上测试功能，发现失败只能回到测试租户修复测试数据、权限或实现。
- 使用 `芋道源码/admin` 做只读验证，确认功能在默认管理租户可见且不因测试租户修复造成回归。
- 不使用 mock、接口捷径、静态合同或 `node --check` 冒充 E2E。
- 不执行真实 destructive DR；真实 DR 仍受上一任务 scope waiver 限制，后续只验证门禁和只读证据。

## Worktree

- 后端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-implementation\ruoyi-vue-pro`
- 前端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-implementation\yudao-ui-admin-vue3`
- 分支：`task/20260526-foolproof-ops-implementation`

## 前置任务状态

- `doc/tasks/20260526-foolproof-ops-implementation/task-state.json` 为 `completed`。
- 上一任务最终结论为 `PASS_WITH_SCOPE_WAIVER`。
- 真实 DR 未执行，不能声明 `REAL_DR_VERIFIED`。

## 里程碑

- [x] M1：确认上一任务已完成且 worktree 干净。
- [x] M2：建立本任务文档、覆盖矩阵和 TDD 计划。
- [x] M3：写入 AC-01 到 AC-11 真实数据 E2E 的 RED 测试。
- [x] M4：补齐 E2E helper、真实数据探测和只读校验能力。
- [x] M5：在测试租户运行全量 E2E；失败时回测试租户修复。
- [x] M6：在 `芋道源码/admin` 执行只读复核。
- [x] M7：独立 reviewer 审查、按复审意见修复并更新证据。

## 当前状态

- 状态：completed
- 当前阶段：M7
- 当前结论：AC-01 到 AC-11 均已有真实数据 Playwright E2E 覆盖，并在测试租户通过；`芋道源码/admin` 复核通过且脚本断言复核阶段未调用运行控制台非 GET 请求；已融合进前后端 `int_main` 并在主工作区服务上完成最终 E2E；新增 `芋道源码/admin` 专用只读 E2E 复验通过。
- 注意：真实 destructive DR 未执行，仍不能声明 `REAL_DR_VERIFIED`。
- 注意：`GET /capacity/status` 当前会保存采样快照，超阈值时还可能生成容量告警；严格零副作用读模型不在本任务范围内。

## 最终验证

- `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS，输出 `AC-01 PASS` 到 `AC-11 PASS`、`TEST_TENANT_PASS`、`YUDAO_ADMIN_VERIFY_PASS`；融合前复验最终事故证据为 `E2E事故-1779870533303`，并在清洁运行态下通过真实 `POST /infra/runtime-control/alerts` 准备 AC-01 告警。
- `node tests\e2e\runtime-control-foolproof-static.spec.js` -> PASS。
- `node --check tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS。
- `node --check tests\e2e\runtime-control-rollback-app.e2e.js` -> PASS。
- `node --check tests\e2e\runtime-control-restore-data.e2e.js` -> PASS。
- `mvn -pl yudao-module-infra -Dtest=RuntimeControlNotifyTemplateSeedTest test` -> PASS，2 tests passed；确认 `RUNTIME_OPS_ALERT` seed 存在且不会覆盖、启用或复活已有模板。
- `python -X utf8 -m pytest script\tests\test_runtime_control_notify_sql.py -q` -> PASS，2 tests passed；满足 SQL 变更脚本级 TDD 门禁。
- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlNotifyTemplateSeedTest,RuntimeStorageGuardServiceImplTest,RuntimeProbeServiceImplTest" test` -> PASS，6 tests passed。
- `int_main` 合并后复验：`mvn -pl yudao-server -am -DskipTests package` -> PASS；主工作区 48098/8098 服务启动通过；`node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS，最终事故证据为 `E2E事故-1779871222493`。
- `int_main` 芋道源码/admin 专用只读复验：`node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS，输出 `AC-01 ADMIN_READONLY_PASS` 到 `AC-11 ADMIN_READONLY_PASS`、`YUDAO_ADMIN_READONLY_PASS`；脚本断言无运行控制台非 GET 请求。

## 放行标准

- AC-01 到 AC-11 每项至少有一个 Playwright 真实用户路径断言。
- 测试租户执行路径必须使用真实登录、真实页面、真实后端响应；缺数据必须失败并记录。
- `芋道源码/admin` 复核只读可见性和关键状态，不修改 live 租户数据。
- 所有 E2E 脚本必须显式要求 `RUNTIME_CONTROL_E2E_BASE_URL` 和需要动作校验时的 `RUNTIME_CONTROL_E2E_ACTION_ORIGIN`。
- 真实 DR 仍不执行；只验证 fail-fast、候选和只读证据。
- `execution-log.md` 必须记录每项 BDD、RED、GREEN 和双租户验证证据。

## Cleanup Keep

- `doc/tasks/20260527-runtime-control-real-data-e2e/task.md`
- `doc/tasks/20260527-runtime-control-real-data-e2e/test-plan.md`
- `doc/tasks/20260527-runtime-control-real-data-e2e/execution-log.md`
- `doc/tasks/20260527-runtime-control-real-data-e2e/task-state.json`
- `doc/tasks/20260527-runtime-control-real-data-e2e/test-report.md`
- `doc/tasks/20260527-runtime-control-real-data-e2e/review-report.md`
- `doc/tasks/20260527-runtime-control-real-data-e2e/verification-report.md`
- `doc/tasks/20260527-runtime-control-real-data-e2e/subagent-review-log.md`
