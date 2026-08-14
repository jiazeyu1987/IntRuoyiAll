# Execution Log

## User Intent

- 将压力泵工序设备参数 Excel 中的工序设备关系和参数标准配置到生产组长“工序配置”列表。
- 目标为本机 `芋道源码` tenantId=1、admin userId=1。
- 先补正式设备台账，再建立班组设备、工序设备映射和参数标准。
- 光固机编号使用用户明确修正后的 `A05075`。

## Preconditions

- OfficeCLI 已读取并校验工作簿，文件哈希固定为 `7AA1EF1A9B8981175B9C8A05375C19B71D66D29127F7DC6E33F669199A9E580E`。
- 目标路线 `RT000028 / 球囊扩张压力泵` 的 14 道路线工序已只读核对。
- tenantId=1 当前不含 Excel 的 11 个设备编号；userId=1 当前无生产组长班组设备、映射或参数规则。
- tenantId=1 其他组长及 tenantId=122 现有数据不属于本任务，不修改。

## BDD Scenarios

BDD: 数值设备参数标准按原文和结构化边界保存 -> Given Excel 参数为精确值、范围或公差 When 生产组长在目标路线工序保存参数标准 Then 列表显示 Excel 原文且后端保存对应 lower/target/upper，不推断范围中点

BDD: 文本设备参数标准只读展示 -> Given Excel 参数为清洗介质、清洗温度或最大压力保持观察文本 When 生产组长保存 TEXT_STANDARD Then 工序配置列表显示原文且一线生产不要求填写数值读数

BDD: 压力泵工序只绑定指定设备 -> Given tenantId=1 已补齐 11 台正式设备和 admin 班组设备 When 按 Excel 配置 13 条工序设备映射 Then 每道工序只展示 Excel 指定设备且光固Ⅰ/光固Ⅱ仅绑定 A05075

BDD: Excel 全量参数标准进入生产组长列表 -> Given 13 条工序设备映射已存在 When 逐项保存 45 条参数标准 Then 工序配置列表与只读数据库核对均为 45 条且无额外推断参数

BDD: 非目标数据保持不变 -> Given tenantId=1 其他组长和 tenantId=122 已有数据 When 完成本任务写入 Then 非 userId=1 的既有班组设备、映射和参数规则数量及业务键不变

## Command Intent

- 只读检查：规则文件、现有 schema、API、页面、测试和运行态归属。
- 写入顺序：任务文档 -> RED 测试 -> 最小正式实现 -> GREEN/回归 -> 本机迁移 -> 真实页面业务写入 -> 只读终态核验 -> 收尾。
- 不执行 Git stage/commit/push/branch/worktree 操作。

## Milestone Status

- M1：completed；Excel、tenantId=1/userId=1、RT000028、11/13/45 目标和非目标数据基线已冻结。
- M2：completed；数据库正式支持 `standard_text`、可空数值边界和 `TEXT_STANDARD`，后端与前端正式链路已扩展，首条设备映射所需的组长设备列表改为正式 leader-scoped 接口。
- M3：blocked；真实 UI E2E 在任何业务写入前确认当前 `48081` 运行 Jar 缺少 `team-device/list`，未继续写入。
- M4：进行中；定向测试已取得部分完整证据，运行态回归与终态核验等待部署授权。
- M5：未开始。

## Verification Evidence

- OfficeCLI workbook validate：PASS，0 issues。
- `Get-Command npx`：PASS，路径 `D:\Programs\npx.ps1`。
- `docs/experience-index.md`：已读取；匹配生产组长工序配置维护权限、前端新增按钮行为、写入型远程下拉候选新鲜度、Schema-backed E2E 和一线设备权限边界。
- RED: `node tests/e2e/pressure-pump-device-parameter-standard-static.spec.cjs` -> FAIL，原因是首条工序设备映射没有正式的组长设备候选来源。
- GREEN: `node tests/e2e/pressure-pump-device-parameter-standard-static.spec.cjs` -> PASS，新增 leader-scoped `team-device/list` 正式数据链路。
- GREEN: `node tests/e2e/frontline-team-config-static.spec.cjs` -> PASS。
- GREEN: 目标前端文件 ESLint -> PASS；E2E 脚本 `node --check` -> PASS。
- GREEN/PARTIAL: Maven 定向 profile 共执行 39 项，38 项通过；唯一失败为 schema 合同把 `ADD COLUMN standard_text ... NOT NULL` 误写成 `MODIFY COLUMN` 断言，已按实际原子迁移修正。修正后的复跑被共享工作区其它 PQC/ERP 未完成源码与测试编译错误阻塞。
- SCHEMA: `20260805_mes_process_pool_device_parameter_route_process_constraints.sql` 与 `20260807_mes_process_pool_device_parameter_standard_text.sql` 已应用本机库；`route_process_id` 非空、三个数值字段可空、`standard_text` 非空，release migration policy gate PASS。
- E2E PREWRITE RED: `node ../doc/tasks/20260807-pressure-pump-process-device-standards/pressure-pump-config-real.e2e.cjs` -> FAIL，`team-device/list` 返回业务 404；失败发生在设备台账、班组设备、映射和参数规则写入之前。
- DATA SAFETY: 失败后只读复核仍为 target master=0、admin devices=0、bindings=0、rules=0；tenantId=1 其他组长为 1/3/0，tenantId=122 为 1/1/0，与写入前一致。
- RESTART RECHECK: 用户通知已重启后，`48081` PID=40088，运行 Jar 为 `backend-runtime-control-20260807-erp-connection-switch.jar`，health=`UP`；再次执行登录态 E2E 前置核验，`team-device/list` 仍返回“请求地址不存在”业务 404。该次也在任何业务写入前停止，数据库目标计数仍为 0/0/0/0。
- SECOND RESTART RECHECK: 用户再次通知已重启后，`48081` PID=61676，运行 Jar 为 `backend-runtime-control-20260807-active-order-abnormal-fix.jar`，health=`UP`；第三次登录态 E2E 前置核验仍为 `team-device/list` 业务 404。任务专属最新后端快照 Jar 随后已在 `output/runtime/20260807-team-device-list-endpoint-not-found/snapshot-backend/yudao-server/target/yudao-server-exec.jar` 生成，但当前 `48081` 尚未运行该 Jar。

## Resolved Blockers

- 已解除：此前 `48081` 运行不含 `team-device/list` 的旧 Jar，真实页面无法建立首条工序设备映射；用户要求重启到最新后，当前运行 Jar 已切换并通过真实 UI E2E。
- 已记录风险：共享主工作区仍存在其它任务脏改动，因此本任务只记录当前目标验证和收尾证据，不执行 Git 提交、合并或推送。

## Final Completion Evidence

- BACKEND RESTART: 用户要求重启到最新后，`48081` 当前 PID=53868，运行 Jar 为 `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260807-2158-process-config-responsible-routes.jar`，SHA256=`99014581D86A569120C0754EAA4472B50BAF0E9BDF804E0A69EA4E99FB5E6D58`，health=`UP`。
- ROUTE RECHECK: `team-device/list` 未登录探针返回业务 `401`，不再是旧运行态“请求地址不存在”的业务 404；最终成功性以登录态真实 Playwright E2E 和只读数据库终态为准。
- E2E GREEN: `node doc/tasks/20260807-pressure-pump-process-device-standards/pressure-pump-config-real.e2e.cjs` -> PASS，输出 `PASS: pressure-pump process/device standards configured through real UI (11/13/45)`。
- E2E RESUME: 部分写入后按稳定设备编号、工序设备业务键和参数编码只读核对，使用 `PRESSURE_PUMP_E2E_EXISTING_MASTER_CODES`、`PRESSURE_PUMP_E2E_EXISTING_TEAM_CODES`、`PRESSURE_PUMP_E2E_SKIP_MAPPINGS=1`、`PRESSURE_PUMP_E2E_EXISTING_PARAMETER_CODES` 跳过已完成目标，禁止盲目重放或删除并发非目标数据。
- DB FINAL GREEN: target master=11、admin team devices=11、process-device mappings=13、parameter rules=45、text standards=7、numeric standards=38。
- DB CORRECTIONS GREEN: `A05075` 存在 1 条，旧编号 `A05059` 存在 0 条；`C01017 撤压机` 和 `B04091 箱型干燥机` 已包含在目标 11 台设备内。
- DATA SAFETY: tenantId=1 非 admin 组长数据保持 `1/3/0`；tenantId=122 当前 `2/2/1` 中新增行 creator=`codex-ffs-submit`、create_time=`2026-08-07 19:54:27`，早于压力泵写入时间段 `2026-08-07 21:24:48` 至 `22:04:06`，判定为并发任务非目标变化，本任务未修改 tenantId=122。
- EVIDENCE ARCHIVE: 已生成 `verification-report.md`，并保留 `pressure-pump-config-real.e2e.cjs`、`artifacts/pressure-pump-process-config-desktop.png`、`artifacts/pressure-pump-process-config-mobile.png` 作为最终证据。
- EVIDENCE VALIDATORS GREEN: `validate_database_schema.py`、`validate_backend_api.py`、`validate_frontend_feature.py` 均 PASS；临时 evidence 核心结论已复制到保留报告，满足 cleanup 前归档门禁。
- EXPERIENCE CONSOLIDATION: 本任务复用并补充 `docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁` 的证据索引；写入型 E2E 断点恢复规则已有 `docs/e2e-rules.md#写入型-e2e-响应不确定断点恢复门禁` 覆盖，无需新建经验文档。
- CLEANUP GREEN: task-closeout-cleanup preview/apply 均 PASS，删除范围仅限本任务临时探针、失败截图和中间 evidence，保留 task.md、execution-log.md、verification-report.md、真实 E2E 脚本和最终截图。
- FINAL RECHECK GREEN: cleanup 后复跑 preview 为 delete=<none>、blocked=<none>、warnings=<none>；`48081` health=`UP`；只读数据库仍为 master=11、A05075=1、A05059=0、team devices=11、bindings=13、rules=45、text=7、numeric=38。
- STATUS: 任务已标记 `completed`。
