# DCC 受控浏览隐藏筛选条件提示区

## Task Goal

隐藏 DCC 受控浏览列表顶部截图红框中的“当前筛选条件”提示区，保留快速过滤、查询、当前目录/全域切换、高级筛选、显示字段、列表数据和行操作能力。

## Milestones

- [x] 创建任务记录并确认适用前端门禁。
- [x] 以静态契约先记录红框提示区不应渲染的 RED 预期。
- [x] 最小修改受控浏览页面模板，移除该提示区渲染。
- [x] 运行目标静态契约、相邻静态契约和类型检查。
- [x] 更新验证报告与收尾状态。

## Expected Verification

- `node tests/e2e/dcc-controlled-browser-ux-optimization-static.spec.js`
- `node tests/e2e/dcc-browser-unified-list-template-static.spec.js`
- `pnpm ts:check`

## Current Status

completed

实现、验证、cleanup preview/apply 和本任务实现提交已完成；本任务实现提交为 `e92e53bbe`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除目标提示区 DOM，不用 CSS 透明/遮挡伪装。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端列表状态口径完整性门禁：本任务只隐藏顶部筛选条件说明区，不改变当前有效版查询口径、状态过滤集合或行操作逻辑。
- DCC 受控浏览当前有效版与权限隔离门禁：保留受控浏览列表、当前有效版行操作和权限隔离，不引入 API-only 验收替代。
