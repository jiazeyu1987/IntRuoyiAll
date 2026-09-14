# 生产组长报工管理随机数据

## Task Goal

在本机 `int_main` 环境中，为生产组长的报工管理新增 5 条任务自有随机报工数据。新增记录必须符合一线生产提交后的正式报工数据格式，并可在生产组长报工管理中查看。

## Milestones

- [x] M1 核对报工管理数据入口、正式提交接口和目标表结构。
- [x] M2 生成 5 条带任务标识的一线生产格式报工数据。
- [x] M3 通过正式链路或经核准的数据写入方式新增数据。
- [x] M4 核验生产组长报工管理可见性、字段格式和可清理范围。
- [x] M5 完成任务记录、验证报告和收尾状态更新。
- [x] M6 复核用户截图中的空表问题，补齐本机 admin 可见性和生产“报工管理”页签加载触发。

## Expected Verification

- 只读核对目标 schema、接口 VO 和既有一线报工样本格式。
- 写入前记录目标租户、账号标签、任务标识、预期新增数量和回滚或清理条件。
- 写入后核对新增 5 条正式报工记录，并确认关键字段符合一线生产提交格式。
- 如果本机前后端运行态可用，优先通过真实页面或登录态接口核验生产组长报工管理可见。

## Applicable Gates

### 经验门禁：第三方报工直报正式链路

- Trigger: 报工数据必须落到正式 `MesProFeedbackDO`，不能只写导入记录、进度或前端假数据。
- Preflight check: 确认新增路径创建正式报工、报工人、工序、数量、时间和来源字段完整。
- Blocker: 缺报工人、正式工序、目标租户、正式路线工序快照或排产工序剩余数量时停止，不写默认成功数据。
- Verification: 新增后核对正式报工列表记录数、任务标识和关键字段；不得用 API-only 冒充页面可见性。
- Forbidden action: 禁止直接改进度、空列表刷新、前端假新增或默认成功。

## Current Status

completed - 5 条本机任务自有报工随机数据已写入；用户截图对应的 admin 空表问题已通过正式负责员工范围和前端页签加载触发修复，并通过静态合同、SQL、登录态接口和真实页面只读路径验证；2026-08-07 已完成任务级 cleanup preview/apply。

## Completed Work

- 已读取数据库、任务收尾、本机运行、登录访问、E2E、服务器访问、发布备份恢复、PowerShell 编码和相关报工门禁。
- 已确认生产组长报工管理走 `mes_pro_process_pool_event` 时间线读取，入口为 `/mes/pro/process-pool/team-leader/submission/page`，并按组长责任员工集合筛选。
- 已在本机 Docker MySQL `ruoyi-vue-pro` 中新增标识为 `CODX-RPT-20260806` 的 5 条报工链路数据。
- 已确认生产组长用户 `1520/lvyujie`、员工 `964`、工单 `980008`、任务 `981941`、路线 `922119`、路线工序 `928611`、工序 `922987`、工作站 `980009`、设备 `41`、记录本 `980011`、模板 `980010` 的链路可解析。
- 已确认用户截图对应的本机默认账号 `admin` / 用户 `1` 原先没有 `PRODUCTION + EMPLOYEE` 负责范围，导致报工管理按正式员工范围过滤后为空。
- 已为本机 `admin` 增加任务自有 `PRODUCTION + EMPLOYEE + 964` 负责范围记录 `980044`，备注为 `CODX-RPT-20260806 admin production report visibility`。
- 已修复生产组长独立页默认停留在“人员管理”后，切换到“报工管理”不会触发 `getSubmissionList()` 的前端缺口。
- 已于 2026-08-07 执行 `task-closeout-cleanup` preview/apply，保留核心任务记录并清理两份临时 evidence 文件。

## Verification Evidence

- RED：写入前 `mes_pro_feedback` 与 `mes_pro_process_pool_event` 对 `CODX-RPT-20260806-%` 的计数均为 `0`。
- GREEN：写入后 `mes_pro_feedback`、`mes_pro_process_pool_event`、`mes_pro_edhr_recordbook_entry`、`mes_pro_edhr_recordbook_event`、`mes_pro_process_pool_quantity_fragment` 均命中 `5` 条。
- REGRESSION：以生产组长 `1520/lvyujie` 登录本机后端，`/team-leader/submission/page?leaderType=PRODUCTION&submitDate=2026-08-06&pageNo=1&pageSize=50` 返回业务码 `0`，总数 `25`，任务事件 ID `161-165` 命中 `5` 条。
- BUG RED：`node tests/e2e/production-leader-function-tabs-static.spec.js` 先失败，缺少 `watch(activeProductionModuleTab)` 在生产“报工管理”页签选中时自动按 `PRODUCTION` 拉取报工列表。
- BUG GREEN：`node tests/e2e/production-leader-function-tabs-static.spec.js` 与 `node tests/e2e/team-leader-workbench-static.spec.cjs` 均 PASS。
- DATA GREEN：本机 SQL 复验 `admin_visible_marker_count=5`；admin 登录态接口 pageSize=50 返回业务码 `0`，任务事件 ID `161-165` 命中 `5` 条。
- UI REGRESSION：真实页面只读路径登录 `芋道源码/admin`，进入 `/mes/pro/process-pool/production-leader` 后点击“报工管理”，实际请求 `leaderType=PRODUCTION&submitDate=2026-08-06&pageNo=1&pageSize=10`，返回 `total=25`、页面可见 `10` 行、组长写请求数 `0`、`pageErrors=0`。
- 证据校验：`database-schema-delivery` validator PASS，详见 `verification-report.md`。
- CLOSEOUT GREEN：`task_closeout.py --task-id 20260806-production-leader-feedback-random-data --mode preview` 与 `--mode apply` 均成功；仅删除临时 `bug-regression-evidence.md` 和 `database-schema-evidence.md`。

## Remaining Blockers

- 无。Git 提交或推送未获用户请求，且按项目规则不作为本任务完成门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按正式报工链路造数，不直接伪造页面数据或进度。
- `是否存在临时补丁或绕过`：否。
