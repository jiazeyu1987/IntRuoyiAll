# 隐藏工艺路线 MES 工序页签

## Task Goal

根据截图要求，在 MES 工艺路线详情/编辑内容页中不再显示黄框标出的 `MES 工序` 页签，并确保旧的 `?tab=mesProcess` 入口不会继续激活隐藏内容或挂载 MES 工序映射组件。

## Milestones

- [x] 建立聚焦静态合同，先证明当前 `MES 工序` 页签仍存在且旧 query 仍被接受。
- [x] 从工艺路线内容组件移除 `MES 工序` 页签及其异步组件挂载。
- [x] 同步编辑页和列表页的合法页签白名单，仅保留 `basic`、`flow`、`product`。
- [x] 运行目标静态合同和相邻工艺路线页签合同，记录 RED/GREEN 证据。
- [x] 收尾前记录验证报告、经验门禁和并发基线影响。

## Expected Verification

- `node tests/e2e/mes-route-mes-process-tab-static.spec.js`
- 相邻工艺路线页签静态合同按必要范围更新并运行。
- 如 Vue/TS 类型受影响，运行 `pnpm ts:check`；若被并发脏工作区或既有环境阻塞，记录精确 blocker。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本任务直接移除页签入口，不新增降级或默认成功分支。
- `是否从根因和长期维护角度解决`：是；同步组件渲染、类型白名单和 query 入口，避免隐藏页签仍被旧链接激活。
- `是否存在临时补丁或绕过`：否。

## Verification Summary

- RED: `node tests/e2e/mes-route-mes-process-tab-static.spec.js` -> FAIL，旧实现仍懒加载 `RouteMesProcessList` 并渲染 `MES 工序` tab。
- GREEN: `node tests/e2e/mes-route-mes-process-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-entry-readonly-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- BLOCKER: `node tests/e2e/mes-route-product-standard-list-static.spec.js` 仍断言旧 `@request-submit="submitForm"`，但当前实现使用既有 `handleSubmitRequest`；非本任务改动。
- BLOCKER: `node tests/e2e/mes-route-resource-tab-static.spec.js` 引用不存在的 `RouteProcessList.vue`；非本任务改动。

## 经验门禁

- 命中 `docs/frontend-development.md#前端权限页签正向授权门禁`：先明确允许页签集合，本任务允许 `basic`、`flow`、`product`，禁止只隐藏一个截图页签但仍让组件 mount 或旧 query 进入无权限/隐藏页签。
- 命中 `docs/powershell-memory.md#共享分支并发基线提交门禁` 与 `#同文件并行改动选择性暂存门禁`：当前主工作区存在并发任务提交和残余脏改动，本任务提交必须只暂存当前任务文件，不得混入并发任务文档或 DCC 上传文件改动。
