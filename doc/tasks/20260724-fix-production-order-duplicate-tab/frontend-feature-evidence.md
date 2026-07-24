# Frontend Feature Evidence

## Feature

生产工单顶部标签页按路由 path 去重，同一路由不同 query 不再生成 `生产工单 (2)`。

## Acceptance

- `生产工单` 页面再次进入时只保留一个顶部 tab。
- 修复位于动态路由元信息生成层，不改页面 UI、不新增 fallback、不吞异常。
- 相邻 DCC tabs 去重契约保持通过。

## BDD

BDD: production order tab de-duplication -> Given 用户已经打开 `生产工单` 页面 When 再次进入同一个生产工单路由 Then 顶部页签继续复用原有 `生产工单` tab 且不会新增 `生产工单 (2)`。

## RED

RED: `node tests/e2e/workorder-single-tags-view-static.spec.js` -> FAIL, `动态路由覆盖必须声明生产工单组件路径。`

## GREEN

GREEN: `node tests/e2e/workorder-single-tags-view-static.spec.js` -> PASS。

## Verification

- PASS: `node tests/e2e/workorder-single-tags-view-static.spec.js`
- PASS: `node tests/e2e/dcc-browser-single-tab-static.spec.js`
- PASS: `node tests/e2e/dcc-permission-single-tags-view-static.spec.js`
- BLOCKED: `pnpm ts:check` 被既有 DCC 浏览器类型错误阻塞，非本次生产工单路由改动引入。

## Blockers

全量 TypeScript 校验仍受既有 DCC 类型错误阻塞；未执行真实浏览器 E2E，因为本次修复为动态路由静态契约且不启动本地服务。
