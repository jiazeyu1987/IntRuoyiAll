# Execution Log

## User Intent

- 用户要求：删除当前失败批记录表单填写人规则中的其他 86 条，保留 1 条正确规则。

## BDD

- `BDD: 重复填写人规则清理 -> Given 当前租户某批记录报表和版本下存在 87 条启用的表单级 FILL 规则且业务确认其中 1 条正确 When 执行受控数据修复 Then 仅删除其余 86 条，保留规则内容不变，且规则读取接口不再因多结果异常失败。`

## Preflight

- 已读取 `docs/database-rules.md`。
- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/powershell-encoding.md`。
- 已读取 `docs/powershell-memory.md`。
- 已读取 `docs/experience-index.md`，命中业务数据修复和批记录版本治理门禁。
- 当前尚未执行任何写入或删除。
- 真实表：`mes_pro_edhr_process_form_permission_rule`，MySQL 8/InnoDB，本地 Docker 数据库 `int-ruoyi-mysql/ruoyi-vue-pro`。
- 目标唯一范围：`tenant_id=1`、`route_process_id=0`、`batch_record_report_id=1d05410f1d3140c5b8aa6786887ae69c`、`batch_record_version_id=130`、`rule_type=FILL`、`enabled=1`、`deleted=0`。
- 报表映射：产品“球囊扩张压力泵”，表单“粗洗工序生产记录”，版本 `V14.0`，版本 ID `130`，定义 ID `47`。
- `RED: 只读范围计数与接口日志 -> FAIL, 目标范围存在 87 条启用规则，日志出现 TooManyResultsException: expected one result (or null) to be returned by selectOne(), but found: 87`。
- 87 条记录均为 `CODX_VFC_ASSIST_*` / `E2E辅助行*`，创建时间会被 `edhr-visual-fill-config-real-flow.e2e.js` 的恢复逻辑重写；已等待冲突中的 Playwright 进程自然结束，未强停并行任务。
- 版本限制已确认：Mapper 通过 `batch_record_version_id = 130` 查询当前版本；本次 SQL 也固定相同版本条件。

## Retention Decision

- 当前 87 条中不存在正式表单级 `ALL` 规则，不能任意保留某条单元格测试规则。
- 当前 V14.0 的 `source_version_id=118`，对应 V13.0；V13.0 的粗洗规则为 `ALL / ROLE / 910405`。
- 同一 V14.0 的其余 14 个表单均为 `ALL / ROLE / 910405`，角色名称“压力泵生产1”，启用成员为“王歆、任丹”。
- 因此保留当前范围中 `scope_key=CODX_VFC_ASSIST_1` 的一条物理记录作为载体，事务内将其规范为 `ALL / ROLE / 910405`，清空单元格范围，并删除同范围其余 86 条。

## Recovery Plan

- 删除前使用 `mysqldump --no-create-info` 导出目标 87 行到任务目录，导出后核对 87 条 `INSERT` 值记录。
- 若事务校验失败，事务内 `ROLLBACK`，不留下部分删除。
- 若提交后需要恢复，先删除目标范围当前规则，再从任务快照导入 87 行；恢复前后均按目标租户、报表 ID、版本 ID 和规则类型核对行数。

## Verification

- 已完成 schema、租户、报表 ID、版本和保留规则依据核对。
- 删除前快照重新导出为 87 条独立 `INSERT`，SHA-256：
  `FCB40150DCA3216DA66746213689EDEDD08799B2F51F4A378AD560E3E035AA60`。
- 快照对应主键范围为 `3217..3303`，全字段校验值为
  `ffc016241194cdd3dea3bd14375f788f5cfd7ba2630af514d6a17a28013b54f8`。
- `GREEN: doc/tasks/20260727-delete-duplicate-fill-rules/execute-repair.ps1 -> PASS, retained_rule_id=3217, updated_rows=1, deleted_rows=86, remaining_rows=1`。
- 临时过程 `codex_repair_fill_rules_20260727` 已删除，数量为 0。
- 事务后数据库规则为 `ALL / ROLE / 910405 / version 130`，角色“压力泵生产1”解析启用成员为 `810:王歆, 910181:任丹`。
- 18:54:31 并发 `edhr-visual-fill-config-real-flow.e2e.js` 再次运行，测试期间临时重建 87 条；18:56:25 恢复完成后数据库重新稳定为 1 条正式规则，物理 ID 变为 `3391`，`scope_key=ALL`、`candidate_source_type=ROLE`、`candidate_source_ids=910405`、`batch_record_version_id=130`。
- 18:59:41 又启动一轮同名 E2E；最新只读复验仍为 1 条正式规则、0 条 `CODX_VFC_ASSIST_*` 辅助规则。
- 该轮 E2E 已自然结束；最终恢复后最新物理 ID 为 `3479`，目标范围仍为 1 条 `ALL / ROLE / 910405 / version 130`，未检测到同名 E2E 进程。
- 版本限制静态证据：Mapper 的 `withBatchRecordVersion(...)` 在版本非空时明确追加 `batch_record_version_id = batchRecordVersionId`；本次目标版本为 `130`。
- 登录态 API 复验第一次在租户名称解析请求上 15 秒超时；随后另一任务重启本地后端，`48081` 不再监听，直接登录请求被拒绝。未把接口复验标记为通过。
- 后端恢复后健康检查为 `UP`；使用目标租户 `1` 的本机默认登录身份执行只读接口复验：
  `API_CODE=0`、`FILL_RULE_STATUS=CONFIGURED`、`SOURCE_TYPE=ROLE`、
  `SOURCE_IDS=910405`、`CANDIDATE_USERS=王歆,任丹`，未再出现 `TooManyResultsException`。
- `GREEN: node doc/tasks/20260727-delete-duplicate-fill-rules/verify-page-readonly.mjs -> PASS, 球囊扩张压力泵 / 粗洗工序生产记录显示“已配置 王歆、任丹”，MES 写请求数为 0`。
- 数据库 schema evidence validator：PASS。

## Current Status

ready_for_closeout

## Blockers

- 无当前阻塞。若后续再次运行写入同一范围的 E2E，仍需等待其恢复阶段完成后复验最终稳定状态。

## Git And Closeout

- `task-closeout-cleanup preview -> PASS`：保留 7 个正式证据文件，计划删除 3 个临时文件，无 blocked/warnings。
- `task-closeout-cleanup apply -> PASS`：已删除
  `execute-repair.ps1`、`repair-86-rules.sql.template`、`snapshot-metadata.sql`。
- 脏工作区基线提交：`85afb6fea8e67c0724f117d2da5a86794cc023d8`。
- 基线提交共 41 个非本任务文件：
  - `.review-fix-loop/runs/20260727T110834Z-6f3e83/review/packet-round-1.md`
  - `.review-fix-loop/runs/20260727T110834Z-6f3e83/run.json`
  - `.review-fix-loop/runs/20260727T110834Z-6f3e83/supervisor/log.md`
  - `.review-fix-loop/runs/20260727T110834Z-6f3e83/task.md`
  - `.runtime/20260727-form-template-validation/form-template-buttons-real-e2e.mjs`
  - `IntRuoyiBackend/.gitattributes`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchWorkbenchServiceImpl.java`
  - `IntRuoyiBackend/yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/controller/admin/ShowroomClientDownloadController.java`
  - `IntRuoyiBackend/yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/controller/admin/ShowroomClientDownloadFile.java`
  - `IntRuoyiBackend/yudao-module-showroom/src/main/resources/showroom/client-downloads/v1.0/YingtaiShowroomClient-Win7-v1.0.zip`
  - `IntRuoyiBackend/yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/controller/admin/ShowroomClientDownloadControllerTest.java`
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/codextest/vo/CodexTestNodeChainOptionRespVO.java`
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/codextest/CodexTestCaseServiceImpl.java`
  - `IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/codextest/CodexTestCaseServiceImplTest.java`
  - `IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/codextest/CodexTestRunnerServiceImplTest.java`
  - `IntRuoyiFronted/.gitattributes`
  - `IntRuoyiFronted/doc/tasks/20260615-showroom-award-export-import-real-e2e/产品资料修改版-补充产品资料.xlsx`
  - `IntRuoyiFronted/src/api/showroom-admin/index.ts`
  - `IntRuoyiFronted/src/api/system/codexTestManagement/index.ts`
  - `IntRuoyiFronted/src/views/showroom-admin/company/CompanyWorkbench.vue`
  - `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`
  - `IntRuoyiFronted/tests/e2e/mes-route-flow-tab-return-state-real.e2e.js`
  - `IntRuoyiFronted/tests/e2e/showroom-award-export-import-roundtrip-real.e2e.js`
  - `IntRuoyiFronted/tests/e2e/showroom-client-download-retirement-static.spec.js`
  - `doc/tasks/20260727-edhr-notify-all-valid-candidates/backend-api-evidence.md`
  - `doc/tasks/20260727-edhr-notify-all-valid-candidates/bug-regression-evidence.md`
  - `doc/tasks/20260727-edhr-notify-all-valid-candidates/execution-log.md`
  - `doc/tasks/20260727-edhr-notify-all-valid-candidates/task.md`
  - `doc/tasks/20260727-edhr-notify-all-valid-candidates/verification-report.md`
  - `doc/tasks/20260727-edhr-release-owner-from-end-config/backend-api-evidence.md`
  - `doc/tasks/20260727-edhr-release-owner-from-end-config/execution-log.md`
  - `doc/tasks/20260727-edhr-release-owner-from-end-config/verification-report.md`
  - `doc/tasks/20260727-remove-lfs-assets/backend-api-evidence.md`
  - `doc/tasks/20260727-remove-lfs-assets/execution-log.md`
  - `doc/tasks/20260727-remove-lfs-assets/task.md`
  - `doc/tasks/20260727-route-flow-tab-return-state/execution-log.md`
  - `doc/tasks/20260727-route-flow-tab-return-state/task.md`
  - `doc/tasks/20260727-route-flow-tab-return-state/verification-report.md`
  - `docs/changes/20260727-remove-lfs-assets.md`
  - `docs/experience-index.md`
  - `docs/frontend-development.md`
- 本任务实现提交：`e42fa5d20a0aac5dc1607195c438863bf5a34be9`，仅包含 8 个任务自有文件：
  - `doc/tasks/20260727-delete-duplicate-fill-rules/before-87-rules.sql`
  - `doc/tasks/20260727-delete-duplicate-fill-rules/database-schema-evidence.md`
  - `doc/tasks/20260727-delete-duplicate-fill-rules/execution-log.md`
  - `doc/tasks/20260727-delete-duplicate-fill-rules/repair-86-rules.sql`
  - `doc/tasks/20260727-delete-duplicate-fill-rules/task.md`
  - `doc/tasks/20260727-delete-duplicate-fill-rules/verification-report.md`
  - `doc/tasks/20260727-delete-duplicate-fill-rules/verify-page-readonly.mjs`
  - `docs/database-rules.md`
- 推送前分支运行端口门禁：PASS，`int_main=8081/48081`。
- 待推送历史大文件扫描：`PENDING_LARGE_BLOBS=0`。
- `git push origin int_main -> PASS`，远端更新到 `e8049307`，推送后分支不再领先 `origin/int_main`。
- 最终状态：`completed`。
