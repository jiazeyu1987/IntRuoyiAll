# PQC 管理列表测试数据

## Task Goal

- 在本机 `芋道源码/admin` 可访问的 PQC 管理列表中添加一条可追踪测试数据。
- 测试数据必须进入正式提交列表读模型，不使用前端 mock、不改生产代码、不污染远端环境。
- 测试数据必须带任务标识，便于后续只读核验和清理。

## Milestones

- [x] 建立任务目录、BDD 场景和数据写入边界。
- [x] 核对 PQC 管理列表 API、读模型、表结构和 admin 可见范围。
- [x] 写入一条本机测试租户 PQC 提交事件及必要关联记录。
- [x] 用 SQL 读模型验证 `芋道源码/admin` PQC 管理列表口径可查询到该测试数据。
- [x] 记录验证结果、数据主键和清理方式。
- [x] 运行态登录态 API 验证：本机 `48081` 健康，PQC 管理列表接口可查到该记录。
- [x] 修复 `PQC管理` 页签空列表回归：默认并展示必填提交日期，切换页签后加载正式提交列表。
- [x] 真实浏览器验证：本机 `芋道源码/admin` 打开 `PQC管理` 可看到测试工单。
- [x] 调整 PQC 管理展示：列表不显示逐件/样本值，详情保留样本值并将详情抽屉宽度翻倍。
- [x] 清理 PQC 详情弹框：隐藏结构化报工内容和原始提交内容，并将左侧标签列宽增至原 4 倍。
- [ ] 将 PQC 管理详情从弹框迁移为页签内标准列表展示。

## Expected Verification

- 只读核对真实库表结构：`mes_pro_process_pool_event`、`mes_pro_process_pool_pqc_record`、`mes_pqc_inspection_task`、PQC 组长 scope。
- 写入后用只读 SQL/API 核对任务标识、事件类型、实际员工、今天提交日期、PQC 任务和原始 payload。
- 本机运行态可用时，用 `芋道源码/admin` 登录态或对应列表 API 验证 PQC 管理列表能看到该记录。

## BDD Scenarios

- BDD: PQC 管理列表显示测试提交 -> Given 本机 `芋道源码/admin` 打开 PQC 管理列表 / When 今天存在一条 admin 负责范围内的 PQC 测试提交 / Then 列表能显示生产工单、工序、PQC 检验员、检验项、检验数量、损耗数量和逐件样本值。
- BDD: 测试数据可追踪可清理 -> Given 测试提交写入正式库 / When 后续需要清理 / Then 可通过任务标识定位并删除本次事件和关联 PQC 记录，不影响其它业务数据。
- BDD: PQC 样本值只在详情展示 -> Given PQC 管理列表存在带逐件样本值的提交 / When 用户停留在列表页 / Then 列表不显示逐件/样本值列；When 用户点击详情 / Then 详情中的 PQC 项目明细仍显示样本值，并且详情抽屉宽度为原 `620px` 的 2 倍。
- BDD: PQC 详情只展示业务摘要和项目明细 -> Given 用户打开 PQC 管理提交详情 / When 详情抽屉展示提交内容 / Then 不显示 `结构化报工内容` 和 `原始提交内容`；And 左侧详情标签列宽为原 `100px` 的 4 倍，即 `400px`，避免标签竖向换行。
- BDD: PQC 详情页签展示结构化明细 -> Given PQC 管理列表存在结构化 PQC 提交 / When 用户点击列表行的 `详情` / Then 页面切换到 `详情` 页签内展示业务摘要和 PQC 项目明细；And 不打开详情弹框；And 项目明细使用标准列表模板展示样本值。

## Applicable Gates

- 数据库写入门禁：写 SQL 前必须核对真实库 schema、目标租户、影响范围和清理方式。
- PQC 项目级检验快照门禁：PQC 列表展示应来自结构化 `pqcItemDetails/itemResults` 和 rawPayload 快照。
- 登录访问门禁：仅使用本机 `localhost:8081` / `127.0.0.1:48081` 和 `芋道源码/admin` 标签，不记录密码。
- No fallback：不得用 mock、默认成功、前端硬编码或 API-only 伪造页面数据。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，测试数据写入正式列表读模型所需数据源。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

- 已完成测试数据写入：`mes_pro_process_pool_event.id=160`，marker `PQC_TEST_20260806_MGMT_LIST_20260806181357559250`。
- 本轮新增需求处理中：点击 `PQC管理` 列表详情时改为页签内展示，详情项目明细也按标准列表模板承载。
- SQL 读模型验证通过：tenant `1`、admin/PQC 今日列表口径可命中事件 `160`。
- 运行态登录态 API 验证通过：`http://127.0.0.1:48081/actuator/health` 返回 `UP`，`/admin-api/mes/pro/process-pool/team-leader/submission/page` 返回事件 `160`。
- 前端空列表回归已修复：`PQC管理` 切换时自动带可见 `提交日期=2026-08-06` 条件并加载列表。
- 真实浏览器验证通过：页面显示 `RRM-20260801-PP-MO-001` / `清洗工序`，列表响应 `code=0,total=1`。
- 列表/详情展示调整已完成：样本值移出 `PQC管理` 列表与列配置池，仅保留在详情 PQC 项目明细中，详情抽屉宽度由 `620px` 调整为 `1240px`。
- 详情弹框清理已完成：隐藏 `结构化报工内容` 与 `原始提交内容`，左侧描述标签列实测为 `400px`。
- 剩余非本轮阻塞：部分旧静态合同仍使用“默认空条件”或旧生产列池口径，需要作为单独列表结构清理任务处理。

## Cleanup Keep

- doc/tasks/20260806-pqc-management-list-test-data/insert-pqc-test-data.sql
- doc/tasks/20260806-pqc-management-list-test-data/fix-pqc-test-payload-json.sql
