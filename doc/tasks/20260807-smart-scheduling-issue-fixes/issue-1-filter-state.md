# Issue 1: 筛选草稿与已执行结果状态一致性

## Feature Goal

- 智能排产“同步工单”保持“编辑后点击查询才应用”的正式交互。
- 条件 Tab 展示新草稿、删除条件或重置未成功时，必须明确显示“筛选条件待应用”，不得让草稿标签冒充当前列表结果口径。
- 只有列表重载成功后才更新“已执行条件”快照；重载失败必须回滚本次尚未生效的 query 参数并原样抛出异常。
- 同步工单首屏继续遵守 `20260805-standard-list-empty-tabs` 已确立的空条件契约。

## Non-goals

- 不修改入池状态枚举值、后端接口、入池判定或分页逻辑。
- 不恢复页面级默认“可入池”隐藏条件。
- 不启动或操作本地/远程服务，不使用 mock 结果代替真实列表。

## Bug Summary

- 普通下拉编辑会立即更改条件 Tab 标签，但列表仍保持上一次查询结果；页面没有任何待应用状态，导致“标签是阻断，列表实际是可入池”。

## Expected Behavior

- 草稿条件可继续编辑，但在点击查询并成功加载前必须明确标记待应用；已执行快照和后续分页 query 保持上一次成功口径。

## Reproduction

- 在旧实现上运行 `node tests\e2e\unified-list-template-multi-filter-static.spec.js`，“草稿/已执行状态”聚焦契约稳定失败，首个失败为缺少“筛选条件待应用”。

## Acceptance And BDD

- AC1：标准筛选状态同时持有当前草稿与最后一次成功查询快照。
- AC2：两者不同时，共享 `TableMultiFilter` 显示“筛选条件待应用”。
- AC3：列表重载成功后更新已执行快照；失败时保留原快照、回滚 query 参数并抛出原异常。
- AC4：同步工单首屏 `admissionStatus` 为空，不存在 `DEFAULT_WORK_ORDER_ADMISSION_STATUS` 或页面级 `setCondition(...)` 预置。

- BDD: 编辑入池状态但未查询 -> Given 列表上一次成功查询为“可入池” / When 用户把条件改为“阻断”但未点击查询 / Then Tab 可显示当前草稿，同时必须显示“筛选条件待应用”，已执行快照仍为“可入池”。
- BDD: 查询成功后更新结果口径 -> Given 当前草稿为“阻断” / When 用户点击查询且列表重载成功 / Then 正式请求提交 `admissionStatus=BLOCKED`，已执行快照更新为“阻断”，待应用提示消失。
- BDD: 查询失败不冒充已执行 -> Given 已成功应用“可入池”且草稿改为“阻断” / When 阻断查询重载失败 / Then 已执行快照和后续分页参数仍为 `READY_TO_ADMIT`，页面保持待应用提示，原异常向上抛出。
- BDD: 同步工单首屏空条件 -> Given 用户首次进入同步工单 / When 页面加载列表 / Then 标准条件 Tab 为空且 `admissionStatus` 为空，不恢复旧的默认“可入池”隐藏条件。

## Root Cause

- `TableMultiFilter` 直接使用可编辑 `state.conditions` 生成 Tab 标签，但 `useTableMultiFilter` 只在点击查询时才把条件写入正式 query 参数。
- 原状态模型没有“最后一次成功查询”快照，因此组件无法判断新 Tab 文案是草稿还是当前结果口径。
- 查询在 `reload()` 前会就地修改 query 参数；若重载失败而不回滚，后续分页会带上未成功应用的草稿参数。
- `mes-schedule-order-sync-tab-static.spec.js` 仍保留 `20260804-standard-list-multi-filter` 时期“首屏默认可入池”的旧断言，与 `20260805-standard-list-empty-tabs` 正式空条件契约冲突。

## TDD Evidence

- RED: `node tests\e2e\unified-list-template-multi-filter-static.spec.js` -> FAIL，预期原因：组件不存在 `hasUnappliedChanges` / `table-multi-filter__pending-status` / `筛选条件待应用`，状态模型也没有 `appliedConditions`。
- RED: `node tests\e2e\unified-list-template-multi-filter-static.spec.js` -> FAIL，预期原因：列表重载失败时未回滚已就地修改的正式 query 参数。
- GREEN: `node tests\e2e\unified-list-template-multi-filter-static.spec.js` -> PASS: `unified list template multi-filter static and state contract`。运行时契约同时验证未查询、成功查询、失败查询回滚、从“可入池”编辑为“阻断”和成功重置。
- GREEN: `node tests\e2e\mes-schedule-order-sync-tab-static.spec.js` -> PASS: `MES schedule order sync tab static contract`。过期默认条件断言已替换为首屏空 `admissionStatus` 和禁止页面级 `setCondition(...)` 断言。
- GREEN: `node -e "<TypeScript transpile check>"` -> PASS: `multi-filter hook TypeScript transpile`，覆盖 `useTableMultiFilter.ts` 和 `useTableQuickFilter.ts`。

## Verification And Regression Evidence

- `node tests\e2e\unified-list-template-static.spec.js` -> PASS。
- `node tests\e2e\mes-schedule-order-replan-visible-filter-static.spec.js` -> PASS（脚本退出码 `0`，无额外输出）。
- `node tests\e2e\pqc-leader-standard-list-template-static.spec.js` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260807-smart-scheduling-issue-fixes\issue-1-filter-state.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260807-smart-scheduling-issue-fixes\issue-1-filter-state.md` -> PASS。
- `git diff --check -- <6 个本问题文件>` -> PASS；只有 Git 的 LF/CRLF 工作区提示，无空白错误。
- `pnpm ts:check:schedule` -> FAIL，失败仅在并发任务正在修改的 `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:1233,1379`：检查运行时报 `openAbnormalDialog` 和 `resetAbnormalForm` 不存在。该文件当时为 `581/277` 行的并发大幅脏改动，不属于本问题；本问题四个生产文件的聚焦静态/运行时契约和 TypeScript transpile 均通过。
- `node tests\e2e\schedule-order-main-multi-filter-static.spec.js` -> FAIL，剩余失败为既存过期断言：仍要求 `scheduleOrderMultiFilter.setCondition({ id: 'completionFilter', ... })`。页面已遵守 `20260805-standard-list-empty-tabs` 空条件契约，本问题不扩大范围修改该相邻合同。
- 独立 Vue SFC 编译命令 -> BLOCKED：项目未直接提供 `@vue/compiler-sfc` 模块，命令明确返回 `MODULE_NOT_FOUND`；未切换其它工具或伪造通过。

## Changed Files

- `IntRuoyiFronted/src/hooks/web/useTableMultiFilter.ts`
- `IntRuoyiFronted/src/hooks/web/useTableQuickFilter.ts`
- `IntRuoyiFronted/src/components/TableMultiFilter/index.vue`
- `IntRuoyiFronted/src/components/UnifiedListTemplate/index.vue`
- `IntRuoyiFronted/tests/e2e/unified-list-template-multi-filter-static.spec.js`
- `IntRuoyiFronted/tests/e2e/mes-schedule-order-sync-tab-static.spec.js`

## Implementation Summary

- `ListMultiFilterState` 新增 `appliedConditions`，将当前可编辑草稿与最后一次成功查询快照分开。
- 共享组件按正规化后条件比较草稿/已执行快照，存在差异时显示明确待应用标签。
- `useTableMultiFilter` 和标准快速筛选桥接都只在 `reload()` 成功后提交已执行快照。
- 正式 query 参数在查询/重置前保留管理范围快照；`reload()` 失败时通过 `try/finally` 回滚，不吞异常、不返回默认成功。

## UI, API And State Checks

- UI entry: `MES > 智能排产 > 同步工单`，共享组件为 `TableMultiFilter`。
- API contract: 不变更后端契约，入池状态继续映射正式 `admissionStatus`。
- Loading/error: 已执行快照只在重载成功后更新；失败保持待应用提示、回滚 query 参数并抛出原异常。
- Empty: 首屏和成功重置的草稿/已执行条件均为空。
- Permission: 本修复不改变权限或操作入口。
- Responsive: 待应用标签使用稳定的 `flex: 0 0 auto`，既有 Tab 容器继续 `min-width: 0` 并可收缩，不改变条件行布局。
- Accessibility: 待应用状态使用可见文字而非仅靠颜色；既有增删按钮的 `aria-label` 保持不变。

## Design Constraints Check

- 是否引入 fallback/降级/吞异常：否。失败路径回滚未成功应用的参数后原样抛出异常。
- 是否从根因和长期维护角度解决：是。草稿/已执行是共享标准列表状态契约，不是同步工单页面特例。
- 是否存在临时补丁或绕过：否。

## Remaining Risks And Blockers

- 本子问题按任务分工未启动或操作远程测试服务，因此真实页面的请求计数、待应用文案和 1499/126/1332 数据结果需由总任务在已登录测试账号下做最终 Playwright 复验。
- 全量 `ts:check:schedule` 被并发任务修改中的 `TeamLeaderWorkbenchPage.vue` 阻断；总任务应在该并发修复稳定后重跑。
- 排产主列表的过期默认 `completionFilter` 合同不属于本子问题；若总任务要求相邻回归全绿，应按同一 `20260805-standard-list-empty-tabs` 正式契约单独更新。
