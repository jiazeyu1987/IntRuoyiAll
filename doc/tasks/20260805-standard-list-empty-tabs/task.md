# Task: 标准列表模板统一为空条件 Tab 筛选模式

## Task Goal

- 梳理当前系统所有 `UnifiedListTemplate` 标准列表模板接入点。
- 将标准列表模板统一为红框里的空条件 Tab 模式：默认没有筛选条件，只显示“暂无筛选条件”和左右加减按钮，点击加号后新增条件。
- 逐个迁移仍使用旧 quick filter 或显式默认筛选条件的标准列表，复用同一 `TableMultiFilter` 条件 Tab 机制。
- 不改变后端接口契约，不引入前端本地过滤、mock、静默降级或兼容分支。

## Milestones

- [x] M1: 统计所有 `UnifiedListTemplate` 标准列表模板接入点并分类。
- [x] M2: 为“默认空条件 Tab”和“旧 quick filter 迁移”新增 RED 静态合同。
- [x] M3: 调整标准列表模板默认行为和各页面接入参数。
- [x] M4: 逐个运行静态合同、类型检查和真实页面 E2E 抽样验证。
- [x] M5: 更新任务文档、证据报告和收尾状态。

## Expected Verification

- `node tests/e2e/unified-list-template-multi-filter-static.spec.js`
- `node tests/e2e/unified-list-template-static.spec.js`
- 新增或更新的标准列表接入点静态合同。
- `pnpm ts:check:schedule`
- `pnpm ts:check`
- 至少一个真实页面 Playwright E2E 验证红框模式默认空条件、加号新增、交集查询、重置清参、目标写请求数为 0。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-standard-list-empty-tabs/frontend-feature-evidence.md`

## Current Status

completed

- 已完成标准列表模板清单扫描：当前系统共有 84 个 `UnifiedListTemplate` 标准列表模板接入点，分布在 67 个 Vue 文件。
- 已将模板默认筛选入口统一桥接到 `TableMultiFilter` 条件 Tab：默认无筛选条件，点击加号新增条件；旧 quick filter definitions 由模板层转换复用。
- 已移除排产工单和同步工单的默认隐藏筛选条件，首屏请求只保留分页参数。
- 静态合同、类型检查和排产工单真实 Playwright E2E 均已通过。
- `frontend-feature-evidence.md` 已通过技能 validator 后按 cleanup 规则归档到 `execution-log.md` / `verification-report.md` 并删除临时文件；cleanup apply 无阻塞。

## Cleanup Keep

- doc/tasks/20260805-standard-list-empty-tabs/schedule-order-empty-tabs-real.e2e.cjs
- doc/tasks/20260805-standard-list-empty-tabs/artifacts/standard-list-template-inventory.json
- doc/tasks/20260805-standard-list-empty-tabs/artifacts/schedule-order-empty-tabs-real/result.json

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是统一标准列表模板入口和页面接入契约，而不是页面级特例。
- `是否存在临时补丁或绕过`：否。
