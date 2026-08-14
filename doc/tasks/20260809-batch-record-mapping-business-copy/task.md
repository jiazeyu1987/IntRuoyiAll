# 批记录映射页签业务化文案

## Task Goal

将“批记录测试”页面“批记录映射”页签中的列名、映射项标题和说明改为业务描述，去除字段名、状态码、程序组件、测试工具等实现细节，同时保持现有 15 个业务环节和测试执行能力不变。

## Milestones

- [x] 定位批记录映射页签及现有静态合同。
- [x] 先补充业务化文案静态合同并记录 RED。
- [x] 重写列名、标题、描述和测试范围业务口径。
- [x] 完成聚焦回归、类型检查和证据验证。
- [x] 完成任务清理与收尾。

## Expected Verification

- `node tests/e2e/edhr-batch-record-test-mapping-static.spec.cjs`
- `node tests/e2e/edhr-batch-record-test-tab-static.spec.cjs`
- `node tests/e2e/edhr-batch-record-test-description-wrap-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue tests/e2e/edhr-batch-record-test-mapping-static.spec.cjs tests/e2e/edhr-batch-record-test-description-wrap-static.spec.cjs ../doc/tasks/20260809-batch-record-mapping-business-copy ../docs/e2e-rules.md ../docs/experience-index.md`
- `python -X utf8 C:/Users/BJB110/.codex/skills/frontend-feature-delivery/scripts/validate_frontend_feature.py --evidence ../doc/tasks/20260809-batch-record-mapping-business-copy/frontend-feature-evidence.md`

## Current Status

completed

业务化文案、测试项名称和表头均已完成改写；聚焦合同、五项相邻回归、TypeScript、空白检查、证据校验和 cleanup preview/apply 全部通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接改写正式页签内容并用聚焦合同约束，不使用 CSS 隐藏或替换展示来源。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

### 用户可见描述与内部编码隔离门禁

- 用户可见标题和说明只表达业务条件、业务资料、业务责任和业务结果。
- 字段名、状态码、程序组件名、测试工具名只允许留在内部实现，不得出现在批记录映射页签的标题和说明中。
- 固定测试项名称同步业务化以隔离旧技术描述；测试项数量、排序、列表状态、测试入口和提交结构保持不变。

### 前端静态合同隔离门禁

- 聚焦抽取 `batchRecordMappingRows` 和页签模板，正向锁定业务文案，负向禁止程序细节。
- 使用明确相邻声明作为数据块结束边界，不扫描整个 Vue 文件造成相邻功能误报。

### Experience Gate

- `docs/experience-index.md` 已存在。
- 匹配 `docs/frontend-development.md#用户可见描述与内部编码隔离门禁`：展示内容直接使用正式业务描述，编码和内部身份仅保留在内部结构中。
- 匹配 `docs/e2e-rules.md#测试管理测试节点闭环门禁`：已补充业务页签固定测试项及持久化旧说明覆盖风险的通用规则，并更新经验索引。

## Cleanup Keep

- doc/tasks/20260809-batch-record-mapping-business-copy/task.md
- doc/tasks/20260809-batch-record-mapping-business-copy/execution-log.md
- doc/tasks/20260809-batch-record-mapping-business-copy/verification-report.md
