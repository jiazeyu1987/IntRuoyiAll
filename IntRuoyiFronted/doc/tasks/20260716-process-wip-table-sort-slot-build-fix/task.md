# Task: ProcessWipTable sort slot build fix

## Task Goal

修复 `ProcessWipTable.vue` 在 release `pnpm build:test` 中触发的 `vue/no-unused-vars`，保持 `UnifiedListTemplate` 的 `sortColumnAttrs` 与 `handleSortChange` slot 能力向下游表格 slot 透传。

## Milestones

- [x] 记录 BDD 与 RED 构建失败证据。
- [x] 最小修复 slot props 透传，不关闭 ESLint，不改变 API。
- [x] 运行聚焦验证与前端构建命令。
- [x] 提交本任务前端修复。

## Expected Verification

- `pnpm build:test` 不再因 `ProcessWipTable.vue` 的 `sortColumnAttrs` / `handleTemplateSortChange` 未使用而失败。
- 本任务不引入 fallback、mock、静默降级或临时绕过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，保持 slot props 正式透传。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed
