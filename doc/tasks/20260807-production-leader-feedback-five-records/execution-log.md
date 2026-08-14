# Execution Log

## User Intent

- 用户要求给生产组长的报工管理增加 5 条符合从一线生产提交条件的数据。
- 本次范围限定为本机 `int_main` 测试环境，不操作远程测试服、生产服或备用服。

## BDD Scenarios

- BDD: 生产组长查看新增一线提交数据 -> Given 本机测试租户存在生产组长、其责任范围内一线员工和完整工单/任务/路线工序/工序池链路 / When 按正式一线提交数据合同新增 5 条任务自有报工 / Then 生产组长报工管理今日列表命中 5 条，员工姓名可解析，且正式报工、记录本、工序池事件、数量片段和汇总关系完整。
- BDD: 缺正式提交前置时停止 -> Given 缺目标租户、生产组长责任员工、正式工单任务、路线工序、工序池或真实 schema / When 尝试新增报工数据 / Then 停止写入并报告精确缺口，不创建孤立报工、默认成功或前端假数据。
- BDD: 重复任务标识不得再次写入 -> Given `CODX-RPT-20260807-%` 已存在任一正式链路记录 / When 再次执行本任务写入 / Then 事务必须 fail fast，不重复生成数据。

## Command And Evidence Log

- 2026-08-07: 已读取数据库、E2E、登录、本机运行、服务器、发布恢复、PowerShell 编码、任务收尾规则，以及 `database-schema-delivery` 技能和数据库证据合同。
- 2026-08-07: 已读取 `docs/experience-index.md` 并命中 `docs/backend-development.md#第三方报工直报正式链路门禁` 的“生产组长报工管理造数必须补齐工序池时间线”门禁。
- 2026-08-07: 已核对历史任务 `doc/tasks/20260806-production-leader-feedback-random-data/`，确认上一批正式样本使用员工 `964`、生产组长 `1520/lvyujie`、工单 `980008`、任务 `981941`、路线 `922119`、路线工序 `928611`、工序 `922987`、工作站 `980009`、设备 `41` 和工序池 `37`；本轮仍需用真实库重新核对，不能直接假设这些对象有效。
- 2026-08-07: Git 预检显示根仓库 `int_main` 已领先 `origin/int_main` 2 个提交，并存在其它任务未跟踪文档；本任务不修改或清理这些并发任务文件。
- 2026-08-07: 本机 `8081/48081` 运行态和健康检查通过，复用既有 `int_main` 前后端进程，未执行停止、重启或端口切换。
- 2026-08-07: 真实库核对确认员工 `964/liuyueyue`、生产组长 `1520/lvyujie`、工单 `980008`、任务 `981941`、路线 `922119`、路线工序 `928611`、工序 `922987`、工作站 `980009`、设备 `41`、记录本 `980011`、模板 `980010`、工序池 `37` 均存在且属于租户 `1`；生产组长的 `PRODUCTION + EMPLOYEE + 964` scope 启用。
- 2026-08-07: 核对员工 `964` 的正式权限，已具备 `mes:pro-feedback:create` 和 `mes:pro-edhr-batch-execution:query`。
- 2026-08-07: 使用 Playwright 登录员工 `964` 成功；直接访问隐藏的一线生产填写路由时只渲染系统品牌，未产生任何写入。本任务的数据写入仍严格使用既有正式样本对应的六表数据合同，并保留生产组长真实页面验证门禁，不以空白页面伪造提交成功。

## RED / GREEN / REGRESSION

- RED: `SELECT COUNT(*) FROM mes_pro_feedback WHERE code LIKE 'CODX-RPT-20260807-%'` -> FAIL（预期的未实现状态），计数 `0`。
- RED: `SELECT COUNT(*) FROM mes_pro_process_pool_event WHERE event_idempotency_key LIKE 'CODX-RPT-20260807-%'` -> FAIL（预期的未实现状态），计数 `0`。
- RED: `SELECT COUNT(*) FROM mes_pro_process_pool_event WHERE signature_id BETWEEN 202608070001 AND 202608070005` -> FAIL（预期的未实现状态），计数 `0`。
- RED: 首次执行 `data-change.sql` -> FAIL，真实 schema 使用 `employee_user_id` 而非计划稿中的 `scope_target_id`；事务已由异常处理器回滚，任务标识五类正式数据计数仍为 `0`。已按 `SHOW COLUMNS` 证据修正前置条件字段，未引入兼容分支。
- RED: 修正字段后再次执行 `data-change.sql` -> FAIL，`Formal frontline business context is incomplete`；事务回滚。
- BLOCKER: 工序池 `37` 指向 `mes_pro_route_process.id=928611`，该记录为 `deleted=1`；活动替代记录 `980647` 的 `workstation_id` 为 `NULL`。后端 `MesFrontlineDeviceAccountContextServiceImpl` 只读取活动路线工序，并在 `requireRouteProcessWorkstation` 中要求工作站非空，当前不存在可授权的正式一线生产上下文。
- CLEANUP: 已执行 `DROP PROCEDURE IF EXISTS apply_codx_rpt_20260807`；任务临时存储过程计数为 `0`。
- ROLLBACK VERIFY: 正式报工、记录本 entry、记录本 event、工序池事件和数量片段的任务标识计数均为 `0`，未残留部分数据。
- GREEN: 待执行。
- REGRESSION: 待执行。

## Data Safety

- 任务标识：`CODX-RPT-20260807`。
- 写入范围：仅本机 Docker MySQL `ruoyi-vue-pro` 内与 5 条任务报工直接相关的正式链路数据。
- 禁止范围：远程环境、无关租户、无关业务记录、生产进度伪造、权限扩大、mock 或前端假数据。
- 事务策略：所有写入在单一事务内执行；任何前置计数、目标对象唯一性或写入行数不符合预期时 `SIGNAL` 并回滚。
- 回滚口径：按 `CODX-RPT-20260807-%` 精确定位，按数量片段、工序池事件、记录本 event、记录本 entry、正式报工的依赖顺序清理，并恢复工序池汇总到写入前快照；本任务目标为保留数据，不主动执行回滚。
