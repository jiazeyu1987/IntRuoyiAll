# 班组长工作台多维筛选状态渲染崩溃修复

## Task Goal

修复 `TeamLeaderWorkbenchPage.vue` 渲染报错 `Cannot read properties of undefined (reading 'state')`，确保报工列表的多维筛选状态在首屏渲染时始终可用。

## Milestones

- [x] M1: 定位报错模板表达式和 `useTableMultiFilter` 返回契约。
- [x] M2: 编写可稳定复现首屏 `state` 访问崩溃的 RED 回归合同。
- [x] M3: 实施最小根因修复，不引入 fallback 或吞异常。
- [x] M4: 运行目标回归、相邻静态合同和 TypeScript 检查。
- [ ] M5: 完成验证报告、cleanup、提交并推送。

## Expected Verification

- 任务专用静态/运行时回归合同先 RED 后 GREEN。
- `node tests/e2e/production-personnel-management-static.spec.cjs` 通过。
- `node tests/e2e/mes-process-pool-team-leader-static.spec.js` 通过。
- `pnpm ts:check` 通过。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修正多维筛选实例的正式初始化与模板绑定。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

### 统一列表多维筛选接入门禁

- Trigger: 修改 `UnifiedListTemplate`、标准列表多维筛选、`TableMultiFilter` 或业务列表筛选接入。
- Preflight check: 在真实业务列表逐项核对 `showMultiFilter`、definitions、state、events 和正式 query 参数透传；首屏条件默认保持为空。
- Blocker: 模板绑定缺少正式 state/events、渲染阶段直接访问未稳定暴露的筛选成员、或首屏携带隐藏默认条件时停止。
- Verification: 聚焦静态合同覆盖 props/events、默认空条件和正式 query 参数；相邻标准列表合同与 `pnpm ts:check` 通过。
- Forbidden action: 禁止用可选链、默认空包装对象、旧快速筛选或页面隐藏条件掩盖初始化问题。
- Evidence: `docs/frontend-development.md#统一列表复合工具栏布局门禁`。

### Vue Composable 模板顶层绑定门禁

- Trigger: 新 render 直接访问 `hook.state` 等包装对象成员，HMR 后出现父组件 render TypeError。
- Preflight check: 核对 Vite 编译模块中的 setup 返回值和 render 绑定；将模板所需 hook 成员解构为顶层 binding。
- Blocker: 仍使用 `hook.state/updateState/removeCondition` 或准备用可选链、空对象掩盖问题时停止。
- Verification: 静态合同禁止包装对象属性访问，Vite 编译模块只包含 `$setup.<topLevelBinding>`，相邻合同和 `pnpm ts:check` 通过。
- Forbidden action: 禁止可选链、默认空 state、整页刷新提示或吞 render 异常。
- Evidence: `docs/frontend-development.md#vue-composable-模板顶层绑定门禁`。

## Cleanup Keep

- `IntRuoyiFronted/tests/e2e/team-leader-multifilter-render-state-static.spec.js`
