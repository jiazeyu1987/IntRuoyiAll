# 20260627 前端发布重定向页空模板构建阻塞

BDD: 重定向壳页面可编译 -> Given DCC 与 eDHR 的遗留审批入口页只负责 mounted 后跳转到统一审批中心 / When 前端执行静态契约检查与 test 模式构建 / Then 页面仍保留原有 redirect 行为，且模板必须提供合法根节点，不得再因空模板阻塞发布构建。

- 根因摘要：`src/views/dcc/controlled-file/approval-tasks/index.vue` 与 `src/views/mes/pro/edhr/ApprovalPage.vue` 使用空模板 `<template></template>` 作为重定向壳页面；在发布构建时会被 `vite-plugin-eslint` 按 `vue/valid-template-root` 拦截。
- 预期行为：重定向壳页面必须保留原有 `router.replace(...)` 跳转契约，同时提供合法模板根节点，确保静态检查与 `vite build --mode test` 能通过。

- RED: `node node_modules/vite/bin/vite.js build --mode test` -> FAIL, `src/views/dcc/controlled-file/approval-tasks/index.vue` 触发 `vue/valid-template-root`
- RED: `node tests/e2e/redirect-template-root-static.spec.js` -> FAIL, `src/views/dcc/controlled-file/approval-tasks/index.vue` 仍使用空模板根节点
- GREEN: `node tests/e2e/redirect-template-root-static.spec.js` -> PASS
- GREEN: `node tests/e2e/approval-center-phase5-retirement-static.spec.mjs` -> PASS
- GREEN: `pnpm build:test` -> PASS，`Build successful. Please see dist-test directory`
- 修复说明：为 DCC 与 eDHR 的遗留审批重定向页补充最小合法根节点 `<div aria-hidden="true" ...></div>`，保持 mounted 后跳转统一审批中心的既有行为不变；同时新增静态契约，阻止空模板再次进入发布链路。
