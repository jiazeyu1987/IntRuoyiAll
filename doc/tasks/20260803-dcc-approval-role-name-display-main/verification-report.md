# Verification Report

## Summary

- 主工作区 `E:\IntRuoyi` 已同步 DCC 审批路线角色名称显示修复。
- 红框节点列现在优先从 POSITION `candidateSourceIds` 解析审批角色名称，并过滤权限编码和 `审批角色#ID` 技术标签。
- 节点2首屏为空的回归已修复：列表查询现在先加载审批角色/用户字典，再赋值审批路线数据。
- 本机真实页面只读 E2E 已确认节点2不再为空，且显示审批角色名称而不是编码、权限名或 `审批角色#ID`。
- 运行中的 8081 Vite 服务已直接返回新源码；如页面仍旧，需要刷新页面重新加载当前列表状态。

## Commands

- `node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js` -> RED before fix, then PASS after fix.
- `node tests/e2e/dcc-controlled-file-routes-node-columns-static.spec.js` -> PASS.
- `node tests/e2e/dcc-controlled-file-routes-list-display-static.spec.js` -> PASS.
- `node tests/e2e/dcc-route-summary-static.spec.js` -> PASS.
- `node tests/e2e/dcc-controlled-file-routes-standard-list-template-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- Inline Playwright readonly check against `http://127.0.0.1:8081/dcc/controlled-file/routes` using system Chrome -> PASS.

## Node 2 Evidence

- Read-only DB check: active route stage 2 nodes contain POSITION IDs `900834/900847/900859/900860/900844`.
- Read-only DB check: those IDs resolve to `编制人直接主管/QA/QMS/注册/文档管理员` in `dcc_approval_position`.
- Regression contract now asserts `handleQuery` calls `loadRouteSubjectLookups()` before `getApprovalRoutePage(queryParams)`.
- Real page check: visible node2 cells matched the official names from `/admin-api/dcc/approval-positions`, including `编制人直接主管、QA、QMS、注册、文档管理员` and `编制人直接主管、QA、QMS`.
- Real page check: route total `64`, visible rows `20`, DCC write requests `0`, page errors `0`; node2 did not show `审批角色#ID`, `doc-control`, or `matrix-review`.

## Runtime Source

- `http://127.0.0.1:8081/src/views/dcc/controlled-file/shared/utils.ts` contains `[900332, "文控"]`.
- `http://127.0.0.1:8081/src/views/dcc/controlled-file/routes/index.vue` contains `activePositions`, `TECHNICAL_ROUTE_NODE_LABEL_PATTERN`, `resolveRouteNodePositionNames`, and `positions.value = positionList`.
- `http://127.0.0.1:8081/src/views/dcc/controlled-file/routes/index.vue` contains `await loadRouteSubjectLookups()` before `getApprovalRoutePage(queryParams)` inside `handleQuery`.

## Closeout Blocker

- Local implementation is verified, but formal commit/push is blocked by unrelated existing dirty changes in `E:\IntRuoyi` and unavailable GitHub HTTPS 443 connectivity.
