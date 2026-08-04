# Task: 标准列表模板支持多维度筛选

## Task Goal

- 在标准列表模板中设计并实现配置驱动的多维度筛选能力。
- 保留现有单条件快速过滤、额外筛选插槽、表格插槽、分页、排序和显示字段能力。
- 不改变后端接口契约，不引入前端兜底筛选、mock、静默降级或兼容分支。

## Milestones

- [x] M1: 梳理现有标准列表模板和快速筛选契约。
- [x] M2: 记录 BDD 场景并新增 RED 静态契约。
- [x] M3: 实现多维筛选类型、状态、模板入口和样式。
- [x] M4: 运行定向验证和前端特性证据校验。
- [x] M5: 更新任务文档、验证报告和收尾状态。
- [x] M6: 按用户指定在排产工单真实页面启用多维筛选，并完成静态合同与真实 E2E 验证。
- [x] M7: 按用户反馈把多维筛选改为可增删条件 Tab，并验证所有已填写 Tab 条件按交集提交。
- [x] M8: 修复排产工单页面重复筛选区域，只保留右侧条件 Tab 筛选并完成真实 E2E 复验。

## Expected Verification

- `node tests/e2e/unified-list-template-multi-filter-static.spec.js`
- `node tests/e2e/schedule-order-main-multi-filter-static.spec.js`
- `node tests/e2e/unified-list-template-static.spec.js`
- `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js`
- `node tests/e2e/mes-schedule-order-replan-visible-filter-static.spec.js`
- `node -e '...'` target TypeScript syntax transpile check for touched hook and SFC script blocks.
- `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs`
- `pnpm ts:check:schedule`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-standard-list-multi-filter/frontend-feature-evidence.md`

## Current Status

ready_for_closeout

- 排产工单真实页面 pilot 已改为可增删条件 Tab，并通过静态合同、`ts:check:schedule` 和真实 Playwright E2E。
- 真实 E2E 验证了默认完成状态 Tab、排产工单号 Tab、来源生产工单号 Tab 同时提交为 `completionFilter`、`code`、`erpWorkOrderCode` 正式 query 参数交集，目标写请求数为 0。
- 用户截图反馈的重复筛选区域已修复：排产工单启用右侧条件 Tab 多维筛选时，左侧旧 quick filter 区域不再显示；真实 E2E 记录 `legacyQuickFilterVisibleCount=0`。
- task-closeout-cleanup preview/apply 已在 Tab 方案复验后通过；仅删除本任务旧失败产物 `artifacts/schedule-order-multi-filter-real/error.txt`，保留本轮 `frontend-feature-evidence.md`、真实 E2E 脚本与 `result.json`。
- 最新全量 `pnpm ts:check` 已通过。
- Full closeout/commit/push 仍被当前仓库已有的 dirty/ahead 并行任务状态阻塞；本轮不会宽泛提交或推送，以避免混入非本任务改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按配置驱动的多条件筛选模型扩展标准列表模板，而不是在页面内继续堆散落筛选控件。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 适用门禁：`docs/frontend-development.md#前端静态契约隔离门禁`。本任务新增专用最小静态契约先 RED 后 GREEN；当前全量 `pnpm ts:check` 已通过。
- 已沉淀门禁：`docs/frontend-development.md#统一列表复合工具栏布局门禁`，并在 `docs/experience-index.md` 增加标准列表多维筛选、multi-filter 0 width、正式 query 透传等关键词。
- 本轮 Tab 方案复验后已补充同一门禁：标准列表多维筛选优先采用可增删条件 Tab、稳定 condition id、重复正式参数校验和交集查询验证，禁止页面级 inline filter 数量特例。
- 适用门禁：`docs/powershell-memory.md#共享分支并发基线提交门禁`。当前分支已有并行基线提交和未提交改动，本轮只触碰排产工单多维筛选代码、任务专用测试和本任务文档。
- 已运行真实 Playwright 排产工单多维筛选 E2E；一个首屏 GET 因后续筛选请求被浏览器取消并记录为 `net::ERR_ABORTED`，目标 HTTP 错误数和目标写请求数均为 0。
- 不适用门禁：真实 E2E 用户列配置与列表可见性门禁；本任务未修改表格列配置或用户列可见性。

## Cleanup Keep

- doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs
- doc/tasks/20260804-standard-list-multi-filter/artifacts/schedule-order-multi-filter-real/result.json
- doc/tasks/20260804-standard-list-multi-filter/frontend-feature-evidence.md
- doc/tasks/20260804-standard-list-multi-filter/bug-regression-evidence.md
