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

## Expected Verification

- `node tests/e2e/unified-list-template-multi-filter-static.spec.js`
- `node tests/e2e/unified-list-template-static.spec.js`
- `node -e '...'` target TypeScript syntax transpile check for touched hook and SFC script blocks.
- `pnpm ts:check` regression check; current run is blocked by unrelated existing QA template export errors.
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-standard-list-multi-filter/frontend-feature-evidence.md`

## Current Status

blocked

- Implementation and target static validation passed.
- Formal full closeout is blocked by existing unrelated `pnpm ts:check` failures in `src/views/mes/qc/template/index.vue` referencing missing QA inspection regulation exports, and by the repository's pre-existing dirty/ahead state.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按配置驱动的多条件筛选模型扩展标准列表模板，而不是在页面内继续堆散落筛选控件。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 适用门禁：`docs/frontend-development.md#前端静态契约隔离门禁`。本任务新增专用最小静态契约先 RED 后 GREEN；全量 `pnpm ts:check` 的无关 QA 模板导出失败已记录为 blocker，未作为本任务通过证据。
- 不适用门禁：真实 E2E 用户列配置与列表可见性门禁；本任务未运行真实 Playwright，也未修改表格列配置或用户列可见性。
