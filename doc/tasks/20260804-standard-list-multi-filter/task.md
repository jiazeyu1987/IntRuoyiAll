# Task: 标准列表模板支持多维度筛选

## Task Goal

- 在标准列表模板中设计并实现配置驱动的多维度筛选能力。
- 保留现有单条件快速过滤、额外筛选插槽、表格插槽、分页、排序和显示字段能力。
- 不改变后端接口契约，不引入前端兜底筛选、mock、静默降级或兼容分支。

## Milestones

- [ ] M1: 梳理现有标准列表模板和快速筛选契约。
- [ ] M2: 记录 BDD 场景并新增 RED 静态契约。
- [ ] M3: 实现多维筛选类型、状态、模板入口和样式。
- [ ] M4: 运行定向验证和前端特性证据校验。
- [ ] M5: 更新任务文档、验证报告和收尾状态。

## Expected Verification

- `node tests/e2e/unified-list-template-multi-filter-static.spec.js`
- `node tests/e2e/unified-list-template-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-standard-list-multi-filter/frontend-feature-evidence.md`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按配置驱动的多条件筛选模型扩展标准列表模板，而不是在页面内继续堆散落筛选控件。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 待创建任务目录后读取 `docs/experience-index.md` 并补充适用经验门禁。
