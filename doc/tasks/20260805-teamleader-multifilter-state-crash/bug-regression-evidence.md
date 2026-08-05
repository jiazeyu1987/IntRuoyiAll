# Bug Regression Evidence

## Bug Summary

`TeamLeaderWorkbenchPage.vue` 在模板读取 `submissionMultiFilter.state` 时，`submissionMultiFilter` 为 `undefined`，导致路由进入页面后渲染中断。

## Expected Behavior

报工列表首屏渲染前，多维筛选实例已经完成初始化，页面和筛选操作均可正常使用。

## Reproduction

- 用户浏览器堆栈：`TypeError: Cannot read properties of undefined (reading 'state')`。
- 源码位置：`TeamLeaderWorkbenchPage.vue` 的 `:multi-filter-state="submissionMultiFilter.state"`。

## Root Cause

- Vite 当前编译模块证明完整 setup 会创建并返回 `submissionMultiFilter`，hook 本身不会返回 `undefined`。
- 崩溃发生在模板热更新与旧组件 setup state 不同步的窗口：新 render 直接读取旧实例中不存在的 `submissionMultiFilter.state`。
- 模板应绑定 hook 解构后的稳定顶层 state/events，避免父组件 render 对包装对象做属性解引用。

## Regression Test

- `tests/e2e/team-leader-multifilter-render-state-static.spec.js`
- 合同要求模板绑定顶层 `submissionMultiFilterState`、`updateSubmissionMultiFilterState`、`removeSubmissionMultiFilterCondition`，并禁止 `submissionMultiFilter.state/updateState/removeCondition`。

## RED

- `node tests/e2e/team-leader-multifilter-render-state-static.spec.js` -> FAIL，命中“必须把 hook 状态和模板事件方法暴露为稳定顶层绑定”断言。

## GREEN

- 待执行。

## Risk And Scope

- 范围限定在班组长报工列表多维筛选实例初始化和模板绑定。
- 不修改后端接口、查询字段、权限、人员管理或其它模块业务逻辑。

## Blockers

- 暂无。
