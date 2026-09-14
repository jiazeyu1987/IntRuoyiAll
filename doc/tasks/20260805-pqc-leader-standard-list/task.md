# 20260805-pqc-leader-standard-list

## Task Goal

将 `PQC组长 > PQC管理` 中黄框范围内的手写搜索表单、表格和分页迁移到标准列表模板，并让搜索使用标准列表的多条件筛选能力，同时保留现有 PQC 提交分页接口参数、列表字段和复核操作。

本轮追加：按用户截图将筛选区域放在左侧黄框位置，将“显示字段”放在右侧红框位置，桌面端保持同一行。

## Milestones

- [x] M1 记录 BDD 场景和标准列表门禁，新增聚焦静态合同并跑出 RED。
- [x] M2 扩展标准筛选组件对单日期条件的正式支持，避免把提交日期降级成普通文本。
- [x] M3 将 PQC 管理列表迁移到 `UnifiedListTemplate`，接入多条件筛选、列配置和标准分页。
- [x] M4 运行聚焦静态合同、相邻标准列表合同和 TypeScript 检查，记录 GREEN/REGRESSION 证据。
- [x] M5 增加标准模板单行筛选工具栏能力，并仅在 PQC 管理提交列表启用。
- [x] M6 运行布局聚焦合同、相邻多条件合同、TypeScript 检查和真实页面响应式验证，归档本轮验证。

## Expected Verification

- `node tests/e2e/pqc-leader-standard-list-template-static.spec.js`
- `node tests/e2e/unified-list-template-multi-filter-static.spec.js`
- `node tests/e2e/table-quick-filter-static.spec.js`
- `pnpm ts:check`

## Additional Compatibility Verification

- `node tests/e2e/team-leader-multifilter-render-state-static.spec.js`

## Applicable Gates

- 前端统一列表复合工具栏布局门禁：标准列表必须接入 `UnifiedListTemplate` / `TableMultiFilter`，不能保留旧 quick/manual form 冒充完成；筛选控件必须可见、透传正式 query 参数，不得发送临时参数或隐藏默认业务筛选。
- 标准条件 Tab 默认空状态门禁：首屏和重置后不预置提交日期、模板类型或其它隐藏业务筛选；提交日期由用户在标准多条件搜索中显式填写，并在查询前 fail fast 校验。
- E2E 静态合同门禁：本任务新增聚焦静态合同，只能声明静态合同 PASS；未启动真实前后端和 Playwright 登录路径时，不把静态合同写成真实 E2E PASS。
- PowerShell/UTF-8 门禁：中文任务文档和测试断言使用 UTF-8，写入优先使用 `apply_patch`。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用标准列表模板和通用筛选能力，不在页面内维护第二套搜索表单。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

- 目标列表已迁移到 `UnifiedListTemplate`，接入标准多条件搜索、用户列配置和标准分页。
- 单日期字段已加入通用 `TableMultiFilter` / `TableQuickFilter` 正式类型和控件支持。
- `UnifiedListTemplate` 已增加显式 `singleLineToolbar` 开关，仅 PQC 管理提交列表启用。
- 4 项聚焦/相邻静态合同、TypeScript 检查和真实 Playwright 布局验证全部通过，验证结果见 `verification-report.md`。
- 桌面视口 `1680x960` 下筛选区与“显示字段”顶部坐标均为 `222px`，保持同一行且无重叠；窄屏 `1100x900` 下自动换行且控件保持可见。
- 用户已允许基于并发任务的最新 `TeamLeaderWorkbenchPage.vue` 继续合并；新的多条件状态解构接线已保留并通过兼容合同。
- 当前轮次的 frontend evidence 校验器、自测和 task-closeout-cleanup preview/apply 均已通过；临时 evidence 与浏览器产物已清理，核心三份任务记录保留。
- Git 收尾尚未完成：`int_main` 当前存在本地未推送提交，且共享工作区包含大量其它任务的未暂存/未跟踪改动；无法安全形成仅属于本任务的实现与收尾提交。

## Cleanup Candidates

- output/playwright/20260805-pqc-leader-standard-list/
