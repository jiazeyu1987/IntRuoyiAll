# 隐藏工艺路线 MES 工序页签验证报告

## Result

- Status: ready_for_closeout
- Scope: 前端工艺路线详情/编辑内容页隐藏 `MES 工序` tab，并拒绝旧 `?tab=mesProcess` 入口。
- No fallback: 未引入降级、默认成功、吞异常或 CSS 遮挡；直接移除 tab 渲染和组件挂载。

## Verification

- RED: `node tests/e2e/mes-route-mes-process-tab-static.spec.js` -> FAIL，旧代码仍懒加载 `RouteMesProcessList`。
- GREEN: `node tests/e2e/mes-route-mes-process-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-entry-readonly-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS。

## Residual Blockers

- `node tests/e2e/mes-route-product-standard-list-static.spec.js` -> FAIL，测试仍要求旧的 `@request-submit="submitForm"`，当前实现已经是非本任务引入的 `handleSubmitRequest`。
- `node tests/e2e/mes-route-resource-tab-static.spec.js` -> FAIL，测试引用不存在的 `src/views/mes/pro/route/RouteProcessList.vue`。
- 主工作区仍存在并发任务修改的非本任务文件，本任务提交需选择性暂存。
