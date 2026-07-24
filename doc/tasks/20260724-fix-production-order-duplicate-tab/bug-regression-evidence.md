# Bug Regression Evidence

## Bug

生产工单页面顶部标签页会出现重复的 `生产工单` 与 `生产工单 (2)`。

## Expected

同一个生产工单路由再次进入或仅 query 变化时，顶部标签页继续复用同一个 `生产工单` tab。

## Reproduction

`node tests/e2e/workorder-single-tags-view-static.spec.js`

## Root Cause

`TagsView` 已支持 `tagsViewKeyMode = 'path'`，但动态菜单生成逻辑没有把生产工单路由组件和历史菜单路径纳入 path 身份覆盖，导致默认使用 `fullPath`，query-only 变化会被当成不同 tab。

## RED

RED: `node tests/e2e/workorder-single-tags-view-static.spec.js` -> FAIL, `动态路由覆盖必须声明生产工单组件路径。`

## GREEN

GREEN: `node tests/e2e/workorder-single-tags-view-static.spec.js` -> PASS。

## Verification

- PASS: `node tests/e2e/workorder-single-tags-view-static.spec.js`
- PASS: `node tests/e2e/dcc-browser-single-tab-static.spec.js`
- PASS: `node tests/e2e/dcc-permission-single-tags-view-static.spec.js`
- BLOCKED: `pnpm ts:check` 被既有 `src/views/dcc/controlled-file/browser/index.vue` 类型错误阻塞，错误文件不属于本次改动范围。

## Blockers

全量 TypeScript 校验当前无法通过，阻塞点为既有 DCC 文件类型错误；本次生产工单 tabs 修复的目标静态回归已通过。
