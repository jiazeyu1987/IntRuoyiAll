# Execution Log

## User Intent

- 用户要求删除红框中导致填写人加载失败的 86 条多余规则，最终保留 1 条。

## BDD

- `BDD: 清理粗洗表单多余填写人规则 -> Given 目标租户、报表和版本下存在 87 条启用填写人规则且查询接口因多结果失败 When 按可追溯依据保留 1 条并删除其余 86 条 Then 数据库只剩 1 条正式规则，接口返回成功且页面不再显示加载失败。`

## Reproduction And Root Cause

- 目标产品：`CODX-VFC-20260726-批记录`。
- 目标表单：`粗洗工序生产记录`。
- 报表 ID：`249d8d8d9b3f4041a3e71951bf603a19`。
- 批记录版本 ID：`134`。
- 接口：`GET /admin-api/mes/pro/edhr-process-form-permission-rule/get-by-report`。
- `RED: 登录态页面与后端日志 -> FAIL, 接口返回 code=500；MyBatis selectOne 预期 1 条但查到 87 条，抛出 TooManyResultsException`。
- 当前 87 条均为 `scope_key=CODX_VFC_ASSIST_1..87`，其中用户 `795` 为 44 条、用户 `810` 为 43 条，没有 `scope_key=ALL` 的正式表单级规则。
- 规则形态与 `edhr-visual-fill-config-real-flow.e2e.js` 生成的辅助行测试数据一致。

## Preflight

- 已读取 `docs/database-rules.md`、`docs/task-closeout-rules.md`、
  `docs/powershell-encoding.md`、`docs/powershell-memory.md`、
  `docs/server-access.md`、`docs/release-backup-restore.md`。
- 已读取 `docs/experience-index.md`，命中业务数据精确删除和并发恢复门禁。
- 已读取 `bug-regression-fix-loop` 与 bug evidence contract。
- 本次只操作本地 Docker MySQL `int-ruoyi-mysql/ruoyi-vue-pro`，不访问远端环境。
- 数据写入前已核对真实 schema、目标租户、目标版本和精确删除范围。
- `GREEN: experience-preflight -> PASS, 已明确精确范围、备份、并发检查、事务影响行数和回滚门禁`。

## Git Preflight

- 当前分支：`int_main`，跟踪 `origin/int_main`。
- 工作区存在多个并行任务的既有脏改动；本任务文件不得混入脏工作区基线提交。
- 脏工作区基线提交：`cfbd6a38`，包含提交前既有的 35 个非本任务文件。
- 基线提交后出现并行任务继续修改
  `IntRuoyiFronted/src/views/form-center/template/index.vue`；该文件与本任务无关，不修改、不暂存。
- 基线提交完整文件清单：
  `.review-fix-loop/runs/20260727T110834Z-6f3e83/worker/result-round-1.md`、
  `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/formcenter/FormCenterController.java`、
  `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeService.java`、
  `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java`、
  `IntRuoyiBackend/yudao-module-bpm/src/test/java/cn/iocoder/yudao/module/bpm/formcenter/controller/FormCenterRuntimeContractTest.java`、
  `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteFlowConfigServiceImpl.java`、
  `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteFlowConfigServiceImplTest.java`、
  `IntRuoyiFronted/src/api/form-center/template.ts`、
  `IntRuoyiFronted/src/router/modules/remaining.ts`、
  `IntRuoyiFronted/src/views/form-center/template/index.vue`、
  `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`、
  `IntRuoyiFronted/tests/e2e/form-template-button-interaction-parity-static.spec.js`、
  `IntRuoyiFronted/tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js`、
  `IntRuoyiFronted/tests/e2e/mes-route-flow-batch-record-form-source-static.spec.js`、
  `IntRuoyiFronted/tests/e2e/mes-route-flow-batch-record-item-restore-static.spec.js`、
  `IntRuoyiFronted/tests/e2e/mes-route-flow-batch-record-panel-visible-static.spec.js`、
  `IntRuoyiFronted/tests/e2e/mes-route-flow-binding-border-static.spec.js`、
  `IntRuoyiFronted/tests/e2e/mes-route-flow-clickable-detail-values-static.spec.js`、
  `IntRuoyiFronted/tests/e2e/mes-route-flow-legacy-batch-record-detail-static.spec.js`、
  `doc/tasks/20260727-codex-runner-token-invalid/execution-log.md`、
  `doc/tasks/20260727-codex-test-node-chain/execution-log.md`、
  `doc/tasks/20260727-codex-test-node-chain/local-node-chain-assignment.sql`、
  `doc/tasks/20260727-codex-test-node-chain/task.md`、
  `doc/tasks/20260727-edhr-notify-all-valid-candidates/backend-api-evidence.md`、
  `doc/tasks/20260727-edhr-notify-all-valid-candidates/bug-regression-evidence.md`、
  `doc/tasks/20260727-edhr-notify-all-valid-candidates/execution-log.md`、
  `doc/tasks/20260727-edhr-notify-all-valid-candidates/task.md`、
  `doc/tasks/20260727-edhr-notify-all-valid-candidates/verification-report.md`、
  `doc/tasks/20260727-edhr-release-owner-from-end-config/execution-log.md`、
  `doc/tasks/20260727-edhr-release-owner-from-end-config/task.md`、
  `doc/tasks/20260727-edhr-release-owner-from-end-config/verification-report.md`、
  `doc/tasks/20260727-form-template-button-interaction-parity/execution-log.md`、
  `doc/tasks/20260727-form-template-button-interaction-parity/task.md`、
  `doc/tasks/20260727-route-flow-batch-record-form-source/execution-log.md`、
  `doc/tasks/20260727-route-flow-batch-record-form-source/task.md`。

## Retention Decision

- 用户确认产品 `CODX-VFC-20260726-批记录`、版本 `V1.0`、表单“粗洗工序生产记录”
  的目标 87 条中，最终只保留一条填写人“王歆”。
- 目标 87 条分布为：贾泽宇 `44` 条、王歆 `43` 条。
- 选择最早的王歆物理记录 `id=1033` 作为保留载体。
- 保留记录将规范为 `scope_key=ALL`、`candidate_source_type=USERS`、
  `candidate_source_ids=810`、`completion_policy=ANY_ONE`、
  `due_minutes=2147483647`，清空单元格 `fillable_scope_json`，避免残留为某一辅助行规则。
- 删除前稳定范围：`count=87`、`min_id=1032`、`max_id=1118`、
  全字段校验值
  `0d97bb86f81483abdbe008f0a471858e649324d10f87044e7c504d5db526b959`。
- 并发检查：未发现运行中的
  `edhr-visual-fill-config-real-flow.e2e.js`。

## Data Repair

- 删除前快照：
  `doc/tasks/20260727-delete-codx-vfc-duplicate-fill-rules/before-87-rules.sql`。
- 快照包含 `87` 条 INSERT，其中用户 `795`（贾泽宇）`44` 条、用户 `810`
  （王歆）`43` 条。
- 快照 SHA-256：
  `D8EC21C8CAF756BD6D73CC738D3A0359594702F16D069976DE0D98F792414C05`。
- 事务脚本先断言 `count=87`、`min_id=1032`、`max_id=1118` 和全字段校验值，
  再规范保留记录 `id=1033` 并删除其余记录。
- `GREEN: repair-86-rules.sql -> PASS, updated_rows=1, deleted_rows=86, remaining_rows=1`。

## Verification

- 数据库最终范围 `count=1`，唯一记录为
  `id=1033, scope_key=ALL, candidate_source_type=USERS,
  candidate_source_ids=810, completion_policy=ANY_ONE,
  due_minutes=2147483647`。
- `system_users.id=810` 在租户 `1` 下为启用用户“王歆”。
- 临时存储过程 `codex_repair_codx_vfc_fill_rules_20260727` 数量为 `0`。
- 登录态请求
  `GET /admin-api/mes/pro/edhr-process-form-permission-rule/get-by-report`
  返回 HTTP `200`、业务码 `0`，`candidateUsers=[{userId:810, displayName:"王歆"}]`。
- 真实 Playwright 页面
  `http://127.0.0.1:8081/mes/pro/batch-record-form-list`
  的目标行显示
  `CODX-VFC-20260726-批记录 / 粗洗工序生产记录 / 已配置 王歆 / V1.0`。
- `GREEN: get-by-report -> PASS, code=0 and unique candidate user 王歆`。
- `GREEN: real-page -> PASS, target row shows 已配置 王歆`。
- Playwright 会话 `codex-vfc-verify` 已正常关闭。
- `project-experience-consolidation` 复核结果：本次可复用经验已由
  `docs/database-rules.md` 的“数据修复与写入型 E2E 恢复并发门禁”覆盖，
  无需新建或修改长期经验文档。

## Closeout

- Bug regression evidence 校验通过：
  `validate_bug_regression.py -> Bug regression evidence is valid`。
- `task-closeout-cleanup` preview：保留回滚快照、`task.md`、
  `execution-log.md`、`verification-report.md`；删除临时
  `bug-regression-evidence.md` 和 `repair-86-rules.sql`；无 blocked/warnings。
- `task-closeout-cleanup` apply：成功删除上述 2 个临时文件，未触碰任务外文件。
- 脏工作区基线提交：`cfbd6a3896fadb436213976aace97df2f2bcd602`。
- 本任务修复与验证提交：`138f978a`，仅包含本任务目录 4 个文件。
- GitHub 大文件门禁：`origin/int_main..HEAD` 无 `>=100 MB` blob。
- 首次推送：`git push origin int_main -> PASS`，
  远端从 `177ebefb` 更新到 `138f978a`。
- 收尾提交：`1acd8f9dbb994f7d3385ab46faa9583243b9608a`。
- 第二次推送：`git push origin int_main -> PASS`，
  远端从 `138f978a` 更新到 `1acd8f9d`，推送后分支不再 ahead。
- 最终稳定性复核：无活动的
  `edhr-visual-fill-config-real-flow.e2e.js` Node/CMD 进程；数据库仍为
  `count=1, id=1033, scope_key=ALL, candidate_source_ids=810`。

## Current Status

completed

## Blockers

- 无当前阻塞。
