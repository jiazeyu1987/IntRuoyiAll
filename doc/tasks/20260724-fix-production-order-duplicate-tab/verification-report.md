# Verification Report

## Summary

生产工单重复 tab 修复已通过目标静态回归和相邻 tagsView 去重回归。全量 TypeScript 校验被既有 DCC 浏览器页面类型错误阻塞，未指向本次改动文件。

## Commands

- RED: `node tests/e2e/workorder-single-tags-view-static.spec.js` -> FAIL, `动态路由覆盖必须声明生产工单组件路径。`
- GREEN: `node tests/e2e/workorder-single-tags-view-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/dcc-browser-single-tab-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/dcc-permission-single-tags-view-static.spec.js` -> PASS。
- BLOCKED: `pnpm ts:check` -> FAIL, existing errors in `src/views/dcc/controlled-file/browser/index.vue`.

## Result

target_verified_with_global_tscheck_blocked
